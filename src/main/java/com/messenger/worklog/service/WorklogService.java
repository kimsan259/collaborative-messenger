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
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorklogService {

    @Value("${app.worklog.owner:kimsan259}")
    private String owner;

    @Value("${app.worklog.repo:collaborative-messenger}")
    private String repo;

    @Value("${app.worklog.default-author:kimsan4515}")
    private String defaultAuthor;

    @Value("${app.worklog.github-token:}")
    private String githubToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public WorklogSummaryResponse generateTodaySummary(String authorOrNull) {
        String requestedAuthor = (authorOrNull == null || authorOrNull.isBlank())
                ? defaultAuthor
                : authorOrNull.trim();

        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zone);
        OffsetDateTime since = today.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime until = OffsetDateTime.now(zone);

        List<Map<String, Object>> commits = fetchCommitsByAuthor(requestedAuthor, since, until);
        boolean fallbackToRepoWide = false;
        boolean fallbackToRecentWindow = false;

        if (commits.isEmpty()) {
            List<Map<String, Object>> repoCommits = fetchCommitsWithoutAuthor(since, until);
            commits = filterByPossibleAuthor(repoCommits, requestedAuthor);

            if (commits.isEmpty()) {
                commits = repoCommits;
                fallbackToRepoWide = !commits.isEmpty();
            }
        }

        if (commits.isEmpty()) {
            OffsetDateTime recentSince = since.minusDays(2);
            commits = fetchCommitsByAuthor(requestedAuthor, recentSince, until);
            fallbackToRecentWindow = !commits.isEmpty();

            if (commits.isEmpty()) {
                List<Map<String, Object>> repoRecent = fetchCommitsWithoutAuthor(recentSince, until);
                commits = filterByPossibleAuthor(repoRecent, requestedAuthor);
                if (commits.isEmpty()) {
                    commits = repoRecent;
                    fallbackToRepoWide = !commits.isEmpty();
                } else {
                    fallbackToRecentWindow = true;
                }
            }
        }

        return buildSummary(requestedAuthor, today, commits, fallbackToRepoWide, fallbackToRecentWindow);
    }

    private List<Map<String, Object>> fetchCommitsByAuthor(String author, OffsetDateTime since, OffsetDateTime until) {
        String url = UriComponentsBuilder
                .fromUriString("https://api.github.com/repos/{owner}/{repo}/commits")
                .queryParam("author", author)
                .queryParam("since", since.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .queryParam("until", until.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .queryParam("per_page", 100)
                .buildAndExpand(owner, repo)
                .toUriString();
        return fetchCommitList(url);
    }

    private List<Map<String, Object>> fetchCommitsWithoutAuthor(OffsetDateTime since, OffsetDateTime until) {
        String url = UriComponentsBuilder
                .fromUriString("https://api.github.com/repos/{owner}/{repo}/commits")
                .queryParam("since", since.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .queryParam("until", until.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .queryParam("per_page", 100)
                .buildAndExpand(owner, repo)
                .toUriString();
        return fetchCommitList(url);
    }

    private List<Map<String, Object>> fetchCommitList(String url) {
        try {
            HttpEntity<Void> request = new HttpEntity<>(defaultHeaders());
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);
            if (response.getBody() == null) {
                return List.of();
            }
            return response.getBody();
        } catch (Exception e) {
            log.warn("[worklog] failed to fetch commits. url={}, reason={}", url, e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> fetchCommitDetail(String sha) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/commits/" + sha;
        try {
            HttpEntity<Void> request = new HttpEntity<>(defaultHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            return response.getBody() == null ? Map.of() : response.getBody();
        } catch (Exception e) {
            log.warn("[worklog] failed to fetch commit detail. sha={}, reason={}", sha, e.getMessage());
            return Map.of();
        }
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        headers.set("User-Agent", "collaborative-messenger-worklog");
        if (githubToken != null && !githubToken.isBlank()) {
            headers.setBearerAuth(githubToken);
        }
        return headers;
    }

    private WorklogSummaryResponse buildSummary(
            String requestedAuthor,
            LocalDate date,
            List<Map<String, Object>> commitsRaw,
            boolean fallbackToRepoWide,
            boolean fallbackToRecentWindow
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

            Map<String, Object> detail = fetchCommitDetail(sha);
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

        if (fallbackToRepoWide) {
            comments.add("작성자 일치 커밋이 없어 저장소 전체 커밋으로 대체했습니다.");
            comments.add("author 값(깃허브 사용자명)을 다시 확인하세요.");
        }
        if (fallbackToRecentWindow) {
            comments.add("오늘 범위 커밋이 없어 최근 72시간 커밋으로 확장 조회했습니다.");
        }

        if (commitItems.isEmpty()) {
            comments.add("오늘 반영된 커밋이 없습니다.");
            comments.add("점검: author 값 / 기본 브랜치 푸시 여부 / 시간대(Asia/Seoul)");
        } else {
            comments.add("커밋 " + commitItems.size() + "건, 파일 " + totalFilesChanged + "개 변경");
            comments.add("라인 변경: +" + totalAdditions + " / -" + totalDeletions);
            comments.add("핵심 수정 영역: " + String.join(", ", topEntries(areaCounts, 4)));
        }

        String text = buildShareText(
                requestedAuthor,
                date,
                commitItems,
                featureChanges,
                refactorChanges,
                fixChanges,
                comments,
                topEntries(areaCounts, 5),
                topEntries(fileCounts, 6)
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

    private List<Map<String, Object>> filterByPossibleAuthor(List<Map<String, Object>> commits, String requestedAuthor) {
        String needle = requestedAuthor.toLowerCase();
        return commits.stream().filter(item -> {
            String authorLogin = string(map(item.get("author")).get("login")).toLowerCase();
            String committerLogin = string(map(item.get("committer")).get("login")).toLowerCase();

            Map<String, Object> commitMap = map(item.get("commit"));
            String authorName = string(map(commitMap.get("author")).get("name")).toLowerCase();
            String committerName = string(map(commitMap.get("committer")).get("name")).toLowerCase();

            return authorLogin.contains(needle)
                    || committerLogin.contains(needle)
                    || authorName.contains(needle)
                    || committerName.contains(needle);
        }).collect(Collectors.toList());
    }

    public String buildShareText(WorklogSummaryResponse summary) {
        return summary.getGeneratedText();
    }

    private String buildShareText(
            String author,
            LocalDate date,
            List<WorklogCommitItem> commits,
            List<String> features,
            List<String> refactors,
            List<String> fixes,
            List<String> comments,
            List<String> topAreas,
            List<String> topFiles
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("[오늘 작업 공유] ").append(date).append(" / 담당: ").append(author).append("\n");
        sb.append("저장소: ").append(owner).append("/").append(repo).append("\n\n");

        sb.append("1) 왜 변경했는가\n");
        List<String> why = inferReasons(features, refactors, fixes, comments);
        for (String line : why) {
            sb.append("- ").append(line).append("\n");
        }

        sb.append("\n2) 무엇을 수정했는가\n");
        sb.append("- 커밋 수: ").append(commits.size()).append("\n");
        if (!topAreas.isEmpty()) {
            sb.append("- 주요 영역: ").append(String.join(", ", topAreas)).append("\n");
        }
        if (!topFiles.isEmpty()) {
            sb.append("- 대표 파일: ").append(String.join(", ", topFiles)).append("\n");
        }

        if (!features.isEmpty()) {
            sb.append("- 기능 추가/개선\n");
            dedupe(features).stream().limit(4).forEach(m -> sb.append("  * ").append(m).append("\n"));
        }
        if (!fixes.isEmpty()) {
            sb.append("- 버그/안정화\n");
            dedupe(fixes).stream().limit(4).forEach(m -> sb.append("  * ").append(m).append("\n"));
        }
        if (!refactors.isEmpty()) {
            sb.append("- 구조/리팩터링\n");
            dedupe(refactors).stream().limit(4).forEach(m -> sb.append("  * ").append(m).append("\n"));
        }

        sb.append("\n3) 동료 확인 포인트\n");
        sb.append("- 아래 커밋 메시지 기준으로 영향 기능을 같이 점검해주세요.\n");
        commits.stream().limit(10).forEach(c -> sb.append("  * [").append(c.getSha()).append("] ")
                .append(c.getMessage()).append(" (+").append(c.getAdditions())
                .append("/-").append(c.getDeletions()).append(")\n"));

        if (!comments.isEmpty()) {
            sb.append("\n4) 참고 메모\n");
            comments.forEach(c -> sb.append("- ").append(c).append("\n"));
        }

        return sb.toString().trim();
    }

    private List<String> inferReasons(List<String> features, List<String> refactors, List<String> fixes, List<String> comments) {
        List<String> reasons = new ArrayList<>();
        if (!features.isEmpty()) {
            reasons.add("신규 기능 또는 사용자 흐름 개선을 반영하기 위해 변경했습니다.");
        }
        if (!fixes.isEmpty()) {
            reasons.add("기존 오류를 줄이고 서비스 안정성을 높이기 위해 수정했습니다.");
        }
        if (!refactors.isEmpty()) {
            reasons.add("코드 유지보수성과 이후 확장성을 높이기 위해 구조를 정리했습니다.");
        }
        if (reasons.isEmpty()) {
            reasons.add("운영 점검 및 코드 정리를 위해 변경했습니다.");
        }
        if (comments.stream().anyMatch(c -> c.contains("72시간"))) {
            reasons.add("당일 커밋이 적어 최근 작업까지 포함해 맥락을 보강했습니다.");
        }
        return reasons;
    }

    private void classify(String message, List<String> features, List<String> refactors, List<String> fixes) {
        String m = message.toLowerCase();
        if (containsAny(m, "feat", "feature", "implement", "add", "new")) {
            features.add(message);
            return;
        }
        if (containsAny(m, "refactor", "cleanup", "clean-up", "restructure", "rename")) {
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
            return m.entrySet().stream().collect(Collectors.toMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue));
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
                converted.add(m.entrySet().stream().collect(Collectors.toMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue)));
            }
        }
        return converted;
    }

    private List<String> dedupe(List<String> source) {
        Set<String> set = new LinkedHashSet<>(source);
        return new ArrayList<>(set);
    }
}
