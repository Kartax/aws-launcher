package de.kartax.awslauncher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AwsLauncherApplication {

    public static void main(String[] args) {
        SpringApplication.run(AwsLauncherApplication.class, args);
    }

}
