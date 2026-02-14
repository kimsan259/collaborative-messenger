package com.messenger.common.debug;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class RecentErrorLogService {

    private static final int MAX_SIZE = 300;

    private final Deque<ErrorLogEntry> entries = new ConcurrentLinkedDeque<>();

    public void capture(String source, String message, String detail) {
        ErrorLogEntry entry = new ErrorLogEntry(
                LocalDateTime.now(),
                source,
                message,
                detail
        );
        entries.addFirst(entry);
        while (entries.size() > MAX_SIZE) {
            entries.pollLast();
        }
    }

    public List<ErrorLogEntry> recent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<ErrorLogEntry> result = new ArrayList<>(safeLimit);
        int i = 0;
        for (ErrorLogEntry e : entries) {
            if (i++ >= safeLimit) break;
            result.add(e);
        }
        return result;
    }

    public record ErrorLogEntry(
            LocalDateTime at,
            String source,
            String message,
            String detail
    ) {}
}
