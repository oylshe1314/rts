/**
 * store.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import {defineStore} from "pinia";

export interface UserDetails {
    roleName: string;
    username: string;
    phone: string;
    email: string;
    nickname: string;
    avatar: string;
}

interface UserStore {
    token: string | null;
    details: UserDetails | null;
}

export const useUserStore = defineStore("user", {
    state: (): UserStore => {
        const token = sessionStorage.getItem("user_token");
        const details = sessionStorage.getItem("user_details");
        return {
            token: token,
            details: details ? JSON.parse(details) : null,
        };
    },
    actions: {
        setToken(token: string): void {
            this.token = token
            sessionStorage.setItem("user_token", token);
        },
        setDetails(details: UserDetails): void {
            this.details = details;
            sessionStorage.setItem("user_details", JSON.stringify(details));
        },
        clear(): void {
            sessionStorage.removeItem("user_token");
            sessionStorage.removeItem("user_details");
            this.$reset();
        }
    },
    getters: {
        getToken() {
            return (): string | null => {
                return this.token;
            }
        },
        getDetails() {
            return (): UserDetails => {
                return this.details!;
            }
        }
    }
});
