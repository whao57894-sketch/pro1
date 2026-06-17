# 记账应用 - 后端 API 需求文档

本文档列出前端 Android 应用所需的后端 Spring Boot API 接口。

## 基础 URL
```
http://127.0.0.1:8084/api
```

---

## 1. 用户管理 API (`/user`)

### 1.1 用户注册
- **路径**: `POST /user/register`
- **请求体**:
```json
{
  "phone": "13800138000",
  "password": "password123",
  "confirmPassword": "password123",
  "name": "张三",
  "age": 25,
  "occupation": "软件工程师",
  "gender": "男"
}
```
- **响应**:
```json
{
  "success": true,
  "message": "注册成功",
  "data": null
}
```

### 1.2 用户登录
- **路径**: `POST /user/login`
- **请求体**:
```json
{
  "phone": "13800138000",
  "password": "password123"
}
```
- **响应**:
```json
{
  "success": true,
  "message": "登录成功",
  "data": null
}
```

### 1.3 获取用户信息
- **路径**: `GET /user/{phone}`
- **响应**:
```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "phone": "13800138000",
    "name": "张三",
    "age": 25,
    "occupation": "软件工程师",
    "gender": "男"
  }
}
```

### 1.4 更新用户信息
- **路径**: `PUT /user/{phone}`
- **请求体**:
```json
{
  "name": "张三",
  "age": 26,
  "occupation": "高级软件工程师",
  "gender": "男"
}
```
- **响应**:
```json
{
  "success": true,
  "message": "更新成功",
  "data": null
}
```

### 1.5 获取记账总次数
- **路径**: `GET /user/{phone}/bookkeeping-count`
- **响应**:
```json
{
  "success": true,
  "message": "获取成功",
  "data": 156
}
```

---

## 2. 记账管理 API (`/bookkeeping`)

### 2.1 按月份查询记账记录
- **路径**: `GET /bookkeeping?phone={phone}&month={month}`
- **参数**:
  - `phone`: 手机号
  - `month`: 月份 (格式: yyyy-MM)
- **响应**:
```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "incomeTotal": "15000.00",
    "expenseTotal": "8500.00",
    "records": [
      {
        "id": 1,
        "type": "收入",
        "category": "工资",
        "amount": "15000.00",
        "recordDate": "2024-01-05",
        "remark": "月工资"
      },
      {
        "id": 2,
        "type": "支出",
        "category": "餐饮",
        "amount": "500.00",
        "recordDate": "2024-01-10",
        "remark": "聚餐"
      }
    ]
  }
}
```

### 2.2 获取单条记账记录
- **路径**: `GET /bookkeeping/{id}`
- **响应**:
```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "id": 1,
    "phone": "13800138000",
    "type": "支出",
    "category": "餐饮",
    "amount": "500.00",
    "recordDate": "2024-01-10",
    "remark": "聚餐"
  }
}
```

### 2.3 新增记账记录
- **路径**: `POST /bookkeeping`
- **请求体**:
```json
{
  "phone": "13800138000",
  "type": "支出",
  "category": "餐饮",
  "amount": "500.00",
  "recordDate": "2024-01-10",
  "remark": "聚餐"
}
```
- **响应**:
```json
{
  "success": true,
  "message": "添加成功",
  "data": null
}
```

### 2.4 更新记账记录
- **路径**: `PUT /bookkeeping/{id}`
- **请求体**: 同新增
- **响应**:
```json
{
  "success": true,
  "message": "更新成功",
  "data": null
}
```

### 2.5 删除记账记录
- **路径**: `DELETE /bookkeeping/{id}`
- **响应**:
```json
{
  "success": true,
  "message": "删除成功",
  "data": null
}
```

### 2.6 AI 识别账单图片
- **路径**: `POST /bookkeeping/recognize`
- **请求**: `multipart/form-data`，包含图片文件
- **响应**:
```json
{
  "success": true,
  "message": "识别成功",
  "data": {
    "type": "支出",
    "category": "餐饮",
    "amount": "58.50",
    "recordDate": "2024-01-10",
    "remark": "肯德基"
  }
}
```

---

## 3. 预算管理 API (`/budget`)

### 3.1 查询预算列表
- **路径**: `GET /budget?phone={phone}&month={month}`
- **响应**:
```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "id": 1,
      "phone": "13800138000",
      "category": "餐饮",
      "amount": 2000.00,
      "month": "2024-01",
      "currentSpent": 1580.00
    },
    {
      "id": 2,
      "phone": "13800138000",
      "category": "交通",
      "amount": 500.00,
      "month": "2024-01",
      "currentSpent": 320.00
    }
  ]
}
```

### 3.2 新增预算
- **路径**: `POST /budget`
- **请求体**:
```json
{
  "phone": "13800138000",
  "category": "餐饮",
  "amount": 2000.00,
  "month": "2024-01"
}
```

### 3.3 更新预算
- **路径**: `PUT /budget/{id}`
- **请求体**: 同新增

### 3.4 删除预算
- **路径**: `DELETE /budget/{id}`

---

## 4. 债务管理 API (`/debt`)

### 4.1 查询债务列表
- **路径**: `GET /debt?phone={phone}`
- **响应**:
```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "id": 1,
      "phone": "13800138000",
      "type": "借出",
      "counterparty": "张三",
      "amount": 5000.00,
      "repaidAmount": 2000.00,
      "debtDate": "2024-01-01",
      "dueDate": "2024-12-31",
      "status": "UNPAID",
      "remark": "借款买电脑"
    }
  ]
}
```

