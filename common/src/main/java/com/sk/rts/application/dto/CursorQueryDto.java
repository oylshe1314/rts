package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.component.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.Range;

@Getter
@Schema(description = "游标查询参数")
public class CursorQueryDto<Cursor, Query> {

    @NotNull
    @Range(min = 1, max = 100, message = "每页数量须在1-100之间")
    @Schema(description = "每页数量，值范围(1-100)")
    private final Integer pageSize;

    @Schema(description = "游标")
    private final Cursor cursor;

    @NullOrNotBlank
    @Schema(description = "排序字段")
    private final String sort;

    @Schema(description = "是否降序")
    private final Boolean desc;

    @Schema(description = "查询参数")
    private final Query query;

    @JsonCreator
    public CursorQueryDto(@JsonProperty("pageSize") Integer pageSize,
                          @JsonProperty("cursor") Cursor cursor,
                          @JsonProperty("sort") String sort,
                          @JsonProperty("desc") Boolean desc,
                          @JsonProperty("query") Query query) {
        this.pageSize = pageSize;
        this.cursor = cursor;
        this.sort = sort;
        this.desc = desc;
        this.query = query;
    }
}
