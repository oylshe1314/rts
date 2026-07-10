<template>
    <div>
        <div>
            <el-form :model="formData" inline>
                <el-form-item>
                    <el-select v-model.number="formData.type" placeholder="类型" style="width: 200px">
                        <el-option :key="0" :value="0" label="所有"/>
                        <el-option :key="1" :value="1" label="目录"/>
                        <el-option :key="2" :value="2" label="菜单"/>
                        <el-option :key="3" :value="3" label="接口"/>
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-input v-model="formData.name" placeholder="名称" type="text" style="width: 200px"></el-input>
                </el-form-item>
                <el-form-item>
                    <el-input v-model="formData.path" placeholder="路径" type="text" style="width: 200px"></el-input>
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
                <el-table-column label="序号" type="index" width="80" align="center"/>
                <el-table-column label="上级名称" prop="parentName" width="160" align="left"/>
                <el-table-column label="类型" prop="typeName" width="100" align="center"/>
                <el-table-column label="图标" prop="icon" width="100" align="center">
                    <template #default="scope">
                        <el-icon v-if="scope.row.icon !== ''">
                            <component :is="scope.row.icon"/>
                        </el-icon>
                    </template>
                </el-table-column>
                <el-table-column label="名称" prop="name" width="160" align="left"/>
                <el-table-column :show-overflow-tooltip="true" label="路径" prop="path" width="260" align="left"/>
                <el-table-column label="排序值" prop="sortBy" width="80" align="center"/>
                <el-table-column label="状态" prop="state" width="80" align="center">
                    <template #default="scope">
                        <span :style="{color: scope.row.state === 0 ? '#FF0000' : '#409EFF'}">{{ scope.row.state === 0 ? '禁用' : '启用' }}</span>
                    </template>
                </el-table-column>
                <el-table-column :show-overflow-tooltip="true" label="备注" prop="remark" align="left"/>
                <el-table-column label="操作人" prop="updateBy" width="120" align="center"/>
                <el-table-column :formatter="(row: RoleDto) => formatTime(row.updateTime)" label="操作时间" prop="updateTime" width="220" align="center"/>
                <el-table-column fixed="right" label="操作" width="120" align="center">
                    <template #default="scope">
                        <el-dropdown trigger="click" @command="(command: string) => {handleCommand(command, scope.row)}">
                            <el-button link size="default" type="primary">编辑</el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <!--<el-dropdown-item command="detail">详情</el-dropdown-item>-->
                                    <el-dropdown-item command="add" :disabled="scope.row.type === 3">添加</el-dropdown-item>
                                    <el-dropdown-item command="modify">修改</el-dropdown-item>
                                    <el-dropdown-item v-if="scope.row.state === 0" command="enable">启用</el-dropdown-item>
                                    <el-dropdown-item v-else command="disable">禁用</el-dropdown-item>
                                    <el-dropdown-item command="delete">删除</el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
                    </template>
                </el-table-column>
            </el-table>
        </div>
        <div>
            <el-pagination style="justify-content: center" v-bind="pagination"/>
        </div>
        <div>
            <menu-add v-model="showAddRef" :parent-data="parentDataRef" @add-success="handleEditSuccess"/>
        </div>
        <div>
            <menu-update v-model="showUpdateRef" :edit-data="editDataRef" @update-success="handleEditSuccess"/>
        </div>
    </div>
</template>

<script lang="ts" setup>

import {onMounted, reactive, ref} from "vue";

import {ElMessage, ElMessageBox} from "element-plus";

import type {MenuDto, MenuQueryDto, MenuUpdateDto} from '@/api/menu.ts';
import menuApi from '@/api/menu.ts';

import MenuAdd from "@/pages/views/menu/MenuAdd.vue";
import MenuUpdate from "@/pages/views/menu/MenuUpdate.vue";
import adminApi, {type RoleDto} from "@/api/admin.ts";
import {formatTime} from "@/util/time.ts";

const formData = reactive<MenuQueryDto>({
    type: 0,
    name: '',
    path: '',
})

const btnDeleteDisabledRef = ref<boolean>(true);

const tableLoadingRef = ref<boolean>(false)
const tableData = ref<MenuDto[]>([])

function query() {
    const queryDto: MenuQueryDto = {type: null, name: null, path: null}
    if (formData.type && formData.type !== 0) {
        queryDto.type = formData.type
    }
    if (formData.name && formData.name !== '') {
        queryDto.name = formData.name
    }
    if (formData.path && formData.path !== '') {
        queryDto.path = formData.path
    }

    tableLoadingRef.value = true
    menuApi.query(pagination.value.currentPage, pagination.value.pageSize, queryDto).then(res => {
        tableData.value = res.results
        pagination.value.total = res.total
        pagination.value.pageCount = res.pages < 1 ? 1 : res.pages
    }).catch(e => {
        ElMessage({type: 'error', showClose: true, message: e.message})
    }).finally(() => {
        tableLoadingRef.value = false
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
            pagination.value.currentPage = pageNo;
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
)

function handleQuery() {
    pagination.value.currentPage = 1
    query()
}

onMounted(() => query());

const selectionIds: number[] = [];

function handleSelect(rows: RoleDto[]) {
    if (rows.length === 0) {
        selectionIds.splice(0);
        btnDeleteDisabledRef.value = true;
    } else {
        rows.forEach((row) => selectionIds.push(row.id));
        btnDeleteDisabledRef.value = false;
    }
}

const showAddRef = ref<boolean>(false);
const parentDataRef = ref<MenuDto | null>(null);

const showUpdateRef = ref<boolean>(false);
const editDataRef = ref<MenuUpdateDto>({id: 0, parentId: null, type: null, icon: null, name: null, path: null, sortBy: null, remark: null})

function handleEditSuccess() {
    query()
}

function changeState(row: MenuDto, state: number) {
    menuApi.stateChange([row.id], state)
        .then(() => {
            row.state = state
            ElMessage({type: 'success', showClose: true, message: '修改成功'})
        })
        .catch((e) => {
            ElMessage({type: 'error', showClose: true, message: e.message})
        })
}

function handleCommand(command: string, row: MenuDto | null) {
    switch (command) {
        case 'add':
            if (row !== null) {
                parentDataRef.value = row;
            }
            showAddRef.value = true;
            break;
        case 'detail':
            break;
        case 'modify':
            editDataRef.value = {
                id: row!.id,
                parentId: row!.parentId,
                type: row!.type,
                icon: row!.icon,
                name: row!.name,
                path: row!.path,
                sortBy: row!.sortBy,
                remark: row!.remark
            };
            showUpdateRef.value = true;
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
            break;
        case 'delete':
            ElMessageBox.confirm('确认删除', '警告', {type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消'})
                .then(() => {
                    menuApi.delete([row!.id])
                        .then(() => {
                            query()
                        })
                        .catch((e) => {
                            ElMessage({type: 'error', showClose: true, message: e.message})
                        });
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

<style scoped>

</style>