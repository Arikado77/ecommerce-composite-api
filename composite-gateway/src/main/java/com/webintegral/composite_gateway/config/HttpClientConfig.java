package com.webintegral.composite_gateway.config;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    @Bean
    RestClient restClient(
            @Value("${http.client.connect-timeout:3s}") Duration connectTimeout,
            @Value("${http.client.read-timeout:5s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService compositeExecutor() {
        return Executors.newFixedThreadPool(3, task -> {
            Thread thread = new Thread(task, "composite-upstream-");
            thread.setDaemon(false);
            return thread;
        });
    }
}
