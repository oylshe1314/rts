package com.sk.rts.application.util;

import java.time.*;
import java.util.Date;

public final class TimeUtil {

    public static final long PER_MINUTE_SECONDS = 60;
    public static final long PER_HOUR_SECONDS = 3600;
    public static final long PER_DAY_SECONDS = 86400;

    public static long nowSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    public static long nowMillis() {
        return System.currentTimeMillis();
    }

    public static OffsetDateTime todayBeginTime() {
        OffsetDateTime now = OffsetDateTime.now();
        return now.minusSeconds((now.toEpochSecond() + now.getOffset().getTotalSeconds()) % PER_DAY_SECONDS);
    }

    public static OffsetDateTime weekBeginTime() {
        OffsetDateTime now = OffsetDateTime.now();
        return now.minusSeconds(((now.toEpochSecond() + now.getOffset().getTotalSeconds()) % PER_DAY_SECONDS) + (PER_DAY_SECONDS * (now.getDayOfWeek().getValue() - 1L)));
    }

    public static OffsetDateTime monthBeginTime() {
        OffsetDateTime now = OffsetDateTime.now();
        return now.minusSeconds(((now.toEpochSecond() + now.getOffset().getTotalSeconds()) % PER_DAY_SECONDS) + (PER_DAY_SECONDS * (now.getDayOfMonth() -1L)));
    }

    public static long toEpochSeconds(OffsetDateTime source) {
        return source.toEpochSecond();
    }

    public static long toEpochSeconds(LocalDateTime source) {
        return source.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    public static long toEpochSeconds(LocalDateTime source, ZoneId zoneId) {
        return source.atZone(zoneId).toEpochSecond();
    }

    public static long toEpochSeconds(Date source) {
        return source.getTime() / 1000;
    }

    public static OffsetDateTime toOffsetDateTime(long timestamp) {
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
    }

    public static OffsetDateTime toOffsetDateTime(long timestamp, ZoneId zoneId) {
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(timestamp), zoneId);
    }

    public static OffsetDateTime toOffsetDateTime(LocalDateTime source) {
        return source.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    public static OffsetDateTime toOffsetDateTime(LocalDateTime source, ZoneId zoneId) {
        return source.atZone(zoneId).toOffsetDateTime();
    }

    public static OffsetDateTime toOffsetDateTime(Date source) {
        return toOffsetDateTime(source, ZoneId.systemDefault());
    }

    public static OffsetDateTime toOffsetDateTime(Date source, ZoneId zoneId) {
        return OffsetDateTime.ofInstant(source.toInstant(), zoneId);
    }

    public static LocalDateTime toLocalDateTime(Long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
    }

    public static LocalDateTime toLocalDateTime(Long timestamp, ZoneId zoneId) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), zoneId);
    }

    public static LocalDateTime toLocalDateTime(OffsetDateTime source) {
        return source.toLocalDateTime();
    }

    public static LocalDateTime toLocalDateTime(OffsetDateTime source, ZoneId zoneId) {
        return source.toInstant().atZone(zoneId).toLocalDateTime();
    }

    public static LocalDateTime toLocalDateTime(Date source) {
        return LocalDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
    }

    public static LocalDateTime toLocalDateTime(Date source, ZoneId zoneId) {
        return LocalDateTime.ofInstant(source.toInstant(), zoneId);
    }

    public static Date toDate(long timestamp) {
        return new Date(timestamp * 1000);
    }

    public static Date toDate(OffsetDateTime source) {
        return Date.from(source.toInstant());
    }

    public static Date toDate(LocalDateTime source) {
        return Date.from(source.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date toDate(LocalDateTime source, ZoneId zoneId) {
        return Date.from(source.atZone(zoneId).toInstant());
    }
}
