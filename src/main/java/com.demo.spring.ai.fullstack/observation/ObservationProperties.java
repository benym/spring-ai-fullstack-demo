package com.demo.spring.ai.fullstack.observation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = ObservationProperties.PREFIX)
@Getter
@Setter
public class ObservationProperties {

	public static final String PREFIX = "demo.ai.observation";

	private boolean enabled;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}