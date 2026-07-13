<template>
    <el-dialog v-model="showEditRef" title="修改管理员" width="800px" @opened="handleOpened" @close="handleEditClose" destroy-on-close center>
        <el-form :model="formData" label-width="100px">
            <el-form-item label="角色" required>
                <el-select v-model="formData.roleId" placeholder="选择角色" style="width: 240px">
                    <el-option :key="0" :value="0" label="选择角色"/>
                    <el-option v-for="roleOption in roleSelectorOptionsRef" :key="roleOption.id" :value='roleOption.id' :label="roleOption.name"/>
                </el-select>
            </el-form-item>
            <el-form-item label="用户名" required>
                <el-input v-model="formData.username" :disabled="true" type="text" style="width: 240px"/>
            </el-form-item>
            <el-form-item label="密码">
                <el-input v-model="formData.password" type="text" style="width: 240px"/>
            </el-form-item>
            <el-form-item label="手机">
                <el-input v-model="formData.phone" type="text" style="width: 240px"/>
            </el-form-item>
            <el-form-item label="邮箱">
                <el-input v-model="formData.email" type="text" style="width: 240px"/>
            </el-form-item>
            <el-form-item label="昵称" required>
                <el-input v-model="formData.nickname" type="text" style="width: 240px"/>
            </el-form-item>
            <el-form-item label="头像">
                <avatar-selector v-model="formData.avatar!" :disabled="false"></avatar-selector>
            </el-form-item>
            <el-form-item label="备注">
                <el-input v-model="formData.remark" type="textarea" :rows="4" style="width: 100%"/>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" style="width: 80px" @click="close(false)">取消</el-button>
                <el-button type="primary" style="width: 80px" @click="handleSubmit">提交</el-button>
            </el-form-item>
        </el-form>
    </el-dialog>
</template>

<script setup lang="ts">

import {ref, watch, reactive} from 'vue';

import {ElMessage, ElMessageBox} from "element-plus";

import type {RoleOptionDto} from '@/api/common.ts';
import commonApi from '@/api/common.ts';

import type {AdminUpdateDto} from '@/api/admin.ts';
import adminApi from '@/api/admin.ts';

import AvatarSelector from "@/components/AvatarSelector.vue";

export interface AdminExtendedDto extends AdminUpdateDto {
    username: string | null;
}

interface Props {
    modelValue: boolean;
    editData: AdminExtendedDto;
}

const props = withDefaults(
    defineProps<Props>(),
    {
        modelValue: false,
        editData: () => ({id: 0, roleId: null, username: null, password: null, phone: null, email: null, nickname: null, avatar: null, remark: null}),
    }
);

interface Emits {
    (emit: 'update:modelValue', value: boolean): void;

    (emit: 'updateSuccess'): void;
}

const emits = defineEmits<Emits>();

const showEditRef = ref<boolean>(props.modelValue)

watch(() => props.modelValue, (value: boolean) => {
    showEditRef.value = value;
})

function close(success: boolean) {
    showEditRef.value = false
    if (success) {
        emits('updateSuccess');
    }
}

const formData = reactive<AdminExtendedDto>({
    id: 0,
    roleId: 0,
    username: '',
    password: '',
    phone: '',
    email: '',
    nickname: '',
    avatar: '',
    remark: '',
})

const roleSelectorOptionsRef = ref<RoleOptionDto[]>([]);

function init() {
    formData.id = 0;
    formData.roleId = 0;
    formData.username = '';
    formData.password = '';
    formData.phone = '';
    formData.email = '';
    formData.nickname = '';
    formData.avatar = 'avatar1';
    formData.remark = '';
}

watch(() => props.editData, (editData: AdminExtendedDto) => {
    formData.id = editData.id;
    formData.roleId = editData.roleId;
    formData.username = editData.username;
    formData.password = editData.password;
    formData.phone = editData.phone;
    formData.email = editData.email;
    formData.nickname = editData.nickname;
    formData.avatar = editData.avatar;
    formData.remark = editData.remark;
})

function handleEditClose() {
    emits('update:modelValue', false)
}

function handleOpened() {
    commonApi.roleOptions()
        .then((res) => {
            roleSelectorOptionsRef.value = res;
        })
        .catch((e) => {
            ElMessage({type: 'error', showClose: true, message: '查询角色列表失败: ' + e.message});
        })
}

function handleSubmit() {
    const updateDto: AdminUpdateDto = {id: formData.id, roleId: null, password: null, phone: null, email: null, nickname: null, avatar: null, remark: null};
    if (formData.roleId !== props.editData.roleId) {
        if (formData.roleId === 0) {
            ElMessage({type: 'error', showClose: true, message: '请选择角色'});
            return;
        }
        updateDto.roleId = formData.roleId;
    }
    if (formData.password !== '') {
        updateDto.password = formData.password;
    }
    if (formData.phone !== props.editData.phone && formData.phone !== '') {
        updateDto.phone = formData.phone;
    }
    if (formData.email !== props.editData.email && formData.email !== '') {
        updateDto.email = formData.email;
    }
    if (formData.nickname != props.editData.nickname) {
        if (!formData.nickname || formData.nickname === '') {
            ElMessage({type: 'error', showClose: true, message: '请输入昵称'});
            return;
        }
        updateDto.nickname = formData.nickname;
    }
    ElMessageBox.confirm('确认提交', '警告', {type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消'})
        .then(() => {
            adminApi.update(updateDto)
                .then(() => {
                    close(true);
                    ElMessage({type: 'success', showClose: true, message: '修改成功'});
                    init();
                })
                .catch((e) => {
                    ElMessage({type: 'error', showClose: true, message: e.message});
                })
        })
        .catch(() => {
        });
}

</script>

<style scoped>

</style>