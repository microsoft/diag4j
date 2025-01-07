package com.microsoft.cxy.sbatestclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@Configuration
@EnableDiscoveryClient
@EnableAutoConfiguration
public class SbaTestClientApplication {

	private static boolean DBDowngrade = false;
	public static void main(String[] args) {
		SpringApplication.run(SbaTestClientApplication.class, args);
	}

	@PostMapping("/latency")
	public String downgradeDB() {
		DBDowngrade = true;
		return "DB downgrade set to " + DBDowngrade;
	}

	@PutMapping("/infor")
	public String updateInformation() {
		validation();
		accessDB(false);
		filterData();

		return "Information updated";
	}

	public void accessDB(boolean isHealthCheck) {
		if (DBDowngrade) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		if (isHealthCheck) {
			return;
		}

		System.out.println("Done with the DB access");
	}

	private void validation() {
		System.out.println("Done with the validation");
	}

	private void filterData() {
		System.out.println("Done with the data filtering");
	}

}
