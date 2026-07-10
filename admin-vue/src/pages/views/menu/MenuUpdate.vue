<template>
    <el-dialog v-model="showEditRef" title="修改菜单" width="800px" @close="handleEditClose" destroy-on-close center>
        <el-form :model="formData" label-width="100px">
            <el-form-item label="类型" required>
                <el-select v-model.number="formData.type" @change="handleTypeChange" style="width: 240px">
                    <el-option :value='1' label="目录"/>
                    <el-option :value='2' label="菜单"/>
                    <el-option :value='3' label="接口"/>
                </el-select>
            </el-form-item>
            <el-form-item label="上级菜单">
                <el-select v-model="formData.parentId" placeholder="选择上级菜单" :disabled="parentSelectorDisableRef" style="width: 240px">
                    <el-option :key="0" :value="0" label="选择上级菜单"/>
                    <el-option v-for="parent in parentSelectOptionsRef" :key="parent.id" :value='parent.id' :label="parent.name"/>
                </el-select>
            </el-form-item>
            <el-form-item label="图标">
                <icon-selector v-model="formData.icon!" :disabled="iconSelectorDisabledRef" style="width: 240px"/>
            </el-form-item>
            <el-form-item label="名称" required>
                <el-input v-model="formData.name" type="text" style="width: 240px"/>
            </el-form-item>
            <el-form-item label="路径">
                <el-input v-model="formData.path" type="text" :disabled="pathDisabledRef" style="width: 100%"/>
            </el-form-item>
            <el-form-item label="排序值" required>
                <el-input-number v-model.number="formData.sortBy" :min="0" :max="9999" style="width: 100px" controls-position="right"></el-input-number>
            </el-form-item>
            <el-form-item label="备注">
                <el-input v-model="formData.remark" type="textarea" :rows="4" style="width: 100%"/>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" style="width: 80px" @click="close(false)">取消</el-button>
                <el-button type="primary" style="width: 80px" @click="handleSubmit">提交</el-button>
            </el-form-item>
        </el-form>
    </el-dialog>
</template>

<script setup lang="ts">

import {reactive, ref, watch} from "vue";

import {ElMessage, ElMessageBox} from "element-plus";

import type {MenuOptionDto} from '@/api/common';
import commonApi from '@/api/common';


import type {MenuUpdateDto} from '@/api/menu';
import menuApi from '@/api/menu';

import IconSelector from "@/components/IconSelector.vue";

interface Props {
    modelValue: boolean;
    editData: MenuUpdateDto;
}

const props = withDefaults(
    defineProps<Props>(),
    {
        modelValue: () => false,
        editData: () => ({id: 0, parentId: 0, type: 0, icon: '', name: '', path: '', sortBy: 0, remark: ''}),
    }
);

interface Emits {
    (emit: 'update:modelValue', value: boolean): void;

    (emit: 'updateSuccess'): void;
}

const emits = defineEmits<Emits>();

const showEditRef = ref(props.modelValue);

watch(() => props.modelValue, (value) => {
    showEditRef.value = value;
})

function close(success: boolean) {
    showEditRef.value = false;
    if (success) {
        emits('updateSuccess');
    }
}

const formData = reactive<MenuUpdateDto>({
    id: 0,
    type: 1,
    parentId: 0,
    icon: '',
    name: '',
    path: '',
    sortBy: 0,
    remark: '',
});

const parentSelectorDisableRef = ref<boolean>(true);
const parentSelectOptionsRef = ref<MenuOptionDto[]>([]);

const iconSelectorDisabledRef = ref<boolean>(false);
const pathDisabledRef = ref<boolean>(true);

const parentsMap = new Map<number, MenuOptionDto[]>();

watch(() => props.editData, (editData: MenuUpdateDto) => {
    formData.id = editData.id;
    formData.type = editData.type;
    formData.parentId = editData.parentId;
    formData.icon = editData.icon;
    formData.name = editData.name;
    formData.path = editData.path;
    formData.sortBy = editData.sortBy;
    formData.remark = editData.remark;

    handleTypeChange(formData.type!);
});

