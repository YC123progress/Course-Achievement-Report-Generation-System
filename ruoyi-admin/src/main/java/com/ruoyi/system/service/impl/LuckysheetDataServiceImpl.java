package com.ruoyi.system.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.LuckysheetDataMapper;
import com.ruoyi.system.domain.LuckysheetData;
import com.ruoyi.system.service.ILuckysheetDataService;
import java.util.List;

@Service
public class LuckysheetDataServiceImpl implements ILuckysheetDataService {
    @Autowired
    private LuckysheetDataMapper luckysheetDataMapper;

    @Override
    public int saveLuckysheetData(LuckysheetData luckysheetData) {
        // 如果存在ID则更新，否则新增
        if (luckysheetData.getId() != null) {
            return luckysheetDataMapper.updateLuckysheetData(luckysheetData);
        } else {
            return luckysheetDataMapper.insertLuckysheetData(luckysheetData);
        }
    }

    @Override
    public List<LuckysheetData> selectLuckysheetDataList(LuckysheetData luckysheetData) {
        return luckysheetDataMapper.selectLuckysheetDataList(luckysheetData);
    }
} 