/**
 * admin.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import type {PageResultDto} from '@/api/request.ts';
import service from '@/api/request.ts';

import type {RoleOptionDto} from '@/api/common.ts';

export interface RoleQueryDto {
    name: string | null;
    code: string | null;
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
    type: number;
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

export interface AdminQueryDto {
    roleId: number | null;
    username: string | null;
    phone: string | null;
    email: string | null;
}

export interface AdminDto {
    id: number;
    roleId: number;
    roleName: string;
    username: string;
    nickname: string;
    avatar: string;
    email: string;
    phone: string;
    state: number;
    remark: string;
    updateBy: string;
    updateTime: string;
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
    id: number;
    roleId: number | null;
    password: string | null;
    phone: string | null;
    email: string | null;
    nickname: string | null;
    avatar: string | null;
    remark: string | null;
}

export default {
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
    roleStateChange: (ids: number[], state: number): Promise<void> => {
        console.log(ids, state)
        return service({
            url: '/admin/role/state/change',
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
    roleAuthorityCompare: (roleIds: number[]): Promise<RoleAuthorityComparisonListDto> => {
        return service({
            url: '/admin/role/authority/compare',
            method: 'post',
            data: {ids: roleIds}
        })
    },
    query: (pageNo: number, pageSize: number, params: AdminQueryDto): Promise<PageResultDto<AdminDto>> => {
        return service({
            url: '/admin/query',
            method: 'get',
            params: {pageNo: pageNo, pageSize: pageSize, ...params}
        })
    },
    add: (data: AdminAddDto): Promise<AdminDto> => {
        return service({
            url: '/admin/add',
            method: 'post',
            data: data
        })
    },
    update: (data: AdminUpdateDto): Promise<AdminDto> => {
        return service({
            url: '/admin/update',
            method: 'post',
            data: data
        })
    },
    stateChange: (ids: number[], state: number): Promise<void> => {
        return service({
            url: '/admin/state/change',
            method: 'post',
            data: {ids: ids, state: state}
        })
    },
    delete: (ids: number[]): Promise<void> => {
        return service({
            url: '/admin/delete',
            method: 'post',
            data: {ids: ids}
        })
    },
}