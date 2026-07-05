package com.sk.rts.application.controller;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.auth.AdminAuthToken;
import com.sk.rts.application.dto.*;
import com.sk.rts.application.service.AdminService;
import com.sk.rts.application.service.MenuService;
import com.sk.rts.application.service.RoleService;
import com.sk.rts.application.validation.Integers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Slf4j
@Validated
@NullMarked
@RestController
@AllArgsConstructor
@RequestMapping("/common")
@Tag(name = "公共", description = "公共接口")
public class CommonController {

    private final MenuService menuService;
    private final RoleService roleService;
    private final AdminService adminService;

    @Operation(summary = "管理员详情")
    @RequestMapping(value = "/admin/details", method = RequestMethod.GET)
    public Mono<ResponseDto<AdminDetailDto>> adminDetails(AdminAuthToken authToken) {
        AdminAuthDetails authDetails = (AdminAuthDetails) authToken.getPrincipal();
        return Mono.just(ResponseDto.success(new AdminDetailDto(authDetails)));
    }

    @Operation(summary = "角色菜单")
    @RequestMapping(value = "/role/menus", method = RequestMethod.GET)
    public Mono<ResponseDto<Collection<RoleMenuDto>>> roleMenus(AdminAuthToken authToken) {
        return roleService.roleMenus((AdminAuthDetails) authToken.getPrincipal()).map(ResponseDto::success);
    }

    @Operation(summary = "菜单选择列表", parameters = {
            @Parameter(name = "type", required = true, description = "菜单类型")
    })
    @RequestMapping(value = "/options/menus", method = RequestMethod.GET)
    public Mono<ResponseDto<Collection<MenuOptionDto>>> selectMenus(@NotNull @Integers({1, 2}) Integer type) {
        return menuService.menuSelectList(type).map(ResponseDto::success);
    }

    @Operation(summary = "角色选择列表")
    @RequestMapping(value = "/options/roles", method = RequestMethod.GET)
    public Mono<ResponseDto<Collection<RoleOptionDto>>> selectRoles() {
        return roleService.roleSelectList().map(ResponseDto::success);
    }

    @Operation(summary = "管理员选择列表", parameters = {
            @Parameter(name = "roleId", description = "角色ID")
    })
    @RequestMapping(value = "/options/admins", method = RequestMethod.GET)
    public Mono<ResponseDto<Collection<AdminOptionDto>>> selectRoles(@RequestParam(required = false) @Nullable @Positive Long roleId) {
        return adminService.adminSelectList(roleId).map(ResponseDto::success);
    }
}
