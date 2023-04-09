package cn.itcast.crawler;

import cn.itcast.DatasourceApplication;
import cn.itcast.bean.CovidBean;
import cn.itcast.util.HttpUtils;
import cn.itcast.util.TimeUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 实现疫情数据爬取
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes= DatasourceApplication.class)
public class Covid19DataCrawler {
    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Test
    public void testKafkaTemplate() throws InterruptedException {
        kafkaTemplate.send("test", 1, "abc");
        Thread.sleep(10000000);
    }
    //后续需要将方法改为定时任务，如每天8点定时爬取疫情数据
    @Test
    public void test(){
        String datetime = TimeUtils.format(System.currentTimeMillis(), "yyyy-MM-dd");

        //1.爬取指定页面
        String html = HttpUtils.getHtml("https://ncov.dxy.cn/ncovh5/view/pneumonia");
        System.out.println(html);
        //2.解析页面中的指定页面-即id为getAreaStat的标签中的全国疫情数据
        Document doc = Jsoup.parse(html);
        String text = doc.select("script[id=getAreaStat]").toString();
        System.out.println(text);
        //3.使用正则表达式获取疫情JSON格式的数据
        String pattern = "\\[(.*)\\]";//定义正则规则
        Pattern reg = Pattern.compile(pattern);//编译成正则对象
        Matcher matcher = reg.matcher(text);//去text中进行匹配
        String jsonStr = "";
        if (matcher.find()) {//如果text中的内容和正则规则匹配上就去出来赋值给jsonStr变量
            jsonStr = matcher.group(0);
            System.out.println(jsonStr);
        }else {
            System.out.println("no match");
        }

        //对json数据进行更进一步的解析
        //4.将第一层Json（省份数据）解析为JavaBean
        List<CovidBean> pCovidBeans = JSON.parseArray(jsonStr, CovidBean.class);
        for (CovidBean pBean : pCovidBeans) {//pBean为省份
            //先设置时间字段
            pBean.setDatetime(datetime);
            //获取cities
            String citysStr = pBean.getCities();
            List<CovidBean> covidBeans = JSON.parseArray(citysStr, CovidBean.class);
            //5.将第二层json（城市数据）解析为javaBean
            for(CovidBean bean:covidBeans){//bean为城市
                bean.setDatetime(datetime);
                bean.setPid(pBean.getLocationId());//把省份ID作为城市的PID
                bean.setProvinceShortName(pBean.getProvinceShortName());
                //后续需要将城市疫情数据发送给Kafka
            }
            //6.获取第一层json（省份数据）中的每一天的统计数据
            String statisticsDataUrl = pBean.getStatisticsData();
            String statisticsData = HttpUtils.getHtml(statisticsDataUrl);
            //获取statisticsData中的data字段对应的数据
            JSONObject jsonObject = JSON.parseObject(statisticsData);
            String dataStr = jsonObject.getString("data");
            //将爬取解析出来的每一天的数据设置回pBean（省份bean）中的statisticsData字段中（之前该字段只是一个URL）
            pBean.setStatisticsData(dataStr);
            pBean.setCities(null);
            //后续需要将省份疫情数据发送给Kafka
        }

    }
}
