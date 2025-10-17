package io.github.devops.exporter.port;

import io.github.devops.exporter.config.PortMonitorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;

@Service
public class PortCheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(PortCheckService.class);
    
    private final PortMonitorProperties properties;
    
    public PortCheckService(PortMonitorProperties properties) {
        this.properties = properties;
    }
    
    public CompletableFuture<PortInfo> checkPortAsync(String target) {
        return CompletableFuture.supplyAsync(() -> checkPort(target));
    }
    
    public PortInfo checkPort(String target) {
        PortInfo portInfo = new PortInfo(target);
        
        // 如果解析目标格式失败，直接返回
        if ("INVALID_FORMAT".equals(portInfo.getStatus())) {
            return portInfo;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            boolean isOpen = isPortOpen(portInfo.getHost(), portInfo.getPort());
            long endTime = System.currentTimeMillis();
            
            portInfo.setResponseTimeMs(endTime - startTime);
            portInfo.setOpen(isOpen);
            
            if (isOpen) {
                portInfo.setStatus("OPEN");
                logger.debug("Port {}:{} is OPEN (response time: {}ms)", 
                    portInfo.getHost(), portInfo.getPort(), portInfo.getResponseTimeMs());
            } else {
                portInfo.setStatus("CLOSED");
                logger.debug("Port {}:{} is CLOSED", portInfo.getHost(), portInfo.getPort());
            }
            
        } catch (Exception e) {
            logger.error("Error checking port {}: {}", target, e.getMessage());
            portInfo.setStatus("ERROR");
            portInfo.setError(e.getMessage());
            portInfo.setOpen(false);
        }
        
        return portInfo;
    }
    
    private boolean isPortOpen(String host, int port) throws IOException {
        try (Socket socket = new Socket()) {
            // 解析主机名到IP地址
            InetAddress address = InetAddress.getByName(host);
            InetSocketAddress socketAddress = new InetSocketAddress(address, port);
            
            // 尝试连接
            socket.connect(socketAddress, properties.getConnectionTimeout());
            return true;
            
        } catch (SocketTimeoutException e) {
            logger.debug("Connection timeout for {}:{}", host, port);
            return false;
        } catch (IOException e) {
            logger.debug("Connection failed for {}:{} - {}", host, port, e.getMessage());
            return false;
        }
    }
}