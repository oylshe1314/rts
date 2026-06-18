package com.sk.rts.application.controller;

import com.sk.rts.application.dto.ResponseDto;
import com.sk.rts.application.exception.HttpStatusException;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StatusException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Hidden
@NullMarked
@RestController
@ControllerAdvice
@AllArgsConstructor
public class ExceptionController implements ErrorWebExceptionHandler {

    private final JsonMapper jsonMapper;

    protected Mono<Void> respond(ServerHttpResponse response, ResponseDto<?> responseDto) {
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return response.writeWith(Mono.just(responseDto).map(dto -> response.bufferFactory().wrap(jsonMapper.writeValueAsBytes(dto))));
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable cause) {
        return handlerException(exchange, (Exception) cause).flatMap(responseDto -> respond(exchange.getResponse(), responseDto));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseDto<?>> handlerException(ServerWebExchange webExchange, Exception exception) {
        StatusException statusException = StatusException.extractStatusException(exception);
        if (statusException != null) {
            log.info("异常响应, status: {}, message: {}", statusException.getStatus(), statusException.getMessage());
            return Mono.just(ResponseDto.failure(statusException));
        } else {
            if (exception instanceof ErrorResponse errorResponse) {
                HttpStatus httpStatus = (HttpStatus) errorResponse.getStatusCode();
                log.info("错误响应, status={}, reason={}", httpStatus.value(), httpStatus.getReasonPhrase());
                webExchange.getResponse().setStatusCode(httpStatus);
                return Mono.just(ResponseDto.failure(httpStatus.value(), httpStatus.getReasonPhrase()));
            } else {
                log.error("服务器内部异常", exception);
                return Mono.just(ResponseDto.failure(ResponseStatus.internal_error));
            }
        }
    }

    @ExceptionHandler({HttpStatusException.class})
    public Mono<ResponseDto<?>> handlerException(ServerWebExchange webExchange, HttpStatusException exception) {
        log.warn("HTTP状态异常， status: {}, message: {}", exception.getStatus(), exception.getMessage());
        webExchange.getResponse().setStatusCode(HttpStatus.valueOf(exception.getStatus()));
        return Mono.just(ResponseDto.failure(exception));
    }

    @ExceptionHandler({MethodNotAllowedException.class})
    public Mono<ResponseDto<?>> handlerException(MethodNotAllowedException exception) {
        String message = String.format("不支持的方法: %s", exception.getHttpMethod());
        Set<HttpMethod> supportedMethods = exception.getSupportedMethods();
        if (!supportedMethods.isEmpty()) {
            if (supportedMethods.size() == 1) {
                message += String.format(", 需要: %s", supportedMethods.stream().findFirst().get().name());
            } else {
                message += String.format(", 需要: %s", supportedMethods.stream().map(HttpMethod::name).collect(Collectors.joining(", ", "[", "]")));
            }
        }
        log.warn("请求方法异常, {}", message);
        return Mono.just(ResponseDto.failure(ResponseStatus.method_error));
    }

    @ExceptionHandler({MissingRequestValueException.class})
    public Mono<ResponseDto<?>> handlerException(MissingRequestValueException exception) {
        String message = String.format("缺少参数: %s, 类型: %s", exception.getName(), exception.getType());
        log.warn("参数缺失错误, {}", message);
        return Mono.just(ResponseDto.failure(ResponseStatus.parameter_error));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public Mono<ResponseDto<?>> handlerException(MethodArgumentTypeMismatchException exception) {
        String message = String.format("参数: %s, 类型错误", exception.getName());
        Class<?> requiredType = exception.getRequiredType();
        if (requiredType != null) {
            message += String.format(", 需要: %s", requiredType.getSimpleName());
        }
        log.warn("参数类型错误, {}", message);
        return Mono.just(ResponseDto.failure(ResponseStatus.parameter_error));
    }

    @ExceptionHandler({WebExchangeBindException.class})
    public Mono<ResponseDto<?>> handlerException(WebExchangeBindException exception) {
        List<String> messages = new ArrayList<>();
        for (FieldError fieldError : exception.getFieldErrors()) {
            messages.add(String.format("参数: %s, %s", fieldError.getField(), fieldError.getDefaultMessage()));
        }
        String message = String.join(", ", messages);
        log.warn("参数绑定错误, {}", message);
        return Mono.just(ResponseDto.failure(ResponseStatus.parameter_error));
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public Mono<ResponseDto<?>> handlerException(ConstraintViolationException exception) {
        List<String> messages = new ArrayList<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            Path.Node paramNode = null;
            for (Path.Node node : violation.getPropertyPath()) {
                if (node.getKind() == ElementKind.PARAMETER) {
                    paramNode = node;
                    break;
                }
            }
            if (paramNode != null) {
                messages.add(String.format("参数: %s, %s", paramNode.getName(), violation.getMessage()));
            } else {
                messages.add(violation.getMessage());
            }
        }
        String message = String.join(", ", messages);
        log.warn("参数校验错误, {}", message);
        return Mono.just(ResponseDto.failure(ResponseStatus.parameter_error));
    }
}
