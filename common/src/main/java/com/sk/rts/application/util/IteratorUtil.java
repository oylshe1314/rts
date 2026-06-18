package com.sk.rts.application.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

public final class IteratorUtil {

    public static <S, R> Collection<R> convert(Collection<S> source, Function<S, R> mapper) {
        return source.stream().map(mapper).toList();
    }

    public static <S, R> Iterable<R> convert(Iterable<S> source, Function<S, R> mapper) {
        List<R> rs = new ArrayList<>();
        for (S s : source) {
            rs.add(mapper.apply(s));
        }
        return rs;
    }

    public static <T> T findOne(Collection<T> ts, Predicate<T> predicate) {
        for (T t : ts) {
            if (predicate.test(t)) {
                return t;
            }
        }
        return null;
    }

    public static <T> Collection<T> findAll(Collection<T> ss, Predicate<T> predicate) {
        return ss.stream().filter(predicate).toList();
    }

    public static <T> void Confusion(List<T> selectList) {
        Random random = new Random(System.currentTimeMillis());
        int listSize = selectList.size();
        for (int i = 0; i < listSize; i++) {
            int ri = random.nextInt(listSize);
            T item = selectList.get(ri);
            selectList.set(ri, selectList.get(i));
            selectList.set(i, item);
        }
    }
}
