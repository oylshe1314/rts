package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.util.IteratorUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.Collection;
import java.util.function.Function;

@Getter
@Schema(description = "分页查询结果")
public class CursorResultDto<Cursor, Result> {

    @Schema(description = "每页数量")
    private final int pageSize;

    @Schema(description = "总数量")
    private final Cursor cursor;

    @Schema(description = "结果数据")
    private final Collection<Result> results;

    @JsonCreator
    public CursorResultDto(@JsonProperty("pageSize") int pageSize,
                           @JsonProperty("cursor") Cursor cursor,
                           @JsonProperty("results") Collection<Result> results) {
        this.pageSize = pageSize;
        this.cursor = cursor;
        this.results = results;
    }

    public <Source> CursorResultDto(int pageSize, Cursor cursor, Collection<Source> results, Function<Source, Result> mapper) {
        this.pageSize = pageSize;
        this.cursor = cursor;
        this.results = IteratorUtil.convert(results, mapper);
    }
}
