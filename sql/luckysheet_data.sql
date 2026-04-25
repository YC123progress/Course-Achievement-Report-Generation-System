-- ----------------------------
-- Table structure for sys_luckysheet_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_luckysheet_data`;
CREATE TABLE `sys_luckysheet_data` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sheet_name` varchar(100) NOT NULL COMMENT '工作表名称',
    `cell_data` longtext COMMENT '单元格数据JSON',
    `sheet_index` int(11) DEFAULT 0 COMMENT '工作表索引',
    `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_sheet_name` (`sheet_name`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Luckysheet表格数据表'; 