package com.sk.rts.application.controller;

import com.sk.rts.application.auth.UserAuthToken;
import com.sk.rts.application.dto.CaptchaLoginDto;
import com.sk.rts.application.dto.PasswordLoginDto;
import com.sk.rts.application.dto.ResponseDto;
import com.sk.rts.application.dto.UserTokenDto;
import com.sk.rts.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Validated
@NullMarked
@RestController
@AllArgsConstructor
@RequestMapping("/auth")
@Tag(name = "auth", description = "认证相关接口")
public class AuthController {

    private final AuthService authService;

//    @Operation(summary = "获取账号状态", description = "账号状态，0：未注册，1：未设置密码，2：已设置密码", parameters = {
//            @Parameter(name = "account", required = true, description = "账号"),
//            @Parameter(name = "accountType", required = false, description = "账号类型，0：用户名，1：手机号，2：邮箱"),
//    })
//    @RequestMapping(value = "/account/status", method = RequestMethod.GET)
//    public Mono<ResponseDto<Integer>> accountStatus(@NotBlank String account, @RequestParam(required = false) @EnumOrdinals(AccountType.class) Integer accountType) {
//        return authService.accountStatus(account, accountType).flatMap(ResponseDto::success);
//    }
//
//    @Operation(summary = "账号创建", parameters = {
//            @Parameter(name = "X-Caller-Platform", required = true, in = ParameterIn.HEADER, description = "设备平台, 值：web/windows/ios/macos/android/linux"),
//            @Parameter(name = "X-Caller-Device-Id", required = true, in = ParameterIn.HEADER, description = "设备UUID"),
//    })
//    @RequestMapping(value = "/account/create", method = RequestMethod.POST)
//    public Mono<ResponseDto<Void>> accountStatus(@RequestBody @Valid AccountCreateDto createDto, ServerWebExchange webExchange) {
//        return authService.accountCreate(createDto, new UserRemoteDetails(webExchange)).then(ResponseDto.success());
//    }

    @Operation(summary = "用户密码登录", parameters = {
            @Parameter(name = "X-Caller-Platform", required = true, in = ParameterIn.HEADER, description = "设备平台, 值：web/windows/ios/macos/android/linux"),
            @Parameter(name = "X-Caller-Device-Id", required = true, in = ParameterIn.HEADER, description = "设备UUID"),
    })
    @RequestMapping(value = "/login/password", method = RequestMethod.POST)
    public Mono<ResponseDto<UserTokenDto>> passwordLogin(@RequestBody @Valid PasswordLoginDto loginDto) {
        return Mono.error(new IllegalStateException("这个接口仅用于文档生成，实际登录由管理器处理"));
    }

    @Operation(summary = "用户验证码登录", parameters = {
            @Parameter(name = "X-Caller-Platform", required = true, in = ParameterIn.HEADER, description = "设备平台, 值：web/windows/ios/macos/android/linux"),
            @Parameter(name = "X-Caller-Device-Id", required = true, in = ParameterIn.HEADER, description = "设备UUID"),
    })
    @RequestMapping(value = "/login/captcha", method = RequestMethod.POST)
    public Mono<ResponseDto<UserTokenDto>> captchaLogin(@RequestBody @Valid CaptchaLoginDto loginDto) {
        return Mono.error(new IllegalStateException("这个接口仅用于文档生成，实际登录由管理器处理"));
    }

    @Operation(summary = "用户TOKEN刷新", parameters = {
            @Parameter(name = "X-Caller-Platform", required = true, in = ParameterIn.HEADER, description = "设备平台, 值：web/windows/ios/macos/android/linux"),
            @Parameter(name = "X-Caller-Device-Id", required = true, in = ParameterIn.HEADER, description = "设备UUID"),
    })
    @RequestMapping(value = "/token/refresh", method = RequestMethod.GET)
    public Mono<ResponseDto<UserTokenDto>> refreshToken(String subject, String token) {
        return Mono.error(new IllegalStateException("这个接口仅用于文档生成，实际登录由管理器处理"));
    }

    @Operation(summary = "用户登出")
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public Mono<ResponseDto<Void>> logout(UserAuthToken authToken) {
        return Mono.error(new IllegalStateException("这个接口仅用于文档生成，实际登录由管理器处理"));
    }
}
