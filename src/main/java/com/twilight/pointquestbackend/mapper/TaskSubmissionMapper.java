package com.twilight.pointquestbackend.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.domain.TaskSubmission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.twilight.pointquestbackend.vo.PageSubmissionsVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

}




