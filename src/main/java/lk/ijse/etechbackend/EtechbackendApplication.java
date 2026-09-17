package lk.ijse.etechbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EtechbackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(EtechbackendApplication.class, args);
	}

}

