<template>
    <el-dialog v-model="showEditRef" title="角色菜单管理" width="800px" @close="handleClose" destroy-on-close center>
        <div id="authorities" class="authorities">
            <div style="height: 600px">

            </div>
            <div style="width: 100%; display: flex; justify-content: center">
                <el-button type="primary" style="width: 80px" @click="close">关闭</el-button>
            </div>
        </div>
    </el-dialog>
</template>

<script setup lang="ts">

import {ref, watch} from 'vue'
import {closeLoading, openLoading} from "@/util/loading.ts";
import adminApi from "@/api/admin.ts";
import {ElMessage} from "element-plus";

interface Props {
    modelValue: boolean;
    editData: { roleIds: number[]; };
}

const props = withDefaults(
    defineProps<Props>(),
    {
        modelValue: () => false,
        editData: () => ({roleIds: []}),
    }
);

const showEditRef = ref(false)

watch(() => props.modelValue, (value) => {
    showEditRef.value = value
})

function close() {
    showEditRef.value = false
}

const emits = defineEmits(['update:modelValue'])

function handleClose() {
    emits('update:modelValue', false)
}

function getRoleAuthorityComparisonList(roleIds: number[]) {
    openLoading('#authorities', '正在加载，请稍候...');
    adminApi.roleAuthorityCompare(roleIds)
        .then((res) => {
            console.log(res);
        })
        .catch((e) => {
            ElMessage({type: 'error', showClose: true, message: e.message});
        })
        .finally(() => {
            closeLoading();
        })
}

watch(() => props.editData, (editData) => {
    getRoleAuthorityComparisonList(editData.roleIds)
});

</script>

<style scoped>

</style>