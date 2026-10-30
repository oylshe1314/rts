package com.sk.rts.application.dto;

import com.sk.rts.application.dto.base.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
public class AdminMenuSortableDto<T extends AdminMenuSortableDto<T>> extends BaseDto {

    @Schema(description = "排序值")
    private final Integer sortBy;

    @Schema(description = "子菜单")
    private final List<T> subMenus;

    public AdminMenuSortableDto(Long id, Integer sortBy) {
        super(id);
        this.sortBy = sortBy;
        this.subMenus = new ArrayList<>();
    }

    public static <T extends AdminMenuSortableDto<T>> void sort(List<T> menus) {
        if (CollectionUtils.isEmpty(menus)) {
            return;
        }
        menus.sort(Comparator.comparingInt(AdminMenuSortableDto::getSortBy));
        for (T menu : menus) {
            sort(menu.getSubMenus());
        }
    }
}
