/**
 * store.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import {defineStore} from "pinia";

interface UserStore {
    token: string | null;
}

export const useUserStore = defineStore("user", {
    state: (): UserStore => {
        const token = sessionStorage.getItem("user_token");
        return {
            token: token,
        };
    },
    actions: {
        setToken(token: string): void {
            this.token = token
            sessionStorage.setItem("user_token", token);
        },
        clear(): void {
            sessionStorage.removeItem("user_token");
            this.$reset();
        }
    },
    getters: {
        getToken() {
            return (): string | null => {
                return this.token;
            }
        },
    }
});
