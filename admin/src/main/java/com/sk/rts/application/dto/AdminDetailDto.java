package com.sk.rts.application.dto;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.dto.base.BaseDto;
import com.sk.rts.application.proto.caching.MsgAdminDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "管理员详细信息")
public class AdminDetailDto extends BaseDto {

    @Schema(description = "角色名称")
    private final String roleName;

    @Schema(description = "用户名")
    private final String username;

    @Schema(description = "手机号")
    private final String phone;

    @Schema(description = "邮箱")
    private final String email;

    @Schema(description = "昵称")
    private final String nickname;

    @Schema(description = "头像")
    private final String avatar;

    public AdminDetailDto(AdminAuthDetails authDetails) {
        super(authDetails.getId());
        this.roleName = authDetails.getRoleName();
        this.username = authDetails.getUsername();
        this.phone = authDetails.getPhone();
        this.email = authDetails.getEmail();
        this.nickname = authDetails.getNickname();
        this.avatar = authDetails.getAvatar();
    }
}
