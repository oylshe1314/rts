package com.sk.rts.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "角色权限对比列表")
public class AdminRoleAuthorityComparisonListDto {

    @Schema(description = "角色列表")
    private final List<AdminRoleOptionDto> roleList;

    @Schema(description = "权限对比列表")
    private final List<AdminRoleAuthorityComparisonDtoAdmin> comparisonList;

    public AdminRoleAuthorityComparisonListDto(List<AdminRoleOptionDto> roleList, List<AdminRoleAuthorityComparisonDtoAdmin> comparisonList) {
        this.roleList = roleList;
        this.comparisonList = comparisonList;
    }
}
