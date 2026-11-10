package com.aidata.insights.controller.user;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.dto.user.FavoriteRequest;
import com.aidata.insights.service.user.FavoriteService;
import com.aidata.insights.vo.user.FavoriteListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户收藏接口：收藏 / 取消收藏 / 收藏列表
 *
 * <p>需要登录（JWT），收藏记录按当前用户写在 user_favorite 表。</p>
 */
@Tag(name = "03. 用户")
@RestController
@RequestMapping("/user/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 收藏岗位
     */
    @Operation(summary = "收藏岗位")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody FavoriteRequest request) {
        favoriteService.addFavorite(request.getJobId());
        return Result.ok();
    }

    /**
     * 取消收藏岗位
     */
    @Operation(summary = "取消收藏岗位")
    @DeleteMapping
    public Result<Void> remove(@Valid @RequestBody FavoriteRequest request) {
        favoriteService.removeFavorite(request.getJobId());
        return Result.ok();
    }

    /**
     * 获取当前用户的收藏列表
     */
    @Operation(summary = "收藏列表：按收藏时间倒序分页")
    @GetMapping("/list")
    public Result<FavoriteListVO> list(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(favoriteService.listFavorites(page, size));
    }
}
