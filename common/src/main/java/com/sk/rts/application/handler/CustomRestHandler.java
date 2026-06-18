package com.sk.rts.application.handler;

import com.sk.rts.application.dto.ResponseDto;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StatusException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@NullMarked
@AllArgsConstructor
public abstract class CustomRestHandler {

    private final JsonMapper jsonMapper;

    protected Mono<Void> respond(ServerHttpResponse response, ResponseDto<?> responseDto) {
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return response.writeWith(Mono.just(responseDto).map(dto -> response.bufferFactory().wrap(jsonMapper.writeValueAsBytes(dto))));
    }

    protected Mono<Void> respond(ServerHttpResponse response, ResponseDto<?> responseDto, HttpHeaders headers) {
        response.getHeaders().addAll(headers);
        return respond(response, responseDto);
    }

    protected Mono<Void> respondError(ServerHttpResponse response, HttpStatus httpStatus) {
        return respond(response, ResponseDto.failure(httpStatus.value(), httpStatus.getReasonPhrase()));
    }

    protected Mono<Void> respondException(ServerHttpResponse response, Throwable cause) {
        StatusException statusException = StatusException.extractStatusException(cause);
        if (statusException != null) {
            log.warn("响应异常, status: {}, message: {}", statusException.getStatus(), statusException.getMessage());
            return respond(response, ResponseDto.failure(statusException));
        } else {
            log.error("服务器内部异常", cause);
            return respond(response, ResponseDto.failure(ResponseStatus.internal_error));
        }
    }
}
