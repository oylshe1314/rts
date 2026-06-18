package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.component.MessageUtil;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StatusException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "公共响应实体")
public class ResponseDto<T> {

    @Schema(description = "响应状态， 200: 成功")
    private final int status;

    @Schema(description = "响应信息")
    private final String message;

    @Schema(description = "响应数据")
    private final T data;

    @JsonCreator
    public ResponseDto(@JsonProperty("status") int status,
                       @JsonProperty("message") String message,
                       @JsonProperty("data") T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static ResponseDto<?> success() {
        return success(null);
    }

    public static <T> ResponseDto<T> success(T data) {
        return new ResponseDto<>(ResponseStatus.success.getStatus(), MessageUtil.getMessage(ResponseStatus.success), data);
    }

    public static ResponseDto<?> failure(int status, String message) {
        return failure(status, message, null);
    }

    public static <T> ResponseDto<T> failure(int status, String message, T data) {
        return new ResponseDto<>(status, message, data);
    }

    public static ResponseDto<?> failure(StatusException statusException) {
        return failure(statusException, null);
    }

    public static <T>  ResponseDto<T> failure(StatusException statusException, T data) {
        return new ResponseDto<>(statusException.getStatus(), MessageUtil.getMessage(statusException), data);
    }
}
