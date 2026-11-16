package com.sk.rts.application.controller;

import com.sk.rts.application.component.validation.EnumOrdinals;
import com.sk.rts.application.dto.CaptchaVerifyDto;
import com.sk.rts.application.dto.ResponseDto;
import com.sk.rts.application.entity.enums.CaptchaType;
import com.sk.rts.application.service.CaptchaService;
import com.sk.rts.application.util.ReactiveUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Validated
@NullMarked
@RestController
@AllArgsConstructor
@RequestMapping("/open/captcha")
@Tag(name = "open-security", description = "安全相关开放接口")
public class OpenCaptchaController {

    private final CaptchaService captchaService;

    @Operation(summary = "获取验证码", parameters = {
            @Parameter(name = "type", required = true, description = "验证码类型，1：登录验证，2：账号绑定，3：密码设置，4：安全验证，5：账号解绑，6：账号注销"),
            @Parameter(name = "account", required = true, description = "账号，手机号/邮箱(手机号码必须带有国际电话区号，以英文冒号隔开，如86/0086/+86)"),
    })
    @RequestMapping(value = "/generate", method = RequestMethod.GET)
    public Mono<ResponseDto<String>> get(@NotNull @EnumOrdinals(value = CaptchaType.class, excludes = {"none"}) Integer type, @NotBlank String account) {
        return ReactiveUtil.toMono(captchaService.generate(type, account)).flatMap(ResponseDto::success);
    }

    @Operation(summary = "校验验证码")
    @RequestMapping(value = "/verify", method = RequestMethod.POST)
    public Mono<ResponseDto<String>> verify(@RequestBody @Valid CaptchaVerifyDto verifyDto) {
        return ReactiveUtil.toMono(captchaService.verify(verifyDto)).flatMap(ResponseDto::success);
    }
}
