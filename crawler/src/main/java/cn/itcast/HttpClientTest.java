package cn.itcast;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zxq
 * @create 2023-04-07 16:44:29
 * @Description: TODO 演示使用HttpClient实现网络爬虫
 */
public class HttpClientTest {
    @Test
    public void testGet() throws IOException {
        //1.创建HttpClient对象
//        DefaultHttpClient defaultHttpClient = new DefaultHttpClient();//过时！
        CloseableHttpClient httpClient = HttpClients.createDefault();

        //2.创建HttpGet请求并进行相关设置
        HttpGet httpGet = new HttpGet("https://www.itcast.cn/?username=java");
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36 Edg/111.0.1661.62");

        //3.发起请求
        CloseableHttpResponse response = httpClient.execute(httpGet);

        //4.判断响应状态码并获取响应数据
        if (response.getStatusLine().getStatusCode()==200) {//200表示响应成功
            String html = EntityUtils.toString(response.getEntity(), "UTF-8");
            System.out.println(html);
        }

        //5.关闭资源
        response.close();
        httpClient.close();
    }

    @Test
    public void testPost() throws IOException {
        //1.创建HttpClient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //2.创建HttpPost对象并进行相关参数设置
        HttpPost httpPost = new HttpPost("https://www.itcast.cn/");
        //准备集合用来存放请求参数
        //设置请求体/参数
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("username", "java"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
        httpPost.setEntity(entity);
        //设置请求头
        httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36 Edg/111.0.1661.62");

        //3.发起请求
        CloseableHttpResponse response = httpClient.execute(httpPost);
        //4.判断响应状态并获取数据
        if (response.getStatusLine().getStatusCode()==200) {
            String html = EntityUtils.toString(response.getEntity(), "UTF-8");
            System.out.println(html);
        }
        //5.关闭资源
        response.close();
        httpClient.close();
    }
}
