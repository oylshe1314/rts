/**
 * common.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import service from "@/api/request.ts";

export interface UserDetailsDto {
    roleName: string;
    username: string;
    phone: string;
    email: string;
    nickname: string;
    avatar: string;
}

export interface RoleMenuDto {
    id: number;
    parentId: number;
    type: number;
    name: string;
    icon: string;
    path: string;
    sortBy: number;
    subMenus: RoleMenuDto[] | null;
}

export interface MenuOptionDto {
    id: number;
    name: string;
}

export interface RoleOptionDto {
    id: number;
    name: string;
}

export interface AdminOptionDto {
    id: number;
    roleId: string;
    username: string;
    nickname: string;
    avatar: string;
}

export default {
    adminDetails: async (): Promise<UserDetailsDto> => {
        return service({
            url: "/common/admin/details",
            method: "GET",
        });
    },
    roleMenus: async (): Promise<RoleMenuDto[]> => {
        return service({
            url: "/common/role/menus",
            method: "GET",
        });
    },
    menuOptions: async (type: number): Promise<MenuOptionDto[]> => {
        return service({
            url: "/common/options/menus",
            method: "GET",
            params: {type: type},
        });
    },
    roleOptions: async (): Promise<RoleOptionDto[]> => {
        return service({
            url: "/common/options/roles",
            method: "GET",
        });
    },
    adminOptions: async (): Promise<AdminOptionDto[]> => {
        return service({
            url: "/common/options/admins",
            method: "GET",
        });
    }
}