package com.messenger.worklog.service;

import com.messenger.worklog.dto.WorklogCommitItem;
import com.messenger.worklog.dto.WorklogSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorklogService {

    @Value("${app.worklog.owner:kimsan259}")
    private String fallbackOwner;

    @Value("${app.worklog.repo:collaborative-messenger}")
    private String fallbackRepo;

    @Value("${app.worklog.default-author:kimsan4515}")
    private String defaultAuthor;

    @Value("${app.worklog.github-token:}")
    private String githubToken;

    @Value("${app.worklog.github-api-base-url:https://api.github.com}")
    private String githubApiBaseUrl;

    @Value("${app.worklog.max-pages:6}")
    private int maxPages;

    @Value("${app.worklog.summary-cache-ttl-seconds:45}")
    private long summaryCacheTtlSeconds;

    @Value("${app.worklog.branch-cache-ttl-seconds:300}")
    private long branchCacheTtlSeconds;

    @Value("${app.worklog.commit-detail-cache-ttl-seconds:120}")
    private long commitDetailCacheTtlSeconds;

    @Value("${app.worklog.recent-window-days:2}")
    private long recentWindowDays;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ConcurrentMap<String, TimedValue<WorklogSummaryResponse>> summaryCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TimedValue<Map<String, Object>>> commitDetailCache = new ConcurrentHashMap<>();
    private volatile TimedValue<List<String>> branchCache;

    public String getDefaultAuthor() {
        return defaultAuthor;
    }

    public WorklogSummaryResponse generateTodaySummary(String authorOrNull) {
        String requestedAuthor = (authorOrNull == null || authorOrNull.isBlank())
                ? defaultAuthor
                : authorOrNull.trim();
        List<String> authorCandidates = parseAuthorCandidates(requestedAuthor, defaultAuthor);

        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zone);
        OffsetDateTime since = today.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime until = OffsetDateTime.now(zone);
        String summaryCacheKey = buildSummaryCacheKey(today, authorCandidates);
        WorklogSummaryResponse cached = getCachedSummary(summaryCacheKey);
        if (cached != null) {
            return cached;
        }

        boolean fallbackToRecentWindow = false;
        boolean fallbackToRepoWide = false;

        // 1차: GitHub Search API로 작성자의 모든 저장소에서 커밋 조회
        List<Map<String, Object>> commits = searchCommitsByAuthors(authorCandidates, since, until);

        // 2차: 오늘 커밋이 없으면 최근 N일로 확장
        if (commits.isEmpty()) {
            OffsetDateTime recentSince = since.minusDays(Math.max(1, recentWindowDays));
            commits = searchCommitsByAuthors(authorCandidates, recentSince, until);
            fallbackToRecentWindow = !commits.isEmpty();
        }

        // 3차: Search API 실패 시 기존 fallback (설정된 저장소에서 조회)
        if (commits.isEmpty()) {
            commits = fetchCommitsByAuthors(authorCandidates, since, until);
        }
        if (commits.isEmpty()) {
            OffsetDateTime recentSince = since.minusDays(Math.max(1, recentWindowDays));
            commits = fetchCommitsByAuthors(authorCandidates, recentSince, until);
            fallbackToRecentWindow = !commits.isEmpty();
        }

        commits = dedupeCommitMapsBySha(commits);

        // 실제 커밋에서 저장소 정보 추출
        String resolvedOwner = fallbackOwner;
        String resolvedRepo = fallbackRepo;
        if (!commits.isEmpty()) {
            Map<String, Object> firstCommit = commits.get(0);
            Map<String, Object> repository = map(firstCommit.get("repository"));
            String fullName = string(repository.get("full_name"));
            if (!fullName.isBlank() && fullName.contains("/")) {
                String[] parts = fullName.split("/", 2);
                resolvedOwner = parts[0];
                resolvedRepo = parts[1];
            }
        }

        WorklogSummaryResponse summary = buildSummary(
                String.join(",", authorCandidates),
                today,
                commits,
                resolvedOwner,
                resolvedRepo,
                fallbackToRepoWide,
                fallbackToRecentWindow,
                false
        );
        putCachedSummary(summaryCacheKey, summary);
        return summary;
    }

    static List<String> parseAuthorCandidates(String rawInput, String fallbackAuthor) {
        String source = rawInput == null ? "" : rawInput;
        String fallback = normalizeAuthorToken(fallbackAuthor);

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String token : source.split("[,;\\s]+")) {
            String candidate = normalizeAuthorToken(token);
            if (!candidate.isBlank()) {
                normalized.add(candidate);
            }
        }

        if (normalized.isEmpty() && !fallback.isBlank()) {
            normalized.add(fallback);
        }
        return new ArrayList<>(normalized);
    }

    static String extractNextLink(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) {
            return null;
        }

        String[] sections = linkHeader.split(",");
        for (String section : sections) {
            String part = section.trim();
            if (!part.contains("rel=\"next\"")) {
                continue;
            }
            int start = part.indexOf('<');
            int end = part.indexOf('>');
            if (start >= 0 && end > start) {
                return part.substring(start + 1, end).trim();
            }
        }
        return null;
    }

    private String buildSummaryCacheKey(LocalDate date, List<String> authorCandidates) {
        return "search:" + date + ":" + String.join("|", authorCandidates);
    }

    private WorklogSummaryResponse getCachedSummary(String key) {
        TimedValue<WorklogSummaryResponse> cached = summaryCache.get(key);
        if (cached == null) {
            return null;
        }
        if (cached.isExpired()) {
            summaryCache.remove(key);
            return null;
        }
        return cached.value();
    }

    private void putCachedSummary(String key, WorklogSummaryResponse summary) {
        summaryCache.put(key, TimedValue.of(summary, summaryCacheTtlSeconds));
        trimCache(summaryCache, 256);
    }

    private List<Map<String, Object>> searchCommitsByAuthors(
            List<String> authorCandidates,
            OffsetDateTime since,
            OffsetDateTime until
    ) {
        List<Map<String, Object>> merged = new ArrayList<>();
        for (String author : authorCandidates) {
            merged.addAll(searchCommitsByAuthor(author, since, until));
        }
        return dedupeCommitMapsBySha(merged);
    }

    private List<Map<String, Object>> searchCommitsByAuthor(String author, OffsetDateTime since, OffsetDateTime until) {
        String sinceDate = since.toLocalDate().toString();
        String untilDate = until.toLocalDate().toString();
        String query = "author:" + author + " author-date:" + sinceDate + ".." + untilDate;

        List<Map<String, Object>> allItems = new ArrayList<>();
        int page = 1;

        while (page <= Math.max(1, maxPages)) {
            String url = UriComponentsBuilder
                    .fromUriString(githubApiBaseUrl + "/search/commits")
                    .queryParam("q", query)
                    .queryParam("sort", "author-date")
                    .queryParam("order", "desc")
                    .queryParam("per_page", 100)
                    .queryParam("page", page)
                    .build()
                    .toUriString();
            try {
                HttpEntity<Void> request = new HttpEntity<>(defaultHeaders());
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
                Map<String, Object> body = response.getBody();
                if (body == null) break;

                List<Map<String, Object>> items = listOfMap(body.get("items"));
                if (items.isEmpty()) break;

                allItems.addAll(items);

                int totalCount = intValue(body.get("total_count"));
                if (allItems.size() >= totalCount) break;
                page++;
            } catch (Exception e) {
                log.warn("[worklog] search commits failed. author={}, reason={}", author, e.getMessage());
                break;
            }
        }

        return allItems;
    }

    private List<Map<String, Object>> fetchCommitsByAuthors(
            List<String> authorCandidates,
            OffsetDateTime since,
            OffsetDateTime until
    ) {
        List<Map<String, Object>> merged = new ArrayList<>();
        for (String author : authorCandidates) {
            merged.addAll(fetchCommitsByAuthor(author, since, until));
        }
        return dedupeCommitMapsBySha(merged);
    }

    private List<Map<String, Object>> fetchCommitsByAuthorsAcrossBranches(
            List<String> authorCandidates,
            OffsetDateTime since,
            OffsetDateTime until
    ) {
        List<Map<String, Object>> merged = new ArrayList<>();
        for (String author : authorCandidates) {
            merged.addAll(fetchCommitsByAuthorAcrossBranches(author, since, until));
        }
        return dedupeCommitMapsBySha(merged);
    }

    private List<Map<String, Object>> fetchCommitsByAuthor(String author, OffsetDateTime since, OffsetDateTime until) {
        String url = UriComponentsBuilder
                .fromUriString(githubApiBaseUrl + "/repos/{owner}/{repo}/commits")
                .queryParam("author", author)
                .queryParam("since", since.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .queryParam("until", until.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .queryParam("per_page", 100)
                .buildAndExpand(fallbackOwner, fallbackRepo)
                .toUriString();
        return fetchCommitList(url);
    }

    private List<Map<String, Object>> fetchCommitsByAuthorAcrossBranches(String author, OffsetDateTime since, OffsetDateTime until) {
        List<String> branches = fetchBranchNames();
        List<Map<String, Object>> merged = new ArrayList<>();
        for (String branch : branches) {
            String url = UriComponentsBuilder
                    .fromUriString(githubApiBaseUrl + "/repos/{owner}/{repo}/commits")
                    .queryParam("sha", branch)
                    .queryParam("author", author)
                    .queryParam("since", since.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    .queryParam("until", until.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    .queryParam("per_page", 100)
                    .buildAndExpand(fallbackOwner, fallbackRepo)
                    .toUriString();
            merged.addAll(fetchCommitList(url));
        }
        return dedupeCommitMapsBySha(merged);
    }

    private List<Map<String, Object>> fetchCommitsAcrossBranches(OffsetDateTime since, OffsetDateTime until) {
        List<String> branches = fetchBranchNames();
        List<Map<String, Object>> merged = new ArrayList<>();
        for (String branch : branches) {
            String url = UriComponentsBuilder
                    .fromUriString(githubApiBaseUrl + "/repos/{owner}/{repo}/commits")
                    .queryParam("sha", branch)
                    .queryParam("since", since.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    .queryParam("until", until.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    .queryParam("per_page", 100)
                    .buildAndExpand(fallbackOwner, fallbackRepo)
                    .toUriString();
            merged.addAll(fetchCommitList(url));
        }
        return dedupeCommitMapsBySha(merged);
    }

    private List<String> fetchBranchNames() {
        TimedValue<List<String>> cached = branchCache;
        if (cached != null && !cached.isExpired() && cached.value() != null && !cached.value().isEmpty()) {
            return cached.value();
        }

        String startUrl = UriComponentsBuilder
                .fromUriString(githubApiBaseUrl + "/repos/{owner}/{repo}/branches")
                .queryParam("per_page", 100)
                .buildAndExpand(fallbackOwner, fallbackRepo)
                .toUriString();

        List<Map<String, Object>> branchRows = fetchPagedMapList(startUrl, "branches");
        List<String> names = new ArrayList<>();
        for (Map<String, Object> row : branchRows) {
            String name = string(row.get("name"));
            if (!name.isBlank()) {
                names.add(name);
            }
        }

        if (names.isEmpty()) {
            names = List.of("main");
        } else {
            names = new ArrayList<>(new LinkedHashSet<>(names));
        }

        branchCache = TimedValue.of(names, branchCacheTtlSeconds);
        return names;
    }

    private List<Map<String, Object>> fetchCommitList(String startUrl) {
        return fetchPagedMapList(startUrl, "commits");
    }

    private List<Map<String, Object>> fetchPagedMapList(String startUrl, String typeLabel) {
        List<Map<String, Object>> merged = new ArrayList<>();
        String nextUrl = startUrl;
        int pageCount = 0;

        while (nextUrl != null && pageCount < Math.max(1, maxPages)) {
            try {
                HttpEntity<Void> request = new HttpEntity<>(defaultHeaders());
                ResponseEntity<List> response = restTemplate.exchange(nextUrl, HttpMethod.GET, request, List.class);
                List<Map<String, Object>> rows = listOfMap(response.getBody());
                merged.addAll(rows);
                pageCount++;
                nextUrl = extractNextLink(response.getHeaders().getFirst("Link"));
            } catch (Exception e) {
                log.warn("[worklog] failed to fetch {}. url={}, reason={}", typeLabel, nextUrl, e.getMessage());
                return merged;
            }
        }

        if (nextUrl != null) {
            log.warn("[worklog] pagination truncated for {}. maxPages={} url={}", typeLabel, maxPages, startUrl);
        }
        return merged;
    }

    private Map<String, Object> fetchCommitDetail(String sha, Map<String, Object> commitItem) {
        TimedValue<Map<String, Object>> cached = commitDetailCache.get(sha);
        if (cached != null && !cached.isExpired()) {
            return cached.value();
        }

        // Search API 결과에서 실제 저장소 정보 추출
        String repoFullName = "";
        Map<String, Object> repository = map(commitItem.get("repository"));
        if (!repository.isEmpty()) {
            repoFullName = string(repository.get("full_name"));
        }
        if (repoFullName.isBlank()) {
            repoFullName = fallbackOwner + "/" + fallbackRepo;
        }

        String url = githubApiBaseUrl + "/repos/" + repoFullName + "/commits/" + sha;
        try {
            HttpEntity<Void> request = new HttpEntity<>(defaultHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            Map<String, Object> detail = response.getBody() == null ? Map.of() : response.getBody();
            commitDetailCache.put(sha, TimedValue.of(detail, commitDetailCacheTtlSeconds));
            trimCache(commitDetailCache, 4096);
            return detail;
        } catch (Exception e) {
            log.warn("[worklog] failed to fetch commit detail. sha={}, repo={}, reason={}", sha, repoFullName, e.getMessage());
            return Map.of();
        }
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        headers.set("User-Agent", "collaborative-messenger-worklog");

        String token = githubToken;
        if (token == null || token.isBlank()) {
            token = System.getenv("WORKLOG_GITHUB_TOKEN");
        }
        if (token != null) {
            token = token.trim();
        }
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private WorklogSummaryResponse buildSummary(
            String requestedAuthor,
            LocalDate date,
            List<Map<String, Object>> commitsRaw,
            String owner,
            String repo,
            boolean fallbackToRepoWide,
            boolean fallbackToRecentWindow,
            boolean fallbackToAllBranches
    ) {
        List<String> featureChanges = new ArrayList<>();
        List<String> refactorChanges = new ArrayList<>();
        List<String> fixChanges = new ArrayList<>();
        List<String> comments = new ArrayList<>();
        List<WorklogCommitItem> commitItems = new ArrayList<>();

        Map<String, Integer> areaCounts = new LinkedHashMap<>();
        Map<String, Integer> fileCounts = new LinkedHashMap<>();

        int totalFilesChanged = 0;
        int totalAdditions = 0;
        int totalDeletions = 0;

        for (Map<String, Object> item : commitsRaw) {
            String sha = string(item.get("sha"));
            if (sha.isBlank()) {
                continue;
            }

            String htmlUrl = string(item.get("html_url"));
            Map<String, Object> commitObj = map(item.get("commit"));
            String message = string(commitObj.get("message"));
            String firstLine = message.contains("\n") ? message.substring(0, message.indexOf('\n')) : message;

            Map<String, Object> detail = fetchCommitDetail(sha, item);
            Map<String, Object> stats = map(detail.get("stats"));
            int additions = intValue(stats.get("additions"));
            int deletions = intValue(stats.get("deletions"));
            int filesChanged = intValue(stats.get("total"));

            totalFilesChanged += filesChanged;
            totalAdditions += additions;
            totalDeletions += deletions;

            classify(firstLine, featureChanges, refactorChanges, fixChanges);
            commitItems.add(WorklogCommitItem.builder()
                    .sha(sha.length() > 8 ? sha.substring(0, 8) : sha)
                    .message(firstLine)
                    .url(htmlUrl)
                    .filesChanged(filesChanged)
                    .additions(additions)
                    .deletions(deletions)
                    .build());

            List<Map<String, Object>> files = listOfMap(detail.get("files"));
            for (Map<String, Object> f : files) {
                String filename = string(f.get("filename"));
                if (filename.isBlank()) {
                    continue;
                }
                String area = topArea(filename);
                areaCounts.put(area, areaCounts.getOrDefault(area, 0) + 1);
                fileCounts.put(filename, fileCounts.getOrDefault(filename, 0) + 1);
            }
        }

        if (fallbackToAllBranches) {
            comments.add("기본 브랜치에서 커밋을 찾지 못해 전체 브랜치 기준으로 집계했습니다.");
        }
        if (fallbackToRepoWide) {
            comments.add("작성자와 일치하는 커밋이 없어 저장소 전체 기준으로 집계했습니다.");
            comments.add("author 값은 GitHub 사용자명과 정확히 일치해야 합니다.");
        }
        if (fallbackToRecentWindow) {
            comments.add("오늘 범위 커밋이 없어 최근 72시간으로 확장 조회했습니다.");
        }

        if (commitItems.isEmpty()) {
            comments.add("오늘 반영된 커밋이 없습니다.");
            comments.add("점검: GitHub 토큰 / 브랜치 / author 값 / 시간대(Asia/Seoul)");
        } else {
            comments.add("커밋 " + commitItems.size() + "건, 파일 " + totalFilesChanged + "개 변경");
            comments.add("라인 변경 +" + totalAdditions + " / -" + totalDeletions);
            comments.add("영향 영역: " + String.join(", ", topEntries(areaCounts, 4)));
        }

        List<String> topAreas = topEntries(areaCounts, 5);
        List<String> topFiles = topEntries(fileCounts, 6);
        List<String> verificationPoints = inferVerificationPoints(topAreas, topFiles, featureChanges, fixChanges);
        List<String> risks = inferRiskPoints(totalDeletions, fallbackToRepoWide, fallbackToRecentWindow, commitItems.size());

        String text = buildShareText(
                requestedAuthor,
                date,
                owner,
                repo,
                commitItems,
                featureChanges,
                refactorChanges,
                fixChanges,
                comments,
                topAreas,
                topFiles,
                verificationPoints,
                risks
        );

        return WorklogSummaryResponse.builder()
                .author(requestedAuthor)
                .owner(owner)
                .repo(repo)
                .date(date)
                .commitCount(commitItems.size())
                .filesChanged(totalFilesChanged)
                .additions(totalAdditions)
                .deletions(totalDeletions)
                .featureChanges(dedupe(featureChanges))
                .refactorChanges(dedupe(refactorChanges))
                .fixChanges(dedupe(fixChanges))
                .comments(comments)
                .commits(commitItems)
                .generatedText(text)
                .build();
    }

    private List<Map<String, Object>> filterByPossibleAuthor(List<Map<String, Object>> commits, List<String> authorCandidates) {
        Set<String> needles = authorCandidates.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return commits.stream().filter(item -> {
            String authorLogin = string(map(item.get("author")).get("login")).toLowerCase();
            String committerLogin = string(map(item.get("committer")).get("login")).toLowerCase();

            Map<String, Object> commitMap = map(item.get("commit"));
            String authorName = string(map(commitMap.get("author")).get("name")).toLowerCase();
            String committerName = string(map(commitMap.get("committer")).get("name")).toLowerCase();
            String authorEmail = string(map(commitMap.get("author")).get("email")).toLowerCase();
            String committerEmail = string(map(commitMap.get("committer")).get("email")).toLowerCase();

            return needles.stream().anyMatch(needle ->
                    authorLogin.contains(needle)
                            || committerLogin.contains(needle)
                            || authorName.contains(needle)
                            || committerName.contains(needle)
                            || authorEmail.contains(needle)
                            || committerEmail.contains(needle));
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> dedupeCommitMapsBySha(List<Map<String, Object>> source) {
        Map<String, Map<String, Object>> dedup = new LinkedHashMap<>();
        for (Map<String, Object> item : source) {
            String sha = string(item.get("sha"));
            if (!sha.isBlank()) {
                dedup.putIfAbsent(sha, item);
            }
        }
        return new ArrayList<>(dedup.values());
    }

    public String buildShareText(WorklogSummaryResponse summary) {
        return summary.getGeneratedText();
    }

    private String buildShareText(
            String author,
            LocalDate date,
            String owner,
            String repo,
            List<WorklogCommitItem> commits,
            List<String> features,
            List<String> refactors,
            List<String> fixes,
            List<String> comments,
            List<String> topAreas,
            List<String> topFiles,
            List<String> verificationPoints,
            List<String> risks
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("[오늘 작업 브리핑] ").append(date).append("\n");
        sb.append("담당: ").append(author).append("\n");
        sb.append("저장소: ").append(owner).append("/").append(repo).append("\n");
        sb.append("커밋 수: ").append(commits.size()).append("\n\n");

        sb.append("1) 왜 변경했는가\n");
        for (String line : inferReasons(features, refactors, fixes, comments)) {
            sb.append("- ").append(line).append("\n");
        }

        sb.append("\n2) 무엇을 수정했는가\n");
        if (!topAreas.isEmpty()) {
            sb.append("- 영향 영역: ").append(String.join(", ", topAreas)).append("\n");
        }
        if (!topFiles.isEmpty()) {
            sb.append("- 핵심 파일: ").append(String.join(", ", topFiles)).append("\n");
        }
        if (!features.isEmpty()) {
            sb.append("- 기능/개선\n");
            dedupe(features).stream().limit(5).forEach(m -> sb.append("  * ").append(m).append("\n"));
        }
        if (!fixes.isEmpty()) {
            sb.append("- 버그 수정\n");
            dedupe(fixes).stream().limit(5).forEach(m -> sb.append("  * ").append(m).append("\n"));
        }
        if (!refactors.isEmpty()) {
            sb.append("- 리팩토링/정리\n");
            dedupe(refactors).stream().limit(5).forEach(m -> sb.append("  * ").append(m).append("\n"));
        }

        sb.append("\n3) 동료 확인 포인트\n");
        if (verificationPoints.isEmpty()) {
            sb.append("- 변경된 경로의 핵심 사용자 흐름(로그인, 친구, 채팅)을 기본 점검해 주세요.\n");
        } else {
            verificationPoints.stream().limit(6).forEach(v -> sb.append("- ").append(v).append("\n"));
        }

        sb.append("\n4) 리스크/후속\n");
        if (risks.isEmpty()) {
            sb.append("- 큰 운영 리스크는 낮아 보이며, 배포 후 에러로그 모니터링을 유지합니다.\n");
        } else {
            risks.stream().limit(5).forEach(r -> sb.append("- ").append(r).append("\n"));
        }

        sb.append("\n5) 커밋 상세\n");
        commits.stream().limit(12).forEach(c -> sb.append("- [").append(c.getSha()).append("] ")
                .append(c.getMessage()).append(" (+").append(c.getAdditions())
                .append(" / -").append(c.getDeletions()).append(")\n"));

        if (!comments.isEmpty()) {
            sb.append("\n6) 참고 메모\n");
            comments.forEach(c -> sb.append("- ").append(c).append("\n"));
        }

        return sb.toString().trim();
    }

    private List<String> inferReasons(List<String> features, List<String> refactors, List<String> fixes, List<String> comments) {
        List<String> reasons = new ArrayList<>();
        if (!features.isEmpty()) {
            reasons.add("사용자 기능 확장 또는 UX 개선 목적의 변경을 반영했습니다.");
        }
        if (!fixes.isEmpty()) {
            reasons.add("오류 가능성과 장애 위험을 낮추기 위해 안정화 패치를 포함했습니다.");
        }
        if (!refactors.isEmpty()) {
            reasons.add("유지보수성과 코드 가독성을 높이기 위해 구조를 정리했습니다.");
        }
        if (comments.stream().anyMatch(c -> c.contains("72시간"))) {
            reasons.add("오늘 커밋이 적은 경우를 대비해 최근 반영분까지 포함해 누락을 줄였습니다.");
        }
        if (reasons.isEmpty()) {
            reasons.add("운영 점검과 코드 정리를 위해 변경했습니다.");
        }
        return reasons;
    }

    private List<String> inferVerificationPoints(
            List<String> topAreas,
            List<String> topFiles,
            List<String> features,
            List<String> fixes
    ) {
        List<String> points = new ArrayList<>();
        String joinedAreas = String.join(" ", topAreas).toLowerCase();
        String joinedFiles = String.join(" ", topFiles).toLowerCase();

        if (joinedAreas.contains("auth") || joinedFiles.contains("auth")) {
            points.add("회원가입/로그인/로그아웃 시나리오를 브라우저 2개로 재검증해 주세요.");
        }
        if (joinedAreas.contains("chat") || joinedFiles.contains("chat")) {
            points.add("1:1 채팅 메시지 전송과 수신 정합성을 양쪽 계정에서 확인해 주세요.");
        }
        if (joinedAreas.contains("worklog") || joinedFiles.contains("worklog")) {
            points.add("요약 생성 시 커밋 수/작성자/핵심 파일이 기대값과 일치하는지 확인해 주세요.");
        }
        if (joinedAreas.contains("user") || joinedAreas.contains("friend") || joinedFiles.contains("friend")) {
            points.add("친구 목록 조회 및 친구에게 전송 동작을 확인해 주세요.");
        }
        if (!features.isEmpty()) {
            points.add("새로 추가된 UI 버튼의 진행 상태(로딩/완료/실패 표시)를 점검해 주세요.");
        }
        if (!fixes.isEmpty()) {
            points.add("기존에 실패하던 케이스가 재현되지 않는지 회귀 테스트해 주세요.");
        }
        return dedupe(points);
    }

    private List<String> inferRiskPoints(
            int totalDeletions,
            boolean fallbackToRepoWide,
            boolean fallbackToRecentWindow,
            int commitCount
    ) {
        List<String> risks = new ArrayList<>();
        if (totalDeletions > 500) {
            risks.add("삭제 라인이 큰 편이라 일부 기능 회귀 가능성을 확인해야 합니다.");
        }
        if (fallbackToRepoWide) {
            risks.add("작성자 매핑이 정확하지 않으면 다른 사람 커밋이 섞일 수 있습니다.");
        }
        if (fallbackToRecentWindow) {
            risks.add("72시간 확장 조회가 켜지면 '오늘 작업' 기준보다 넓게 집계될 수 있습니다.");
        }
        if (commitCount == 0) {
            risks.add("커밋이 0건이면 토큰 권한, 브랜치, 작성자 매핑부터 점검이 필요합니다.");
        }
        return dedupe(risks);
    }

    private void classify(String message, List<String> features, List<String> refactors, List<String> fixes) {
        String m = message.toLowerCase();
        if (containsAny(m, "feat", "feature", "implement", "add", "new")) {
            features.add(message);
            return;
        }
        if (containsAny(m, "refactor", "cleanup", "clean-up", "restructure", "rename", "tune")) {
            refactors.add(message);
            return;
        }
        if (containsAny(m, "fix", "bug", "hotfix", "patch", "error")) {
            fixes.add(message);
            return;
        }
        refactors.add(message);
    }

    private boolean containsAny(String src, String... keys) {
        for (String key : keys) {
            if (src.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private String topArea(String filename) {
        int slash = filename.indexOf('/');
        if (slash <= 0) {
            return filename;
        }
        return filename.substring(0, slash);
    }

    private List<String> topEntries(Map<String, Integer> map, int limit) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(e -> e.getKey() + "(" + e.getValue() + ")")
                .collect(Collectors.toList());
    }

    private String string(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private int intValue(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Object> map(Object v) {
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                converted.put(String.valueOf(e.getKey()), e.getValue());
            }
            return converted;
        }
        return Map.of();
    }

    private List<Map<String, Object>> listOfMap(Object v) {
        if (!(v instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> converted = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    row.put(String.valueOf(e.getKey()), e.getValue());
                }
                converted.add(row);
            }
        }
        return converted;
    }

    private List<String> dedupe(List<String> source) {
        Set<String> set = new LinkedHashSet<>(source);
        return new ArrayList<>(set);
    }

    private static String normalizeAuthorToken(String token) {
        if (token == null) {
            return "";
        }
        String normalized = token.trim();
        while (normalized.startsWith("@")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }

    private <T> void trimCache(ConcurrentMap<String, TimedValue<T>> cache, int hardLimit) {
        if (cache.size() <= hardLimit) {
            return;
        }

        for (Map.Entry<String, TimedValue<T>> e : cache.entrySet()) {
            TimedValue<T> value = e.getValue();
            if (value == null || value.isExpired()) {
                cache.remove(e.getKey());
            }
        }

        if (cache.size() <= hardLimit) {
            return;
        }

        int toRemove = cache.size() - hardLimit;
        int removed = 0;
        for (String key : cache.keySet()) {
            if (removed >= toRemove) {
                break;
            }
            cache.remove(key);
            removed++;
        }
    }

    private record TimedValue<T>(T value, long expiresAtEpochMillis) {
        static <T> TimedValue<T> of(T value, long ttlSeconds) {
            long ttl = Math.max(1L, ttlSeconds);
            return new TimedValue<>(value, System.currentTimeMillis() + ttl * 1000);
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAtEpochMillis;
        }
    }
}
