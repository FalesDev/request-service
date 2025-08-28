package co.com.pragma.logger;

import co.com.pragma.model.gateways.CustomLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Slf4jCustomLogger implements CustomLogger {
    private static final Logger log =  LoggerFactory.getLogger(Slf4jCustomLogger.class);

    @Override
    public void trace(String message, Object... args) {
        log.trace(message, args);
    }

    @Override
    public void info(String message, Object... args) {
        log.info(message, args);
    }

    @Override
    public void warn(String message, Object... args) {
        log.warn(message, args);
    }

    @Override
    public void error(String message, Object... args) {
        log.error(message, args);
    }
}
