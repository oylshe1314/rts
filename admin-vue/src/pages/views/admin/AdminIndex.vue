<template>
    <div>
        <div>
            <el-form :model="formData" inline>
                <el-form-item>
                    <el-select v-model.number="formData.roleId" placeholder="角色" style="width: 200px">
                        <el-option :key="0" :value="0" label="所有角色"/>
                        <el-option v-for="roleOption in roleSelectOptionsRef" :key="roleOption.id" :value="roleOption.id" :label="roleOption.name"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-input v-model="formData.username" placeholder="用户名" style="width: 200px"></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button @click="handleQuery">查询</el-button>
                    <el-button @click="handleCommand('add', null)">添加</el-button>
                    <el-button type="danger" :disabled="btnDeleteDisabledRef" @click="handleCommand('deleteSelection', null)">删除</el-button>
                </el-form-item>
            </el-form>
        </div>
        <div>
            <el-table v-loading="tableLoadingRef" :data="tableData" @selection-change="handleSelect" empty-text="无数据" height="640">
                <el-table-column type="selection" width="40" align="center"/>
                <el-table-column type="index" label="序号" width="60" align="center"/>
                <el-table-column prop="roleName" label="角色" width="120" align="left"/>
                <el-table-column prop="username" label="用户名" width="160" align="left"/>
                <el-table-column prop="nickname" label="昵称" width="160" align="left"/>
                <el-table-column prop="avatar" label="头像" width="80" align="center">
                    <template #default="scope">
                        <el-popover v-if="scope.row.avatar && scope.row.avatar !==  ''" placement="bottom" trigger="click" width="184px">
                            <img alt="" :src="getAvatar(scope.row.avatar)" style="width: 160px; height: 160px;">
                            <template #reference>
                                <el-button type="primary" link>预览</el-button>
                            </template>
                        </el-popover>
                    </template>
                </el-table-column>
                <el-table-column prop="email" label="邮箱" width="200" align="left"/>
                <el-table-column prop="mobile" label="手机" width="160" align="left"/>
                <el-table-column prop="state" label="状态" width="80" align="center">
                    <template #default="scope">
                        <span :style="{color: scope.row.state === 0 ? '#FF0000' : '#409EFF'}">{{ scope.row.state === 0 ? '禁用' : '启用' }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="remark" label="备注" align="left" show-overflow-tooltip/>
                <el-table-column prop="updateBy" label="操作人" width="120" align="center"/>
                <el-table-column :formatter="(row: AdminDto) => formatTime(row.updateTime)" prop="updateTime" label="操作时间" width="200" align="center"/>
                <el-table-column fixed="right" label="操作" align="center" width="120">
                    <template #default="scope">
                        <el-dropdown trigger="click" @command="(command: string) => {handleCommand(command, scope.row)}">
                            <el-button link type="primary" size="default">编辑</el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <!--                                    <el-dropdown-item command="detail">详情</el-dropdown-item>-->
                                    <el-dropdown-item command="modify" :disabled="scope.row.id === 1">修改</el-dropdown-item>
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
            <admin-add v-model="showAddRef" :edit-data="editDataRef" @add-success="handleEditSuccess"/>
        </div>
        <div>
            <admin-update v-model="showUpdateRef" :edit-data="editDataRef" @update-success="handleEditSuccess"/>
        </div>
    </div>
</template>

<script setup lang="ts">

import {ref, reactive, onMounted} from "vue";

import {ElMessage, ElMessageBox} from "element-plus";

import type {AdminDto, AdminQueryDto} from '@/api/admin.ts';
import adminApi from '@/api/admin.ts';

import type {RoleOptionDto} from "@/api/common.ts";
import commonApi from "@/api/common.ts";

import {getAvatar} from "@/util/avatars.ts";

import {formatTime} from "@/util/time.ts";

import AdminAdd from "@/pages/views/admin/AdminAdd.vue";

import type {AdminExtendedDto} from "@/pages/views/admin/AdminUpdate.vue";
import AdminUpdate from "@/pages/views/admin/AdminUpdate.vue";

const formData = reactive({
    roleId: 0,
    username: '',
    phone: '',
    email: '',
});

const roleSelectOptionsRef = ref<RoleOptionDto[]>([]);

const btnDeleteDisabledRef = ref<boolean>(true);

onMounted(() => {
    commonApi.roleOptions()
        .then((res) => {
            roleSelectOptionsRef.value = res;
        })
        .catch((e) => {
            ElMessage({type: 'error', showClose: true, message: '查询角色列表失败: ' + e.message});
        });
})

const tableLoadingRef = ref<boolean>(false);
const tableData = ref<AdminDto[]>([]);

function query() {
    const queryDto: AdminQueryDto = {roleId: null, username: null, phone: null, email: null};
    if (formData.roleId !== 0) {
        queryDto.roleId = formData.roleId;
    }
    if (formData.username !== '') {
        queryDto.username = formData.username;
    }
    if (formData.phone !== '') {
        queryDto.phone = formData.phone;
    }
    if (formData.email !== '') {
        queryDto.email = formData.email;
    }

    tableLoadingRef.value = true;
    adminApi.query(pagination.value.currentPage, pagination.value.pageSize, queryDto).then(res => {
        tableData.value = res.results;
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

function handleQuery() {
    pagination.value.currentPage = 1;
    query();
}

onMounted(() => query());

const selectionIds: number[] = [];

function handleSelect(rows: AdminDto[]) {
    selectionIds.length = 0;
    if (rows.length === 0) {
        btnDeleteDisabledRef.value = true;
    } else {
        rows.forEach((row) => selectionIds.push(row.id));
        btnDeleteDisabledRef.value = false;
    }
}

const showAddRef = ref<boolean>(false);
const showUpdateRef = ref<boolean>(false);
const editDataRef = ref<AdminExtendedDto>({
    id: 0,
    roleId: null,
    username: null,
    password: null,
    phone: null,
    email: null,
    nickname: null,
    avatar: null,
    remark: null
});

function handleEditSuccess() {
    query();
}

function changeState(row: AdminDto, state: number) {
    adminApi.stateChange([row.id], state)
        .then(() => {
            ElMessage({type: 'success', showClose: true, message: '操作成功'});
            row.state = state;
        })
        .catch((e) => {
            ElMessage({type: 'error', showClose: true, message: e.message});
        })
}

function handleCommand(command: string, row: AdminDto | null) {
    switch (command) {
        case 'add':
            showAddRef.value = true;
            break
        case 'detail':
            break
        case 'modify':
            editDataRef.value = {
                id: row!.id,
                roleId: row!.roleId,
                username: row!.username,
                password: '',
                phone: row!.phone,
                email: row!.email,
                nickname: row!.nickname,
                avatar: row!.avatar,
                remark: row!.remark,
            };
            showUpdateRef.value = true;
            break
        case 'enable':
            changeState(row!, 1);
            break
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
                    adminApi.delete([row!.id,])
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
    }
}

</script>

<style>
.cell {
    height: 23px;
}
</style>

<style scoped>

</style>