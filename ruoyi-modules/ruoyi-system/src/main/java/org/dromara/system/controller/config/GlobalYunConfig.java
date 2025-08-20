package org.dromara.system.controller.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "global")
@Data
public class GlobalYunConfig {



    @Configuration
    @ConfigurationProperties(prefix = "global.wx")
    @Data
    public static class WX {
        private String appId;
        private String appSecret;
    }

    @Configuration
    @ConfigurationProperties(prefix = "global.oss")
    @Data
    public static class OSS{
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String fileHost;
        private String bucketName;
        private String bucketHost;
    }
    @Configuration
    @ConfigurationProperties(prefix = "global.local")
    @Data
    public static class LocalFile{
        /**
         * 聘书原图地址
         */
        private String letterPath;
    }
}
