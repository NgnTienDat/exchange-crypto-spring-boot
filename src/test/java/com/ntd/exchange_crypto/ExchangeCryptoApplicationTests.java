package com.ntd.exchange_crypto;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;


@SpringBootTest(properties = "spring.profiles.active=test")
class ExchangeCryptoApplicationTests {
	@Test
	void contextLoads() {
		ApplicationModules modules = ApplicationModules.of(ExchangeCryptoApplication.class);
		modules.verify();
	}
}

//@SpringBootTest
//class ExchangeCryptoApplicationTests {
//
//	@Test
//	void contextLoads() {
//		ApplicationModules modules = ApplicationModules.of(ExchangeCryptoApplication.class);
//		modules.verify();
////		modules.forEach(System.out::println);
//
//
//	}
//
//}
