package com.aidata.insights.service.personalInfo;

import com.aidata.insights.vo.personalInfo.AvatarVO;
import com.aidata.insights.vo.personalInfo.PersonalInfoVO;
import org.springframework.web.multipart.MultipartFile;

public interface IPersonalInfoService {

    /**
     * 当前登录用户的个人信息与统计
     */
    PersonalInfoVO getPersonalInfo();

    /**
     * 更新个人信息
     */
    PersonalInfoVO updatePersonalInfo(PersonalInfoVO personalInfoVO);

    /**
     * 上传当前登录用户的头像，落库后返回可访问地址
     */
    AvatarVO uploadAvatar(MultipartFile file);
}
