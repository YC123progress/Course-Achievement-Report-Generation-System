-- =====================================================
-- 清理若依相关菜单 - 手动执行脚本
-- 数据库：ry
-- 执行前请备份数据库！
-- =====================================================

-- 1. 删除"若依官网"菜单（menu_id = 4）
DELETE FROM sys_menu WHERE menu_id = 4;

-- 2. 删除角色菜单关联（如果存在）
DELETE FROM sys_role_menu WHERE menu_id = 4;

-- 3. 验证删除结果
SELECT * FROM sys_menu WHERE menu_name LIKE '%若依%';
