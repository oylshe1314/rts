package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.component.message.MessageUtil;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StatusException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

@Getter
@NullMarked
@Schema(description = "公共响应实体")
public class ResponseDto<T> {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "响应状态， 200: 成功")
    private final int status;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "响应信息")
    private final String message;

    @Nullable
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "响应数据")
    private final T data;

    @JsonCreator
    public ResponseDto(@JsonProperty("status") int status,
                       @JsonProperty("message") String message,
                       @JsonProperty("data") @Nullable T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static Mono<ResponseDto<Void>> success() {
        return MessageUtil.getMessage(ResponseStatus.success).map(message -> new ResponseDto<>(ResponseStatus.success.getStatus(), message, null));
    }

    public static <T> Mono<ResponseDto<T>> success(T data) {
        return MessageUtil.getMessage(ResponseStatus.success).map(message -> new ResponseDto<>(ResponseStatus.success.getStatus(), message, data));
    }

    public static Mono<ResponseDto<Void>> failure(int status, String message) {
        return Mono.just(new ResponseDto<>(status, message, null));
    }

    public static <T> Mono<ResponseDto<T>> failure(int status, String message, T data) {
        return Mono.just(new ResponseDto<>(status, message, data));
    }

    public static Mono<ResponseDto<Void>> failure(StatusException statusException) {
        return MessageUtil.getMessage(statusException).map(message -> new ResponseDto<>(statusException.getStatus(), message, null));
    }

    public static <T> Mono<ResponseDto<T>> failure(StatusException statusException, T data) {
        return MessageUtil.getMessage(statusException).map(message -> new ResponseDto<>(statusException.getStatus(), message, data));
    }
}
