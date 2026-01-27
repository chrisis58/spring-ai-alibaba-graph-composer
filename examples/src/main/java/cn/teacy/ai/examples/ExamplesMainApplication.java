package cn.teacy.ai.examples;

import cn.teacy.ai.annotation.EnableGraphComposer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableGraphComposer
@SpringBootApplication
public class ExamplesMainApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamplesMainApplication.class, args);
    }

}
