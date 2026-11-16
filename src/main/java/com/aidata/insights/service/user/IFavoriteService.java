package com.aidata.insights.service.user;

import com.aidata.insights.vo.user.FavoriteListVO;

public interface IFavoriteService {

    /**
     * 收藏岗位：岗位不存在报 404，已收藏时按幂等处理
     */
    void addFavorite(Long jobId);

    /**
     * 取消收藏：未收藏时按幂等处理
     */
    void removeFavorite(Long jobId);

    /**
     * 当前登录用户的收藏列表（按收藏时间倒序分页）
     */
    FavoriteListVO listFavorites(long page, long size);

    /**
     * 当前登录用户是否收藏了该岗位；未登录直接返回 false
     */
    boolean isFavorited(Long jobId);
}
