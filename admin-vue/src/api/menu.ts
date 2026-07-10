/**
 * menu.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import type {PageResultDto} from '@/api/request.ts';
import service from '@/api/request.ts';

export interface MenuQueryDto {
    type: number | null;
    name: string | null;
    path: string | null;
}

export interface MenuDto {
    id: number;
    parentId: number;
    type: number;
    typeName: string;
    parentName: string;
    icon: string;
    name: string;
    path: string;
    sortBy: number;
    state: number;
    remark: string;
    updateBy: string;
    updateTime: string;
}

export interface MenuAddDto {
    parentId: number;
    type: number;
    icon: string;
    name: string;
    path: string;
    sortBy: number;
    remark: string;
}

export interface MenuUpdateDto {
    id: number;
    parentId: number | null;
    type: number | null;
    icon: string | null;
    name: string | null;
    path: string | null;
    sortBy: number | null;
    remark: string | null;
}

export default {
    query: (pageNo: number, pageSize: number, params: MenuQueryDto): Promise<PageResultDto<MenuDto>> => {
        return service({
            url: '/menu/query',
            method: 'get',
            params: {pageNo: pageNo, pageSize: pageSize, ...params}
        });
    },
    add: (data: MenuAddDto): Promise<MenuDto> => {
        return service({
            url: '/menu/add',
            method: 'post',
            data: data
        })
    },
    update: (data: MenuUpdateDto): Promise<MenuDto> => {
        return service({
            url: '/menu/add',
            method: 'post',
            data: data
        })
    },
    stateChange: (ids: number[], state: number): Promise<void> => {
        console.log(ids, state)
        return service({
            url: '/menu/state/change',
            method: 'post',
            data: {ids: ids, state: state}
        })
    },
    delete: (ids: number[]): Promise<void> => {
        return service({
            url: '/menu/delete',
            method: 'post',
            data: {ids: ids}
        })
    },
}