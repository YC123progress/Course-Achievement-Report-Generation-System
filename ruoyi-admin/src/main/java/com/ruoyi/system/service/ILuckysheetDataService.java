package com.ruoyi.system.service;

import com.ruoyi.system.domain.LuckysheetData;
import java.util.List;

public interface ILuckysheetDataService {
    /**
     * 保存表格数据
     */
    public int saveLuckysheetData(LuckysheetData luckysheetData);
    
    /**
     * 查询表格数据列表
     */
    public List<LuckysheetData> selectLuckysheetDataList(LuckysheetData luckysheetData);
} 