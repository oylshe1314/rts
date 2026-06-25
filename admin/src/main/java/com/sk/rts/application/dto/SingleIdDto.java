package com.sk.rts.application.dto;

import com.sk.rts.application.dto.base.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "单个ID")
public class SingleIdDto extends BaseDto {
    public SingleIdDto(Long id) {
        super(id);
    }
}
