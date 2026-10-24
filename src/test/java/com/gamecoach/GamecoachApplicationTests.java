package com.gamecoach;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Starts the whole app, with an in-memory H2 database instead of the ./data file. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:context-test;DB_CLOSE_DELAY=-1")
class GamecoachApplicationTests {

	@Test
	void contextLoads() {
	}

}
