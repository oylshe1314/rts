/**
 * auth.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import service from "@/api/request.ts";

const pathAuthLogin = "/auth/login"
const pathAuthLogout = "/auth/logout"

interface AdminLoginDto {
    account: string;
    password: string;
}

export default {
    login: async (data: AdminLoginDto): Promise<string> => {
        return service({
            url: pathAuthLogin,
            method: "POST",
            data: data,
        });
    },
    logout: async (): Promise<any> => {
        return service({
            url: pathAuthLogout,
            method: "POST",
        });
    }
}