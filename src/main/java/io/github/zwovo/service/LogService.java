package io.github.zwovo.service;

import io.github.zwovo.config.TencentCloudConfig;
import io.github.zwovo.sign.QcloudClsSignature;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import io.github.zwovo.proto.Cls;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class LogService {

    private LZ4Factory factory = LZ4Factory.fastestInstance();
    private LZ4Compressor compressor = factory.fastCompressor();
    public String insert(Cls.LogGroupList logGroupList) throws UnsupportedEncodingException {
        // 创建 HttpClient 客户端
        CloseableHttpClient httpClient = HttpClients.createDefault();
        // 创建 HttpGet 请求
        String host = TencentCloudConfig.area + ".cls.tencentyun.com";
        String path = "/structuredlog";
        String parms = "?topic_id=" + TencentCloudConfig.logTopic;
        String url = "http://" + host + path + parms;
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("HOST", host);
        httpPost.setHeader("x-cls-compress-type", "lz4");
        httpPost.setHeader("Content-Type", "application/x-protobuf");
        Map<String, String> paramMap = new HashMap();
        Map<String, String> headerMap = new HashMap();
        paramMap.put("topic_id", TencentCloudConfig.logTopic);
        headerMap.put("Host", host);
        headerMap.put("User-Agent", "AuthSDK");
        String authorization = QcloudClsSignature.buildSignature(TencentCloudConfig.secretId, TencentCloudConfig.secretKey,
                "POST", path, paramMap, headerMap, 1000000);
        httpPost.setHeader("Authorization", authorization);
        // 创建 HttpPost 参数
        CloseableHttpResponse httpResponse = null;
        try {
            httpPost.setEntity(new ByteArrayEntity(compressor.compress(logGroupList.toByteArray())));
            // 请求并获得响应结果
            httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            // 输出请求结果
            String response = EntityUtils.toString(httpEntity);
            return response.length() == 0 ? "上传成功" : response;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 无论如何必须关闭连接
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
