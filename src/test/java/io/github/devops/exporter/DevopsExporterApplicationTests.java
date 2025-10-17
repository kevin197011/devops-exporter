package io.github.devops.exporter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "domain.monitor.enabled=false"  // 测试时禁用域名监控
})
class DevopsExporterApplicationTests {

    @Test
    void contextLoads() {
    }

}
