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

        if (commits.isEmpty()) {
            List<Map<String, Object>> repoCommits = fetchCommitsWithoutAuthor(since, until);
            commits = filterByPossibleAuthor(repoCommits, requestedAuthor);

            if (commits.isEmpty()) {
                commits = repoCommits;
                fallbackToRepoWide = !commits.isEmpty();
            }
        }

        return buildSummary(requestedAuthor, today, commits, fallbackToRepoWide);
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
            boolean fallbackToRepoWide
    ) {
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
        }

        if (fallbackToRepoWide) {
            comments.add("Author-matched commits were not found. Showing today's repository commits instead.");
            comments.add("Check your GitHub username in the author field.");
        }

        if (commitItems.isEmpty()) {
            comments.add("No commits were found for today.");
            comments.add("Quick check: 1) correct author 2) pushed to main/default branch 3) timezone (Asia/Seoul)");
        } else {
            comments.add("Today's commit count: " + commitItems.size());
            comments.add("Changed files: " + totalFilesChanged + ", line delta: +" + totalAdditions + " / -" + totalDeletions);
            comments.add("Auto summary for author: " + requestedAuthor);
        }

        String text = buildShareText(requestedAuthor, date, commitItems, featureChanges, refactorChanges, fixChanges, comments);

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
            List<String> comments
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("[AUTO WORKLOG] ").append(date).append(" / owner: ").append(author).append("\n");
        sb.append("- repository: ").append(owner).append("/").append(repo).append("\n");
        sb.append("- commit count: ").append(commits.size()).append("\n");

        if (!features.isEmpty()) {
            sb.append("- features:\n");
            features.forEach(m -> sb.append("  * ").append(m).append("\n"));
        }
        if (!refactors.isEmpty()) {
            sb.append("- refactors:\n");
            refactors.forEach(m -> sb.append("  * ").append(m).append("\n"));
        }
        if (!fixes.isEmpty()) {
            sb.append("- fixes:\n");
            fixes.forEach(m -> sb.append("  * ").append(m).append("\n"));
        }
        if (!comments.isEmpty()) {
            sb.append("- comments:\n");
            comments.forEach(c -> sb.append("  * ").append(c).append("\n"));
        }

        if (!commits.isEmpty()) {
            sb.append("- commits:\n");
            commits.forEach(c -> sb.append("  * [").append(c.getSha()).append("] ").append(c.getMessage()).append("\n"));
        }

        return sb.toString().trim();
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

    private List<String> dedupe(List<String> source) {
        Set<String> set = new LinkedHashSet<>(source);
        return new ArrayList<>(set);
    }
}
