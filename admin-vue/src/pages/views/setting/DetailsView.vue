<template>
    <div id="detail" class="detail">
        <div class="form">
            <el-form label-width="100px" :model="formData">
                <el-form-item label="角色">
                    <el-input v-model="formData.roleName" name="roleName" style="width: 240px" disabled></el-input>
                </el-form-item>
                <el-form-item label="用户名">
                    <el-input v-model="formData.username" name="username" style="width: 240px" disabled></el-input>
                </el-form-item>
                <el-form-item label="手机">
                    <el-input v-model="formData.phone!" name="mobile" style="width: 240px" :disabled="editState.phone"></el-input>
                </el-form-item>
                <el-form-item label="邮箱">
                    <el-input v-model="formData.email!" name="email" style="width: 240px" :disabled="editState.email"></el-input>
                </el-form-item>
                <el-form-item label="昵称">
                    <el-input v-model="formData.nickname!" name="nickname" style="width: 240px" :disabled="editState.nickname"></el-input>
                </el-form-item>
                <el-form-item label="头像">
                    <avatar-selector v-model="formData.avatar!" :disabled="editState.avatar"></avatar-selector>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleEditState" style="width: 80px">{{ editState.btnText }}</el-button>
                    <el-button v-show="editState.showSubmit" type="primary" style="width: 80px" @click="handleSubmit">提交</el-button>
                </el-form-item>
            </el-form>
        </div>
    </div>
</template>

<script setup lang="ts">

import {reactive} from "vue";

import {ElMessage, ElMessageBox} from "element-plus";
import {closeLoading, openLoading} from "@/util/loading";

import {useUserStore} from "@/store/user.ts";

import type {ChangeDetailsDto} from '@/api/setting.ts';
import settingApi from '@/api/setting.ts';

import AvatarSelector from "@/components/AvatarSelector.vue";

const userStore = useUserStore();

const userDetails = userStore.getDetails();

const editState = reactive({
    phone: true,
    email: true,
    nickname: true,
    avatar: true,
    btnText: '修改',
    showSubmit: false,
})

interface ChangeDetailsExtendedDto extends ChangeDetailsDto {
    roleName: string;
    username: string;
}

const formData = reactive<ChangeDetailsExtendedDto>({
    roleName: userDetails.roleName,
    username: userDetails.username,
    phone: userDetails.phone,
    email: userDetails.email,
    nickname: userDetails.nickname,
    avatar: userDetails.avatar,
});

function handleEditState() {
    editState.phone = !editState.phone;
    editState.email = !editState.email;
    editState.nickname = !editState.nickname;
    editState.avatar = !editState.avatar;
    editState.showSubmit = !editState.showSubmit;
    editState.btnText = editState.showSubmit ? '取消' : '修改';
    if (!editState.showSubmit) {
        formData.roleName = userDetails.roleName;
        formData.username = userDetails.username;
        formData.phone = userDetails.phone;
        formData.email = userDetails.email;
        formData.nickname = userDetails.nickname;
        formData.avatar = userDetails.avatar;
    }
}

function handleSubmit() {
    const changeDto: ChangeDetailsDto = {phone: null, email: null, nickname: null, avatar: null};
    if (formData.phone != userDetails.phone) {
        changeDto.phone = formData.phone;
    }
    if (formData.email != userDetails.email) {
        changeDto.email = formData.email;
    }
    if (formData.nickname != userDetails.nickname) {
        if (formData.nickname === '') {
            ElMessage({type: 'error', showClose: true, message: '昵称不能为空'});
            return;
        }
        changeDto.nickname = formData.nickname;
    }
    if (formData.avatar != userDetails.avatar) {
        changeDto.avatar = formData.avatar;
    }

    ElMessageBox.confirm('确认提交修改', '警告', {confirmButtonText: '确认', cancelButtonText: '取消'})
        .then(() => {
            openLoading('#detail', '正在提交，请稍候...');
            settingApi.changeDetail(changeDto).then(() => {

                userDetails.phone = formData.phone!;
                userDetails.email = formData.email!;
                userDetails.nickname = formData.nickname!;
                userDetails.avatar = formData.avatar!;

                handleEditState();
                ElMessage({type: 'success', showClose: true, message: '修改成功'});
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

.detail {
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

.avatar-select {
    width: 500px;
    height: 250px;
    /*background: pink;*/
}

.avatar-select ul {
    display: block;
    margin: 0;
    padding: 0;
}

.avatar-select li {
    display: block;
    margin-right: 5px;
    padding: 0;
    float: left;
    text-align: center;
    cursor: pointer;
}

.avatar-item {
    width: 120px;
    height: 120px;
}

.avatar-item:active {
    width: 119px;
    height: 119px;
    /*border: #409EFF solid 1px;*/
}

</style>