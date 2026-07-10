<template>
    <el-dialog v-model="showEditRef" :title="'添加角色'" width="800px" @close="handleEditClose" destroy-on-close center>
        <div id="add" class="add">
            <el-form :model="formData" label-width="100px">
                <el-form-item label="名称" required>
                    <el-input v-model="formData.name" type="text" style="width: 240px"></el-input>
                </el-form-item>
                <el-form-item label="代码" required>
                    <el-input v-model="formData.code" type="text" style="width: 240px"></el-input>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="formData.remark" type="textarea" :rows="4" style="width: 100%"/>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" style="width: 80px" @click="close(false)">取消</el-button>
                    <el-button type="primary" style="width: 80px" @click="handleSubmit">添加</el-button>
                </el-form-item>
            </el-form>
        </div>
    </el-dialog>
</template>

<script setup lang="ts">

import {reactive, ref, watch} from 'vue';

import {ElMessage, ElMessageBox} from "element-plus";

import {closeLoading, openLoading} from "@/util/loading.ts";

import type {RoleAddDto} from "@/api/admin";
import adminApi from "@/api/admin";

interface Props {
    modelValue: boolean;
}

const props = withDefaults(
    defineProps<Props>(),
    {
        modelValue: false,
    }
);

interface Emits {
    (emit: 'update:modelValue', value: boolean): void;

    (emit: 'addSuccess'): void;
}

const emits = defineEmits<Emits>();

const showEditRef = ref<boolean>(props.modelValue);

watch(() => props.modelValue, (value: boolean) => {
    showEditRef.value = value;
});

function close(success: boolean) {
    showEditRef.value = false;
    if (success) {
        emits('addSuccess');
    }
}

const formData = reactive<RoleAddDto>({
    name: '',
    code: '',
    remark: '',
})

function init() {
    formData.name = '';
    formData.code = '';
    formData.remark = '';
}

function handleEditClose() {
    emits('update:modelValue', false);
}

function handleSubmit() {
    if (formData.name === '') {
        ElMessage({type: 'error', showClose: true, message: '请输入角色名称'});
        return;
    }

    if (formData.code === '') {
        ElMessage({type: 'error', showClose: true, message: '请输入角色代码'});
        return;
    }

    const addDto: RoleAddDto = {name: formData.name, code: formData.code, remark: formData.remark};
    ElMessageBox.confirm('确认提交', '警告', {type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消'})
        .then(() => {
            openLoading('#add', '正在提交，请稍候...');
            adminApi.roleAdd(addDto)
                .then(() => {
                    close(true);
                    ElMessage({type: 'success', showClose: true, message: '添加成功'});
                    init()
                })
                .catch((e) => {
                    ElMessage({type: 'error', showClose: true, message: e.message});
                })
                .finally(() => {
                    closeLoading();
                })
        })
        .catch(() => {
        })
}

</script>

<style scoped>

.add {
    width: 100%;
    height: 100%;
    /*display: flex;*/
    /*justify-content: center;*/
}

</style>