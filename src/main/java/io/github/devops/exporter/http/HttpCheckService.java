package io.github.devops.exporter.http;

import io.github.devops.exporter.config.HttpMonitorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class HttpCheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpCheckService.class);
    
    private final HttpMonitorProperties properties;
    
    public HttpCheckService(HttpMonitorProperties properties) {
        this.properties = properties;
    }
    
    public CompletableFuture<HttpInfo> checkHttpAsync(String url) {
        return CompletableFuture.supplyAsync(() -> checkHttp(url));
    }
    
    public HttpInfo checkHttp(String url) {
        HttpInfo httpInfo = new HttpInfo(url);
        
        try {
            long startTime = System.currentTimeMillis();
            HttpURLConnection connection = createConnection(url);
            
            // 执行请求
            int statusCode = connection.getResponseCode();
            String statusMessage = connection.getResponseMessage();
            long endTime = System.currentTimeMillis();
            
            httpInfo.setResponseTimeMs(endTime - startTime);
            httpInfo.setStatusCode(statusCode);
            httpInfo.setStatusMessage(statusMessage);
            
            // 获取响应头信息
            Map<String, String> headers = new HashMap<>();
            connection.getHeaderFields().forEach((key, values) -> {
                if (key != null && !values.isEmpty()) {
                    headers.put(key, String.join(", ", values));
                }
            });
            httpInfo.setResponseHeaders(headers);
            
            // 获取内容信息
            httpInfo.setContentLength(connection.getContentLengthLong());
            httpInfo.setContentType(connection.getContentType());
            
            // 检查重定向
            if (isRedirect(statusCode)) {
                String redirectUrl = connection.getHeaderField("Location");
                httpInfo.setRedirectUrl(redirectUrl);
            }
            
            // 判断可用性
            boolean isAvailable = isSuccessStatusCode(statusCode);
            httpInfo.setAvailable(isAvailable);
            
            if (isAvailable) {
                httpInfo.setStatus("AVAILABLE");
                logger.debug("HTTP check for {} successful: {} {} ({}ms)", 
                    url, statusCode, statusMessage, httpInfo.getResponseTimeMs());
            } else {
                httpInfo.setStatus("UNAVAILABLE");
                httpInfo.setError("HTTP " + statusCode + " " + statusMessage);
                logger.debug("HTTP check for {} failed: {} {}", url, statusCode, statusMessage);
            }
            
            connection.disconnect();
            
        } catch (Exception e) {
            logger.error("Error checking HTTP for {}: {}", url, e.getMessage());
            httpInfo.setStatus("ERROR");
            httpInfo.setError(e.getMessage());
            httpInfo.setAvailable(false);
        }
        
        return httpInfo;
    }
    
    private HttpURLConnection createConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 设置超时
        connection.setConnectTimeout(properties.getConnectionTimeout());
        connection.setReadTimeout(properties.getReadTimeout());
        
        // 设置请求方法
        connection.setRequestMethod("GET");
        
        // 设置用户代理
        connection.setRequestProperty("User-Agent", 
            "DevOps-Exporter/1.0 (HTTP Monitor)");
        
        // 设置是否跟随重定向
        connection.setInstanceFollowRedirects(properties.isFollowRedirects());
        
        // 设置其他请求头
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Connection", "close");
        
        return connection;
    }
    
    private boolean isSuccessStatusCode(int statusCode) {
        List<Integer> expectedCodes = properties.getExpectedStatusCodes();
        if (expectedCodes != null && !expectedCodes.isEmpty()) {
            return expectedCodes.contains(statusCode);
        }
        
        // 默认认为 2xx 状态码为成功
        return statusCode >= 200 && statusCode < 300;
    }
    
    private boolean isRedirect(int statusCode) {
        return statusCode >= 300 && statusCode < 400;
    }
}