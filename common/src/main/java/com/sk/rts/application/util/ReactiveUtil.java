package com.sk.rts.application.util;

import io.vertx.core.Future;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public final class ReactiveUtil {

    public static <T> Mono<T> toMono(Future<T> future) {
        return Mono.fromCompletionStage(future.toCompletionStage());
    }

    public static <T> Future<T> toFuture(Mono<T> mono) {
        return Future.fromCompletionStage(mono.toFuture());
    }

    public static <T> Future<List<T>> toFuture(Flux<T> flux) {
        return Future.fromCompletionStage(flux.collectList().toFuture());
    }
}
