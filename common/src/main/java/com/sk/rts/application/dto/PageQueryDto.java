package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.Range;
import org.jspecify.annotations.Nullable;

@Getter
@Schema(description = "分页查询参数")
public class PageQueryDto<T> {

    @NotNull
    @Min(value = 1, message = "页码最小从1开始)")
    @Schema(description = "页码")
    private final Integer pageNo;

    @NotNull
    @Range(min = 1, max = 100, message = "每页数量须在1-100之间")
    @Schema(description = "每页数量，值范围(1-100)")
    private final Integer pageSize;

    @NullOrNotBlank
    @Schema(description = "排序字段")
    private final String sort;

    @Schema(description = "是否降序")
    private final Boolean desc;

    @Schema(description = "查询参数")
    private final T query;

    @JsonCreator
    public PageQueryDto(@JsonProperty("pageNo") Integer pageNo,
                        @JsonProperty("pageSize") Integer pageSize,
                        @JsonProperty("sort") String sort,
                        @JsonProperty("desc") Boolean desc,
                        @JsonProperty("query") @Nullable T query) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.sort = sort;
        this.desc = desc;
        this.query = query;
    }

    public Integer getOffset() {
        return pageSize * (pageNo - 1);
    }
}
