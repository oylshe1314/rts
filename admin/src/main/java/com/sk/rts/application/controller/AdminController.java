package com.sk.rts.application.controller;

import com.sk.rts.application.auth.AdminAuthToken;
import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.*;
import com.sk.rts.application.service.AdminService;
import com.sk.rts.application.service.RoleService;
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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Slf4j
@Validated
@NullMarked
@RestController
@AllArgsConstructor
@RequestMapping("/admin")
@Tag(name = "admin", description = "管理员相关接口")
public class AdminController {

    private final RoleService roleService;
    private final AdminService adminService;

    @Operation(summary = "角色查询", parameters = {
            @Parameter(name = "pageNo", required = true, description = "页码(从1开始)"),
            @Parameter(name = "pageSize", required = true, description = "单页数目(1-100)"),
            @Parameter(name = "sort", description = "排序字段"),
            @Parameter(name = "desc", description = "是否降序"),
            @Parameter(name = "name", description = "查询参数 - 名称", schema = @Schema(implementation = String.class)),
    })
    @RequestMapping(value = "/role/query", method = RequestMethod.GET)
    public Mono<ResponseDto<PageResultDto<RoleDto>>> roleQuery(@NotNull @Positive Integer pageNo,
                                                               @NotNull @Range(min = 10, max = 100) Integer pageSize,
                                                               @Nullable @NullOrNotBlank String sort,
                                                               @Nullable Boolean desc,
                                                               @Parameter(hidden = true) @Nullable @Valid RoleQueryDto queryDto) {
        return roleQuery(Mono.just(new PageQueryDto<>(pageNo, pageSize, sort, desc, queryDto)));
    }

    @Operation(summary = "角色查询")
    @RequestMapping(value = "/role/query", method = RequestMethod.POST)
    public Mono<ResponseDto<PageResultDto<RoleDto>>> roleQuery(@RequestBody @Valid Mono<PageQueryDto<RoleQueryDto>> requestDtoMono) {
        return roleService.query(requestDtoMono).map(ResponseDto::success);
    }

    @Operation(summary = "角色添加")
    @RequestMapping(value = "/role/add", method = RequestMethod.POST)
    public Mono<ResponseDto<RoleDto>> roleAdd(@RequestBody @Valid Mono<RoleAddDto> addDtoMono, AdminAuthToken authToken) {
        return roleService.add(addDtoMono, (AdminAuthDetails) authToken.getPrincipal()).map(ResponseDto::success);
    }

    @Operation(summary = "角色修改")
    @RequestMapping(value = "/role/update", method = RequestMethod.POST)
    public Mono<ResponseDto<RoleDto>> roleUpdate(@RequestBody @Valid Mono<RoleUpdateDto> updateDtoMono, AdminAuthToken authToken) {
        return roleService.update(updateDtoMono, (AdminAuthDetails) authToken.getPrincipal()).map(ResponseDto::success);
    }

    @Operation(summary = "角色删除")
    @RequestMapping(value = "/role/delete", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> roleDelete(@RequestBody @Valid Mono<MultipleIdDto> deleteDtoMono, AdminAuthToken authToken) {
        return roleService.delete(deleteDtoMono, (AdminAuthDetails) authToken.getPrincipal()).thenReturn(ResponseDto.success());
    }

    @Operation(summary = "角色状态修改")
    @RequestMapping(value = "/role/change/state", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> roleChangeState(@RequestBody @Valid Mono<ChangeStateDto> changeDtoMono, AdminAuthToken authToken) {
        return roleService.changeState(changeDtoMono, (AdminAuthDetails) authToken.getPrincipal()).thenReturn(ResponseDto.success());
    }

    @Operation(summary = "角色权限查询", parameters = {
            @Parameter(name = "roleId", required = true, description = "角色ID")
    })
    @RequestMapping(value = "/role/authority/get", method = RequestMethod.GET)
    public Mono<ResponseDto<Collection<RoleMenuAuthorityDto>>> roleAuthorityGet(@NotNull @Positive Long roleId) {
        return roleService.getAuthorities(roleId).map(ResponseDto::success);
    }

    @Operation(summary = "角色权限设置")
    @RequestMapping(value = "/role/authority/set", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> roleAuthoritySet(@RequestBody @Valid Mono<RoleMenuAuthoritySetDto> setDtoMono, AdminAuthToken authToken) {
        return roleService.setAuthorities(setDtoMono, (AdminAuthDetails) authToken.getPrincipal()).thenReturn(ResponseDto.success());
    }

    @Operation(summary = "角色权限对比")
    @RequestMapping(value = "/role/authority/compare", method = RequestMethod.POST)
    public Mono<ResponseDto<Collection<RoleMenuAuthorityComparisonDto>>> roleAuthorityCompare(@RequestBody Mono<MultipleIdDto> idsDtoMono) {
        return roleService.compareAuthorities(idsDtoMono).map(ResponseDto::success);
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
    public Mono<ResponseDto<PageResultDto<AdminDto>>> query(@NotNull @Positive Integer pageNo,
                                                            @NotNull @Range(min = 1, max = 100) Integer pageSize,
                                                            @Nullable @NullOrNotBlank String sort,
                                                            @Nullable Boolean desc,
                                                            @Parameter(hidden = true) @Nullable @Valid AdminQueryDto queryDto) {
        return query(Mono.just(new PageQueryDto<>(pageNo, pageSize, sort, desc, queryDto)));
    }

    @Operation(summary = "管理员查询")
    @RequestMapping(value = "/query", method = RequestMethod.POST)
    public Mono<ResponseDto<PageResultDto<AdminDto>>> query(@RequestBody @Valid Mono<PageQueryDto<AdminQueryDto>> pageRequestDtoMono) {
        return adminService.query(pageRequestDtoMono).map(ResponseDto::success);
    }

    @Operation(summary = "管理员添加")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public Mono<ResponseDto<AdminDto>> add(@RequestBody @Valid Mono<AdminAddDto> addDtoMono, AdminAuthToken authToken) {
        return adminService.add(addDtoMono, (AdminAuthDetails) authToken.getPrincipal()).map(ResponseDto::success);
    }

    @Operation(summary = "管理员修改")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public Mono<ResponseDto<AdminDto>> update(@RequestBody @Valid Mono<AdminUpdateDto> updateDtoMono, AdminAuthToken authToken) {
        return adminService.update(updateDtoMono, (AdminAuthDetails) authToken.getPrincipal()).map(ResponseDto::success);
    }

    @Operation(summary = "管理员删除")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> delete(@RequestBody @Valid Mono<MultipleIdDto> deleteDtoMono, AdminAuthToken authToken) {
        return adminService.delete(deleteDtoMono, (AdminAuthDetails) authToken.getPrincipal()).thenReturn(ResponseDto.success());
    }

    @Operation(summary = "管理员状态修改")
    @RequestMapping(value = "/change/state", method = RequestMethod.POST)
    public Mono<ResponseDto<?>> changeState(@RequestBody @Valid Mono<ChangeStateDto> changeDtoMono, AdminAuthToken authToken) {
        return adminService.changeState(changeDtoMono, (AdminAuthDetails) authToken.getPrincipal()).thenReturn(ResponseDto.success());
    }
}
