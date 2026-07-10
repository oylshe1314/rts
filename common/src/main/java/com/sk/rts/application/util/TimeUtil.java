package com.sk.rts.application.util;

import java.time.*;
import java.util.Date;

public final class TimeUtil {

    public static final long PER_MINUTE_SECONDS = 60;
    public static final long PER_HOUR_SECONDS = 3600;
    public static final long PER_DAY_SECONDS = 86400;

    public static final ZoneOffset ZONE_OFFSET_UTC8 = ZoneOffset.ofHours(8);

    public static long nowSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    public static long nowMillis() {
        return System.currentTimeMillis();
    }

    public static OffsetDateTime toOffsetDateTime(LocalDateTime source) {
        return toOffsetDateTime(source, ZoneId.systemDefault());
    }

    public static OffsetDateTime toOffsetDateTime(LocalDateTime source, ZoneId zoneId) {
        return source.atZone(zoneId).toOffsetDateTime();
    }

    public static OffsetDateTime toOffsetDateTime(LocalDate source) {
        return toOffsetDateTime(source, ZoneId.systemDefault());
    }

    public static OffsetDateTime toOffsetDateTime(LocalDate source, ZoneId zoneId) {
        return source.atStartOfDay(zoneId).toOffsetDateTime();
    }

    public static OffsetDateTime toOffsetDateTime(long timestamp) {
        return toOffsetDateTime(timestamp, ZoneId.systemDefault());
    }

    public static OffsetDateTime toOffsetDateTime(long timestamp, ZoneId zoneId) {
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(timestamp), zoneId);
    }

    public static OffsetDateTime toOffsetDateTime(Date source) {
        return toOffsetDateTime(source, ZoneId.systemDefault());
    }

    public static OffsetDateTime toOffsetDateTime(Date source, ZoneId zoneId) {
        return OffsetDateTime.ofInstant(source.toInstant(), zoneId);
    }

    public static Date toDate(LocalDateTime source) {
        return toDate(source, ZoneId.systemDefault());
    }

    public static Date toDate(LocalDateTime source, ZoneId zoneId) {
        return Date.from(source.atZone(zoneId).toInstant());
    }

    public static Date toDate(LocalDate source) {
        return toDate(source, ZoneId.systemDefault());
    }

    public static Date toDate(LocalDate source, ZoneId zoneId) {
        return Date.from(source.atStartOfDay(zoneId).toInstant());
    }

    public static Date toDate(OffsetDateTime source) {
        source.atZoneSameInstant(ZoneId.systemDefault()).toInstant();
        return Date.from(source.toInstant());
    }

    public static Date toDate(OffsetDateTime source, ZoneId zoneId) {
        source.atZoneSameInstant(zoneId).toInstant();
        return Date.from(source.toInstant());
    }

    public static long todayBeginTime() {
        OffsetDateTime now = OffsetDateTime.now();
        return (now.toEpochSecond() + now.getOffset().getTotalSeconds()) % PER_DAY_SECONDS;
    }
}
