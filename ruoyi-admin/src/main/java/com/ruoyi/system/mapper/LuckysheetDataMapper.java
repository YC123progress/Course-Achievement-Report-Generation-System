package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.LuckysheetData;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LuckysheetDataMapper {
    /**
     * 新增表格数据
     */
    public int insertLuckysheetData(LuckysheetData luckysheetData);
    
    /**
     * 修改表格数据
     */
    public int updateLuckysheetData(LuckysheetData luckysheetData);
    
    /**
     * 查询表格数据列表
     */
    public List<LuckysheetData> selectLuckysheetDataList(LuckysheetData luckysheetData);
} 