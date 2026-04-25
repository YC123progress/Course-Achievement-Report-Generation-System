# 课程目标达成评价报告菜单安装指南

为了使用课程目标达成评价报告功能，您需要在若依系统中添加对应的菜单项。请按照以下步骤操作：

## 方法一：手动添加菜单

1. 登录若依系统管理后台
2. 进入"系统管理" -> "菜单管理"
3. 点击"新增"按钮
4. 填写以下信息：
   - 菜单类型：选择"菜单"
   - 菜单名称：课程目标达成评价
   - 父菜单：系统工具
   - 显示排序：4（或其他合适的序号）
   - 路由地址：assessment
   - 组件路径：luckysheet/assessment
   - 是否外链：否
   - 菜单状态：正常
   - 显示状态：显示
   - 权限标识：（可留空）
   - 菜单图标：chart（或选择其他合适的图标）
5. 点击"确定"保存

## 方法二：使用SQL脚本添加

如果您有数据库管理权限，也可以直接执行以下SQL语句添加菜单：

```sql
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES(1118, '课程目标达成评价', '3', '4', 'assessment', 'luckysheet/assessment', 1, 0, 'C', '0', '0', '', 'chart', 'admin', SYSDATE(), '', NULL, '课程目标达成评价报告生成工具');
```

执行完成后，刷新系统或重新登录，即可在左侧菜单的"系统工具"下看到"课程目标达成评价"菜单项。

## 使用说明

添加完菜单后，点击"课程目标达成评价"菜单项，即可进入课程目标达成评价报告生成页面。您可以：

1. 上传配置文件或使用默认配置
2. 在在线Excel表格中输入学生成绩数据
3. 生成评价报告
4. 查看图表结果并下载完整报告

如需了解更多功能详情，请参考系统帮助文档。 