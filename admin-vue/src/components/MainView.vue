<template>
  <div class="main">
    <el-tabs v-model="$route.name" type="border-card" :value="$route.path" @tab-change="emits('onTabChange')" @tab-remove="emits('onTabRemove')" class="tabs">
        <el-tab-pane v-for="card in tabCards" :key="card.id" :name="card.name" :label="card.title" :closable="card.closable"/>
      <router-view v-slot="{Component, route}">
        <transition mode="out-in">
            <keep-alive :include="tabNames" :exclude="['NotFound']" max="32">
            <component :is="Component" :key="route.name"/>
          </keep-alive>
        </transition>
      </router-view>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">

import type {TabCard} from "@/store/tabs.ts";

defineProps<{
    tabNames: string[]
    tabCards: TabCard[]
}>();

const emits = defineEmits<{
    (e: 'onTabChange'): void
    (e: 'onTabRemove'): void
}>();

</script>

<style scoped>

.main {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  overflow-y: hidden;
}
</style>
