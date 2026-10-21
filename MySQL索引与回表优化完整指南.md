# MySQL 索引与回表优化完整指南

> 本文档系统总结了 MySQL 索引和回表的核心原理、优化技巧及实战经验

---

## 目录

1. [核心概念](#核心概念)
2. [索引的数据结构](#索引的数据结构)
3. [回表机制](#回表机制)
4. [索引类型](#索引类型)
5. [最左匹配原则](#最左匹配原则)
6. [深度分页优化](#深度分页优化)
7. [索引失效场景](#索引失效场景)
8. [优化技巧总结](#优化技巧总结)
9. [实战案例](#实战案例)

---

## 核心概念

### SQL 优化的本质

```
90% 的 SQL 性能优化 = 索引优化 + 减少回表

具体分为：
1. 让查询走索引（而不是全表扫描）
2. 走正确的索引（选最优的）
3. 减少回表次数（覆盖索引/延迟关联）
```

### 优化金字塔

```
       其他优化 (5%)
      ／          ＼
   表设计优化 (10%)
   ／            ＼
SQL写法优化 (15%)
／                ＼
═══════════════════════
索引 + 回表优化 (70%)  ← 最重要！
═══════════════════════
```

---

## 索引的数据结构

### B+树特点

MySQL InnoDB 使用 B+树存储索引和数据：

```
特点：
1. 矮胖（3-4 层存千万数据）
2. 叶子节点存数据，非叶子节点存索引
3. 叶子节点之间有指针（方便范围查询）
4. 每个节点存很多数据（一页 16KB）
```

### B+树结构

```
                [根节点]
                /      \
          [非叶子]     [非叶子]
          /     \      /     \
    [叶子节点] [叶子] [叶子] [叶子]
    ↓         ↓      ↓      ↓
  下一页 → 下一页 → 下一页 → NULL
```

### 为什么用 B+树？

| 对比项 | B+树 | 其他结构 |
|--------|------|---------|
| 树高 | 3-4层 | 二叉树：20+层 |
| IO次数 | 3-4次 | 二叉树：20+次 |
| 范围查询 | ✅ 高效 | 哈希表：❌ 不支持 |
| 磁盘友好 | ✅ 顺序IO | 红黑树：❌ 随机IO |

---

## 回表机制

### 什么是回表？

```
回表 = 从二级索引跳回主键索引查找完整数据

发生条件：
1. 使用二级索引查询
2. 查询的列不全在索引中
3. 需要拿主键值去主键索引查完整数据
```

### InnoDB 的两种索引

#### 1. 主键索引（聚簇索引）

```
主键索引 B+树
┌─────────────────────────────┐
│ 叶子节点 = 完整的行数据      │
├─────────────────────────────┤
│ id=1 → [1, '张三', 18, '一班'] │
│ id=2 → [2, '李四', 19, '二班'] │
│ id=3 → [3, '王五', 18, '三班'] │
└─────────────────────────────┘

特点：叶子节点存完整数据
```

#### 2. 二级索引（辅助索引）

```
name 索引 B+树（二级索引）
┌─────────────────────────┐
│ 叶子节点 = 索引列 + 主键值 │
├─────────────────────────┤
│ '李四' → id=2  ← 只有这两个！ │
│ '王五' → id=3            │
│ '张三' → id=1            │
└─────────────────────────┘

特点：叶子节点只存索引列和主键值
要查其他列 → 拿着 id 去主键索引查（回表）
```

### 回表的完整流程

```sql
CREATE INDEX idx_name ON students(name);
SELECT * FROM students WHERE name = '张三';
```

**执行过程：**

```
步骤1：查 idx_name 索引
      找到：'张三' → id=1
      
步骤2：回表（拿着 id=1 去主键索引查）
      去主键索引找 id=1
      拿到完整数据：[1, '张三', 18, '一班']
      
步骤3：返回完整数据
```

### 什么时候回表？

| 场景 | 是否回表 | 原因 |
|------|---------|------|
| 主键查询 `WHERE id=1` | ✅ 不回表 | 主键索引存完整数据 |
| 二级索引 + `SELECT *` | ❌ 回表 | 需要其他列 |
| 二级索引 + 覆盖索引 | ✅ 不回表 | 查询列都在索引中 |
| 找不到数据 | ✅ 不回表 | 没数据，回表也没用 |

### 覆盖索引（不回表）

```sql
CREATE INDEX idx_name ON students(name);

-- ❌ 需要回表
SELECT * FROM students WHERE name = '张三';
-- 索引只有 (name, id)，缺少 age, class 等

-- ✅ 不需要回表（覆盖索引）
SELECT id, name FROM students WHERE name = '张三';
-- 查询的列 (id, name) 都在索引中
```

---

## 索引类型

### 单列索引

```sql
CREATE INDEX idx_name ON students(name);
```

**特点：**
- 只包含一列
- B+树按该列排序

### 联合索引（复合索引）

```sql
CREATE INDEX idx_name_age_class ON students(name, age, class);
```

**特点：**
- 包含多列
- 多级排序：先按第1列，相同按第2列，再相同按第3列

**排序示例：**

```
联合索引 (name, age, class) 的排序：

('李四', 18, '一班') → id=5
('李四', 19, '二班') → id=2  ← name相同，按age排序
('王五', 18, '三班') → id=3
('张三', 18, '一班') → id=1
('张三', 18, '三班') → id=6  ← name、age都相同，按class排序
('张三', 20, '二班') → id=4  ← name相同，age不同
```

### 主键选择

| 主键类型 | 插入性能 | 空间占用 | 索引效率 | 推荐度 |
|---------|---------|---------|---------|-------|
| 自增 INT/BIGINT | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| UUID v4（随机） | ⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐ |
| UUID v7（有序） | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 自增ID+UUID | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

**推荐方案：**

```sql
CREATE TABLE students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,  -- 自增主键（性能最优）
    uuid VARCHAR(36) UNIQUE,                -- UUID 作为业务ID
    name VARCHAR(50),
    INDEX idx_uuid (uuid)
);
```

---

## 最左匹配原则

### 定义

```
联合索引必须从最左边的列开始匹配
不能跳过左边的列

就像查字典：
- 必须先按姓氏查（第一列）
- 才能按名字查（第二列）
- 不能跳过姓氏直接查名字
```

### 规则表

**联合索引：`INDEX idx_abc(a, b, c)`**

| 查询条件 | 能否用索引 | 用到哪些列 | 说明 |
|---------|-----------|-----------|------|
| `WHERE a=1` | ✅ 完全使用 | a | 从最左开始 |
| `WHERE a=1 AND b=2` | ✅ 完全使用 | a, b | 连续使用 |
| `WHERE a=1 AND b=2 AND c=3` | ✅ 完全使用 | a, b, c | 全部使用 |
| `WHERE b=2` | ❌ 不能用 | 无 | 跳过了a |
| `WHERE c=3` | ❌ 不能用 | 无 | 跳过了a |
| `WHERE b=2 AND c=3` | ❌ 不能用 | 无 | 跳过了a |
| `WHERE a=1 AND c=3` | ⚠️ 部分使用 | 只用a | c用不上（跳过了b） |
| `WHERE a>1 AND b=2` | ⚠️ 部分使用 | 只用a | b用不上（a用了范围） |
| `WHERE a=1 AND b>2 AND c=3` | ⚠️ 部分使用 | a, b | c用不上（b用了范围） |

### 为什么有这个原则？

```
联合索引是多级排序的 B+树

索引 (name, age, class) 的排序：
1. 先按 name 排序（全局有序）
2. name 相同时，按 age 排序（局部有序）
3. name、age 都相同时，按 class 排序（局部有序）

如果跳过 name：
- age 在全局是无序的
- 无法快速定位
- 必须全索引扫描
```

### 实战建议

```sql
-- ❌ 错误：为每个字段建单独索引
CREATE INDEX idx_name ON students(name);
CREATE INDEX idx_age ON students(age);
CREATE INDEX idx_class ON students(class);
-- 问题：查询 WHERE name=? AND age=? 只能用一个索引

-- ✅ 正确：根据查询模式建联合索引
CREATE INDEX idx_name_age_class ON students(name, age, class);
-- 可以满足多种查询：
-- WHERE name=?
-- WHERE name=? AND age=?
-- WHERE name=? AND age=? AND class=?
```

---

## 深度分页优化

### 问题：LIMIT 不是智能跳过

```sql
SELECT * FROM students ORDER BY id LIMIT 100000, 20;
```

**LIMIT 的真实机制：**

```
MySQL 必须：
1. 从头开始读取
2. 读取前 100020 条
3. 丢弃前 100000 条
4. 返回后 20 条

不能"跳过"！必须"扫描过"！
```

**原因：**
- B+树没有"第N条"的概念（像链表，只有 next）
- 节点大小不固定，无法计算位置
- id 可能不连续（删除导致空洞）

### ORDER BY 主键 vs 二级索引

#### 场景1：ORDER BY id（主键）

```sql
SELECT * FROM students ORDER BY id LIMIT 100000, 20;
```

**执行过程：**

```
✅ 走主键索引
✅ 不需要回表（主键索引存完整数据）
⚠️ 但要扫描 100020 条

耗时：0.5 秒
```

#### 场景2：ORDER BY name（二级索引）

```sql
SELECT * FROM students ORDER BY name LIMIT 100000, 20;
```

**执行过程：**

```
✅ 走 idx_name 索引
❌ 需要回表 100020 次（二级索引只有 name+id）
⚠️ 扫描 100020 条 + 回表 100020 次

耗时：15 秒（比主键慢 30 倍！）
```

### 优化方案

#### 方案1：游标分页（最优）⭐⭐⭐⭐⭐

```sql
-- 第一页
SELECT * FROM students ORDER BY id LIMIT 20;
-- 记住最后的 id=20

-- 第二页（不用 OFFSET）
SELECT * FROM students WHERE id > 20 ORDER BY id LIMIT 20;
-- 直接从 id>20 开始 ✅

-- 第三页
SELECT * FROM students WHERE id > 40 ORDER BY id LIMIT 20;
```

**优势：**
- ✅ 只扫描 20 条（不是 100020 条）
- ✅ 性能稳定（不受页码影响）
- ✅ 最优解决方案

**适用场景：**
- 无限滚动
- 上一页/下一页
- 不需要跳页

#### 方案2：延迟关联（减少回表）⭐⭐⭐⭐

```sql
-- 原始查询（回表 100020 次）
SELECT * FROM students ORDER BY name LIMIT 100000, 20;

-- 优化：延迟关联（只回表 20 次）
SELECT * FROM students t
JOIN (
    SELECT id FROM students ORDER BY name LIMIT 100000, 20
) tmp ON t.id = tmp.id
ORDER BY t.name;
```

**执行过程：**

```
【子查询】
步骤1：扫描 idx_name 索引（100020 条）
步骤2：✅ 不回表（只查 id，覆盖索引）
步骤3：返回 20 个 id

【外层查询】
步骤4：拿 20 个 id 去主键索引查
步骤5：✅ 只回表 20 次

回表次数：100020 次 → 20 次（优化 99.98%）
```

**优势：**
- ✅ 大幅减少回表
- ✅ 支持跳页
- ✅ 性能提升 10-100 倍

**适用场景：**
- 必须支持跳页
- 无法改用游标分页

#### 方案3：覆盖索引 + LIMIT ⭐⭐⭐

```sql
-- 创建覆盖索引
CREATE INDEX idx_name_age_class ON students(name, age, class);

-- 查询（查询的列都在索引中）
SELECT id, name, age, class 
FROM students 
ORDER BY name 
LIMIT 100000, 20;
```

**执行过程：**

```
步骤1：扫描 idx_name_age_class 索引（100020 条）
步骤2：✅ 不回表（覆盖索引）
步骤3：返回 20 条

优势：
- ✅ 不回表
- ⚠️ 但还是要扫描 100020 条索引

比回表快很多，但不如游标分页
```

### 性能对比（100万数据）

| 方案 | 扫描行数 | 回表次数 | 执行时间 | 推荐度 |
|------|---------|---------|---------|-------|
| 原始 LIMIT + 回表 | 100020 | 100020 | 15秒 | ⭐ |
| 延迟关联 | 100020 | 20 | 0.8秒 | ⭐⭐⭐⭐ |
| 覆盖索引 + LIMIT | 100020 | 0 | 0.5秒 | ⭐⭐⭐⭐ |
| 游标分页 | 20 | 20/0 | 0.01秒 | ⭐⭐⭐⭐⭐ |

---

## 索引失效场景

### 1. OR 条件

```sql
-- 如果任一条件没索引 → 通常全表扫描
SELECT * FROM students WHERE age > 100 OR id > 100;
-- age 没索引 → 全表扫描 ❌

-- 优化：改写为 UNION
SELECT * FROM students WHERE age > 100
UNION
SELECT * FROM students WHERE id > 100;
```

### 2. 函数/计算

```sql
-- ❌ 索引失效
SELECT * FROM students WHERE YEAR(birth_date) = 2000;
SELECT * FROM students WHERE age + 1 = 19;

-- ✅ 改写
SELECT * FROM students WHERE birth_date BETWEEN '2000-01-01' AND '2000-12-31';
SELECT * FROM students WHERE age = 18;
```

### 3. 类型转换

```sql
-- phone 是 VARCHAR 类型
-- ❌ 索引失效（隐式类型转换）
SELECT * FROM students WHERE phone = 12345678;

-- ✅ 正确
SELECT * FROM students WHERE phone = '12345678';
```

### 4. LIKE 左模糊

```sql
-- ❌ 索引失效
SELECT * FROM students WHERE name LIKE '%三';

-- ✅ 可以用索引
SELECT * FROM students WHERE name LIKE '张%';
```

### 5. NOT、!=、<>

```sql
-- ❌ 通常不走索引
SELECT * FROM students WHERE age != 18;
SELECT * FROM students WHERE age <> 18;
SELECT * FROM students WHERE name NOT IN ('张三', '李四');

-- ✅ 改写
SELECT * FROM students WHERE age < 18 OR age > 18;
```

### 6. IS NULL / IS NOT NULL

```sql
-- 可能失效（取决于数据分布）
SELECT * FROM students WHERE age IS NULL;
SELECT * FROM students WHERE age IS NOT NULL;

-- 建议：重要字段设为 NOT NULL，用默认值
```

### 7. 多个单列索引

```sql
CREATE INDEX idx_id ON students(id);
CREATE INDEX idx_age ON students(age);

-- 问题：只能用一个索引
SELECT * FROM students WHERE id > 100 AND age > 18;
-- 只用 idx_id，age 在内存过滤 ❌

-- 优化：创建联合索引
CREATE INDEX idx_id_age ON students(id, age);
```

---

## 优化技巧总结

### 索引设计原则

```
✅ 1. 为高频查询的字段建索引
✅ 2. 优先使用联合索引（而不是多个单列索引）
✅ 3. 把选择性高的列放在联合索引前面
✅ 4. 覆盖索引优先（查询的列都在索引中）
✅ 5. 控制索引数量（一般 3-5 个）
✅ 6. 避免冗余索引（idx_a 和 idx_a_b 可能重复）

❌ 不要给每个字段都建索引
❌ 不要创建太多索引（影响写入性能）
❌ 不要在低选择性字段建索引（如性别、布尔值）
```

### SQL 编写原则

```
✅ 1. 只查需要的列（避免 SELECT *）
✅ 2. 使用 LIMIT 限制返回行数
✅ 3. 避免在 WHERE 中使用函数和计算
✅ 4. 用 UNION 代替 OR（如果OR条件没索引）
✅ 5. 用游标分页代替 OFFSET（深度分页）
✅ 6. 用延迟关联减少回表
✅ 7. 注意隐式类型转换

❌ 不要 SELECT *
❌ 不要使用 OFFSET 深度分页
❌ 不要在 WHERE 中使用函数
```

### 查询优化流程

```
1. 用 EXPLAIN 查看执行计划
   ├─ type = ALL？ → 全表扫描，需要加索引
   ├─ key = NULL？ → 没用索引，检查索引失效
   └─ rows 很大？ → 过滤不够精确

2. 检查是否走索引
   ├─ 没走索引 → 创建索引/避免索引失效
   └─ 走了索引 → 继续

3. 检查回表次数
   ├─ 回表很多 → 覆盖索引/延迟关联
   └─ 回表少 → 继续

4. 检查是否深度分页
   ├─ 是 → 游标分页/延迟关联
   └─ 否 → 优化完成
```

### EXPLAIN 关键字段

| 字段 | 说明 | 优劣 |
|------|------|------|
| type=ALL | 全表扫描 | ❌ 最差 |
| type=index | 全索引扫描 | ⚠️ 较差 |
| type=range | 索引范围扫描 | ✅ 较好 |
| type=ref | 索引等值查询 | ✅ 好 |
| type=const | 主键/唯一索引 | ✅ 最好 |
| key=NULL | 没用索引 | ❌ |
| Extra=Using index | 覆盖索引 | ✅ 很好 |
| Extra=Using where | 内存过滤 | ⚠️ 一般 |
| Extra=Using filesort | 文件排序 | ❌ 差 |
| Extra=Using temporary | 临时表 | ❌ 差 |

---

## 实战案例

### 案例1：电商订单查询优化

**需求：**
```sql
-- 查询用户的订单，按时间倒序，分页显示
SELECT * FROM orders 
WHERE user_id = 12345 
ORDER BY create_time DESC 
LIMIT 100, 20;
```

**问题：**
- 深度分页
- user_id 和 create_time 都需要用到

**优化方案：**

```sql
-- 1. 创建联合索引
CREATE INDEX idx_user_time ON orders(user_id, create_time);

-- 2. 使用覆盖索引 + 延迟关联
SELECT o.* FROM orders o
JOIN (
    SELECT order_id 
    FROM orders 
    WHERE user_id = 12345 
    ORDER BY create_time DESC 
    LIMIT 100, 20
) tmp ON o.order_id = tmp.order_id
ORDER BY o.create_time DESC;

-- 3. 或使用游标分页
SELECT * FROM orders 
WHERE user_id = 12345 
  AND create_time < '上次最后时间'
ORDER BY create_time DESC 
LIMIT 20;
```

**效果：**
- 性能提升 50 倍
- 从 5 秒 → 0.1 秒

---

### 案例2：商品搜索优化

**需求：**
```sql
-- 搜索商品，多条件筛选
SELECT * FROM products 
WHERE category = '电子产品' 
  AND price BETWEEN 1000 AND 5000 
  AND brand = '华为';
```

**分析：**
```
查询条件：category、price、brand
应该创建什么索引？
```

**优化方案：**

```sql
-- 分析选择性：
-- category: 20种类别，选择性一般
-- brand: 100个品牌，选择性较好
-- price: 连续值，选择性好

-- 1. 创建联合索引（选择性高的在前）
CREATE INDEX idx_brand_cat_price ON products(brand, category, price);

-- 2. 如果经常只按类别查，额外创建
CREATE INDEX idx_cat_price ON products(category, price);

-- 3. 查询改写（充分利用索引）
SELECT product_id, name, price, brand 
FROM products 
WHERE brand = '华为' 
  AND category = '电子产品' 
  AND price BETWEEN 1000 AND 5000
LIMIT 100;
```

**效果：**
- 走索引，不全表扫描
- 如果查询列在索引中，还能用覆盖索引

---

### 案例3：统计查询优化

**需求：**
```sql
-- 统计每个类别的商品数
SELECT category, COUNT(*) 
FROM products 
GROUP BY category;
```

**问题：**
- 全表扫描
- GROUP BY 没索引支持

**优化方案：**

```sql
-- 1. 创建索引
CREATE INDEX idx_category ON products(category);

-- 2. 查询会自动利用索引
SELECT category, COUNT(*) 
FROM products 
GROUP BY category;
-- 走 idx_category 索引，利用索引的有序性加速 GROUP BY

-- 3. 如果还需要其他列，使用覆盖索引
CREATE INDEX idx_cat_status ON products(category, status);

SELECT category, status, COUNT(*) 
FROM products 
GROUP BY category, status;
-- 覆盖索引，不回表
```

---

## 总结

### 核心记忆

```
SQL 优化 = 索引 + 回表

索引：
1. 让查询走索引（而不是全表扫描）
2. 走对索引（选择性好的）
3. 遵循最左匹配原则（联合索引）

回表：
1. 覆盖索引（不回表）
2. 延迟关联（减少回表）
3. 游标分页（避免深度分页的大量回表）
```

### 优化优先级

```
1. 全表扫描 → 走索引（最高优先级）
   影响：10-1000 倍性能提升

2. 回表太多 → 减少回表（高优先级）
   影响：10-100 倍性能提升

3. 深度分页 → 游标分页（高优先级）
   影响：100-1000 倍性能提升

4. SQL 写法优化（中优先级）
   影响：2-10 倍性能提升
```

### 记忆口诀

```
索引加在查询列
联合优于单列建
最左匹配要记牢
范围之后索引断

全表扫描要避免
走索引是第一步
回表次数要减少
覆盖索引是目标

深度分页要警惕
游标分页是王道
延迟关联可折中
性能提升百倍高
```

---

## 附录：常用命令

### 查看索引

```sql
-- 查看表的索引
SHOW INDEX FROM students;

-- 查看索引使用情况
SHOW STATUS LIKE 'Handler_read%';
```

### 分析执行计划

```sql
-- 查看执行计划
EXPLAIN SELECT * FROM students WHERE name = '张三';

-- 查看详细执行计划（MySQL 8.0+）
EXPLAIN FORMAT=JSON SELECT * FROM students WHERE name = '张三';

-- 查看实际执行情况
EXPLAIN ANALYZE SELECT * FROM students WHERE name = '张三';
```

### 索引管理

```sql
-- 创建索引
CREATE INDEX idx_name ON students(name);
CREATE INDEX idx_name_age ON students(name, age);

-- 删除索引
DROP INDEX idx_name ON students;

-- 添加索引（ALTER TABLE）
ALTER TABLE students ADD INDEX idx_name(name);

-- 查看索引大小
SELECT 
    table_name,
    index_name,
    ROUND(stat_value * @@innodb_page_size / 1024 / 1024, 2) AS size_mb
FROM mysql.innodb_index_stats
WHERE stat_name = 'size'
  AND table_name = 'students';
```

### 慢查询分析

```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 2; -- 超过2秒的查询

-- 查看慢查询配置
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';
```

---

**文档生成时间：** 2026-01-05  
**版本：** v1.0  
**作者：** MySQL 优化学习笔记

