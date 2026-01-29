package cn.teacy.ai.examples.service;

import org.springframework.stereotype.Service;

// #region snippet
@Service
public class GreetingService {

    public String greet(String name) {
        return "Hello, " + name + "! Welcome to SAA Graph Composer.";
    }

}
// #endregion snippet
