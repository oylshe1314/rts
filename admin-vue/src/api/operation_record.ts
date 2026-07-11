/**
 * operation_record.ts
 * create by Snake as 2026-07-03
 * @description:
 */

import type {PageResultDto} from '@/api/request.ts';
import service from '@/api/request.ts';

export interface OperationRecordQueryDto {
    operatorId: number | null;
    operation: string | null;
    beginTime: string | null;
    endTime: string | null;
}

export interface OperationRecordDto {
    id: number;
    operatorId: number;
    operator: string;
    operation: string;
    operateArgs: string;
    remark: string;
    ipAddress: string;
    operateTime: string;
}

export default {
    query: (pageNo: number, pageSize: number, params: OperationRecordQueryDto): Promise<PageResultDto<OperationRecordDto>> => {
        return service({
            url: '/operation/record/query',
            method: 'get',
            params: {pageNo: pageNo, pageSize: pageSize, ...params}
        });
    }
}