package com.sk.rts.application.controller;

import com.sk.rts.application.auth.AdminAuthToken;
import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.AdminUserDetailsChangeDto;
import com.sk.rts.application.dto.PasswordChangeDto;
import com.sk.rts.application.dto.ResponseDto;
import com.sk.rts.application.service.SettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@Validated
@NullMarked
@RestController
@AllArgsConstructor
@RequestMapping("/admin/setting")
@Tag(name = "设置", description = "设置相关接口")
public class SettingController {

    private final SettingService settingService;

    @Operation(summary = "详细信息修改")
    @RequestMapping(value = "/details/change", method = RequestMethod.POST)
    public Mono<ResponseDto<Void>> changeDetails(@RequestBody @Valid AdminUserDetailsChangeDto changeDto, AdminAuthToken authToken) {
        return settingService.changeDetails(changeDto, (AdminAuthDetails) authToken.getPrincipal()).then(ResponseDto.success());
    }

    @Operation(summary = "密码修改")
    @RequestMapping(value = "/password/change", method = RequestMethod.POST)
    public Mono<ResponseDto<Void>> changePassword(@RequestBody @Valid PasswordChangeDto changeDto, AdminAuthToken authToken) {
        return settingService.changePassword(changeDto, (AdminAuthDetails) authToken.getPrincipal()).then(ResponseDto.success());
    }
}
