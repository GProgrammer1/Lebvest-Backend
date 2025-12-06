package com.lebvest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication(exclude = {RabbitAutoConfiguration.class})
@org.springframework.scheduling.annotation.EnableScheduling
public class LebvestApplication {

	public static void main(String[] args) {
		SpringApplication.run(LebvestApplication.class, args);
	}

}
