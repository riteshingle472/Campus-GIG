package org.riteshingle.campusgig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

@SpringBootApplication
@EnableWebSocketMessageBroker
@EnableCaching
public class CampusGigApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusGigApplication.class, args);
    }

}
