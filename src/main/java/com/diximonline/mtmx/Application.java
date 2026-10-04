package com.diximonline.mtmx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class Application {
  
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@EventListener
	public void init(ContextRefreshedEvent event) {
	}
}