package com.group.social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EntityScan(basePackages = "com.group.social") // Sostituisci con il pacchetto radice del tuo progetto
@EnableAsync // <--- FONDAMENTALE per far funzionare l'OutboundDispatcher in background!
public class SocialApplication {
    public static void main(String[] args) {
        SpringApplication.run(SocialApplication.class, args);
    }
}
