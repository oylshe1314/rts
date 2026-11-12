package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.util.IteratorUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.function.Function;

@Getter
@Schema(description = "分页查询结果")
public class PageResultDto<Result> {

    @Schema(description = "页码，从1开始")
    private final int pageNo;

    @Schema(description = "每页数量")
    private final int pageSize;

    @Schema(description = "总数量")
    private final long total;

    @Schema(description = "总页数")
    private final long pages;

    @Schema(description = "结果数据")
    private final Collection<Result> results;

    @JsonCreator
    public PageResultDto(@JsonProperty("pageNo") int pageNo,
                         @JsonProperty("pageSize") int pageSize,
                         @JsonProperty("total") long total,
                         @JsonProperty("results") Collection<Result> results) {
        this.pageNo = pageNo + 1;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = (total  + pageSize - 1) / pageSize;
        this.results = results;
    }

    public <Source> PageResultDto(int pageNo, int pageSize, long total, Collection<Source> results, Function<Source, Result> mapper) {
        this.pageNo = pageNo + 1;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = (total  + pageSize - 1) / pageSize;
        this.results = IteratorUtil.convert(results, mapper);
    }

    public PageResultDto(Pageable pageable, Page<Result> page) {
        this.pageNo = pageable.getPageNumber() + 1;
        this.pageSize = pageable.getPageSize();
        this.total = page.getTotalElements();
        this.pages = page.getTotalPages();
        this.results = page.getContent();
    }

    public <Source> PageResultDto(Pageable pageable, Page<Source> page, Function<Source, Result> mapper) {
        this.pageNo = pageable.getPageNumber() + 1;
        this.pageSize = pageable.getPageSize();
        this.total = page.getTotalElements();
        this.pages = page.getTotalPages();
        this.results = IteratorUtil.convert(page.getContent(), mapper);
    }
}

