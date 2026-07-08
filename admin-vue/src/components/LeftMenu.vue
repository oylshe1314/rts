<template>
    <div class="left-bar">
        <el-menu class="left-menu" :default-active="$route.path" :unique-opened='true' router>
            <template v-for="menu in roleMenus" :key="menu.id">
                <el-sub-menu v-if="menu.type === 1" :index="String(menu.id)">
                    <template #title>
                        <el-icon v-if="menu.icon !== ''">
                            <component :is="menu.icon"/>
                        </el-icon>
                        <el-icon v-else>
                            <Folder/>
                        </el-icon>
                        <span>{{ menu.name }}</span>
                    </template>
                    <template v-for="subMenu in menu.subMenus" :key="subMenu.id">
                        <el-menu-item :index="subMenu.path">
                            <template #title>
                                <el-icon v-if="subMenu.icon !== ''">
                                    <component :is="subMenu.icon"></component>
                                </el-icon>
                                <el-icon v-else>
                                    <Document/>
                                </el-icon>
                                <span>{{ subMenu.name }}</span>
                            </template>
                        </el-menu-item>
                    </template>
                </el-sub-menu>
                <el-menu-item v-else :index="menu.path">
                    <el-icon v-if="menu.icon !== ''">
                        <component :is="menu.icon"></component>
                    </el-icon>
                    <el-icon v-else>
                        <Document/>
                    </el-icon>
                    <span>{{ menu.name }}</span>
                </el-menu-item>
            </template>
        </el-menu>
    </div>
</template>

<script setup lang="ts">

import type {RoleMenuDto} from "@/api/common.ts";

defineProps<{ roleMenus: RoleMenuDto[]; }>();

</script>

<style scoped>

.left-bar {
    width: 200px;
    height: 100%;
    overflow-y: hidden;
}

.left-menu {
    width: 100%;
    height: 100%;
}

</style>