function init() {
    formData.id = 0;
    formData.type = 1;
    formData.parentId = 0;
    formData.icon = '';
    formData.name = '';
    formData.path = '';
    formData.sortBy = 0;
    formData.remark = '';

    handleTypeChange(formData.type);
}

function handleEditClose() {
    parentsMap.clear();
    emits('update:modelValue', false);
}

function queryParents(type: number) {
    const parents = parentsMap.get(type);
    if (!parents) {
        commonApi.menuOptions(type)
            .then((parents) => {
                parentSelectorDisableRef.value = false;
                parentSelectOptionsRef.value = parents;
                parentsMap.set(type, parents);
            })
            .catch((e) => {
                ElMessage({type: 'error', showClose: true, message: '查询上级菜单列表失败: ' + e.message});
            })
    } else {
        parentSelectorDisableRef.value = false;
        parentSelectOptionsRef.value = parents;
    }
}

function handleTypeChange(type: number) {
    switch (type) {
        case 1:
            parentSelectorDisableRef.value = true;
            parentSelectOptionsRef.value = [];
            iconSelectorDisabledRef.value = false;
            pathDisabledRef.value = true;

            formData.parentId = 0;
            if (props.editData) {
                formData.icon = props.editData.icon;
            }
            break
        case 2:
            queryParents(1);
            iconSelectorDisabledRef.value = false;
            pathDisabledRef.value = false;
            if (props.editData && type === props.editData.type) {
                formData.parentId = props.editData.parentId;
                formData.icon = props.editData.icon;
            } else {
                formData.parentId = 0;
            }
            break
        case 3:
            queryParents(2);
            iconSelectorDisabledRef.value = true;
            pathDisabledRef.value = false;
            if (props.editData && type === props.editData.type) {
                formData.parentId = props.editData.parentId;
            } else {
                formData.parentId = 0;
            }
            formData.icon = '';
            break
    }
}

function handleSubmit() {
    const updateDto: MenuUpdateDto = {id: formData.id, parentId: null, type: null, icon: null, name: null, path: null, sortBy: null, remark: null};
    if (formData.parentId !== props.editData.parentId) {
        if (formData.parentId === 0) {
            if (formData.type !== 1) {
                ElMessage({type: 'error', showClose: true, message: '请选择上级菜单'});
                return;
            }
        }
        updateDto.parentId = formData.parentId;
    }
    if (formData.type !== props.editData.type) {
        if (formData.type !== 1 && formData.type !== 2 && formData.type !== 3) {
            ElMessage({type: 'error', showClose: true, message: '请选择菜单类型'});
            return;
        }
        updateDto.type = formData.type;
    }
    if (formData.type !== 3) {
        if (formData.icon !== props.editData.icon) {
            updateDto.icon = formData.icon;
        }
    } else {
        updateDto.icon = '';
    }
    if (formData.name !== props.editData.name) {
        if (formData.name === '') {
            ElMessage({type: 'error', showClose: true, message: '请输入菜单名称'});
            return;
        }
        updateDto.name = formData.name;
    }
    if (formData.path !== props.editData.path) {
        if (formData.path === '' && formData.type !== 1) {
            ElMessage({type: 'error', showClose: true, message: '请输入菜单路径'});
            return;
        }
        updateDto.path = formData.path;
    }
    if (formData.sortBy !== props.editData.sortBy) {
        updateDto.sortBy = formData.sortBy;
    }
    if (formData.remark != props.editData.remark) {
        updateDto.remark = formData.remark;
    }

    ElMessageBox.confirm('确认提交', '警告', {type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消'})
        .then(() => {
            menuApi.update(formData)
                .then(() => {
                    close(true);
                    ElMessage({type: 'success', showClose: true, message: '修改成功'});
                    init();
                })
                .catch((e) => {
                    ElMessage({type: 'error', showClose: true, message: e.message});
                })
        })
        .catch(() => {
        });
}

</script>

<style scoped>

</style>