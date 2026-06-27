package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.validation.Integers;
import com.sk.rts.application.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;

@Getter
@Schema(description = "菜单修改")
public class MenuUpdateDto {

    @NotNull
    @Schema(description = "ID")
    private final Long id;

    @Integers({1, 2, 3})
    @Schema(description = "类型, 1.目录, 2.菜单, 3.接口")
    private final Integer type;

    @PositiveOrZero
    @Schema(description = "上级菜单ID")
    private final Long parentId;

    @Schema(description = "图标")
    private final String icon;

    @NullOrNotBlank
    @Schema(description = "名称")
    private final String name;

    @Schema(description = "路径")
    private final String path;

    @Positive
    @Schema(description = "排序值")
    private final Integer sort;

    @Schema(description = "备注")
    private final String remark;

    public MenuUpdateDto(@JsonProperty("id") Long id,
                         @JsonProperty("parentId") Long parentId,
                         @JsonProperty("type") Integer type,
                         @JsonProperty("icon") String icon,
                         @JsonProperty("name") String name,
                         @JsonProperty("path") String path,
                         @JsonProperty("sortBy") Integer sort,
                         @JsonProperty("remark") String remark) {
        this.id = id;
        this.parentId = parentId;
        this.type = type;
        this.icon = icon;
        this.name = name;
        this.path = path;
        this.sort = sort;
        this.remark = remark;
    }
}
