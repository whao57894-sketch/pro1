package com.example.myapplication.utils

object CategorySuggestion {

    // 支出类目关键词映射
    private val expenseKeywords = mapOf(
        "餐饮" to listOf("饭", "餐", "吃", "食", "lunch", "dinner", "breakfast", "外卖", "美团", "饿了么", "麦当劳", "肯德基", "火锅", "烧烤", "奶茶", "咖啡", "星巴克"),
        "交通" to listOf("车", "地铁", "公交", "打车", "滴滴", "uber", "出租", "加油", "停车", "高速", "etc", "油费"),
        "购物" to listOf("买", "购", "淘宝", "京东", "拼多多", "商场", "超市", "衣服", "鞋", "包", "化妆品", "shopping"),
        "娱乐" to listOf("电影", "游戏", "ktv", "唱歌", "酒吧", "旅游", "景区", "门票", "演唱会", "健身", "运动"),
        "医疗" to listOf("医院", "药", "看病", "体检", "挂号", "医疗", "诊所", "牙医", "health"),
        "教育" to listOf("书", "培训", "课程", "学费", "教材", "考试", "学习", "教育", "培训班"),
        "住房" to listOf("房租", "物业", "水费", "电费", "燃气", "宽带", "网费", "房贷", "装修"),
        "通讯" to listOf("话费", "手机费", "流量", "电话", "宽带", "网络", "移动", "联通", "电信"),
        "日用" to listOf("日用品", "生活用品", "洗发", "牙膏", "纸巾", "清洁", "household"),
        "社交" to listOf("聚餐", "聚会", "礼物", "红包", "请客", "份子钱", "生日", "wedding"),
        "其他" to listOf("其他", "杂项", "other")
    )

    // 收入类目关键词映射
    private val incomeKeywords = mapOf(
        "工资" to listOf("工资", "薪水", "薪资", "salary", "收入", "发薪"),
        "奖金" to listOf("奖金", "年终奖", "季度奖", "绩效", "提成", "bonus"),
        "投资" to listOf("股票", "基金", "理财", "分红", "收益", "利息", "investment"),
        "兼职" to listOf("兼职", "外快", "副业", "接单", "freelance"),
        "红包" to listOf("红包", "礼金", "压岁钱", "gift"),
        "报销" to listOf("报销", "退款", "refund"),
        "其他" to listOf("其他", "other")
    )

    /**
     * 根据备注内容智能推荐类目
     * @param remark 备注内容
     * @param type 收支类型："收入" 或 "支出"
     * @return 推荐的类目列表，按匹配度排序
     */
    fun suggestCategories(remark: String, type: String): List<String> {
        if (remark.isBlank()) {
            return emptyList()
        }

        val keywords = if (type == "收入") incomeKeywords else expenseKeywords
        val remarkLower = remark.lowercase()

        // 计算每个类目的匹配分数
        val scores = keywords.mapNotNull { (category, keywordList) ->
            val score = keywordList.count { keyword ->
                remarkLower.contains(keyword.lowercase())
            }
            if (score > 0) {
                Pair(category, score)
            } else {
                null
            }
        }

        // 按分数排序，返回前3个
        return scores
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }

    /**
     * 根据金额推荐类目
     * @param amount 金额
     * @param type 收支类型
     */
    fun suggestByAmount(amount: Double, type: String): List<String> {
        if (type == "收入") {
            return when {
                amount >= 10000 -> listOf("工资", "奖金", "投资")
                amount >= 1000 -> listOf("兼职", "奖金", "报销")
                else -> listOf("红包", "兼职", "其他")
            }
        } else {
            return when {
                amount >= 5000 -> listOf("住房", "购物", "医疗")
                amount >= 1000 -> listOf("购物", "娱乐", "交通")
                amount >= 100 -> listOf("餐饮", "交通", "日用")
                amount >= 50 -> listOf("餐饮", "交通", "其他")
                else -> listOf("餐饮", "日用", "其他")
            }
        }
    }

    /**
     * 获取所有类目
     */
    fun getAllCategories(type: String): List<String> {
        return if (type == "收入") {
            incomeKeywords.keys.toList()
        } else {
            expenseKeywords.keys.toList()
        }
    }

    /**
     * 综合推荐：结合备注和金额
     */
    fun smartSuggest(remark: String, amount: Double, type: String): List<String> {
        val remarkSuggestions = suggestCategories(remark, type)

        return if (remarkSuggestions.isNotEmpty()) {
            // 备注有匹配，优先使用备注推荐
            remarkSuggestions
        } else {
            // 备注无匹配，使用金额推荐
            suggestByAmount(amount, type)
        }
    }

    /**
     * 获取类目的 emoji 图标
     */
    fun getCategoryEmoji(category: String): String {
        return when (category) {
            // 支出类目
            "餐饮" -> "🍽️"
            "交通" -> "🚗"
            "购物" -> "🛍️"
            "娱乐" -> "🎮"
            "医疗" -> "💊"
            "教育" -> "📚"
            "住房" -> "🏠"
            "通讯" -> "📱"
            "日用" -> "🧴"
            "社交" -> "👥"
            // 收入类目
            "工资" -> "💰"
            "奖金" -> "🎁"
            "投资" -> "📈"
            "兼职" -> "💼"
            "红包" -> "🧧"
            "报销" -> "📝"
            else -> "📌"
        }
    }
}
