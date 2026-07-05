<template>
  <div class="main">
    <el-tabs v-model="$route.name" type="border-card" :value="$route.path" @tab-change="handleTabChange" @tab-remove="handleTabRemove" class="tabs">
      <el-tab-pane v-for="card in tabs.getCards" :key="card.id" :name="card.name" :label="card.title" :closable="card.closable"/>
      <router-view v-slot="{Component, route}">
        <transition mode="out-in">
          <keep-alive :include="tabs.getNames" :exclude="excludeRef" max="32">
            <component :is="Component" :key="route.name"/>
          </keep-alive>
        </transition>
      </router-view>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">

import router from "@/router";

import tabs from '@/store/tabs'

const excludeRef = ['NotFound']

function handleTabChange(name: string) {
  router.push({name: name})
}

function handleTabRemove(name: string) {
  const index = tabs.getIndex(name)
  if (index > 0) {
    tabs.removeCard(index)
    if (name === router.currentRoute.value.name) {
      if (index < tabs.getSize) {
        router.push({name: tabs.getCards[index].name})
      } else {
        router.push({name: tabs.getCards[index - 1].name})
      }
    }
  }
}

</script>

<style scoped>

.main {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  overflow-y: hidden;
}

/*.tabs {*/
/*    width: 100%;*/
/*    height: 100%;*/
/*    margin: 0;*/
/*    padding: 0;*/
/*}*/

</style>
