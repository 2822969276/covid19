package cn.itcast.bean

/**
 * 用来封装省份每一天的统计数据
 */
case class StatisticsDataBean(
                               var dateId: String,//日期
                               var provinceShortName: String,//省份短名
                               var locationId:Int,//位置ID
                               var confirmedCount: Int,//确诊
                               var currentConfirmedCount: Int,//当前确诊
                               var confirmedIncr: Int,//确诊新增
                               var curedCount: Int,//治愈数
                               var currentConfirmedIncr: Int,//当前确诊新增
                               var curedIncr: Int,//治愈新增
                               var suspectedCount: Int,//疑似数
                               var suspectedCountIncr: Int,//疑似新增
                               var deadCount: Int,//死亡数
                               var deadIncr: Int//死亡新增
                             )
