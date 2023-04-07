package cn.itcast;

import org.junit.Test;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author zxq
 * @create 2023-04-07 16:17:03
 * @Description: TODO 演示使用JDK自带的API实现网络爬虫
 */
public class JDKAPITest {
    @Test
    public void testGet() throws IOException {
        //1.确定要访问/爬取的URL
        URL url = new URL("https://www.itcast.cn/?username=xx");
        //2.获取连接对象
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        //3.设置连接信息：请求方式/请求参数/请求头
        urlConnection.setRequestMethod("GET");//请求方式默认是GET，注意要大写
        //User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36 Edg/111.0.1661.62
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36 Edg/111.0.1661.62");
        urlConnection.setConnectTimeout(30000);//设置超时时间，单位毫秒
        //4.获取数据
        InputStream in = urlConnection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        String html = "";
        while ((line = reader.readLine()) != null) {
            html += line +"\n";
        }
        System.out.println(html);
        //5.关闭资源
        in.close();
        reader.close();
    }
    @Test
    public void testPost() throws IOException {
        //1.确定URL
        URL url = new URL("https://www.itcast.cn/");
        //2.获取连接
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        //3.设置连接信息
        urlConnection.setDoOutput(true);//允许向URL输出内容
        urlConnection.setRequestMethod("POST");//请求方式默认是GET，注意要大写
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36 Edg/111.0.1661.62");
        urlConnection.setConnectTimeout(30000);//设置超时时间，单位毫秒
        //注意JDK自带的API不让用out，所以需要设置urlConnection.setDoOutput(true);
        OutputStream out = urlConnection.getOutputStream();
        out.write("username=xx".getBytes());
        //4.获取数据
        InputStream in = urlConnection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        String html = "";
        while ((line = reader.readLine()) != null) {
            html += line +"\n";
        }
        System.out.println(html);
        //5.关闭资源
        in.close();
        reader.close();
    }
}
