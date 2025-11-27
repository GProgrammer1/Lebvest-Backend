package com.lebvest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication(exclude = {
		org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration.class
})
public class LebvestApplication {

	public static void main(String[] args) {
		SpringApplication.run(LebvestApplication.class, args);
	}

}
