package demo.kafka.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class IndexControllerTest {

    private IndexController controller;

    @BeforeEach
    public void setUp() {
        controller = new IndexController();
    }

    @Test
    public void testIndex() {
        ResponseEntity response = controller.index();
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody(), equalTo("Spring Boot Demo"));
    }
}
