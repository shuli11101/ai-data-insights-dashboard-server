package com.aidata.insights.mapper.home;

import com.aidata.insights.vo.home.CityOptionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 城市字典 Mapper（表 city）
 *
 * <p>city 表的 city 列是省内地级市序号（01/02…），城市名在 name 列。</p>
 */
@Mapper
public interface CityMapper {

    /**
     * 城市下拉选项：取 name 去「市」后缀，与 job_positions.city 口径对齐后按拼音排序
     */
    @Select("""
            SELECT DISTINCT TRIM(TRAILING '市' FROM name) AS city
            FROM city
            WHERE name IS NOT NULL
              AND TRIM(name) <> ''
            ORDER BY city
            """)
    List<CityOptionVO> selectCityOptions();
}
