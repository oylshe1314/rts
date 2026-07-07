/**
 * setting.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import service from "@/api/request.ts";

interface ChangeDetailsDto {
    phone: string;
    email: string;
    nickname: string;
    avatar: string;
}

interface ChangePasswordDto {
    oldPassword: string;
    newPassword: string;
}

export default {
    changeDetail: async (data: ChangeDetailsDto): Promise<void> => {
        return service({
            url: '/admin/api/setting/change/detail',
            method: "post",
            data: data
        })
    },
    changePassword: async (data: ChangePasswordDto): Promise<void> => {
        return service({
            url: '/admin/api/setting/change/password',
            method: "post",
            data: data
        })
    }
}