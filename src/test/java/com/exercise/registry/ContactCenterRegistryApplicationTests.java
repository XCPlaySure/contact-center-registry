package com.exercise.registry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ContactCenterRegistryApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void mainStartsNonWebContext() {
		ContactCenterRegistryApplication.main(new String[] {
			"--spring.main.web-application-type=none",
			"--spring.main.lazy-initialization=true"
		});
	}

}
