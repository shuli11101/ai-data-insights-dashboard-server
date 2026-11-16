package com.aidata.insights.controller.personalInfo;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.service.personalInfo.IPersonalInfoService;
import com.aidata.insights.vo.personalInfo.PersonalInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人信息接口
 *
 * <p>需要登录（JWT），只返回当前用户自己的资料与统计。</p>
 */
@Tag(name = "05. 个人信息")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class PersonalInfoController {

    private final IPersonalInfoService personalInfoService;

    /**
     * 获取个人信息
     */
    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public Result<PersonalInfoVO> personalInfo() {
        return Result.ok(personalInfoService.getPersonalInfo());
    }
}
