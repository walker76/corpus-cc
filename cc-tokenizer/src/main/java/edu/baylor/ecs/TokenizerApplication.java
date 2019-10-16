package edu.baylor.ecs;

import edu.baylor.ecs.service.FileService;
import edu.baylor.ecs.service.TokenService;
import edu.baylor.ecs.service.TokenServiceImpl;
import edu.baylor.ecs.service.TreeService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
public class TokenizerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TokenizerApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public TokenService tokenService(FileService fileService, TreeService treeService){
        return new TokenServiceImpl(fileService, treeService);
    }
}
