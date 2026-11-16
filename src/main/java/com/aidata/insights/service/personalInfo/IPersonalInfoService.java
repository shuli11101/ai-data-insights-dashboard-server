package com.aidata.insights.service.personalInfo;

import com.aidata.insights.vo.personalInfo.PersonalInfoVO;

public interface IPersonalInfoService {

    /**
     * 当前登录用户的个人信息与统计
     */
    PersonalInfoVO getPersonalInfo();
}
