<template>
    <div id="form" class="form">
        <el-form ref="formRef" :model="formData" @keyup.enter="handleLogin">
            <el-form-item prop="account">
                <el-input v-model="formData.account" :prefix-icon="User" class="form-ipt" maxlength="24" size="large" type="text">
                </el-input>
            </el-form-item>
            <el-form-item prop="password">
                <el-input v-model="formData.password" :prefix-icon="Lock" class="form-ipt" maxlength="24" show-password size="large" type="password"/>
            </el-form-item>
            <el-form-item>
                <el-button class="form-ipt" size="large" type="primary" @click="handleLogin">登录</el-button>
            </el-form-item>
        </el-form>
    </div>
</template>

<script setup lang="ts">

import {reactive, ref} from 'vue';
import {ElMessage} from 'element-plus';
import {Lock, User} from '@element-plus/icons-vue';

import router from "@/router";
import authApi from '@/api/auth.ts';
import {useUserStore} from "@/store/user.ts";

import {closeLoading, openLoading} from "@/util/loading";

const formRef = ref({});

const formData = reactive({
    account: 'admin',
    password: 'rts.sk.org'
})

// const formData = reactive({
//     account: '',
//     password: ''
// });

function handleLogin() {
    if (!formData.account || formData.account === '') {
        ElMessage({type: 'error', showClose: true, message: '请输入账号'});
        return;
    }
    if (!formData.password || formData.password === '') {
        ElMessage({type: 'error', showClose: true, message: '请输入密码'});
        return;
    }

    const userStore = useUserStore();

    openLoading('#form', '正在登录...');
    authApi.login(formData).then(token => {
        userStore.setToken(token);
        router.push({name: 'HomeView'});
    }).catch(e => {
        ElMessage({type: 'error', showClose: true, message: e.message});
    }).finally(() => {
        closeLoading();
    })
}
</script>

<style scoped>

.form-ipt {
    width: 320px;
    height: 40px;
}

.form {
    width: 320px;
    height: 156px;
    border-radius: 16px;
    /*background:  #a0cfff;*/
    position: absolute;
    top: 50%;
    left: 50%;
    margin: -118px 0 0 -200px;
    padding: 40px 40px 40px 40px;
}
</style>