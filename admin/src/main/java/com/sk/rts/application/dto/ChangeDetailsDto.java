package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "详细信息修改")
public class ChangeDetailsDto {

    @Schema(description = "手机")
    private final String phone;

    @Schema(description = "邮箱")
    private final String email;

    @NullOrNotBlank
    @Schema(description = "昵称")
    private final String nickname;

    @NullOrNotBlank
    @Schema(description = "头像")
    private final String avatar;

    public ChangeDetailsDto(
            @JsonProperty("account") String phone,
            @JsonProperty("email") String email,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("avatar") String avatar) {
        this.email = email;
        this.phone = phone;
        this.nickname = nickname;
        this.avatar = avatar;
    }
}
