package com.messenger.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================
 * DateTimeUtil - 날짜/시간 유틸리티
 * ============================================================
 *
 * 【역할】
 * 날짜/시간 관련 자주 사용하는 기능을 한 곳에 모아둔 유틸리티 클래스입니다.
 * 프로젝트 전체에서 날짜 포맷이 일관되도록 합니다.
 *
 * 【사용법】
 * DateTimeUtil.formatDateTime(LocalDateTime.now())  → "2026-02-08 14:30:25"
 * DateTimeUtil.todayStart()                         → 2026-02-08 00:00:00
 * DateTimeUtil.todayEnd()                           → 2026-02-08 23:59:59
 * ============================================================
 */
public class DateTimeUtil {

    /** 날짜+시간 포맷: "2026-02-08 14:30:25" */
    public static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 날짜 포맷: "2026-02-08" */
    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 시간 포맷: "14:30:25" */
    public static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // private 생성자: 이 클래스는 인스턴스를 만들지 않고 static 메서드만 사용
    private DateTimeUtil() {}

    /**
     * LocalDateTime을 "2026-02-08 14:30:25" 형태의 문자열로 변환합니다.
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_TIME_FORMAT);
    }

    /**
     * LocalDate를 "2026-02-08" 형태의 문자열로 변환합니다.
     */
    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMAT);
    }

    /**
     * 오늘 날짜의 시작 시각을 반환합니다. (00:00:00)
     * 업무일지 생성 시 "오늘 보낸 메시지" 조회의 시작 범위로 사용됩니다.
     */
    public static LocalDateTime todayStart() {
        return LocalDate.now().atStartOfDay();  // 오늘 00:00:00
    }

    /**
     * 오늘 날짜의 마지막 시각을 반환합니다. (23:59:59)
     * 업무일지 생성 시 "오늘 보낸 메시지" 조회의 끝 범위로 사용됩니다.
     */
    public static LocalDateTime todayEnd() {
        return LocalDate.now().atTime(LocalTime.MAX);  // 오늘 23:59:59.999999999
    }

    /**
     * 특정 날짜의 시작 시각을 반환합니다. (00:00:00)
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    /**
     * 특정 날짜의 마지막 시각을 반환합니다. (23:59:59)
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }
}
