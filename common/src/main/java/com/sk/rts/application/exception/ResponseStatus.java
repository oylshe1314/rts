package com.sk.rts.application.exception;

import lombok.Getter;

@Getter
public enum ResponseStatus implements StatusException {

    //成功
    success(200, "request.successful", "成功"),

    //客户端错误
    failure(4000, "request.failed", "请求失败"),
    bad_request(4001, "request.bad-request", "无效请求"),
    repeated_request(4002, "request.repeated", "请勿重复请求"),
    method_error(4003, "request.method.error", "方法错误"),
    content_type_error(4004, "request.content-type.error", "内容类型错误"),
    parameter_error(4005, "request.parameter.error", "参数错误"),

    not_logged_in(4006, "user.not-logged-in", "请先登录"),
    access_denied(4007, "user.access.denied", "权限不足"),
    token_invalid(4008, "user.token.invalid", "无效的TOKEN"),
    token_expired(4009, "user.token.expired", "TOKEN已过期"),
    need_authenticate(4010, "user.need.authenticate", "需要安全验证"),

    //服务端错误
    internal_error(5000, "server.internal.error", "内部错误"),
    unknown_error(5001, "server.unknown.error", "未知错误"),
    ;

    private final int status;

    private final String code;

    private final String message;

    ResponseStatus(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public static ResponseStatus get(int status) {
        for (ResponseStatus value : ResponseStatus.values()) {
            if (value.getStatus() == status) {
                return value;
            }
        }
        return ResponseStatus.unknown_error;
    }
}
