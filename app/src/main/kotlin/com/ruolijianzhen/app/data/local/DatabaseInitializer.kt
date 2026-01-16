package com.ruolijianzhen.app.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ruolijianzhen.app.data.local.dao.ObjectDao
import com.ruolijianzhen.app.data.local.dao.QuotaDao
import com.ruolijianzhen.app.data.local.entity.ApiQuotaEntity
import com.ruolijianzhen.app.data.local.entity.ObjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库初始化器 - 预填充内置物品数据和API额度配置
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val context: Context,
    private val objectDao: ObjectDao,
    private val quotaDao: QuotaDao
) {
    companion object {
        private const val OBJECTS_FILE = "objects_data.json"
    }
    
    /**
     * 初始化数据库
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        initializeObjects()
        initializeApiQuotas()
    }
    
    /**
     * 初始化内置物品数据
     */
    private suspend fun initializeObjects() {
        val count = objectDao.getObjectCount()
        if (count > 0) return // 已初始化
        
        val objects = loadObjectsFromAssets()
        if (objects.isNotEmpty()) {
            objectDao.insertObjects(objects)
        }
    }

    /**
     * 从assets加载物品数据
     */
    private fun loadObjectsFromAssets(): List<ObjectEntity> {
        return try {
            context.assets.open(OBJECTS_FILE).use { inputStream ->
                val json = inputStream.bufferedReader().readText()
                val type = object : TypeToken<List<ObjectEntity>>() {}.type
                Gson().fromJson(json, type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultObjects()
        }
    }
    
    /**
     * 初始化API额度配置
     */
    private suspend fun initializeApiQuotas() {
        val quotas = quotaDao.getAllQuotas()
        if (quotas.isNotEmpty()) return // 已初始化
        
        val defaultQuotas = listOf(
            ApiQuotaEntity(
                apiName = "BAIDU_API",
                dailyLimit = 500,
                monthlyLimit = 15000
            ),
            ApiQuotaEntity(
                apiName = "TENCENT_API",
                dailyLimit = 1000,
                monthlyLimit = 1000
            ),
            ApiQuotaEntity(
                apiName = "ALIYUN_API",
                dailyLimit = 500,
                monthlyLimit = 15000
            )
        )
        quotaDao.insertQuotas(defaultQuotas)
    }
    
    /**
     * 获取默认物品数据（当JSON文件不存在时使用）
     */
    private fun getDefaultObjects(): List<ObjectEntity> = listOf(
        // 动物类
        ObjectEntity("golden_retriever", "金毛寻回犬", "Golden Retriever", 
            "[\"金毛\", \"黄金猎犬\"]", 
            "起源于19世纪的苏格兰，由黄色寻回犬与已绝种的黄色平毛寻回犬杂交培育而成。",
            "家庭伴侣犬、导盲犬、搜救犬、治疗犬。性格温顺友善，智商高，易于训练。",
            "动物-犬类"),
        ObjectEntity("cat", "猫", "Cat",
            "[\"猫咪\", \"喵星人\"]",
            "家猫的祖先是非洲野猫，约在一万年前被人类驯化。",
            "家庭宠物、捕鼠、陪伴。独立性强，爱干净，是世界上最受欢迎的宠物之一。",
            "动物-猫科"),
        ObjectEntity("tiger", "老虎", "Tiger",
            "[\"虎\", \"大虫\", \"山君\"]",
            "老虎是亚洲特有的大型猫科动物，起源于约200万年前。",
            "生态系统顶级捕食者，在中国文化中象征力量和勇气。",
            "动物-猫科"),
        // 电子产品
        ObjectEntity("laptop", "笔记本电脑", "Laptop",
            "[\"笔电\", \"手提电脑\"]",
            "1981年由Epson公司推出第一台商用笔记本电脑。",
            "办公、学习、娱乐、编程等多种用途的便携式计算设备。",
            "电子产品-电脑"),
        ObjectEntity("cellular_telephone", "手机", "Mobile Phone",
            "[\"移动电话\", \"智能手机\"]",
            "1973年摩托罗拉工程师马丁·库帕发明了第一部手机。",
            "通讯、上网、拍照、支付、娱乐等多功能移动设备。",
            "电子产品-通讯"),
        ObjectEntity("television", "电视机", "Television",
            "[\"电视\", \"TV\"]",
            "1925年约翰·洛吉·贝尔德发明了机械电视。",
            "家庭娱乐、新闻资讯、教育节目的主要播放设备。",
            "电子产品-家电"),
        // 食物类
        ObjectEntity("banana", "香蕉", "Banana",
            "[\"芭蕉\"]",
            "原产于东南亚热带地区，是世界上最古老的栽培水果之一。",
            "直接食用、制作甜点、奶昔。富含钾元素和维生素B6。",
            "食物-水果"),
        ObjectEntity("orange", "橙子", "Orange",
            "[\"柳橙\", \"甜橙\"]",
            "原产于中国南方和东南亚，后传播至世界各地。",
            "直接食用、榨汁、制作果酱。富含维生素C。",
            "食物-水果"),
        ObjectEntity("pizza", "披萨", "Pizza",
            "[\"比萨\", \"意大利薄饼\"]",
            "起源于意大利那不勒斯，18世纪开始流行。",
            "主食、快餐。由面饼、番茄酱、奶酪和各种配料组成。",
            "食物-西餐"),
        // 日常用品
        ObjectEntity("cup", "杯子", "Cup",
            "[\"茶杯\", \"水杯\"]",
            "杯子的历史可追溯到新石器时代，最早由陶土制成。",
            "盛装饮料、喝水、喝茶、喝咖啡。",
            "日用品-餐具"),
        ObjectEntity("chair", "椅子", "Chair",
            "[\"座椅\"]",
            "椅子起源于古埃及，最初是权力和地位的象征。",
            "坐具，用于休息、工作、用餐等场合。",
            "日用品-家具"),
        ObjectEntity("umbrella", "雨伞", "Umbrella",
            "[\"伞\", \"遮阳伞\"]",
            "伞的历史可追溯到4000年前的中国和埃及。",
            "遮雨、遮阳、装饰。",
            "日用品-工具")
    )
}
