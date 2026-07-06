/**
 * request.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import type {AxiosRequestConfig, AxiosResponse} from "axios";
import axios from "axios"

import {useUserStore} from "@/store/user";

export interface ResponseDto<T> {
    status: number;
    message: string;
    data: T | null;
}

const service = axios.create({
    withCredentials: true,
})

service.interceptors.request.use(async (config: AxiosRequestConfig): Promise<any> => {
    const userStore = useUserStore();
    const token = userStore.getToken();
    if (token && config.headers) {
        config.headers.Authorization = "Bearer " + token;
    }
    if (config.url) {
        config.url = "/rts/admin/api" + config.url;
    }
    return Promise.resolve(config);
});

service.interceptors.response.use(async (response: AxiosResponse<ResponseDto<any>>): Promise<any> => {
        if (response.data.status === 200) {
            return Promise.resolve(response.data.data);
        } else {
            return Promise.reject(new Error(response.data.message));
        }
    },
    (err) => {
        console.log('请求失败:', err.message)
        return Promise.reject(new Error("请求失败"));
    }
);

export default service