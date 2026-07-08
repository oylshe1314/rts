<template>
    <div id="change-password" class="change-password">
        <div class="form">
            <el-form label-width="100px" ref="formRef" :model="formData" :rules="formRules">
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

import {reactive, ref} from "vue";
import {ElMessage, ElMessageBox} from 'element-plus';

import settingApi from '@/api/setting';
import {closeLoading, openLoading} from "@/util/loading";

const formRef = ref();

const formData = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
});

const formRules = reactive({
    oldPassword: [{required: true, trigger: 'blur', message: '请输入旧密码'}],
    newPassword: [{
        required: true, trigger: 'blur', validator: (_: any, value: any, callback: any) => {
            if (value === '') {
                callback('请输入新密码');
            } else if (value === formData.oldPassword) {
                callback('新密码不能与旧密码一致');
            }
            callback();
        }
    }],
    confirmPassword: [{
        required: true, trigger: 'blur', validator: (_: any, value: string, callback: any) => {
            if (value === '') {
                callback(new Error('请输入确认密码'));
            } else if (value !== formData.newPassword) {
                callback(new Error('两次输入的密码不一致'));
            }
            callback();
        }
    }],
})

function handleSubmit() {
    formRef.value.validate().then((ok: boolean) => {
        if (ok) {
            ElMessageBox.confirm('确认修改密码', '警告', {confirmButtonText: '确认', cancelButtonText: '取消'})
                .then(() => {
                    openLoading('#changePassword', '已提交，请稍候...');
                    settingApi.changePassword(formData).then(() => {
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
                })
        }
    }).catch((err: any) => {
        if (err.oldPassword) {
            ElMessage({type: 'error', showClose: true, message: err.oldPassword[0].message});
        } else if (err.newPassword) {
            ElMessage({type: 'error', showClose: true, message: err.newPassword[0].message});
        } else if (err.confirmPassword) {
            ElMessage({type: 'error', showClose: true, message: err.confirmPassword[0].message});
        }
    })
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