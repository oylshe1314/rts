package com.sk.rts.application.util;

import io.vertx.core.Future;
import org.jspecify.annotations.NullMarked;
import reactor.core.publisher.Mono;

@NullMarked
public final class ReactiveUtil {

    public static <T> Mono<T> toMono(Future<T> future) {
        return Mono.fromCompletionStage(future.toCompletionStage());
    }

    public static <T> Future<T> toFuture(Mono<T> mono) {
        return Future.fromCompletionStage(mono.toFuture());
    }
}
