/**
 * auth.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import service from "@/api/request.ts";

interface AdminLoginDto {
    account: string;
    password: string;
}

export default {
    login: async (data: AdminLoginDto): Promise<string> => {
        return service({
            url: "/auth/login",
            method: "POST",
            data: data,
        });
    },
    logout: async (): Promise<void> => {
        return service({
            url: "/auth/logout",
            method: "POST",
        });
    }
}