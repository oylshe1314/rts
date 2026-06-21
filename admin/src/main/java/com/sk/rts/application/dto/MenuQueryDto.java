package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.validation.Integers;
import com.sk.rts.application.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "菜单查询")
public class MenuQueryDto {

    @NullOrNotBlank
    @Schema(description = "上级菜单名称")
    private final String parentName;

    @Integers({1, 2, 3})
    @Schema(description = "类型, 1.目录, 2.菜单, 3.接口")
    private final Integer type;

    @NullOrNotBlank
    @Schema(description = "名称")
    private final String name;

    @NullOrNotBlank
    @Schema(description = "路径")
    private final String path;

    public MenuQueryDto(@JsonProperty("parentName") String parentName,
                        @JsonProperty("type") Integer type,
                        @JsonProperty("name") String name,
                        @JsonProperty("path") String path) {
        this.parentName = parentName;
        this.type = type;
        this.name = name;
        this.path = path;
    }
}
