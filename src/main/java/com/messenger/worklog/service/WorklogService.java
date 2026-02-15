package com.messenger.worklog.service;

import com.messenger.worklog.dto.WorklogCommitItem;
import com.messenger.worklog.dto.WorklogSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
        String author = (authorOrNull == null || authorOrNull.isBlank()) ? defaultAuthor : authorOrNull.trim();
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zone);
        OffsetDateTime since = today.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime until = OffsetDateTime.now(zone);

        List<Map<String, Object>> commits = fetchCommits(author, since, until);
        return buildSummary(author, today, commits);
    }

    private List<Map<String, Object>> fetchCommits(String author, OffsetDateTime since, OffsetDateTime until) {
        String url = UriComponentsBuilder
                .fromUriString("https://api.github.com/repos/{owner}/{repo}/commits")
                .queryParam("author", author)
                .queryParam("since", since.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .queryParam("until", until.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .queryParam("per_page", 50)
                .buildAndExpand(owner, repo)
                .toUriString();

        HttpEntity<Void> request = new HttpEntity<>(defaultHeaders());
        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);
        if (response.getBody() == null) {
            return List.of();
        }
        return response.getBody();
    }

    private Map<String, Object> fetchCommitDetail(String sha) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/commits/" + sha;
        HttpEntity<Void> request = new HttpEntity<>(defaultHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
        return response.getBody() == null ? Map.of() : response.getBody();
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

    private WorklogSummaryResponse buildSummary(String author, LocalDate date, List<Map<String, Object>> commitsRaw) {
        List<String> featureChanges = new ArrayList<>();
        List<String> refactorChanges = new ArrayList<>();
        List<String> fixChanges = new ArrayList<>();
        List<String> comments = new ArrayList<>();
        List<WorklogCommitItem> commitItems = new ArrayList<>();

        int totalFilesChanged = 0;
        int totalAdditions = 0;
        int totalDeletions = 0;

        for (Map<String, Object> item : commitsRaw) {
            String sha = string(item.get("sha"));
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
        }

        if (commitItems.isEmpty()) {
            comments.add("오늘 반영된 커밋이 없습니다.");
        } else {
            comments.add("오늘 총 " + commitItems.size() + "개 커밋을 반영했습니다.");
            comments.add("파일 변경 " + totalFilesChanged + "건, +" + totalAdditions + " / -" + totalDeletions + " 라인입니다.");
            comments.add("담당자 " + author + " 기준 자동 요약입니다.");
        }

        String text = buildShareText(author, date, commitItems, featureChanges, refactorChanges, fixChanges, comments);
        return WorklogSummaryResponse.builder()
                .author(author)
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
            List<String> comments
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("[자동 작업 공유] ").append(date).append(" / 담당: ").append(author).append("\n");
        sb.append("- 저장소: ").append(owner).append("/").append(repo).append("\n");
        sb.append("- 커밋 수: ").append(commits.size()).append("\n");

        if (!features.isEmpty()) {
            sb.append("- 기능 추가:\n");
            features.forEach(m -> sb.append("  • ").append(m).append("\n"));
        }
        if (!refactors.isEmpty()) {
            sb.append("- 리팩토링:\n");
            refactors.forEach(m -> sb.append("  • ").append(m).append("\n"));
        }
        if (!fixes.isEmpty()) {
            sb.append("- 수정/버그픽스:\n");
            fixes.forEach(m -> sb.append("  • ").append(m).append("\n"));
        }
        if (!comments.isEmpty()) {
            sb.append("- 주석(자동):\n");
            comments.forEach(c -> sb.append("  • ").append(c).append("\n"));
        }

        if (!commits.isEmpty()) {
            sb.append("- 커밋 목록:\n");
            commits.forEach(c -> sb.append("  • [").append(c.getSha()).append("] ").append(c.getMessage()).append("\n"));
        }
        return sb.toString().trim();
    }

    private void classify(String message, List<String> features, List<String> refactors, List<String> fixes) {
        String m = message.toLowerCase();
        if (containsAny(m, "feat", "feature", "추가", "구현")) {
            features.add(message);
            return;
        }
        if (containsAny(m, "refactor", "리팩토", "개선", "정리")) {
            refactors.add(message);
            return;
        }
        if (containsAny(m, "fix", "bug", "오류", "버그", "수정")) {
            fixes.add(message);
            return;
        }
        refactors.add(message);
    }

    private boolean containsAny(String src, String... keys) {
        for (String key : keys) {
            if (src.contains(key)) return true;
        }
        return false;
    }

    private String string(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private int intValue(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Object> map(Object v) {
        if (v instanceof Map<?, ?> m) {
            return m.entrySet().stream()
                    .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue));
        }
        return Map.of();
    }

    private List<String> dedupe(List<String> source) {
        return new ArrayList<>(new LinkedHashSet<>(source));
    }
}
