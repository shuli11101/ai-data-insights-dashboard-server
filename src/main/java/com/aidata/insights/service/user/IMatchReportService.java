package com.aidata.insights.service.user;

import com.aidata.insights.dto.user.SaveMatchReportRequest;
import com.aidata.insights.vo.user.MatchReportDeleteVO;
import com.aidata.insights.vo.user.MatchReportListVO;
import com.aidata.insights.vo.user.MatchReportVO;

import java.util.List;

public interface IMatchReportService {

    /**
     * 当前用户已保存的全部分析报告，按最后更新时间倒序
     */
    MatchReportListVO listReports();

    /**
     * 保存分析报告
     */
    MatchReportVO saveReport(SaveMatchReportRequest request);

    /**
     * 获取当前用户已保存的分析报告
     */
    MatchReportVO getReport(Long jobId);

    /**
     * 批量删除当前用户已保存的分析报告，不存在的岗位 ID 计入 missing
     */
    MatchReportDeleteVO deleteReports(List<Long> jobIds);
}
