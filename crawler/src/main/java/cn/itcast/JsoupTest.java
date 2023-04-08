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

    //Jsoup选择器,注意不同的用法规则. # 【】
    @Test
    public void testSelect() throws IOException {
        Document doc = Jsoup.parse(new File("jsoup.html"), "UTF-8");
        //根据标签名获取/选择元素
        Elements spans = doc.select("span");
        for (Element span : spans) {
            System.out.println(span.text());
        }
        //根据id获取元素
        System.out.println(doc.select("#city_bj").text());
        //根据class获取元素
        System.out.println(doc.select(".class_a").text());
        //根据属性获取元素
        System.out.println(doc.select("[abc]").text());
        //根据属性值获取元素
        System.out.println(doc.select("[class=s_name]").text());
    }
    //Jsoup选择器的高级用法
    @Test
    public void testSelect2() throws IOException {
        Document doc = Jsoup.parse(new File("jsoup.html"), "UTF-8");
        //根据元素标签名+id组合选取元素
        System.out.println(doc.select("li#test").text());
        //根据元素标签名+class组合选取元素
        System.out.println(doc.select("li.class_a").text());
        //根据标签名+元素名
        System.out.println(doc.select("span[abc]").text());
        //任意组合
        System.out.println(doc.select("span[abc].s_name").text());
        //查找某个元素的直接子元素
        System.out.println(doc.select(".city_con > ul > li").text());
        //查找某个元素的所有子元素
        System.out.println(doc.select(".city_con li").text());
        //查找某个元素的所有直接子元素
        System.out.println(doc.select(".city_con > *").text());
    }
}
