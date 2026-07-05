/**
 * request.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import axios from "axios"
import type {AxiosRequestConfig, AxiosResponse} from "axios";

import user from "@/store/user";


class ResponseDto<T> {
    status: number = 0;
    message: string = "";
    data: T | null = null;
}

const service = axios.create({
    withCredentials: true,
})

service.interceptors.request.use((config: AxiosRequestConfig): Promise<any> => {
    const token = user.getToken;
    if (token && config.headers) {
        config.headers.Authorization = "Bearer " + token;
    }
    return Promise.resolve();
});

service.interceptors.response.use((response: AxiosResponse<ResponseDto<any>>): Promise<any> => {
        if (response.data.status === 200) {
            return Promise.resolve(response.data.data);
        } else {
            return Promise.reject(response.data.message);
        }
    },
    (err) => {
        console.log('请求失败:', err.message)
        return Promise.reject(new Error("请求失败"));
    }
);

export default service