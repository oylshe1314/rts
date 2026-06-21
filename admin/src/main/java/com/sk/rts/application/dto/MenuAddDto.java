package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.validation.Integers;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
@Schema(description = "菜单添加")
public class MenuAddDto {

    @NotNull
    @Schema(description = "上级菜单ID, 类型为2,3时必填")
    private final Long parentId;

    @NotNull
    @Integers({1, 2, 3})
    @Schema(description = "类型, 1.目录, 2.菜单, 3.接口")
    private final Integer type;

    @Schema(description = "图标")
    private final String icon;

    @NotBlank
    @Schema(description = "名称")
    private final String name;

    @Schema(description = "路径, 类型为2,3时必填")
    private final String path;

    @NotNull
    @Positive
    @Schema(description = "排序值")
    private final Integer sortBy;

    @Schema(description = "备注")
    private final String remark;

    public MenuAddDto(@JsonProperty("parentId") Long parentId,
                      @JsonProperty("type") Integer type,
                      @JsonProperty("icon") String icon,
                      @JsonProperty("name") String name,
                      @JsonProperty("path") String path,
                      @JsonProperty("sortBy") Integer sortBy,
                      @JsonProperty("remark") String remark) {
        this.parentId = parentId;
        this.type = type;
        this.icon = icon;
        this.name = name;
        this.path = path;
        this.sortBy = sortBy;
        this.remark = remark;
    }
}
