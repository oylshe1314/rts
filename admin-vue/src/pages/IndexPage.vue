<template>
    <el-container>
        <el-header height="100px" class="top">
            <head-bar :user-details="userDetailsRef" @on-logout="handleLogout"/>
        </el-header>
        <el-container>
            <el-aside width="200px">
                <left-menu :role-menus="roleMenusRef"/>
            </el-aside>
            <el-main>
                <main-view :tab-names="tabNamesRef" :tab-cards="tabCardsRef" @tab-change="handleTabChange" @tab-remove="handleTabRemove"/>
            </el-main>
        </el-container>
    </el-container>
</template>

<script setup lang="ts">

import {onMounted, ref} from "vue";

import authApi from "@/api/auth.ts"

import type {RoleMenuDto} from "@/api/common.ts";
import commonApi from "@/api/common.ts";

import {ElMessage} from "element-plus";

import HeadBar from '@/components/HeadBar.vue';
import LeftMenu from '@/components/LeftMenu.vue';
import MainView from '@/components/MainView.vue';

import {closeLoading, openLoading} from "@/util/loading.ts";

import type {UserDetails} from "@/store/user.ts";
import {useUserStore} from "@/store/user.ts";

import type {TabCard} from "@/store/tabs.ts";
import {useTabsStore} from "@/store/tabs.ts";

import router from "@/router";

const userStore = useUserStore();
const tabsStore = useTabsStore();

const userDetailsRef = ref<UserDetails>({avatar: "", email: "", nickname: "", phone: "", roleName: "", username: ""});
const roleMenusRef = ref<RoleMenuDto[]>([]);
const tabNamesRef = ref<string[]>(tabsStore.getNames());
const tabCardsRef = ref<TabCard[]>(tabsStore.getCards());

onMounted(() => {
    openLoading('#app', '正在加载...');
    Promise.all([commonApi.adminDetails(), commonApi.roleMenus()])
        .then(([userDetails, roleMenus]) => {
            userStore.setDetails({
                roleName: userDetails.roleName,
                username: userDetails.username,
                phone: userDetails.phone,
                email: userDetails.email,
                nickname: userDetails.nickname,
                avatar: userDetails.avatar,
            });

            userDetailsRef.value = userStore.getDetails();
            roleMenusRef.value = roleMenus;
        })
        .catch((e) => {
            ElMessage({type: 'error', showClose: true, message: e.message})
        })
        .finally(() => {
            closeLoading();
        });
});

function handleLogout() {
    openLoading('#form', '正在退出...');
    authApi.logout()
        .then(() => {
            ElMessage({type: 'success', showClose: true, message: '退出成功'});
        })
        .catch((e) => {
            ElMessage({type: 'error', showClose: true, message: e.message});
        })
        .finally(() => {
            closeLoading();
            userStore.clear();
            tabsStore.clear();
            router.push({name: 'LoginPage'});
        });
}

function handleTabChange(name: string) {
    router.push({name: name});
}

function handleTabRemove(name: string) {
    const index = tabsStore.getIndex(name);
    if (index > 0) {
        tabsStore.removeCard(index);
        if (name === router.currentRoute.value.name) {
            if (index < tabsStore.getSize()) {
                router.push({name: tabsStore.getCards()[index].name});
            } else {
                router.push({name: tabsStore.getCards()[index - 1].name});
            }
        }
    }
}

</script>