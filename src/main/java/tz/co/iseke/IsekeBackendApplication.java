package tz.co.iseke;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IsekeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(IsekeBackendApplication.class, args);
    }

}
