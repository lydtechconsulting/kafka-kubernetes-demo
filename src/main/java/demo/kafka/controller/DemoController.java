package demo.kafka.controller;

import demo.kafka.rest.api.TriggerEventsRequest;
import demo.kafka.service.DemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/demo")
public class DemoController {

    @Autowired
    private final DemoService demoService;

    @GetMapping("/version")
    public ResponseEntity<String> version() {
        return ResponseEntity.ok("v1");
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> trigger(@RequestBody TriggerEventsRequest request) {
        if(request.getNumberOfEvents() == null || request.getNumberOfEvents()<1) {
            log.error("Invalid number of events");
            return ResponseEntity.badRequest().build();
        }
        try {
            demoService.process(request.getNumberOfEvents());
            return ResponseEntity.ok().build();
        } catch(Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
