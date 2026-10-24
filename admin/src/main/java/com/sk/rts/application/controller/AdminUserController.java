package com.sk.rts.application.controller;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.auth.AdminAuthToken;
import com.sk.rts.application.dto.*;
import com.sk.rts.application.service.AdminRoleService;
import com.sk.rts.application.service.AdminUserService;
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

import java.util.Collection;

@Slf4j
@Validated
@NullMarked
@RestController
@AllArgsConstructor
@RequestMapping("/admin")
@Tag(name = "admin", description = "管理员相关接口")
public class AdminUserController {

    private final AdminRoleService adminRoleService;
    private final AdminUserService adminUserService;

    @Operation(summary = "角色查询", parameters = {
            @Parameter(name = "pageNo", required = true, description = "页码(从1开始)"),
            @Parameter(name = "pageSize", required = true, description = "单页数目(1-100)"),
            @Parameter(name = "sort", description = "排序字段"),
            @Parameter(name = "desc", description = "是否降序"),
            @Parameter(name = "name", description = "查询参数 - 名称", schema = @Schema(implementation = String.class)),
    })
    @RequestMapping(value = "/role/query", method = RequestMethod.GET)
    public Mono<ResponseDto<PageResultDto<AdminRoleDto>>> roleQuery(@NotNull @Positive Integer pageNo,
                                                                    @NotNull @Range(min = 10, max = 100) Integer pageSize,
                                                                    @Nullable @NullOrNotBlank String sort,
                                                                    @Nullable Boolean desc,
                                                                    @Parameter(hidden = true) @Nullable @Valid RoleQueryDto queryDto) {
        return roleQuery(new PageQueryDto<>(pageNo, pageSize, sort, desc, queryDto));
    }

    @Operation(summary = "角色查询")
    @RequestMapping(value = "/role/query", method = RequestMethod.POST)
    public Mono<ResponseDto<PageResultDto<AdminRoleDto>>> roleQuery(@RequestBody @Valid PageQueryDto<RoleQueryDto> pageQueryDto) {
        return adminRoleService.query(pageQueryDto).map(ResponseDto::success);
    }

    @Operation(summary = "角色添加")
    @RequestMapping(value = "/role/add", method = RequestMethod.POST)
    public Mono<ResponseDto<AdminRoleDto>> roleAdd(@RequestBody @Valid AdminRoleAddDto addDto, AdminAuthToken authToken) {
        return adminRoleService.add(addDto, (AdminAuthDetails) authToken.getPrincipal()).map(ResponseDto::success);
    }

    @Operation(summary = "角色修改")
    @RequestMapping(value = "/role/update", method = RequestMethod.POST)
    public Mono<ResponseDto<AdminRoleDto>> roleUpdate(@RequestBody @Valid RoleUpdateDto updateDto, AdminAuthToken authToken) {
        return adminRoleService.update(updateDto, (AdminAuthDetails) authToken.getPrincipal()).map(ResponseDto::success);
    }

