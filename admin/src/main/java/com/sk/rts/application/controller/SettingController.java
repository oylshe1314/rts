package com.sk.rts.application.controller;

import com.sk.rts.application.auth.AdminAuthToken;
import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.ChangeDetailsDto;
import com.sk.rts.application.dto.ChangePasswordDto;
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
@RequestMapping("/setting")
@Tag(name = "设置", description = "设置相关接口")
public class SettingController {

    private final SettingService settingService;

    @Operation(summary = "修改详细信息")
    @RequestMapping(value = "/change/details", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> changeDetails(@RequestBody @Valid Mono<ChangeDetailsDto> changeDtoMono, AdminAuthToken authToken) throws Exception {
        return settingService.changeDetails(changeDtoMono, (AdminAuthDetails) authToken.getPrincipal()).thenReturn(ResponseDto.success());
    }

    @Operation(summary = "修改密码")
    @RequestMapping(value = "/change/password", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> changePassword(@RequestBody @Valid Mono<ChangePasswordDto> changeDtoMono, AdminAuthToken authToken) throws Exception {
        return settingService.changePassword(changeDtoMono, (AdminAuthDetails) authToken.getPrincipal()).thenReturn(ResponseDto.success());
    }
}
