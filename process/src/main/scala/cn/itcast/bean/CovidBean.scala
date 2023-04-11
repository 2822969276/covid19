package cn.itcast.bean

case class CovidBean(
                      provinceName: String, //省份名称
                      provinceShortName: String, //省份短名
                      cityName: String, //城市名
                      currentConfirmedCount: Int, //当前确诊人数
                      confirmedCount: Int, //累计确诊人数
                      suspectedCount: Int, //疑似确诊人数
                      curedCount: Int, //治愈人数
                      deadCount: Int, //死亡人数
                      locationId: Int, //位置id
                      pid: Int, //（父省ID）
                      cities: String, //下属城市
                      statisticsData: String, //每一天的统计数据
                      datetime: String //时间
                    )
