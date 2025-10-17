package io.github.devops.exporter.domain;

import io.github.devops.exporter.config.DomainMonitorProperties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DomainCheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(DomainCheckService.class);
    
    private final DomainMonitorProperties properties;
    
    // WHOIS 服务器映射
    private static final String DEFAULT_WHOIS_SERVER = "whois.internic.net";
    
    // 常见的过期时间字段模式
    private static final Pattern[] EXPIRY_PATTERNS = {
        Pattern.compile("Registry Expiry Date:\\s*(.+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Registrar Registration Expiration Date:\\s*(.+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Expiry Date:\\s*(.+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Expiration Date:\\s*(.+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Expires:\\s*(.+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Expiration Time:\\s*(.+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Expire Date:\\s*(.+)", Pattern.CASE_INSENSITIVE)
    };
    
    // 日期格式模式
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };
    
    public DomainCheckService(DomainMonitorProperties properties) {
        this.properties = properties;
    }
    
    public CompletableFuture<DomainInfo> checkDomainAsync(String domain) {
        return CompletableFuture.supplyAsync(() -> checkDomain(domain));
    }
    
    public DomainInfo checkDomain(String domain) {
        DomainInfo domainInfo = new DomainInfo(domain);
        
        try {
            String whoisData = queryWhois(domain);
            if (StringUtils.isNotBlank(whoisData)) {
                processWhoisData(domainInfo, whoisData);
            } else {
                domainInfo.setStatus("WHOIS_NOT_FOUND");
                domainInfo.setError("Unable to retrieve WHOIS data");
                domainInfo.setDaysUntilExpiration(-999); // WHOIS 查询失败标记为 -999
            }
        } catch (Exception e) {
            logger.error("Error checking domain {}: {}", domain, e.getMessage());
            domainInfo.setStatus("ERROR");
            domainInfo.setError(e.getMessage());
            domainInfo.setDaysUntilExpiration(-999); // 查询异常标记为 -999
        }
        
        return domainInfo;
    }
    
    private String queryWhois(String domain) throws IOException {
        String whoisServer = getWhoisServer(domain);
        
        try (Socket socket = new Socket(whoisServer, 43)) {
            socket.setSoTimeout(properties.getReadTimeout());
            
            // 发送查询
            socket.getOutputStream().write((domain + "\r\n").getBytes());
            socket.getOutputStream().flush();
            
            // 读取响应
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
            }
            
            return response.toString();
        }
    }
    
    private String getWhoisServer(String domain) {
        // 根据域名后缀选择合适的 WHOIS 服务器
        String tld = domain.substring(domain.lastIndexOf('.') + 1).toLowerCase();
        
        switch (tld) {
            case "com":
            case "net":
                return "whois.verisign-grs.com";
            case "org":
                return "whois.pir.org";
            case "info":
                return "whois.afilias.net";
            case "biz":
                return "whois.neulevel.biz";
            case "cn":
                return "whois.cnnic.net.cn";
            case "uk":
                return "whois.nominet.uk";
            case "de":
                return "whois.denic.de";
            case "fr":
                return "whois.afnic.fr";
            case "jp":
                return "whois.jprs.jp";
            default:
                return DEFAULT_WHOIS_SERVER;
        }
    }
    
    private void processWhoisData(DomainInfo domainInfo, String whoisData) {
        try {
            LocalDateTime expirationDate = extractExpirationDate(whoisData);
            
            if (expirationDate != null) {
                domainInfo.setExpirationDate(expirationDate);
                
                // 计算距离过期的天数
                LocalDateTime now = LocalDateTime.now();
                long daysUntilExpiration = ChronoUnit.DAYS.between(now, expirationDate);
                domainInfo.setDaysUntilExpiration(daysUntilExpiration);
                
                // 判断状态
                if (daysUntilExpiration < 0) {
                    domainInfo.setExpired(true);
                    domainInfo.setStatus("EXPIRED");
                } else if (daysUntilExpiration <= properties.getWarningDays()) {
                    domainInfo.setWarning(true);
                    domainInfo.setStatus("WARNING");
                } else {
                    domainInfo.setStatus("VALID");
                }
                
                logger.info("Domain {} expires in {} days ({})", 
                    domainInfo.getDomain(), daysUntilExpiration, expirationDate);
            } else {
                domainInfo.setStatus("PARSE_ERROR");
                domainInfo.setError("Unable to parse expiration date from WHOIS data");
                domainInfo.setDaysUntilExpiration(-999); // 查询失败标记为 -999
                logger.warn("Could not extract expiration date for domain: {}", domainInfo.getDomain());
            }
                
        } catch (Exception e) {
            logger.error("Error processing WHOIS data for {}: {}", domainInfo.getDomain(), e.getMessage());
            domainInfo.setStatus("ERROR");
            domainInfo.setError("Error processing WHOIS data: " + e.getMessage());
            domainInfo.setDaysUntilExpiration(-999); // 处理错误标记为 -999
        }
    }
    
    private LocalDateTime extractExpirationDate(String whoisData) {
        for (Pattern pattern : EXPIRY_PATTERNS) {
            Matcher matcher = pattern.matcher(whoisData);
            if (matcher.find()) {
                String dateStr = matcher.group(1).trim();
                LocalDateTime date = parseDate(dateStr);
                if (date != null) {
                    return date;
                }
            }
        }
        return null;
    }
    
    private LocalDateTime parseDate(String dateStr) {
        // 清理日期字符串
        dateStr = dateStr.replaceAll("\\s+", " ").trim();
        
        // 移除时区信息
        dateStr = dateStr.replaceAll("\\s*UTC.*$", "");
        dateStr = dateStr.replaceAll("\\s*GMT.*$", "");
        dateStr = dateStr.replaceAll("\\s*\\+\\d{4}.*$", "");
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // 尝试下一个格式
            }
        }
        
        logger.debug("Could not parse date: {}", dateStr);
        return null;
    }
}