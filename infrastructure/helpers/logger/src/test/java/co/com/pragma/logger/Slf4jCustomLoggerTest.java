package co.com.pragma.logger;

import co.com.pragma.model.gateways.CustomLogger;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Slf4jCustomLoggerTest {

    private CustomLogger logger;
    private TestLogger testLogger;

    @BeforeEach
    void setup() {
        logger = new Slf4jCustomLogger();
        testLogger = TestLoggerFactory.getTestLogger(Slf4jCustomLogger.class);
        testLogger.clear();
    }

    @Test
    void testTrace() {
        logger.trace("Trace message {}", 123);

        assertThat(testLogger.getLoggingEvents())
                .hasSize(1)
                .allSatisfy(event -> {
                    assertThat(event.getLevel().toString()).isEqualTo("TRACE");
                    assertThat(event.getMessage()).isEqualTo("Trace message {}");
                    assertThat(event.getArguments()).containsExactly(123);
                });
    }

    @Test
    void testInfo() {
        logger.info("Info message {}", "hello");

        assertThat(testLogger.getLoggingEvents())
                .hasSize(1)
                .allSatisfy(event -> {
                    assertThat(event.getLevel().toString()).isEqualTo("INFO");
                    assertThat(event.getMessage()).isEqualTo("Info message {}");
                    assertThat(event.getArguments()).containsExactly("hello");
                });
    }

    @Test
    void testWarn() {
        logger.warn("Warn message {}", "warning");

        assertThat(testLogger.getLoggingEvents())
                .hasSize(1)
                .allSatisfy(event -> {
                    assertThat(event.getLevel().toString()).isEqualTo("WARN");
                    assertThat(event.getMessage()).isEqualTo("Warn message {}");
                    assertThat(event.getArguments()).containsExactly("warning");
                });
    }

    @Test
    void testError() {
        RuntimeException ex = new RuntimeException("oops");
        logger.error("Error message {}", ex);

        assertThat(testLogger.getLoggingEvents())
                .hasSize(1)
                .allSatisfy(event -> {
                    assertThat(event.getLevel().toString()).isEqualTo("ERROR");
                    assertThat(event.getMessage()).isEqualTo("Error message {}");
                    assertThat(event.getArguments()).isEmpty();
                    assertThat(event.getThrowable().isPresent()).isTrue();
                    assertThat(event.getThrowable().get()).isSameAs(ex);
                });
    }
}
