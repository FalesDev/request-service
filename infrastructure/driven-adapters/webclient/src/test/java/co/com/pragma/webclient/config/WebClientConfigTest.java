package co.com.pragma.webclient.config;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = WebClientConfig.class)
@TestPropertySource(properties = {
        "services.auth.url=http://localhost:8081/auth"
})
public class WebClientConfigTest {

    @Autowired
    private WebClient authWebClient;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(8081);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testAuthWebClientBeanExists() {
        assertNotNull(authWebClient);
    }

    @Test
    void testAuthWebClientBaseUrl() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("test response"));

        String response = authWebClient.get()
                .uri("/test-endpoint")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/auth/test-endpoint", recordedRequest.getPath());
    }

    @Test
    void testAuthWebClientDefaultHeaders() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("test response"));

        authWebClient.get()
                .uri("/test")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader(HttpHeaders.ACCEPT));
        assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE));
    }
}