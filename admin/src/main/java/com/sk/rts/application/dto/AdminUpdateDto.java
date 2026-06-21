package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.dto.base.BaseDto;
import com.sk.rts.application.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
@Schema(description = "管理员修改")
public class AdminUpdateDto extends BaseDto {

    @Positive
    @Schema(description = "角色ID")
    private final Long roleId;

    @NullOrNotBlank
    @Schema(description = "密码")
    private final String password;

    @Schema(description = "手机")
    private final String phone;

    @Schema(description = "邮箱")
    private final String email;

    @NullOrNotBlank
    @Schema(description = "昵称")
    private final String nickname;

    @Schema(description = "头像")
    private final String avatar;

    @Schema(description = "备注")
    private final String remark;

    public AdminUpdateDto(@JsonProperty("id") Long id,
                          @JsonProperty("roleId") Long roleId,
                          @JsonProperty("password") String password,
                          @JsonProperty("account") String phone,
                          @JsonProperty("email") String email,
                          @JsonProperty("nickname") String nickname,
                          @JsonProperty("avatar") String avatar,
                          @JsonProperty("remark") String remark) {
        super(id);
        this.roleId = roleId;
        this.password = password;
        this.phone = phone;
        this.email = email;
        this.nickname = nickname;
        this.avatar = avatar;
        this.remark = remark;
    }
}
