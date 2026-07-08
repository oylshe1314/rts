/**
 * loading.ts
 * create by Snake as 2026-07-06
 * @description:
 */

import {ElLoading} from "element-plus";
import {ref} from "vue";

const loading = ref();

export function openLoading(target: string, text: string) {
    loading.value = ElLoading.service({
        target: target,
        text: (!text || text === '') ? '处理中，请稍候' : text,
    });
}

export function closeLoading() {
    loading.value.close();
    loading.value = false;
}