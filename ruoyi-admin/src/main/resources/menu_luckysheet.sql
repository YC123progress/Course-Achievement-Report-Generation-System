-- 添加课程目标达成评价报告菜单
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES(1118, '课程目标达成评价', '3', '4', 'assessment', 'luckysheet/assessment', 1, 0, 'C', '0', '0', '', 'chart', 'admin', SYSDATE(), '', NULL, '课程目标达成评价报告生成工具'); 