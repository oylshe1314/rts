package com.sk.rts.application.filter;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@NullMarked
public class TimingWebFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        long beginTime = System.currentTimeMillis();
        return chain.filter(exchange).doOnSuccess(_ -> {
            long endTime = System.currentTimeMillis();
            long consumeTime = endTime - beginTime;
            if (consumeTime < 1000L) {
                log.debug("请求路径：{}，响应时间：{}ms", path, consumeTime);
            } else {
                log.warn("请求路径：{}，响应时间：{}ms", path, consumeTime);
            }
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
