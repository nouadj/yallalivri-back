package dz.nadjtech.yallalivri;

import dz.nadjtech.yallalivri.config.JwtProperties;
import dz.nadjtech.yallalivri.config.NotificationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, NotificationProperties.class})
public class YallaLivriApplication {

    public static void main(String[] args) {
        SpringApplication.run(YallaLivriApplication.class, args);
    }
}
