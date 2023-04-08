package cn.itcast.crawler;

import cn.itcast.util.HttpUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 实现疫情数据爬取
 */
public class Covid19DataCrawler {
    //后续需要将方法改为定时任务，如每天8点定时爬取疫情数据
    @Test
    public void test(){
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

    }
}
