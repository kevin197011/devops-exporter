package io.github.devops.exporter.ssl;

import io.github.devops.exporter.config.SslMonitorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

@Service
public class SslCheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(SslCheckService.class);
    
    private final SslMonitorProperties properties;
    
    public SslCheckService(SslMonitorProperties properties) {
        this.properties = properties;
    }
    
    public CompletableFuture<SslCertificateInfo> checkSslAsync(String domain) {
        return CompletableFuture.supplyAsync(() -> checkSsl(domain));
    }
    
    public SslCertificateInfo checkSsl(String domain) {
        SslCertificateInfo sslInfo = new SslCertificateInfo(domain);
        
        try {
            X509Certificate certificate = getCertificate(domain);
            if (certificate != null) {
                processCertificate(sslInfo, certificate);
            } else {
                sslInfo.setStatus("CERTIFICATE_NOT_FOUND");
                sslInfo.setError("Unable to retrieve SSL certificate");
                sslInfo.setDaysUntilExpiration(-999);
            }
        } catch (Exception e) {
            logger.error("Error checking SSL for domain {}: {}", domain, e.getMessage());
            sslInfo.setStatus("ERROR");
            sslInfo.setError(e.getMessage());
            sslInfo.setDaysUntilExpiration(-999);
        }
        
        return sslInfo;
    }
    
    private X509Certificate getCertificate(String domain) throws IOException {
        // 首先尝试通过 HTTPS 连接获取证书
        try {
            URL url = new URL("https://" + domain);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(properties.getConnectionTimeout());
            connection.setReadTimeout(properties.getReadTimeout());
            connection.connect();
            
            Certificate[] certificates = connection.getServerCertificates();
            connection.disconnect();
            
            if (certificates.length > 0 && certificates[0] instanceof X509Certificate) {
                return (X509Certificate) certificates[0];
            }
        } catch (Exception e) {
            logger.debug("HTTPS connection failed for {}, trying SSL socket: {}", domain, e.getMessage());
        }
        
        // 如果 HTTPS 连接失败，尝试直接 SSL 连接
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(domain, 443)) {
                socket.setSoTimeout(properties.getReadTimeout());
                socket.startHandshake();
                
                Certificate[] certificates = socket.getSession().getPeerCertificates();
                if (certificates.length > 0 && certificates[0] instanceof X509Certificate) {
                    return (X509Certificate) certificates[0];
                }
            }
        } catch (Exception e) {
            logger.debug("SSL socket connection failed for {}: {}", domain, e.getMessage());
        }
        
        return null;
    }
    
    private void processCertificate(SslCertificateInfo sslInfo, X509Certificate certificate) {
        try {
            // 获取证书过期时间
            Instant expirationInstant = certificate.getNotAfter().toInstant();
            LocalDateTime expirationDate = LocalDateTime.ofInstant(expirationInstant, ZoneId.systemDefault());
            sslInfo.setExpirationDate(expirationDate);
            
            // 设置证书信息
            sslInfo.setIssuer(certificate.getIssuerDN().getName());
            sslInfo.setSubject(certificate.getSubjectDN().getName());
            
            // 计算距离过期的天数
            LocalDateTime now = LocalDateTime.now();
            long daysUntilExpiration = ChronoUnit.DAYS.between(now, expirationDate);
            sslInfo.setDaysUntilExpiration(daysUntilExpiration);
            
            // 判断状态
            if (daysUntilExpiration < 0) {
                sslInfo.setExpired(true);
                sslInfo.setStatus("EXPIRED");
            } else if (daysUntilExpiration <= properties.getWarningDays()) {
                sslInfo.setWarning(true);
                sslInfo.setStatus("WARNING");
            } else {
                sslInfo.setStatus("VALID");
            }
            
            logger.info("SSL certificate for {} expires in {} days ({})", 
                sslInfo.getDomain(), daysUntilExpiration, expirationDate);
                
        } catch (Exception e) {
            logger.error("Error processing SSL certificate for {}: {}", sslInfo.getDomain(), e.getMessage());
            sslInfo.setStatus("ERROR");
            sslInfo.setError("Error processing SSL certificate: " + e.getMessage());
            sslInfo.setDaysUntilExpiration(-999);
        }
    }
}