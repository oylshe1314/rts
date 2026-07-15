/**
 * setting.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import service from "@/api/request.ts";

export interface ChangeDetailsDto {
    phone: string | null;
    email: string | null;
    nickname: string | null;
    avatar: string | null;
}

export interface ChangePasswordDto {
    oldPassword: string;
    newPassword: string;
}

export default {
    changeDetail: async (data: ChangeDetailsDto): Promise<void> => {
        return service({
            url: '/setting/details/change',
            method: "post",
            data: data
        })
    },
    changePassword: async (data: ChangePasswordDto): Promise<void> => {
        return service({
            url: '/setting/password/change',
            method: "post",
            data: data
        })
    }
}