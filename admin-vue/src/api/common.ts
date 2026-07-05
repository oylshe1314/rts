/**
 * common.ts
 * create by Snake as 2026-07-03
 * @description:
 */
import service from "@/api/request.ts";

const pathAdminDetails = "/common/admin/details"
const pathRoleMenus = "/common/role/menus"
const pathOptionsMenus = "/common/options/menus"
const pathOptionsRoles = "/common/options/roles"
const pathOptionsAdmins = "/common/options/admins"

class UserDetailsDto {
    roleName: string = "";
    username: string = "";
    phone: string = "";
    email: string = "";
    nickname: string = "";
    avatar: string = "";
}

class RoleMenuDto {
    parentId: number = 0;
    type: number = 0;
    name: string = "";
    icon: string = "";
    path: string = "";
}

class MenuOptionDto {
    id: number = 0;
    name: string = "";
}

class RoleOptionDto {
    id: number = 0;
    name: string = "";
}

class AdminOptionDto {
    id: number = 0;
    roleId: string = "";
    username: string = "";
    nickname: string = "";
    avatar: string = "";
}

export default {
    adminDetails: (): Promise<UserDetailsDto> => {
        return service({
            url: pathAdminDetails,
            method: "GET",
        });
    },
    roleMenus: (): Promise<RoleMenuDto[]> => {
        return service({
            url: pathRoleMenus,
            method: "GET",
        });
    },
    optionsMenus: (): Promise<MenuOptionDto[]> => {
        return service({
            url: pathOptionsMenus,
            method: "GET",
        });
    },
    optionsRoles: (): Promise<RoleOptionDto[]> => {
        return service({
            url: pathOptionsRoles,
            method: "GET",
        });
    },
    optionsAdmins: (): Promise<AdminOptionDto[]> => {
        return service({
            url: pathOptionsAdmins,
            method: "GET",
        });
    }
}