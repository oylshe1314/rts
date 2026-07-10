<template>
    <el-dialog v-model="showEditRef" title="添加管理员" width="800px" @opened="handleOpened" @close="handleEditClose" destroy-on-close center>
        <el-form :model="formData" label-width="100px">
            <el-form-item label="角色" required>
                <el-select-v2 v-model="formData.roleId" :props="roleSelectorProps" :options="roleSelectorOptionsRef" style="width: 240px"></el-select-v2>
            </el-form-item>
            <el-form-item label="用户名" required>
                <el-input v-model="formData.username" type="text" style="width: 240px"/>
            </el-form-item>
            <el-form-item label="密码" required>
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
                <avatar-selector v-model="formData.avatar" :disabled="false"></avatar-selector>
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

import {ElSelectV2, ElMessage, ElMessageBox} from "element-plus";

import type {RoleOptionDto} from '@/api/common.ts'
import commonApi from '@/api/common.ts'

import type {AdminAddDto} from '@/api/admin.ts'
import adminApi from '@/api/admin.ts'

import AvatarSelector from "@/components/AvatarSelector.vue";

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

    (emit: "addSuccess"): void;
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

const formData = reactive<AdminAddDto>({
    roleId: 0,
    username: '',
    password: '123456',
    phone: '',
    email: '',
    nickname: '',
    avatar: 'avatar1',
    remark: '',
});

const roleSelectorProps = {label: 'name', value: 'id'};
const roleSelectorOptionsRef = ref<RoleOptionDto[]>([{id: 0, name: '请选择角色'}]);

function rolesToSelectOptions(roles: RoleOptionDto[]) {
    const options: RoleOptionDto[] = [{id: 0, name: '请选择角色'}];
    roles.forEach((role) => options.push(role));
    return options;
}

function init() {
    formData.roleId = 0;
    formData.username = '';
    formData.password = '123456';
    formData.phone = '';
    formData.email = '';
    formData.nickname = '';
    formData.avatar = 'avatar1';
    formData.remark = '';
}

function handleEditClose() {
    emits('update:modelValue', false)
}

function handleOpened() {
    commonApi.roleOptions()
        .then((res) => {
            roleSelectorOptionsRef.value = rolesToSelectOptions(res);
        })
        .catch((e) => {
            ElMessage({type: 'error', showClose: true, message: '查询角色列表失败: ' + e.message});
        });
}

function handleSubmit() {
    if (formData.roleId === 0) {
        ElMessage({type: 'error', showClose: true, message: '请选择角色'});
        return;
    }
    if (!formData.username || formData.username === '') {
        ElMessage({type: 'error', showClose: true, message: '请输入用户名'});
        return;
    }
    if (!formData.password || formData.password === '') {
        ElMessage({type: 'error', showClose: true, message: '请输入密码'});
        return;
    }
    if (!formData.nickname || formData.nickname === '') {
        ElMessage({type: 'error', showClose: true, message: '请输入昵称'});
        return;
    }

    const addDto: AdminAddDto = {
        roleId: formData.roleId,
        username: formData.username,
        password: formData.password,
        phone: formData.phone,
        email: formData.email,
        nickname: formData.nickname,
        avatar: formData.avatar,
        remark: formData.remark,
    };
    ElMessageBox.confirm('确认提交', '警告', {type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消'})
        .then(() => {
            adminApi.add(addDto)
                .then(() => {
                    close(true);
                    ElMessage({type: 'success', showClose: true, message: '添加成功'});
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