package co.com.pragma.model.application.gateways;

import co.com.pragma.model.application.Application;
import reactor.core.publisher.Mono;

public interface ApplicationRepository {
    Mono<Application> save(Application application);
}
