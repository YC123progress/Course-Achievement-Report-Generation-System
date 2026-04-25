package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class LuckysheetData extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    
    /** 工作表名称 */
    private String sheetName;
    
    /** 单元格数据 */
    private String cellData;
    
    /** 工作表索引 */
    private Integer sheetIndex;
    
    /** 工作表状态（0正常 1停用） */
    private String status;
    
    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;
    
    /** 备注 */
    private String remark;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSheetName() {
        return sheetName;
    }
    
    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }
    
    public String getCellData() {
        return cellData;
    }
    
    public void setCellData(String cellData) {
        this.cellData = cellData;
    }
    
    public Integer getSheetIndex() {
        return sheetIndex;
    }
    
    public void setSheetIndex(Integer sheetIndex) {
        this.sheetIndex = sheetIndex;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getDelFlag() {
        return delFlag;
    }
    
    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
} 