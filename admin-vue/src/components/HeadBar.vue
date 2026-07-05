<template>
  <div class="header">
    <div class="user">
      <el-avatar class="tt ta" :size="60" :src="'@/assets/images/avatars/' + ((userDetails.avatar === '') ? 'avatar1.png' : userDetails.avatar)"/>
      <router-link to="/setting/detail" style="text-decoration: none"><span class="tt ts">{{ userDetails.nickname }}</span></router-link>
      <el-link type="primary" :underline=false @click="handleLogout"><span class="tt ts">退出</span></el-link>
    </div>
  </div>
</template>

<script setup lang="ts">

import {ElMessage} from "element-plus";

import router from "@/router";
import authApi from '@/api/auth'
import user from "@/store/user";
import tabs from "@/store/tabs";

const userDetails = user.getDetails!;

function handleLogout() {
  authApi.logout()
      .then(() => {
        ElMessage({type: 'success', showClose: true, message: '退出成功'})
      })
      .catch((e) => {
        ElMessage({type: 'error', showClose: true, message: e.message})
      })
      .finally(() => {
        tabs.clear()
        user.clear()
        router.push({name: 'LoginPage'})
      })
}

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
