package com.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServer {

	public static void main(String[] args) {
//        javax.net.debug=ssl,handshake,trustmanager
        System.setProperty("javax.net.debug", "ssl,handshake");

        // Keystore: contains client cert & key for mTLS
        System.setProperty("javax.net.ssl.keyStore", "src/main/resources/mTLS/eureka-keystore-v2.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "Da123!@#");
        System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");
        System.setProperty("javax.net.ssl.keyAlias", "eureka");

        // Truststore: contains Eureka server cert
        System.setProperty("javax.net.ssl.trustStore", "src/main/resources/mTLS/eureka-truststore-v2.p12");
        System.setProperty("javax.net.ssl.trustStorePassword", "Da123!@#");
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");

        SpringApplication.run(EurekaServer.class, args);
	}

}
