<template>
    <el-dialog v-model="showEditRef" title="角色权限比较" width="800px" @close="handleClose" destroy-on-close center>
        <div id="authorities" class="authorities">
            <div style="height: 600px">
                <el-table v-loading="tableLoadingRef"
                          :data="tableDataRef"
                          :row-class-name="tableRowClassName"
                          row-key="id"
                          empty-text="无数据"
                          height="584"
                          border>
                    <el-table-column prop="label" width="200" align="left" style="padding: 0;" :resizable="false">
                        <template #header>
                            <div class="diagonal-header">
                                <svg style="position:absolute;top:0;left:0;width:100%;height:100%;">
                                    <line
                                        x1="0"
                                        y1="0"
                                        x2="100%"
                                        y2="100%"
                                        stroke="#EBEEF5"
                                        stroke-width="1"
                                    />
                                </svg>
                                <span class="text-bottom-left">权限</span>
                                <span class="text-top-right">角色</span>
                            </div>
                        </template>
                    </el-table-column>
                    <el-table-column v-for="(column, index) in tableColumnsRef" :label="column.label" :resizable="false" align="center">
                        <template #default="scope">
                            <el-icon v-if="scope.row.status[index] === 0">
                                <SemiSelect/>
                            </el-icon>
                            <el-icon v-else>
                                <el-icon v-if="scope.row.status[index] === 1">
                                    <Select/>
                                </el-icon>
                                <el-icon v-else>
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024">
                                        <path fill="none" stroke="currentColor" stroke-width="100" stroke-linejoin="round" d="M160 160 h704 v704 h-704 z"/>
                                    </svg>
                                </el-icon>
                            </el-icon>
                        </template>
                    </el-table-column>
                </el-table>
            </div>
            <div style="width: 100%; display: flex; justify-content: center">
                <el-button type="primary" style="width: 80px" @click="close">关闭</el-button>
            </div>
        </div>
    </el-dialog>
</template>

<script setup lang="ts">

import {ref, watch} from 'vue'

import {closeLoading, openLoading} from "@/util/loading.ts";

import type {RoleAuthorityComparisonDto, RoleAuthorityComparisonListDto} from "@/api/admin.ts";
import adminApi from "@/api/admin.ts";

import {ElMessage} from "element-plus";

interface Props {
    modelValue: boolean;
    editData: { roleIds: number[]; };
}

const props = withDefaults(
    defineProps<Props>(),
    {
        modelValue: false,
        editData: () => ({roleIds: []}),
    }
);

const showEditRef = ref(false)

watch(() => props.modelValue, (value) => {
    showEditRef.value = value
})

function close() {
    showEditRef.value = false
}

interface Emits{
    (emit: 'update:modelValue', value: boolean): void;
}

const emits = defineEmits<Emits>();

function handleClose() {
    emits('update:modelValue', false)
}

interface TableColumn {
    id: number;
    label: string;
}

interface TableRow {
    id: number;
    type: number;
    label: string;
    icon: string;
    status: number[];
    highlight: boolean;
    children: TableRow[];
}

interface TableData {
    columns: TableColumn[];
    rows: TableRow[];
}

const tableLoadingRef = ref<boolean>(false);
const tableDataRef = ref<TableRow[]>([]);
const tableColumnsRef = ref<TableColumn[]>([]);

function rolesToSet(roles: number[]): Set<number> {
    const idSet = new Set<number>();
    for (const roleId of roles) {
        idSet.add(roleId);
    }
    return idSet;
}

function comparisonsToTableRow(parent: TableRow | null, columns: TableColumn[], comparisons: RoleAuthorityComparisonDto[]): TableRow[] {
    if (comparisons.length === 0) {
        return [];
    }

    return comparisons.map(comparison => {
        const roleIds = rolesToSet(comparison.roles)
        const row: TableRow = {
            id: comparison.id,
            type: comparison.type,
            label: comparison.name,
            icon: comparison.icon,
            status: columns.map((column: TableColumn) => (roleIds.has(column.id) ? 1 : 0)),
            highlight: comparison.highlight,
            children: [],
        };

        if (comparison.subMenus !== null) {
            row.children = comparisonsToTableRow(row, columns, comparison.subMenus);
        }
        if (parent) {
            if (row.highlight) {
                parent.highlight = true;
            }
            for (let i = 0; i < row.status.length; i++) {
                if (parent.status[i] !== row.status[i]) {
                    parent.status[i] = 2;
                }
            }
        }
        return row;
    });
}

function comparisonListToTableData(comparisonList: RoleAuthorityComparisonListDto): TableData {
    const tableData: TableData = {columns: [], rows: []};
    tableData.columns = comparisonList.roleList.map(role => ({id: role.id, label: role.name}));
    tableData.rows = comparisonsToTableRow(null, tableData.columns, comparisonList.comparisonList);
    return tableData;
}

function getRoleAuthorityComparisonList(roleIds: number[]) {
    openLoading('#authorities', '正在加载，请稍候...');
    adminApi.roleAuthorityCompare(roleIds)
        .then((res) => {
            const tableData = comparisonListToTableData(res);
            tableColumnsRef.value = tableData.columns;
            tableDataRef.value = tableData.rows;
        })
        .catch((e) => {
            ElMessage({type: 'error', showClose: true, message: e.message});
        })
        .finally(() => {
            closeLoading();
        });
}

watch(() => props.editData, (editData) => {
    getRoleAuthorityComparisonList(editData.roleIds);
});

const tableRowClassName = (scope: any): string => {
    return scope.row.highlight ? 'primary-row' : '';
}

</script>

<style>
.el-table .primary-row {
    color: #409EFF;
    --el-table-tr-bg-color: var(--el-color-primary-light-9);
}
</style>

<style scoped>

:deep(.el-table__header-wrapper .el-table__header tr) {
    height: auto !important;
}

:deep(.el-table__header-wrapper .el-table__header th) {
    padding: 0 !important;
    height: 48px !important;
    vertical-align: middle !important;
}

:deep(.el-table__header-wrapper .el-table__header th .cell) {
    padding: 0 !important;
    line-height: 1 !important;
}

:deep(.el-table__body-wrapper .el-table__body td .cell) {
    padding: 0 !important;
}

.diagonal-header {
    position: relative;
    width: 100%;
    height: 48px;
}

.text-bottom-left {
    position: absolute;
    bottom: 12px;
    left: 24px;
    color: #A7A8AA;
    z-index: 2;
}

.text-top-right {
    position: absolute;
    top: 12px;
    right: 24px;
    color: #A7A8AA;
    z-index: 2;
}
</style>