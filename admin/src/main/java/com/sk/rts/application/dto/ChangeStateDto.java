package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.validation.Integers;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "通用状态修改")
public class ChangeStateDto extends MultipleIdDto {

    @NotNull
    @Integers({0, 1})
    @Schema(description = "状态")
    private final Integer state;

    public ChangeStateDto(@JsonProperty("ids") List<Long> ids, @JsonProperty("state") Integer state) {
        super(ids);
        this.state = state;
    }
}
