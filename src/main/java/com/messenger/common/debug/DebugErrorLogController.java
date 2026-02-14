package com.messenger.common.debug;

import com.messenger.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DebugErrorLogController {

    private final RecentErrorLogService recentErrorLogService;

    @Value("${debug.log.token:}")
    private String debugLogToken;

    @GetMapping("/api/debug/errors/recent")
    public ResponseEntity<ApiResponse<List<RecentErrorLogService.ErrorLogEntry>>> recent(
            @RequestParam(defaultValue = "30") int limit,
            @RequestHeader(value = "X-Debug-Token", required = false) String headerToken,
            @RequestParam(value = "token", required = false) String queryToken
    ) {
        if (debugLogToken == null || debugLogToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("debug log token not configured"));
        }

        String provided = (headerToken != null && !headerToken.isBlank()) ? headerToken : queryToken;
        if (provided == null || !debugLogToken.equals(provided)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("invalid debug token"));
        }

        return ResponseEntity.ok(ApiResponse.success("ok", recentErrorLogService.recent(limit)));
    }
}
