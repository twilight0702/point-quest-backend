package com.twilight.pointquestbackend.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.domain.TaskSubmission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.twilight.pointquestbackend.vo.AdminSubmissionDetailVO;
import com.twilight.pointquestbackend.vo.AdminSubmissionVO;
import com.twilight.pointquestbackend.vo.PageSubmissionsVO;
import org.apache.ibatis.annotations.Param;

/**
* @author twilight
* @description 针对表【task_submission(任务提交表：用户提交任务完成证明及审批状态)】的数据库操作Mapper
* @createDate 2025-12-26 20:16:34
* @Entity com.twilight.pointquestbackend.domain.TaskSubmission
*/
public interface TaskSubmissionMapper extends BaseMapper<TaskSubmission> {
    /**
     * 分页获得某用户的提交记录
     */
    Page<PageSubmissionsVO.TaskSubmissionSummaryVO> pageSubmissions(Page<PageSubmissionsVO.TaskSubmissionSummaryVO> page, @Param("userId") Long userId);

    /**
     * 管理端分页查看提交
     */
    Page<AdminSubmissionVO> pageAdminSubmissions(Page<AdminSubmissionVO> page, @Param("status") String status);

    /**
     * 管理端查看提交详情
     */
    AdminSubmissionDetailVO getAdminSubmissionDetail(@Param("submissionNo") String submissionNo);

}




