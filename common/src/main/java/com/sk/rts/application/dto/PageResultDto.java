package com.sk.rts.application.dto;

import com.sk.rts.application.util.IteratorUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.function.Function;

@Getter
@NullMarked
@Schema(description = "分页查询结果")
public class PageResultDto<T> {

    @Schema(description = "页码，从1开始")
    private final int pageNo;

    @Schema(description = "每页数量")
    private final int pageSize;

    @Schema(description = "总数量")
    private final long total;

    @Schema(description = "总页数")
    private final long pages;

    @Schema(description = "结果数据")
    private final Collection<T> results;

    public PageResultDto(int pageNo, int pageSize, long total, Collection<T> results) {
        this.pageNo = pageNo + 1;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = total / pageSize + ((total % pageSize == 0) ? 0 : 1);
        this.results = results;
    }

    public<S>  PageResultDto(int pageNo, int pageSize, long total, Collection<S> results, Function<S, T> mapper) {
        this.pageNo = pageNo + 1;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = total / pageSize + ((total % pageSize == 0) ? 0 : 1);
        this.results = IteratorUtil.convert(results, mapper);
    }

    public PageResultDto(Pageable pageable, Page<T> page) {
        this.pageNo = pageable.getPageNumber() + 1;
        this.pageSize = pageable.getPageSize();
        this.total = page.getTotalElements();
        this.pages = page.getTotalPages();
        this.results = page.getContent();
    }

    public <S> PageResultDto(Pageable pageable, Page<S> page, Function<S, T> mapper) {
        this.pageNo = pageable.getPageNumber() + 1;
        this.pageSize = pageable.getPageSize();
        this.total = page.getTotalElements();
        this.pages = page.getTotalPages();
        this.results = IteratorUtil.convert(page.getContent(), mapper);
    }
}

