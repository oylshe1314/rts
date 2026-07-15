<template>
    <div id="change-password" class="change-password">
        <div class="form">
            <el-form label-width="100px" :model="formData">
                <el-form-item label="旧密码" prop="oldPassword">
                    <el-input type="password" v-model="formData.oldPassword" maxlength="24" style="width: 240px" show-password/>
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                    <el-input type="password" v-model="formData.newPassword" maxlength="24" style="width: 240px" show-password/>
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input type="password" v-model="formData.confirmPassword" maxlength="24" style="width: 240px" show-password/>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSubmit" style="width: 160px">提交</el-button>
                </el-form-item>
            </el-form>
        </div>
    </div>
</template>

<script setup lang="ts">

import {reactive} from "vue";
import {ElMessage, ElMessageBox} from 'element-plus';

import type {ChangePasswordDto} from '@/api/setting.ts';
import settingApi from '@/api/setting.ts';

import {closeLoading, openLoading} from "@/util/loading";

interface ChangePasswordExtendedDto extends ChangePasswordDto {
    confirmPassword: string;
}

const formData = reactive<ChangePasswordExtendedDto>({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
});

function handleSubmit() {
    if (formData.oldPassword === '') {
        ElMessage({type: 'error', showClose: true, message: '请输入旧密码'});
        return;
    }
    if (formData.newPassword === '') {
        ElMessage({type: 'error', showClose: true, message: '请输入新密码'});
        return;
    }
    if (formData.newPassword === formData.oldPassword) {
        ElMessage({type: 'error', showClose: true, message: '新密码不能与旧密码一致'});
        return;
    }
    if (formData.confirmPassword === '') {
        ElMessage({type: 'error', showClose: true, message: '请输入确认密码'});
        return;
    }
    if (formData.confirmPassword !== formData.newPassword) {
        ElMessage({type: 'error', showClose: true, message: '两次输入的密码不一致'});
        return;
    }

    ElMessageBox.confirm('确认修改密码', '警告', {confirmButtonText: '确认', cancelButtonText: '取消'})
        .then(() => {
            const changeDto: ChangePasswordDto = {oldPassword: formData.oldPassword, newPassword: formData.newPassword};

            openLoading('#changePassword', '正在提交，请稍候...');
            settingApi.changePassword(changeDto).then(() => {
                ElMessage({type: 'success', showClose: true, message: '修改成功'});
                formData.oldPassword = '';
                formData.newPassword = '';
                formData.confirmPassword = '';
            }).catch((e) => {
                ElMessage({type: 'error', showClose: true, message: e.message});
            }).finally(() => {
                closeLoading();
            })
        })
        .catch(() => {
        });
}

</script>

<style scoped>

.change-password {
    width: 100%;
    height: 100%;
    /*display: flex;*/
    /*justify-content: center;*/
}

.form {
    width: 680px;
    height: 642px;
    /*background: pink;*/
    /*position: relative;*/
    /*top: 200px;*/
    /*left: 50%;*/
    /*margin: 180px 0 0 -180px;*/
    padding: 40px 40px 40px 40px;
}

</style>