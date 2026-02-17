package com.messenger.worklog.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorklogServiceParsingTest {

    @Test
    void parseAuthorCandidates_shouldNormalizeAndDedupe() {
        List<String> result = WorklogService.parseAuthorCandidates(" @kimsan4515, kimsan259 ; @kimsan4515 ", "fallback");

        assertThat(result).containsExactly("kimsan4515", "kimsan259");
    }

    @Test
    void parseAuthorCandidates_shouldUseFallbackWhenInputEmpty() {
        List<String> result = WorklogService.parseAuthorCandidates("   ", "@fallbackUser");

        assertThat(result).containsExactly("fallbackUser");
    }

    @Test
    void extractNextLink_shouldReturnNextUrlFromLinkHeader() {
        String linkHeader = "<https://api.github.com/repos/o/r/commits?page=2>; rel=\"next\", " +
                "<https://api.github.com/repos/o/r/commits?page=5>; rel=\"last\"";

        String next = WorklogService.extractNextLink(linkHeader);

        assertThat(next).isEqualTo("https://api.github.com/repos/o/r/commits?page=2");
    }

    @Test
    void extractNextLink_shouldReturnNullWhenNoNext() {
        String linkHeader = "<https://api.github.com/repos/o/r/commits?page=5>; rel=\"last\"";

        String next = WorklogService.extractNextLink(linkHeader);

        assertThat(next).isNull();
    }
}
