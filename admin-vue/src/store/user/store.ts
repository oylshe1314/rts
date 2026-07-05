/**
 * store.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import {defineStore} from "pinia";

class UserDetails {
    roleName: string = "";
    username: string = "";
    phone: string = "";
    email: string = "";
    nickname: string = "";
    avatar: string = "";
}

class AuthDetails {
    token: string | null = null;
    details: UserDetails | null = null;
}

const userStore = defineStore("user", {
        state: () => {
            return new AuthDetails();
        },
        actions: {
            setToken(token: string): void {
                this.token = token
            },
            setDetails(details: UserDetails): void {
                this.details = details;
            },
            clear():void {
                this.token = null;
                this.details = null;
            }
        },
        getters: {
            getToken(): string | null {
                return this.token;
            },
            getDetails(): UserDetails | null {
                return this.details;
            }
        }
    })
;

export default userStore;