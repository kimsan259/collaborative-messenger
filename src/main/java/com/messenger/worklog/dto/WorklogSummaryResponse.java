package com.messenger.worklog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class WorklogSummaryResponse {

    private String author;
    private String owner;
    private String repo;
    private LocalDate date;
    private int commitCount;
    private int filesChanged;
    private int additions;
    private int deletions;
    private List<String> featureChanges;
    private List<String> refactorChanges;
    private List<String> fixChanges;
    private List<String> comments;
    private List<WorklogCommitItem> commits;
    private String generatedText;
}