    @Operation(summary = "角色删除")
    @RequestMapping(value = "/role/delete", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> roleDelete(@RequestBody @Valid MultipleIdDto deleteDto, AdminAuthToken authToken) {
        return adminRoleService.delete(deleteDto, (AdminAuthDetails) authToken.getPrincipal()).thenReturn(ResponseDto.success());
    }

    @Operation(summary = "角色状态修改")
    @RequestMapping(value = "/role/state/change", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> roleStateChange(@RequestBody @Valid StateChangeDto changeDto, AdminAuthToken authToken) {
        return adminRoleService.changeState(changeDto, (AdminAuthDetails) authToken.getPrincipal()).thenReturn(ResponseDto.success());
    }

    @Operation(summary = "角色权限查询", parameters = {
            @Parameter(name = "roleId", required = true, description = "角色ID")
    })
    @RequestMapping(value = "/role/authority/get", method = RequestMethod.GET)
    public Mono<ResponseDto<Collection<RoleAuthorityDto>>> roleAuthorityGet(@NotNull @Positive Long roleId) {
        return adminRoleService.getAuthorities(roleId).map(ResponseDto::success);
    }

    @Operation(summary = "角色权限设置")
    @RequestMapping(value = "/role/authority/set", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> roleAuthoritySet(@RequestBody @Valid RoleAuthoritySetDto setDto, AdminAuthToken authToken) {
        return adminRoleService.setAuthorities(setDto, (AdminAuthDetails) authToken.getPrincipal()).thenReturn(ResponseDto.success());
    }

    @Operation(summary = "角色权限对比")
    @RequestMapping(value = "/role/authority/compare", method = RequestMethod.POST)
    public Mono<ResponseDto<RoleAuthorityComparisonListDto>> roleAuthorityCompare(@RequestBody MultipleIdDto idsDto) {
        return adminRoleService.compareAuthorities(idsDto).map(ResponseDto::success);
    }

    @Operation(summary = "管理员查询", parameters = {
            @Parameter(name = "pageNo", required = true, description = "页码(从1开始)"),
            @Parameter(name = "pageSize", required = true, description = "单页数目(10-100)"),
            @Parameter(name = "sort", description = "排序字段"),
            @Parameter(name = "desc", description = "是否降序"),
            @Parameter(name = "roleId", description = "查询参数 - 角色ID(下拉框选择)", schema = @Schema(implementation = Long.class)),
            @Parameter(name = "username", description = "查询参数 - 用户名", schema = @Schema(implementation = String.class)),
            @Parameter(name = "phone", description = "查询参数 - 手机号", schema = @Schema(implementation = String.class)),
            @Parameter(name = "email", description = "查询参数 - 邮箱", schema = @Schema(implementation = String.class)),
    })
    @RequestMapping(value = "/query", method = RequestMethod.GET)
    public Mono<ResponseDto<PageResultDto<AdminUserDto>>> query(@NotNull @Positive Integer pageNo,
                                                                @NotNull @Range(min = 1, max = 100) Integer pageSize,
                                                                @Nullable @NullOrNotBlank String sort,
                                                                @Nullable Boolean desc,
                                                                @Parameter(hidden = true) @Nullable @Valid AdminQueryDto queryDto) {
        return query(new PageQueryDto<>(pageNo, pageSize, sort, desc, queryDto));
    }

    @Operation(summary = "管理员查询")
    @RequestMapping(value = "/query", method = RequestMethod.POST)
    public Mono<ResponseDto<PageResultDto<AdminUserDto>>> query(@RequestBody @Valid PageQueryDto<AdminQueryDto> pageQueryDto) {
        return adminUserService.query(pageQueryDto).map(ResponseDto::success);
    }

    @Operation(summary = "管理员添加")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public Mono<ResponseDto<AdminUserDto>> add(@RequestBody @Valid AdminAddDto addDto, AdminAuthToken authToken) {
        return adminUserService.add(addDto, (AdminAuthDetails) authToken.getPrincipal()).map(ResponseDto::success);
    }

    @Operation(summary = "管理员修改")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public Mono<ResponseDto<AdminUserDto>> update(@RequestBody @Valid AdminUpdateDto updateDto, AdminAuthToken authToken) {
        return adminUserService.update(updateDto, (AdminAuthDetails) authToken.getPrincipal()).map(ResponseDto::success);
    }

    @Operation(summary = "管理员删除")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> delete(@RequestBody @Valid MultipleIdDto deleteDto, AdminAuthToken authToken) {
        return adminUserService.delete(deleteDto, (AdminAuthDetails) authToken.getPrincipal()).thenReturn(ResponseDto.success());
    }

    @Operation(summary = "管理员状态修改")
    @RequestMapping(value = "/state/change", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> stateChange(@RequestBody @Valid StateChangeDto changeDto, AdminAuthToken authToken) {
        return adminUserService.changeState(changeDto, (AdminAuthDetails) authToken.getPrincipal()).thenReturn(ResponseDto.success());
    }
}
