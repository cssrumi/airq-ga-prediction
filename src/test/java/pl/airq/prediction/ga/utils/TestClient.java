package pl.airq.prediction.ga.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestClient.class);
    private final String targetUtl;
    private ObjectMapper mapper;
    private String clientName = "TestClient";

    private TestClient(String targetUrl) {
        this.targetUtl = targetUrl;
        LOGGER.info("[{}] -> {} created", clientName, targetUrl);
    }

    public TestClient setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
        return this;
    }

    public TestClient setClientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    public WebTarget target() {
        final Client client = ClientBuilder.newClient();
        return client.target(targetUtl);
    }

    public <T> T get(Class<T> clazz) {
        final WebTarget target = target();
        String result = (String) target.request().get().getEntity();
        if (result != null && mapper != null) {
            try {
                return mapper.readValue(result, clazz);
            } catch (JsonProcessingException e) {
                LOGGER.warn("Unable to parse {} into {}", result, clazz.getSimpleName());
                return null;
            }
        }
        LOGGER.info("Empty result or mapper. Default mapper used.");
        return target.request().get(clazz);
    }

    public static TestClient fromUrl(String url) {
        return new TestClient(url);
    }

    public static TestClient fromUri(String uri, String... objects) {
        String finalUri = uri;
        for (String object : objects) {
            finalUri = uri.replaceFirst("\\{(.*?)}", object);
        }
        return new TestClient("http://localhost:" + RestAssured.port + finalUri);
    }
}
