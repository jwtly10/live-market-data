package com.jwtly.livemarketdata;

import org.springframework.boot.SpringApplication;

public class TestLiveMarketDataApplication {

    public static void main(String[] args) {
        SpringApplication.from(LiveMarketDataApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
