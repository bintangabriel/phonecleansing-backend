package cleansing.engine.phoneCleansing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PhoneCleansingApplication {
	public static void main(String[] args) {
		SpringApplication.run(PhoneCleansingApplication.class, args);
	}
}