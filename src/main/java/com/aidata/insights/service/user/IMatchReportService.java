package com.aidata.insights.service.user;

import com.aidata.insights.dto.user.SaveMatchReportRequest;
import com.aidata.insights.vo.user.MatchReportListVO;
import com.aidata.insights.vo.user.MatchReportVO;

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
     * 删除当前用户已保存的分析报告
     */
    boolean deleteReport(Long jobId);
}
