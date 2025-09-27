package co.com.pragma.settingsadapter;

import co.com.pragma.model.gateways.ApplicationConfigurationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SpringApplicationConfigurationProvider implements ApplicationConfigurationProvider {

    private final String timezone;

    public SpringApplicationConfigurationProvider(
            @Value("${app.timezone}") String timezone) {
        this.timezone = timezone;
    }

    @Override
    public String getTimezone() {
        return timezone;
    }
}
