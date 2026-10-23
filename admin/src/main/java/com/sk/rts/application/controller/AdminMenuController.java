package com.sk.rts.application.controller;

import com.sk.rts.application.auth.AdminAuthToken;
import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.*;
import com.sk.rts.application.service.AdminMenuService;
import com.sk.rts.application.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Range;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
@RequestMapping("/menu")
@Tag(name = "menu", description = "菜单相关接口")
public class AdminMenuController {

    private final AdminMenuService menuService;

    @Operation(summary = "菜单查询", parameters = {
            @Parameter(name = "pageNo", required = true, description = "页码(从1开始)"),
            @Parameter(name = "pageSize", required = true, description = "单页数目(1-100)"),
            @Parameter(name = "sort", description = "排序字段"),
            @Parameter(name = "desc", description = "是否降序"),
            @Parameter(name = "type", description = "查询参数 - 类型(1.目录, 2.菜单, 3.接口)", schema = @Schema(implementation = Integer.class)),
            @Parameter(name = "parentName", description = "查询参数 - 上级菜单名称", schema = @Schema(implementation = String.class)),
            @Parameter(name = "name", description = "查询参数 - 名称", schema = @Schema(implementation = String.class)),
            @Parameter(name = "path", description = "查询参数 - 路径", schema = @Schema(implementation = String.class)),
    })
    @RequestMapping(value = "/query", method = RequestMethod.GET)
    public Mono<ResponseDto<PageResultDto<AdminMenuDto>>> query(@NotNull @Positive Integer pageNo,
                                                                @NotNull @Range(min = 1, max = 100) Integer pageSize,
                                                                @Nullable @NullOrNotBlank String sort,
                                                                @Nullable Boolean desc,
                                                                @Parameter(hidden = true) @Nullable @Valid MenuQueryDto queryDto) {
        return query(new PageQueryDto<>(pageNo, pageSize, sort, desc, queryDto));
    }

    @Operation(summary = "菜单查询")
    @RequestMapping(value = "/query", method = RequestMethod.POST)
    public Mono<ResponseDto<PageResultDto<AdminMenuDto>>> query(@RequestBody @Valid PageQueryDto<MenuQueryDto> pageQueryDto) {
        return menuService.query(pageQueryDto).map(ResponseDto::success);
    }

    @Operation(summary = "菜单添加")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public Mono<ResponseDto<AdminMenuDto>> add(@RequestBody @Valid AdminMenuAddDto addDto, AdminAuthToken authToken) {
        return menuService.add(addDto, (AdminAuthDetails) authToken.getPrincipal()).map(ResponseDto::success);
    }

    @Operation(summary = "菜单修改")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public Mono<ResponseDto<AdminMenuDto>> update(@RequestBody @Valid AdminMenuUpdateDto updateDto, AdminAuthToken authToken) {
        return menuService.update(updateDto, (AdminAuthDetails) authToken.getPrincipal()).map(ResponseDto::success);
    }

    @Operation(summary = "菜单删除")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> delete(@RequestBody @Valid MultipleIdDto deleteDto, AdminAuthToken authToken) {
        return menuService.delete(deleteDto, (AdminAuthDetails) authToken.getPrincipal()).thenReturn(ResponseDto.success());
    }

    @Operation(summary = "菜单状态修改")
    @RequestMapping(value = "/state/change", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> stateChange(@RequestBody @Valid StateChangeDto changeDto, AdminAuthToken authToken) {
        return menuService.changeState(changeDto, (AdminAuthDetails) authToken.getPrincipal()).thenReturn(ResponseDto.success());
    }
}
