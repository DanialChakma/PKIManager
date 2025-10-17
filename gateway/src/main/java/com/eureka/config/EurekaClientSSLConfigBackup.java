//package com.eureka.config;
//
//import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
//import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl;
//import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
//import org.apache.http.conn.ssl.TrustStrategy;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.ssl.SSLContexts;
//import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.client.ClientHttpRequestFactory;
//import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
//
//import javax.net.ssl.KeyManagerFactory;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.TrustManagerFactory;
//import java.io.FileInputStream;
//import java.security.KeyStore;
//
//@Configuration
//public class EurekaClientSSLConfig {
//
//    private static final String KEYSTORE_PATH = "src/main/resources/mTLS/gateway-keystore-v2.p12";
//    private static final String KEY_ALIAS = "gateway";
//    private static final String TRUSTSTORE_PATH = "src/main/resources/mTLS/gateway-truststore.p12";
//    private static final String PASSWORD = "Da123!@#";
//
//
//    private ClientHttpRequestFactory createHttpRequestFactory() {
//        try {
//            // Load KeyStore and TrustStore
//            KeyStore keyStore = KeyStore.getInstance("PKCS12");
//            keyStore.load(new FileInputStream(KEYSTORE_PATH), PASSWORD.toCharArray());
//
//            KeyStore trustStore = KeyStore.getInstance("PKCS12");
//            trustStore.load(new FileInputStream(TRUSTSTORE_PATH), PASSWORD.toCharArray());
//
//            // Build SSL context with client + trust material
//            SSLContext sslContext = SSLContexts.custom()
////                    .loadKeyMaterial(keyStore, PASSWORD.toCharArray())
//                    .loadKeyMaterial(keyStore, PASSWORD.toCharArray(), (aliases, socket) -> KEY_ALIAS)
//                    .loadTrustMaterial(trustStore, (TrustStrategy) null)
//                    .build();
//
//            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);
//
//            CloseableHttpClient httpClient = HttpClients.custom()
//                    .setSSLSocketFactory(socketFactory)
//                    .build();
////            ClientHttpRequestFactory(httpClient);
//            return new HttpComponentsClientHttpRequestFactory(httpClient);
//
//        } catch (Exception e) {
//            throw new IllegalStateException("Failed to create SSL request factory for Eureka", e);
//        }
//    }
//
//    @Bean
//    public RestTemplateDiscoveryClientOptionalArgs discoveryClientOptionalArgs() {
//        try {
//            // Load keystore and truststore
//            KeyStore keyStore = KeyStore.getInstance("PKCS12");
//            keyStore.load(new FileInputStream(KEYSTORE_PATH), PASSWORD.toCharArray());
//
//            KeyStore trustStore = KeyStore.getInstance("PKCS12");
//            trustStore.load(new FileInputStream(TRUSTSTORE_PATH), PASSWORD.toCharArray());
//
//            // Create key/trust managers
//            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//            kmf.init(keyStore, PASSWORD.toCharArray());
//
//            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//            tmf.init(trustStore);
//
//            // Build SSL context
//            SSLContext sslContext = SSLContexts.custom()
////                    .loadKeyMaterial(keyStore, PASSWORD.toCharArray())
//                    .loadKeyMaterial(keyStore, PASSWORD.toCharArray(), (aliases, socket) -> KEY_ALIAS)
//                    .loadTrustMaterial(trustStore, null)
//                    .build();
//
//            // Build Eureka Jersey client
//            EurekaJerseyClient jerseyClient = new EurekaJerseyClientImpl.EurekaJerseyClientBuilder()
//                    .withClientName("gateway-eureka-client")
//                    .withCustomSSL(sslContext)
//                    .build();
//
////            ClientHttpRequestFactory clientRequestFactory = this.createHttpRequestFactory();
//            RestTemplateDiscoveryClientOptionalArgs args = new RestTemplateDiscoveryClientOptionalArgs();
////            RestTemplateDiscoveryClientOptionalArgs args = new RestTemplateDiscoveryClientOptionalArgs(clientRequestFactory);
//            args.setEurekaJerseyClient(jerseyClient);
////            args.setSSLContext(sslContext);
//
//            return args;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new IllegalStateException("Failed to configure SSL for Eureka client", e);
//        }
//    }
//
//}
