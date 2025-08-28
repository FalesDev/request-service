package co.com.pragma.model.gateways;

public interface CustomLogger {
    void trace(String message, Object ... args);
    void info(String message, Object ... args);
    void warn(String message, Object ... args);
    void error(String message, Object ... args);
}
