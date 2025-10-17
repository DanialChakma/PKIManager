package com.eureka.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import javax.net.ssl.SSLContext;
import org.apache.http.ssl.SSLContextBuilder;
import java.io.File;

@Configuration
public class EurekaSSLConfig {
    private static final Log logger = LogFactory.getLog(EurekaSSLConfig.class);
    @Bean
    public RestTemplate eurekaRestTemplate() throws Exception {
        logger.info("::::::::::eurekaRestTemplate called::::::::::::::");
        SSLContext sslContext = SSLContextBuilder.create()
                .loadKeyMaterial(
                        new File("src/main/resources/mTLS/gateway-keystore-v2.p12"),
                        "Da123!@#".toCharArray(),  // Keystore password
                        "Da123!@#".toCharArray())  // Key password (same if combined)
                .loadTrustMaterial(
                        new File("src/main/resources/mTLS/gateway-truststore.p12"),
                        "Da123!@#".toCharArray())
                .build();

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .build();

        RestTemplate restTemplate = new RestTemplate(
                new HttpComponentsClientHttpRequestFactory(httpClient));
        return restTemplate;
    }
}

