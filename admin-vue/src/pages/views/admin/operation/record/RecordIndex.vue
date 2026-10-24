<template>
    <div>
        <div>
            <el-form :model="formData" inline>
                <el-form-item>
                    <el-select v-model.number="formData.operatorId"
                               filterable
                               clearable
                               remote
                               :remote-method="adminOptionsQuery"
                               :loading="adminSelectorLoadingRef"
                               placeholder="操作人"
                               style="width: 200px">
                        <el-option v-for="adminOption in adminSelectorOptionsRef" :key="adminOption.id" :value='adminOption.id' :label="adminOption.username"/>
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-select v-model.number="formData.operation" placeholder="操作" style="width: 200px">
                        <el-option key="" value="" label="操作"/>
                        <el-option key="add" value="add" label="add"/>
                        <el-option key="update" value="update" label="update"/>
                        <el-option key="delete" value="delete" label="delete"/>
                        <el-option key="changeState" value="changeState" label="changeState"/>
                        <el-option key="login" value="login" label="login"/>
                        <el-option key="logout" value="logout" label="logout"/>
                        <el-option key="kickout" value="kickout" label="kickout"/>
                        <el-option key="changeDetails" value="changeDetails" label="changeDetails"/>
                        <el-option key="changePassword" value="changePassword" label="changePassword"/>
                        <el-option key="setAuthorities" value="setAuthorities" label="setAuthorities"/>
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-date-picker
                        v-model="formData.beginTime"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                        placeholder="开始时间"
                        style="width: 200px"
                    />
                </el-form-item>
                <el-form-item>
                    <el-date-picker
                        v-model="formData.endTime"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                        placeholder="结束时间"
                        style="width: 200px"
                    />
                </el-form-item>
                <el-form-item>
                    <el-button @click="handleQuery">查询</el-button>
                </el-form-item>
            </el-form>
        </div>
        <div>
            <el-table v-loading="tableLoadingRef" :data="tableData" empty-text="无数据" height="640">
                <el-table-column label="序号" type="index" width="80" align="center"/>
                <el-table-column label="操作人" prop="operator" width="200" align="left"/>
                <el-table-column label="操作" prop="operation" width="160" align="left"/>
                <el-table-column label="操作参数" prop="operateArgs" width="480" align="left"/>
                <el-table-column label="备注" prop="remark" align="left" show-overflow-tooltip/>
                <el-table-column label="登录地址" prop="ipAddress" width="160" align="center"/>
                <el-table-column :formatter="(row: OperationRecordDto) => formatTime(row.operateTime)"
                                 label="操作时间"
                                 prop="updateTime" width="200"
                                 align="center"
                />
            </el-table>
        </div>
        <div>
            <el-pagination style="justify-content: center" v-bind="pagination"/>
        </div>
    </div>
</template>

<script setup lang="ts">

import {onMounted, reactive, ref} from "vue";

import {ElMessage} from "element-plus";

import type {AdminOptionDto} from '@/api/common.ts';
import commonApi from '@/api/common.ts';

import type {OperationRecordDto, OperationRecordQueryDto} from "@/api/operation_record.ts";
import operationRecordApi from "@/api/operation_record.ts";

import {formatTime} from "@/util/time.ts";

const formData = reactive<OperationRecordQueryDto>({
    operatorId: null,
    operation: '',
    beginTime: '',
    endTime: '',
});

const adminSelectorLoadingRef = ref<boolean>(false);
const adminSelectorOptionsRef = ref<AdminOptionDto[]>([]);

function adminsToSelectOptions(roles: AdminOptionDto[]) {
    const options: AdminOptionDto[] = [];
    roles.forEach((role) => options.push(role));
    return options;
}

function adminOptionsQuery(username: string) {
    adminSelectorLoadingRef.value = true;
    commonApi.adminOptions(username === '' ? null : username)
        .then(res => {
            adminSelectorOptionsRef.value = adminsToSelectOptions(res);
        })
        .catch((e) => {
            ElMessage({type: 'error', showClose: true, message: '查询角色列表失败: ' + e.message});
        })
        .finally(() => {
            adminSelectorLoadingRef.value = false;
        })
    ;
}

const tableLoadingRef = ref<boolean>(false);
const tableData = ref<OperationRecordDto[]>([]);

function query() {
    const queryDto: OperationRecordQueryDto = {operatorId: null, operation: null, beginTime: null, endTime: null};
    if (formData.operatorId !== null && formData.operatorId !== 0) {
        queryDto.operatorId = formData.operatorId;
    }
    if (formData.operation !== '') {
        queryDto.operation = formData.operation;
    }
    if (formData.beginTime !== '') {
        queryDto.beginTime = formData.beginTime;
    }
    if (formData.endTime !== '') {
        queryDto.endTime = formData.endTime;
    }

    tableLoadingRef.value = true;
    operationRecordApi.query(pagination.value.currentPage, pagination.value.pageSize, queryDto).then(res => {
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
            pagination.value.currentPage = pageNo;
            query()
        },
        onPrevClick: (pageNo: number) => {
            if (pageNo > 1) {
                pagination.value.currentPage = pageNo - 1;
                query();
            }
        },
        onNextClick: (pageNo: number) => {
            if (pageNo < pagination.value.pageCount) {
                pagination.value.currentPage = pageNo + 1;
                query();
            }
        }
    }
);

function handleQuery() {
    pagination.value.currentPage = 1;
    query();
}

onMounted(() => query());

</script>

<style scoped>

</style>