package com.aidata.insights.controller.personalInfo;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.service.personalInfo.IPersonalInfoService;
import com.aidata.insights.vo.personalInfo.AvatarVO;
import com.aidata.insights.vo.personalInfo.PersonalInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 更新个人信息
     */
    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public Result<PersonalInfoVO> updatePersonalInfo(@RequestBody PersonalInfoVO personalInfoVO) {
        return Result.ok(personalInfoService.updatePersonalInfo(personalInfoVO), "更新成功");
    }

    /**
     * 上传头像
     */
    @Operation(summary = "上传头像")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AvatarVO> uploadAvatar(
            @Parameter(description = "头像文件，支持 jpg/png/webp，不超过 2MB", required = true)
            @RequestParam("file") MultipartFile file) {
        // 成功响应的 code 固定为 200，无需显式传状态码
        return Result.ok(personalInfoService.uploadAvatar(file), "上传成功");
    }
}
