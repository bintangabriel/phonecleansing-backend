package cleansing.engine.phoneCleansing.controller;

import org.springframework.stereotype.Controller;

@Controller
public class MainController {
    private  String renderHomePage(){
        return "index.html";
    }
}
