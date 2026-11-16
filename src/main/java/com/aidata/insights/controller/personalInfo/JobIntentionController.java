package com.aidata.insights.controller.personalInfo;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.dto.personalInfo.SaveJobIntentionRequest;
import com.aidata.insights.service.personalInfo.IJobIntentionService;
import com.aidata.insights.vo.personalInfo.JobIntentionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 求职意向接口
 *
 * <p>需要登录（JWT），只操作当前用户自己的意向。</p>
 */
@Tag(name = "04. 求职意向")
@RestController
@RequiredArgsConstructor
public class JobIntentionController {

    private final IJobIntentionService jobIntentionService;

    /**
     * 获取当前用户求职意向信息
     */
    @Operation(summary = "获取当前用户求职意向")
    @GetMapping("/getPersonal")
    public Result<JobIntentionVO> getPersonal() {
        return Result.ok(jobIntentionService.getPersonal());
    }

    /**
     * 获取当前用户是否有求职意向
     */
    @Operation(summary = "获取当前用户是否有求职意向")
    @GetMapping("/surveyStatus")
    public Result<Boolean> getSurveyStatus() {
        return Result.ok(jobIntentionService.getSurveyStatus());
    }

    /**
     * 保存 / 更新当前用户求职意向信息
     */
    @Operation(summary = "保存/更新当前用户求职意向")
    @PostMapping("/setPersonalInfo")
    public Result<JobIntentionVO> setPersonalInfo(@Valid @RequestBody SaveJobIntentionRequest request) {
        return Result.ok(jobIntentionService.setPersonalInfo(request));
    }
}