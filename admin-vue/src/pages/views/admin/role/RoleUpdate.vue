<template>
    <el-dialog v-model="showEditRef" :title="'修改角色'" width="800px" @close="handleEditClose" destroy-on-close center>
        <div id="update" class="update">
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
                    <el-button type="primary" style="width: 80px" @click="handleSubmit">修改</el-button>
                </el-form-item>
            </el-form>
        </div>
    </el-dialog>
</template>

<script setup lang="ts">

import {reactive, ref, watch} from 'vue';

import {ElMessage, ElMessageBox} from "element-plus";

import {closeLoading, openLoading} from "@/util/loading.ts";

import type {RoleUpdateDto} from "@/api/admin";
import adminApi from "@/api/admin";

interface Props {
    modelValue: boolean;
    editData: RoleUpdateDto;
}

const props = withDefaults(
    defineProps<Props>(),
    {
        modelValue: false,
        editData: {id: 0, name: '', code: '', remark: ''},
    }
);

interface Emits {
    (emit: 'update:modelValue', value: boolean): void;

    (emit: 'updateSuccess'): void;
}

const emits = defineEmits<Emits>();

const showEditRef = ref(props.modelValue);

watch(() => props.modelValue, (value: boolean) => {
    showEditRef.value = value;
});

function close(success: boolean) {
    showEditRef.value = false;
    if (success) {
        emits('updateSuccess');
    }
}

const formData = reactive<RoleUpdateDto>({
    id: 0,
    name: '',
    code: '',
    remark: '',
})

watch(() => props.editData, (editData: RoleUpdateDto) => {
    formData.id = editData.id;
    formData.name = editData.name;
    formData.code = editData.code;
    formData.remark = editData.remark;
})

function init() {
    formData.id = 0;
    formData.name = '';
    formData.code = '';
    formData.remark = '';
}

function handleEditClose() {
    emits('update:modelValue', false);
}

function handleSubmit() {
    const updateDto: RoleUpdateDto = {id: formData.id, name: null, code: null, remark: null};
    if (formData.name != props.editData.name) {
        if (formData.name === '') {
            ElMessage({type: 'error', showClose: true, message: '请输入角色名称'});
            return;
        }
        updateDto.name = formData.name;
    }
    if (formData.code != props.editData.code) {
        if (formData.code === '') {
            ElMessage({type: 'error', showClose: true, message: '请输入角色代码'});
            return;
        }
        updateDto.code = formData.code;
    }
    if (formData.remark != props.editData.remark) {
        updateDto.remark = formData.remark;
    }

    ElMessageBox.confirm('确认提交', '警告', {type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消'})
        .then(() => {
            openLoading('#update', '正在提交，请稍候...');
            adminApi.roleUpdate(updateDto)
                .then(() => {
                    close(true);
                    ElMessage({type: 'success', showClose: true, message: '修改成功'});
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

.update {
    width: 100%;
    height: 100%;
    /*display: flex;*/
    /*justify-content: center;*/
}

</style>