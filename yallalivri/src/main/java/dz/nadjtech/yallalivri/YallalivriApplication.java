package dz.nadjtech.yallalivri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class YallalivriApplication {

	public static void main(String[] args) {
		SpringApplication.run(YallalivriApplication.class, args);
	}

}
