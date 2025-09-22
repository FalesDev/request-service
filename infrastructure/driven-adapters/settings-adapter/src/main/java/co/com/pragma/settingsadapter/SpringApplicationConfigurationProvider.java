package co.com.pragma.settingsadapter;

import co.com.pragma.model.gateways.ApplicationConfigurationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SpringApplicationConfigurationProvider implements ApplicationConfigurationProvider {

    @Value("${app.timezone}")
    private String timezone;

    @Override
    public String getTimezone() {
        return timezone;
    }
}
