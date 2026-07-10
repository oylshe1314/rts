<template>
    <el-dialog v-model="showEditRef" title="角色权限管理" width="800px" @close="handleClose" destroy-on-close center>
        <div id="authorities" class="authorities">
            <div style="height: 600px">
                <el-tree-v2 ref="treeRef"
                            v-model="treeDataRef"
                            :data="treeDataRef"
                            :props="treeProps"
                            :default-checked-keys="treeCheckedKeysRef"
                            :height="572"
                            class="menu-tree"
                            show-checkbox>
                </el-tree-v2>
            </div>
            <div style="width: 100%; display: flex; justify-content: center">
                <el-button type="primary" style="width: 80px" @click="close">取消</el-button>
                <el-button type="primary" style="width: 80px" @click="handleSubmit">提交</el-button>
            </div>
        </div>
    </el-dialog>
</template>

<script setup lang="ts">

import {ref, watch} from 'vue';

import {ElMessage, ElMessageBox} from "element-plus";

import {closeLoading, openLoading} from "@/util/loading.ts";

import type {RoleAuthorityDto} from '@/api/admin';
import adminApi from '@/api/admin';

interface Props {
    modelValue: boolean;
    editData: { roleId: number };
}

const props = withDefaults(
    defineProps<Props>(),
    {
        modelValue: false,
        editData: {roleId: 0},
    }
);

const showEditRef = ref(false);

watch(() => props.modelValue, (value) => {
    showEditRef.value = value;
})

function close() {
    showEditRef.value = false;
}

interface Emits {
    (emit: 'update:modelValue', value: boolean): void;
}

const emits = defineEmits<Emits>();

function handleClose() {
    treeDataRef.value = [];
    treeCheckedKeysRef.value = [];
    emits('update:modelValue', false);
}

interface TreeNode {
    id: number;
    label: string;
    disabled: boolean;
    children: TreeNode[];
}

interface TreeProps {
    value: string;
    label: string;
    children: string;
}

const treeProps: TreeProps = {
    value: 'id',
    label: 'label',
    children: 'children',
}

const treeRef = ref();
const treeDataRef = ref<TreeNode[]>([]);
const treeCheckedKeysRef = ref<number[]>([]);

function authoritiesToTreeNodes(parent: RoleAuthorityDto | null, authorities: RoleAuthorityDto[], checkedKeys: number[]): TreeNode[] {
    if (authorities.length === 0) {
        return [];
    }

    return authorities.map((authority) => {
        const node: TreeNode = {
            id: authority.id,
            label: authority.name,
            disabled: (props.editData.roleId === 1 && authority.id < 29),
            children: [],
        };

        if (authority.subMenus !== null) {
            node.children = authoritiesToTreeNodes(authority, authority.subMenus, checkedKeys);
            if (authority.checked) {
                checkedKeys.push(authority.id);
            } else {
                if (parent !== null) {
                    parent.checked = false;
                }
            }
        }
        return node;
    })
}

function getRoleAuthority(roleId: number) {
    openLoading('#authorities', '正在加载，请稍候...');
    adminApi.roleAuthorityGet(roleId)
        .then((res) => {
            const checkedKeys: number[] = [];
            treeDataRef.value = authoritiesToTreeNodes(null, res, checkedKeys);
            treeCheckedKeysRef.value = checkedKeys;
        })
        .catch((e) => {
            ElMessage({type: 'error', showClose: true, message: e.message});
        })
        .finally(() => {
            closeLoading();
        })
}

watch(() => props.editData, (editData) => {
    getRoleAuthority(editData.roleId);
})

function handleSubmit() {
    const menuIds: number[] = [];
    menuIds.push(...treeRef.value.getHalfCheckedKeys());
    menuIds.push(...treeRef.value.getCheckedKeys());

    ElMessageBox.confirm('确认保存角色菜单', '警告', {type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消'})
        .then(() => {
            openLoading('#authorities', '正在提交，请稍候...');
            adminApi.roleAuthoritySet(props.editData.roleId, menuIds)
                .then(() => {
                    close();
                    ElMessage({type: 'success', showClose: true, message: '设置成功'});
                })
                .catch((e) => {
                    ElMessage({type: 'error', showClose: true, message: e.message});
                })
                .finally(() => {
                    closeLoading();
                })
        })
        .catch(() => {
        })
}

</script>

<style scoped>

.authorities {
    width: 100%;
    height: 100%;
    /*display: flex;*/
    /*justify-content: center;*/
}

.menu-tree {
    width: 100%;
    height: 572px;
    border: #e9e9eb solid 1px;
    border-radius: 5px;
}

</style>