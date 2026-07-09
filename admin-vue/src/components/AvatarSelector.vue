<template>
    <el-popover ref="popoverRef" placement="right-start" width="530" trigger="click" :disabled="disabledRef">
        <div class="avatar-select">
            <ul>
                <li v-for="(value, key) in avatars" :key="key">
                    <div style="width: 120px; height: 120px;">
                        <img alt="" class="avatar-item" :src="value" @click="handleAvatarSelect(key)">
                    </div>
                </li>
            </ul>
        </div>
        <template #reference>
            <div style="width: 120px; height: 120px;">
                <img alt="" class="avatar-item" :src="modelValueRef"/>
            </div>
        </template>
    </el-popover>
</template>

<script setup lang="ts">

import {ref, watch} from 'vue'

import avatars from "@/util/avatars.ts";

const props = withDefaults(defineProps<{ modelValue: string; disabled?: boolean }>(), {disabled: false,});

const popoverRef = ref()

const modelValueRef = ref((!props.modelValue || props.modelValue === '') ? avatars['avatar1'] : avatars[props.modelValue])

watch(() => props.modelValue, (value) => {
    if (!value || value === '') {
        modelValueRef.value = avatars['avatar1'];
    } else {
        modelValueRef.value = avatars[value];
    }
})

const disabledRef = ref(props.disabled)

watch(() => props.disabled, (disabled) => {
    disabledRef.value = disabled
})

const emits = defineEmits(['update:modelValue'])

function handleAvatarSelect(avatar: string) {
    console.log(avatar)
    emits('update:modelValue', avatar)
    popoverRef.value.hide()
}

</script>

<style scoped>

.avatar-select {
    width: 500px;
    height: 250px;
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