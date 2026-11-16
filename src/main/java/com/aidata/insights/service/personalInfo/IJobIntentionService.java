package com.aidata.insights.service.personalInfo;

import com.aidata.insights.dto.personalInfo.SaveJobIntentionRequest;
import com.aidata.insights.vo.personalInfo.JobIntentionVO;

public interface IJobIntentionService {

    /**
     * 获取当前用户的求职意向，未填写时返回 null
     */
    JobIntentionVO getPersonal();

    /**
     * 获取当前用户的求职意向状态，未填写时返回 false
     */
    boolean getSurveyStatus();

    /**
     * 保存 / 更新当前用户的求职意向，返回保存后的最新数据
     */
    JobIntentionVO setPersonalInfo(SaveJobIntentionRequest request);
}