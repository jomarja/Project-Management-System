package com.pms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Starts the whole application once to check that every bean can be created and wired
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:context-test",
        "app.upload.dir=target/test-uploads"
})
class ProjectManagementSystemApplicationTests {

    @Test
    void contextLoads() {
    }
}
