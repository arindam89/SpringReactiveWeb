package com.geekpaul.springweb;

import com.geekpaul.springweb.reactive.greeting.GreetingClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringwebApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringwebApplication.class, args);

		GreetingClient client = new GreetingClient();
		System.out.println(client.getResult());
	}

}
