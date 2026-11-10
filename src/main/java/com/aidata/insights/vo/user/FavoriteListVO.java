package com.aidata.insights.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 收藏列表结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "收藏列表结果")
public class FavoriteListVO {

    @Schema(description = "当前页收藏岗位")
    private List<FavoriteJobVO> list;

    @Schema(description = "收藏总数", example = "1")
    private long total;
}
