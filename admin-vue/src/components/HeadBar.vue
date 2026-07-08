<template>
    <div class="header">
        <div class="user">
            <el-avatar class="tt ta" :size="60" :src="getAvatar(userDetails.avatar)"/>
            <router-link to="/setting/detail" style="text-decoration: none"><span class="tt ts">{{ userDetails.nickname }}</span></router-link>
            <el-link type="primary" :underline="'never'" @click="emits('onLogout')"><span class="tt ts">退出</span></el-link>
        </div>
    </div>
</template>

<script setup lang="ts">

import type {UserDetailsDto} from "@/api/common.ts";

import {getAvatar} from "@/util/avatars.ts";

defineProps<{ userDetails: UserDetailsDto; }>();

const emits = defineEmits<{ (name: 'onLogout'): void }>();

</script>

<style scoped>

.header {
    height: 100px;
    width: 100%;
    margin: 0;
    padding: 0;
    display: flex;
    justify-content: right;
    background: linear-gradient(to right, #909399, white);
}

.user {
    width: 200px;
    height: 60px;
    margin: 20px 20px 0 auto;
    display: flex;
    justify-content: space-between;
}

.tt {
    height: 60px;
    line-height: 60px;
    padding: 0 10px 0 10px;
}

.ta {
    background-color: white;
}

.ts {
    font-size: 16px;
}

</style>
