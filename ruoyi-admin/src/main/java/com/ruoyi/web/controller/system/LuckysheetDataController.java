package com.ruoyi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.LuckysheetData;
import com.ruoyi.system.service.ILuckysheetDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

@RestController
@RequestMapping("/system/luckysheet")
public class LuckysheetDataController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(LuckysheetDataController.class);
    
    @Autowired
    private ILuckysheetDataService luckysheetDataService;

    /**
     * 保存表格数据
     */
    @PreAuthorize("@ss.hasPermi('system:luckysheet:edit')")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody LuckysheetData luckysheetData) {
        try {
            // 参数校验
            if (luckysheetData == null) {
                return AjaxResult.error("保存数据不能为空");
            }
            if (StringUtils.isEmpty(luckysheetData.getSheetName())) {
                return AjaxResult.error("工作表名称不能为空");
            }
            if (StringUtils.isEmpty(luckysheetData.getCellData())) {
                return AjaxResult.error("单元格数据不能为空");
            }

            // 设置操作人信息
            String username = SecurityUtils.getUsername();
            luckysheetData.setUpdateBy(username);
            
            log.info("用户[{}]开始保存表格数据，工作表名称：{}", username, luckysheetData.getSheetName());
            
            int rows = luckysheetDataService.saveLuckysheetData(luckysheetData);
            if (rows > 0) {
                log.info("用户[{}]保存表格数据成功", username);
                return AjaxResult.success("保存成功");
            } else {
                log.warn("用户[{}]保存表格数据失败，影响行数：{}", username, rows);
                return AjaxResult.error("保存失败");
            }
        } catch (Exception e) {
            log.error("保存表格数据失败：", e);
            return AjaxResult.error("保存表格数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取表格数据列表
     */
    @PreAuthorize("@ss.hasPermi('system:luckysheet:query')")
    @GetMapping("/list")
    public AjaxResult list(LuckysheetData luckysheetData) {
        try {
            String username = SecurityUtils.getUsername();
            log.info("用户[{}]开始查询表格数据列表", username);
            
            return AjaxResult.success(luckysheetDataService.selectLuckysheetDataList(luckysheetData));
        } catch (Exception e) {
            log.error("获取表格数据列表失败：", e);
            return AjaxResult.error("获取表格数据列表失败：" + e.getMessage());
        }
    }
} 