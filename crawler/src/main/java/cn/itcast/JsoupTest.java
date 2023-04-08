package cn.itcast;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Desc：使用Jsoup实现页面解析
 */
public class JsoupTest {
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
}
