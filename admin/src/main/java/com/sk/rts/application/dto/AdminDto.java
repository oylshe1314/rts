package com.sk.rts.application.dto;

import com.sk.rts.application.dto.base.StatefulDto;
import com.sk.rts.application.entity.Admin;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "管理员")
public class AdminDto extends StatefulDto {

    @Schema(description = "角色ID")
    private final Long roleId;

    @Schema(description = "用户名")
    private final String roleName;

    @Schema(description = "用户名")
    private final String username;

    @Schema(description = "密码")
    private final String nickname;

    @Schema(description = "昵称")
    private final String avatar;

    @Schema(description = "头像")
    private final String email;

    @Schema(description = "邮箱")
    private final String phone;

    public AdminDto(Admin admin) {
        super(admin.getId(), admin.getRemark(), admin.getUpdateBy(), admin.getUpdateTime(), admin.getState());
        this.roleId = admin.getRoleId();
        this.roleName = admin.getRole().getName();
        this.username = admin.getUsername();
        this.phone = admin.getPhone();
        this.email = admin.getEmail();
        this.nickname = admin.getNickname();
        this.avatar = admin.getAvatar();
    }
}
