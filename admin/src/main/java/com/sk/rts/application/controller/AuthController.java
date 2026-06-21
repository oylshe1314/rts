package com.sk.rts.application.controller;

import com.sk.rts.application.dto.AdminDetailDto;
import com.sk.rts.application.dto.AdminLoginDto;
import com.sk.rts.application.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Validated
@NullMarked
@RestController
@RequestMapping("/auth")
@Tag(name = "auth", description = "认证相关接口")
public class AuthController {

    @Operation(summary = "管理员登录")
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public Mono<ResponseDto<AdminDetailDto>> login(@RequestBody @Valid Mono<AdminLoginDto> loginDto) {
        throw new IllegalStateException("这个接口仅用于文档生成，实际登录由过滤器处理");
    }

    @Operation(summary = "管理员登出")
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> logout(Authentication authentication) {
        throw new IllegalStateException("这个接口仅用于文档生成，实际登出由过滤器处理");
    }
}
