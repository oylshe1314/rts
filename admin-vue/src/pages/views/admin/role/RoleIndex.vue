<template>
    <div>
        <div>
            <el-form :model="formData" inline>
                <el-form-item>
                    <el-input v-model="formData.name" type="text" placeholder="名称" style="width: 200px"></el-input>
                </el-form-item>
                <el-form-item>
                    <el-input v-model="formData.code" type="text" placeholder="代码" style="width: 200px"></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button @click="handleQueryBtnClick">查询</el-button>
                    <el-button type="primary" @click="handleCommand('add', null)">添加</el-button>
                    <el-button type="danger" :disabled="btnDeleteDisabledRef" @click="handleCommand('deleteSelection', null)">删除</el-button>
                    <el-button type="warning" :disabled="btnCompareAuthorityDisabledRef" @click="handleCommand('compareAuthority', null)">比较权限</el-button>
                </el-form-item>
            </el-form>
        </div>
        <div>
            <el-table v-loading="tableLoadingRef" :data="tableDataRef" @selection-change="handleSelect" empty-text="无数据" height="640">
                <el-table-column type="selection" width="40" align="center"/>
                <el-table-column type="index" label="序号" width="80" align="center"/>
                <el-table-column prop="name" label="名称" width="200" align="left"/>
                <el-table-column prop="code" label="代码" width="240" align="left"/>
                <el-table-column prop="state" label="状态" width="120" align="center">
                    <template #default="scope">
                        <span :style="{color: scope.row.state === 0 ? '#FF0000' : '#409EFF'}">{{ scope.row.state === 0 ? '禁用' : '启用' }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="remark" label="备注" align="left" :show-overflow-tooltip="true"/>
                <el-table-column prop="updateBy" label="操作人" align="center" width="80"/>
                <el-table-column :formatter="(row: RoleDto) => formatTime(row.updateTime)" prop="updateTime" label="操作时间" width="220" align="center"/>
                <el-table-column fixed="right" label="操作" align="center" width="120">
                    <template #default="scope">
                        <el-dropdown trigger="click" @command="(command: string) => {handleCommand(command, scope.row)}">
                            <el-button link type="primary" size="default">编辑</el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <!--<el-dropdown-item command="detail">详情</el-dropdown-item>-->
                                    <el-dropdown-item command="modify" :disabled="scope.row.id === 1">修改</el-dropdown-item>
                                    <el-dropdown-item command="authority">权限</el-dropdown-item>
                                    <el-dropdown-item v-if="scope.row.state === 0" command="enable" :disabled="scope.row.id === 1">启用</el-dropdown-item>
                                    <el-dropdown-item v-else command="disable" :disabled="scope.row.id === 1">禁用</el-dropdown-item>
                                    <el-dropdown-item command="delete" :disabled="scope.row.id === 1">删除</el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
                    </template>
                </el-table-column>
            </el-table>
        </div>
        <div>
            <el-pagination v-bind="pagination" style="justify-content: center"/>
        </div>
        <div>
            <role-add v-model="showAddRef" @add-success="handleEditSuccess"/>
        </div>
        <div>
            <role-update v-model="showUpdateRef" :edit-data="editDataRef" @update-success="handleEditSuccess"/>
        </div>
        <div>
            <role-edit-authorities v-model="showEditAuthoritiesRef" :edit-data="editAuthoritiesDataRef"/>
        </div>
        <div>
            <role-compare-authorities v-model="showCompareAuthoritiesRef" :edit-data="compareAuthoritiesDataRef"/>
        </div>
    </div>
</template>

<script setup lang="ts">

import {onMounted, reactive, ref} from "vue";

import {ElMessage, ElMessageBox} from "element-plus";

import type {RoleDto, RoleQueryDto, RoleUpdateDto} from '@/api/admin.ts';
import adminApi from '@/api/admin.ts';

import {formatTime} from "@/util/time.ts"

import RoleAdd from "@/pages/views/admin/role/RoleAdd.vue";
import RoleUpdate from "@/pages/views/admin/role/RoleUpdate.vue";
import RoleEditAuthorities from "@/pages/views/admin/role/RoleEditAuthorities.vue";
import RoleCompareAuthorities from "@/pages/views/admin/role/RoleCompareAuthorities.vue";

const formData = reactive<RoleQueryDto>({
    name: '',
    code: '',
});

const btnDeleteDisabledRef = ref<boolean>(true);
const btnCompareAuthorityDisabledRef = ref<boolean>(true);

const tableLoadingRef = ref<boolean>(false);
const tableDataRef = ref<RoleDto[]>([]);

function query() {
    const queryDto: RoleQueryDto = {name: null, code: null};
    if (formData.name !== '') {
        queryDto.name = formData.name;
    }
    if (formData.code !== '') {
        queryDto.code = formData.code;
    }

    tableLoadingRef.value = true;
    adminApi.roleQuery(pagination.value.currentPage, pagination.value.pageSize, queryDto).then(res => {
        tableDataRef.value = res.results;
        pagination.value.total = res.total;
        pagination.value.pageCount = res.pages < 1 ? 1 : res.pages;
    }).catch(e => {
        ElMessage({type: 'error', showClose: true, message: e.message});
    }).finally(() => {
        tableLoadingRef.value = false;
    })
}

const pagination = ref({
        layout: "prev,pager,next",
        background: true,
        currentPage: 1,
        pageSize: 15,
        total: 0,
        pageCount: 1,
        prevIcon: 'CaretLeft',
        nextIcon: 'CaretRight',
        onCurrentChange: (pageNo: number) => {
            pagination.value.currentPage = pageNo
            query()
        },
        onPrevClick: (pageNo: number) => {
            if (pageNo > 1) {
                pagination.value.currentPage = pageNo - 1
                query()
            }
        },
        onNextClick: (pageNo: number) => {
            if (pageNo < pagination.value.pageCount) {
                pagination.value.currentPage = pageNo + 1
                query()
            }
        }
    }
);

onMounted(() => query());

function handleQueryBtnClick() {
    pagination.value.currentPage = 1;
    query();
}

const selectionIds: number[] = [];

function handleSelect(rows: RoleDto[]) {
    if (rows.length === 0) {
        selectionIds.splice(0);
        btnDeleteDisabledRef.value = true;
        btnCompareAuthorityDisabledRef.value = true;
    } else {
        rows.forEach((row) => selectionIds.push(row.id));
        btnDeleteDisabledRef.value = false;
        if (selectionIds.length < 2 || selectionIds.length > 4) {
            btnCompareAuthorityDisabledRef.value = true;
        } else {
            btnCompareAuthorityDisabledRef.value = false;
        }
    }
}

const showAddRef = ref<boolean>(false);
const showUpdateRef = ref<boolean>(false);
const editDataRef = ref<RoleUpdateDto>({id: 0, name: null, code: null, remark: null});

const showEditAuthoritiesRef = ref<boolean>(false);
const editAuthoritiesDataRef = ref<{ roleId: number }>({roleId: 0});

const showCompareAuthoritiesRef = ref<boolean>(false);
const compareAuthoritiesDataRef = ref<{ roleIds: number[] }>({roleIds: []});

function handleEditSuccess() {
    query();
}

function changeState(row: RoleDto, status: number) {
    adminApi.roleStateChange([row.id,], status)
        .then(() => {
            row.state = status;
            ElMessage({type: 'success', showClose: true, message: '操作成功'});
        })
        .catch((e) => {
            ElMessage({type: 'error', showClose: true, message: e.message});
        })
}

function handleCommand(command: string, row: RoleDto | null) {
    switch (command) {
        case 'add':
            showAddRef.value = true;
            break
        case 'detail':
            break;
        case 'modify':
            editDataRef.value = {
                id: row!.id,
                name: row!.name,
                code: row!.code,
                remark: row!.remark,
            };
            showUpdateRef.value = true;
            break;
        case 'authority':
            editAuthoritiesDataRef.value = {roleId: row!.id};
            showEditAuthoritiesRef.value = true;
            break;
        case 'enable':
            changeState(row!, 1);
            break;
        case 'disable':
            ElMessageBox.confirm('确认禁用', '警告', {type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消'})
                .then(() => {
                    changeState(row!, 0);
                })
                .catch(() => {
                });
            break
        case 'delete':
            ElMessageBox.confirm('确认删除', '警告', {type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消'})
                .then(() => {
                    adminApi.roleDelete([row!.id,])
                        .then(() => {
                            query();
                        })
                        .catch((e) => {
                            ElMessage({type: 'error', showClose: true, message: e.message});
                        })
                })
                .catch(() => {
                });
            break;
        case 'deleteSelection':
            ElMessageBox.confirm('确认删除', '警告', {type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消'})
                .then(() => {
                    adminApi.roleDelete(selectionIds)
                        .then(() => {
                            query();
                        })
                        .catch((e) => {
                            ElMessage({type: 'error', showClose: true, message: e.message});
                        })
                })
                .catch(() => {
                });
            break;
        case 'compareAuthority':
            compareAuthoritiesDataRef.value = {roleIds: selectionIds};
            showCompareAuthoritiesRef.value = true;
            break;
    }
}

</script>

<style scoped>

</style>