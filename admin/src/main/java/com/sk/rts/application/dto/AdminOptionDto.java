package com.sk.rts.application.dto;

import com.sk.rts.application.dto.base.BaseDto;
import com.sk.rts.application.entity.Admin;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "角色选择列表")
public class AdminOptionDto extends BaseDto {

    @Schema(description = "角色名称")
    private final Long roleId;

    @Schema(description = "角色名称")
    private final String roleName;

    @Schema(description = "用户名")
    private final String username;

    @Schema(description = "昵称")
    private final String nickname;

    public AdminOptionDto(Long id, Long roleId, String roleName, String username, String nickname, String avatar) {
        super(id);
        this.roleId = roleId;
        this.roleName = roleName;
        this.username = username;
        this.nickname = nickname;
    }

    public AdminOptionDto(Admin admin) {
        super(admin.getId());
        this.roleId = admin.getRoleId();
        this.roleName = admin.getRole().getName();
        this.username = admin.getUsername();
        this.nickname = admin.getNickname();
    }
}
