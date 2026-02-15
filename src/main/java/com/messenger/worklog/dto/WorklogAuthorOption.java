package com.messenger.worklog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WorklogAuthorOption {
    private Long userId;
    private String displayName;
    private String username;
    private String githubAuthor;
    private boolean self;
}

