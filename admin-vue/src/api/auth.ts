/**
 * auth.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import service from "@/api/request.ts";

const pathAuthLogin = "/auth/login"
const pathAuthLogout = "/auth/logout"

class AdminLoginDto {
    account: string = "";
    password: string = "";
}

export default {
    login: (data: AdminLoginDto): Promise<string> => {
        return service({
            url: pathAuthLogin,
            method: "POST",
            data: data,
        });
    },
    logout: (): Promise<any> => {
        return service({
            url: pathAuthLogout,
            method: "POST",
        });
    }
}