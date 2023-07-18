package demo.kafka.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class IndexController {

    /**
     * Default landing page.
     */
    @GetMapping()
    public ResponseEntity<String> index() {
        return ResponseEntity.ok("Spring Boot Demo");
    }
}
