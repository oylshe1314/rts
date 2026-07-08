package com.sk.rts.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "角色权限对比列表")
public class RoleAuthorityComparisonListDto {

    @Schema(description = "角色列表")
    private final List<RoleOptionDto> roleList;

    @Schema(description = "权限对比列表")
    private final List<RoleAuthorityComparisonDto> comparisonList;

    public RoleAuthorityComparisonListDto(List<RoleOptionDto> roleList, List<RoleAuthorityComparisonDto> comparisonList) {
        this.roleList = roleList;
        this.comparisonList = comparisonList;
    }
}
