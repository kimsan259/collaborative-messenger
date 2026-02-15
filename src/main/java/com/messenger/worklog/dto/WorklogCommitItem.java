package com.messenger.worklog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WorklogCommitItem {
    private String sha;
    private String message;
    private String url;
    private int filesChanged;
    private int additions;
    private int deletions;
}

