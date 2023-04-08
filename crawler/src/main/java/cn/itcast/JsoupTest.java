package cn.itcast;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Desc：使用Jsoup实现页面解析
 */
public class JsoupTest {
    //初识Jsoup及其获取Document的几种不同的方法，jsoup.html为本地测试文件
    @Test
    public void testGetDocument() throws IOException {
        //方法一：使用connect方法直接连接网址获取Document
//        Document doc = Jsoup.connect("https://www.itcast.cn/").get();
        //方法二：通过网址可以以转化的方式获取Document
//        Document doc = Jsoup.parse(new URL("https://www.itcast.cn"), 1000);
        //方法三：通过本地文件路径获取Document
//        Document doc = Jsoup.parse(new File("jsoup.html"), "UTF-8");
        //方法四：通过字符串解析生产Document
        String html = FileUtils.readFileToString(new File("jsoup.html"), "UTF-8");
        Document doc = Jsoup.parse(html);
        System.out.println(doc);
        Element titleElement = doc.getElementsByTag("title").first();
        String tital = titleElement.text();
        System.out.println(tital);
    }
    //通过Jsoup获取Document中的元素
    @Test
    public void testGetElement() throws IOException {
        Document doc = Jsoup.parse(new File("jsoup.html"), "UTF-8");
//        System.out.println(doc);

        //根据标签ID获取元素getElementById
        Element element = doc.getElementById("city_bj");
        System.out.println(element.text());

        //根据标签获取元素
        Elements elements = doc.getElementsByTag("title");
        System.out.println(elements.first().text());

        //根据class获取元素
        Elements elementsByClass = doc.getElementsByClass("s_name");
        System.out.println(elementsByClass.last().text());

        //根据属性名称获取元素
        System.out.println(doc.getElementsByAttribute("abc").first().text());
    }

    //Jsoup的操作Document中的元素
    @Test
    public void testElementOperator() throws IOException {
        Document doc = Jsoup.parse(new File("jsoup.html"), "UTF-8");
        Element element = doc.getElementsByAttributeValue("class", "city_con").first();
        //获取元素中的id
        System.out.println(element.id());
        //获取元素中的classname
        System.out.println(element.className());
        //获取元素中的属性值
        System.out.println(element.attr("id"));
        //从元素中获取所有的属性
        System.out.println(element.attributes().toString());
        //获取元素中的文本内容
        System.out.println(element.text());
    }
}
