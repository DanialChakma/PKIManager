package com.pki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class CimsApplication {

	public static void main(String[] args) {
        System.setProperty("javax.net.debug", "ssl,handshake,trustmanager");
        SpringApplication.run(CimsApplication.class, args);
	}

}
