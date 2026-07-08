/**
 * auth.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import type {PageResultDto} from '@/api/request.ts';
import service from '@/api/request.ts';

import type {RoleOptionDto} from '@/api/common.ts';

export interface RoleQueryDto {
    name: string | null;
}

export interface RoleDto {
    id: number;
    name: string;
    code: string;
    state: number;
    remark: string;
    updateBy: string;
    updateTime: string;
}

export interface RoleAddDto {
    name: string;
    code: string;
    remark: string | null;
}

export interface RoleUpdateDto {
    id: number;
    name: string | null;
    code: string | null;
    remark: string | null;
}

export interface RoleAuthorityDto {
    id: number;
    parentId: number;
    name: string;
    checked: boolean;
    sortBy: number;
    subMenus: RoleAuthorityDto[] | null;
}

export interface RoleAuthorityComparisonDto {
    id: number;
    parentId: number;
    type: string;
    name: string;
    icon: string;
    highlight: boolean;
    roles: number[];
    sortBy: number;
    subMenus: RoleAuthorityComparisonDto[] | null;
}

export interface RoleAuthorityComparisonListDto {
    roleList: RoleOptionDto[];
    comparisonList: RoleAuthorityComparisonDto[];
}

export interface MenuQueryDto {
    roleId: number;
    username: string;
    phone: string;
    email: string;
}

export interface AdminAddDto {
    roleId: number;
    username: string;
    password: string;
    phone: string;
    email: string;
    nickname: string;
    avatar: string;
    remark: string;
}

export interface AdminAddDto {
    roleId: number;
    username: string;
    password: string;
    phone: string;
    email: string;
    nickname: string;
    avatar: string;
    remark: string;
}

export interface AdminUpdateDto {
    roleId: number;
    password: string;
    phone: string;
    email: string;
    nickname: string;
    avatar: string;
    remark: string;
}

const adminApi = {
    roleQuery: (pageNo: number, pageSize: number, params: RoleQueryDto): Promise<PageResultDto<RoleDto>> => {
        return service({
            url: '/admin/role/query',
            method: 'get',
            params: {pageNo: pageNo, pageSize: pageSize, ...params}
        })
    },
    roleAdd: (data: RoleAddDto): Promise<RoleDto> => {
        return service({
            url: '/admin/role/add',
            method: 'post',
            data: data
        })
    },
    roleUpdate: (data: RoleUpdateDto): Promise<RoleDto> => {
        return service({
            url: '/admin/role/update',
            method: 'post',
            data: data
        })
    },
    roleChangeState: (ids: number[], state: number): Promise<void> => {
        console.log(ids, state)
        return service({
            url: '/admin/role/change/state',
            method: 'post',
            data: {ids: ids, state: state}
        })
    },
    roleDelete: (ids: number[]): Promise<void> => {
        return service({
            url: '/admin/role/delete',
            method: 'post',
            data: {ids: ids}
        })
    },
    roleAuthorityGet: (roleId: number): Promise<RoleAuthorityDto[]> => {
        return service({
            url: '/admin/role/authority/get',
            method: 'get',
            params: {roleId: roleId}
        })
    },
    roleAuthoritySet: (roleId: number, menuIds: number[]): Promise<void> => {
        return service({
            url: '/admin/role/authority/set',
            method: 'post',
            data: {roleId: roleId, menuIds: menuIds}
        })
    },
    roleAuthorityCompare: (roleIds: number[]):Promise<RoleAuthorityComparisonListDto> => {
        return service({
            url: '/admin/role/authority/compare',
            method: 'post',
            data: {ids: roleIds}
        })
    },
    query: (pageNo: number, pageSize: number, params: MenuQueryDto) => {
        return service({
            url: '/admin/query',
            method: 'get',
            params: {pageNo: pageNo, pageSize: pageSize, ...params}
        })
    },
    add: (data: AdminAddDto) => {
        return service({
            url: '/admin/add',
            method: 'post',
            data: data
        })
    },
    update: (data: AdminUpdateDto) => {
        return service({
            url: '/admin/update',
            method: 'post',
            data: data
        })
    },
    changeState: (ids: number[], state: number) => {
        return service({
            url: '/admin/change/state',
            method: 'post',
            data: {ids: ids, state: state}
        })
    },
    delete: (ids: number[]) => {
        return service({
            url: '/admin/delete',
            method: 'post',
            data: {ids: ids}
        })
    },
}

export default adminApi