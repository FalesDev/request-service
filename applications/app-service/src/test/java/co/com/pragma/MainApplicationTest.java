package co.com.pragma;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
public class MainApplicationTest {

    @Test
    @DisplayName("Should load Spring application context without errors")
    void contextLoads() {
        // This test is intentionally left empty because
        // it only verifies that the Spring context starts successfully.
    }

    @Test
    @DisplayName("Should run main method without errors")
    void testMainMethod() {
        assertThatCode(() -> MainApplication.main(new String[]{}))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should run Spring application with main method")
    void mainMethodRunsSpringApplication() {
        try (var mocked = mockStatic(SpringApplication.class)) {
            String[] args = new String[]{};

            MainApplication.main(args);

            mocked.verify(() -> SpringApplication.run(MainApplication.class, args));
        }
    }
}

