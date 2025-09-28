package co.com.pragma.webclient.config;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = WebClientConfig.class)
class WebClientConfigTest {

    @Autowired
    @Qualifier("authWebClient")
    private WebClient authWebClient;

    private static MockWebServer mockWebServer;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("services.auth.url", () -> mockWebServer.url("/auth").toString());
    }

    @Test
    void authWebClientBeanExists() {
        assertThat(authWebClient).isNotNull();
    }

    @Test
    void authWebClientBaseUrlIsUsedCorrectly() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("test response"));

        String response = authWebClient.get()
                .uri("/test-endpoint")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertThat(response).isEqualTo("test response");

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/auth/test-endpoint");
    }

    @Test
    void authWebClientDefaultHeadersAreApplied() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok"));

        authWebClient.get()
                .uri("/check-headers")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader(HttpHeaders.ACCEPT))
                .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE))
                .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    }
}