package com.sk.rts.application.dto;

import com.sk.rts.application.dto.base.BaseDto;
import com.sk.rts.application.entity.Admin;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "角色选择列表")
public class AdminSelectDto extends BaseDto {

    @Schema(description = "角色ID")
    private final Long roleId;

    @Schema(description = "用户名")
    private final String username;

    @Schema(description = "昵称")
    private final String nickname;

    @Schema(description = "头像")
    private final String avatar;

    public AdminSelectDto(Long id, Long roleId, String username, String nickname, String avatar) {
        super(id);
        this.roleId = roleId;
        this.username = username;
        this.nickname = nickname;
        this.avatar = avatar;
    }

    public AdminSelectDto(Admin admin) {
        super(admin.getId());
        this.roleId = admin.getRoleId();
        this.username = admin.getUsername();
        this.nickname = admin.getNickname();
        this.avatar = admin.getAvatar();
    }
}
