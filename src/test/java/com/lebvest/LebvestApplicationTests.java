package com.lebvest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LebvestApplicationTests {

	@Test
	void contextLoads() {
		// This integration test verifies that the Spring application context loads successfully
		// It ensures all beans are properly configured and the application can start
		// External services (RabbitMQ, Redis) are configured to fail gracefully in test profile
	}

}

