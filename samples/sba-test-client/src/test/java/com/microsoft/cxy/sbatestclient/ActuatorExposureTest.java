package com.microsoft.cxy.sbatestclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"eureka.client.enabled=false",
		"spring.boot.admin.client.enabled=false"
})
@AutoConfigureMockMvc
class ActuatorExposureTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private Environment environment;

	@Test
	void exposesHealthButNotSensitiveActuatorEndpoints() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk());
		mockMvc.perform(get("/actuator/env"))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/actuator/heapdump"))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/actuator/threaddump"))
			.andExpect(status().isNotFound());
		mockMvc.perform(post("/actuator/loggers/ROOT"))
			.andExpect(status().isNotFound());
		mockMvc.perform(post("/actuator/shutdown"))
			.andExpect(status().isNotFound());
	}

	@Test
	void restrictsJmxExposureToNonSensitiveEndpoints() {
		assertThat(environment.getProperty("management.endpoints.jmx.exposure.include"))
			.isEqualTo("health,info");
	}
}
