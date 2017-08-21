package com.shang.versioncontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@SpringBootApplication
@EnableZuulProxy
public class VersionControlApplication {

	public static void main(String[] args) {
		SpringApplication.run(VersionControlApplication.class, args);
	}
}
