package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "多个ID")
public class MultipleIdDto {

    @NotEmpty
    @Schema(description = "ID列表")
    private final List<Long> ids;

    public MultipleIdDto(@JsonProperty("ids") List<Long> ids) {
        this.ids = ids;
    }
}