### 4.2 新增债务记录
- **路径**: `POST /debt`
- **请求体**:
```json
{
  "phone": "13800138000",
  "type": "借出",
  "counterparty": "张三",
  "amount": 5000.00,
  "debtDate": "2024-01-01",
  "dueDate": "2024-12-31",
  "remark": "借款买电脑"
}
```

### 4.3 更新债务记录
- **路径**: `PUT /debt/{id}`

### 4.4 还款
- **路径**: `POST /debt/{id}/repay`
- **请求体**:
```json
{
  "amount": 1000.00
}
```

### 4.5 删除债务记录
- **路径**: `DELETE /debt/{id}`

---

## 5. 周期账单 API (`/recurring`)

### 5.1 查询周期账单列表
- **路径**: `GET /recurring?phone={phone}`
- **响应**:
```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "id": 1,
      "phone": "13800138000",
      "type": "支出",
      "category": "房租",
      "amount": 3000.00,
      "frequency": "MONTHLY",
      "startDate": "2024-01-01",
      "dayOfMonth": 1,
      "isActive": true,
      "remark": "每月房租"
    }
  ]
}
```

### 5.2 新增周期账单
- **路径**: `POST /recurring`
- **请求体**:
```json
{
  "phone": "13800138000",
  "type": "支出",
  "category": "房租",
  "amount": 3000.00,
  "frequency": "MONTHLY",
  "startDate": "2024-01-01",
  "dayOfMonth": 1,
  "remark": "每月房租"
}
```

### 5.3 更新周期账单
- **路径**: `PUT /recurring/{id}`

### 5.4 启用/停用周期账单
- **路径**: `POST /recurring/{id}/toggle?active={true/false}`

### 5.5 删除周期账单
- **路径**: `DELETE /recurring/{id}`

---

## 6. 账户管理 API (`/account`)

### 6.1 查询账户列表
- **路径**: `GET /account?phone={phone}`
- **响应**:
```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "id": 1,
      "phone": "13800138000",
      "accountName": "工商银行储蓄卡",
      "accountType": "银行卡",
      "balance": 15000.00
    },
    {
      "id": 2,
      "phone": "13800138000",
      "accountName": "支付宝",
      "accountType": "支付宝",
      "balance": 2500.00
    }
  ]
}
```

### 6.2 新增账户
- **路径**: `POST /account`
- **请求体**:
```json
{
  "phone": "13800138000",
  "accountName": "工商银行储蓄卡",
  "accountType": "银行卡",
  "balance": 15000.00
}
```

### 6.3 更新账户
- **路径**: `PUT /account/{id}`

### 6.4 删除账户
- **路径**: `DELETE /account/{id}`

---

## 7. 统计分析 API (`/statistics`)

### 7.1 获取收支趋势
- **路径**: `GET /statistics/trend?phone={phone}&months={months}`
- **参数**:
  - `months`: 查询最近几个月 (3/6/12)
- **响应**:
```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "month": "2023-07",
      "income": 15000.00,
      "expense": 8000.00
    },
    {
      "month": "2023-08",
      "income": 15500.00,
      "expense": 8500.00
    }
  ]
}
```

### 7.2 获取分类统计
- **路径**: `GET /statistics/category?phone={phone}&month={month}`
- **响应**:
```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "category": "餐饮",
      "amount": 2000.00
    },
    {
      "category": "交通",
      "amount": 500.00
    },
    {
      "category": "娱乐",
      "amount": 800.00
    }
  ]
}
```

### 7.3 获取年度报告
- **路径**: `GET /statistics/yearly?phone={phone}&year={year}`
- **响应**:
```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "totalIncome": 180000.00,
    "totalExpense": 95000.00,
    "savingsRate": 47.2,
    "recordCount": 356,
    "topExpenseCategory": "餐饮",
    "topExpenseAmount": 25000.00,
    "avgMonthlyIncome": 15000.00,
    "avgMonthlyExpense": 7916.67
  }
}
```

---

## 通用响应格式

所有 API 返回的 JSON 格式统一为：

```json
{
  "success": true/false,
  "message": "操作结果描述",
  "data": {} 或 [] 或 null
}
```

## 错误处理

当操作失败时，返回：

```json
{
  "success": false,
  "message": "失败原因描述",
  "data": null
}
```

---

## 实施建议

1. **数据库设计**：
   - 用户表 (user)
   - 记账记录表 (bookkeeping_record)
   - 预算表 (budget)
   - 债务表 (debt)
   - 周期账单表 (recurring_bill)
   - 账户表 (account)

2. **AI 识别功能**：
   - 可以使用 OCR + 规则匹配
   - 或集成百度 AI、阿里云 OCR 等服务
   - 解析账单图片中的金额、类别、日期等信息

3. **统计功能**：
   - 使用 SQL 聚合查询实现
   - 可以添加缓存提升性能

4. **安全性**：
   - 密码使用 BCrypt 加密
   - 添加 JWT 或 Session 验证
   - 添加请求频率限制

5. **优化**：
   - 添加分页支持
   - 添加索引优化查询性能
   - 考虑使用 Redis 缓存热点数据
