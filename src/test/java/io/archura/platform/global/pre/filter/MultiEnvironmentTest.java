package io.archura.platform.global.pre.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.archura.platform.api.context.Context;
import io.archura.platform.api.http.HttpServerRequest;
import io.archura.platform.api.logger.Logger;
import io.archura.platform.imperativeshell.global.pre.filter.MultiEnvironment;
import io.archura.platform.imperativeshell.global.pre.filter.exception.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.archura.platform.imperativeshell.global.pre.filter.MultiEnvironment.ATTRIBUTE_ENVIRONMENT;
import static io.archura.platform.imperativeshell.global.pre.filter.MultiEnvironment.DEFAULT_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiEnvironmentTest {

    private MultiEnvironment multiEnvironment;

    @Mock
    private HttpServerRequest request;


    @Mock
    private Context context;

    @Mock
    private Logger logger;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> attributes = new HashMap<>();
    private Map<String, List<String>> headers = new HashMap<>();

    @BeforeEach
    void setup() {
        multiEnvironment = new MultiEnvironment();
        attributes.clear();
        when(context.getLogger()).thenReturn(logger);
        doNothing().when(logger).debug(any(), any());
    }

    @Test
    void should_SetEnvironmentToDefault_When_NoConfigurationProvided() {
        final String expectedEnvironment = DEFAULT_ENVIRONMENT;
        when(request.getAttributes()).thenReturn(attributes);
        attributes.put(Context.class.getSimpleName(), context);

        multiEnvironment.apply(request);

        String actualEnvironment = String.valueOf(attributes.get(ATTRIBUTE_ENVIRONMENT));
        assertEquals(expectedEnvironment, actualEnvironment);
    }

    @Test
    void should_SetEnvironmentToDefault_When_UnreadableConfigurationProvided() throws JsonProcessingException {
        final ObjectMapper objectMapperMock = mock(ObjectMapper.class);
        when(context.getObjectMapper()).thenReturn(objectMapperMock);
        when(objectMapperMock.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        when(request.getAttributes()).thenReturn(attributes);
        attributes.put(Context.class.getSimpleName(), context);
        multiEnvironment.setConfiguration(Collections.emptyMap());

        assertThrows(ConfigurationException.class, () -> multiEnvironment.apply(request));
    }

    @Test
    void should_SetEnvironmentToDefault_When_EmptyConfigurationProvided() {
        when(request.getAttributes()).thenReturn(attributes);
        when(context.getObjectMapper()).thenReturn(objectMapper);
        attributes.put(Context.class.getSimpleName(), context);
        final String expectedEnvironment = DEFAULT_ENVIRONMENT;
        multiEnvironment.setConfiguration(Collections.emptyMap());

        multiEnvironment.apply(request);

        String actualEnvironment = String.valueOf(attributes.get(ATTRIBUTE_ENVIRONMENT));
        assertEquals(expectedEnvironment, actualEnvironment);
    }

    @Test
    void should_SetEnvironment_When_HostConfigurationMatches() throws JsonProcessingException {
        final String configurationOnlyHost = """
                {
                    "host": {
                        "groupName": "",
                        "regex": ".*"
                    }
                }
                """;
        when(request.getAttributes()).thenReturn(attributes);
        when(context.getObjectMapper()).thenReturn(objectMapper);
        attributes.put(Context.class.getSimpleName(), context);
        final String expectedEnvironment = "test.archura.io";
        when(request.getFirstHeader("host")).thenReturn(expectedEnvironment);
        final JsonNode jsonNode = objectMapper.readValue(configurationOnlyHost, JsonNode.class);
        final Map<String, Object> config = objectMapper.convertValue(jsonNode, new TypeReference<>() {
        });
        multiEnvironment.setConfiguration(config);

        multiEnvironment.apply(request);

        String actualEnvironment = String.valueOf(attributes.get(ATTRIBUTE_ENVIRONMENT));
        assertEquals(expectedEnvironment, actualEnvironment);
    }

    @Test
    void should_SetEnvironment_When_HostConfigurationMatchesToGroup() throws JsonProcessingException {
        final String configurationOnlyHostWithGroup = """
                {
                    "host": {
                        "groupName": "environment",
                        "regex": "(?<environment>.*).archura.io"
                    }
                }
                """;
        when(request.getAttributes()).thenReturn(attributes);
        when(context.getObjectMapper()).thenReturn(objectMapper);
        attributes.put(Context.class.getSimpleName(), context);
        final String expectedEnvironment = "test";
        final String hostname = "test.archura.io";
        when(request.getFirstHeader("host")).thenReturn(hostname);
        final JsonNode jsonNode = objectMapper.readValue(configurationOnlyHostWithGroup, JsonNode.class);
        final Map<String, Object> config = objectMapper.convertValue(jsonNode, new TypeReference<>() {
        });
        multiEnvironment.setConfiguration(config);

        multiEnvironment.apply(request);

        String actualEnvironment = String.valueOf(attributes.get(ATTRIBUTE_ENVIRONMENT));
        assertEquals(expectedEnvironment, actualEnvironment);
    }

    @Test
    void should_SetEnvironment_When_HeaderConfigurationMatches() throws JsonProcessingException {
        final String configurationOnlyHost = """
                {
                    "header": {
                        "name": "X-Archura-Environment",
                        "groupName": "",
                        "regex": ".*"
                    }
                }
                """;
        when(request.getAttributes()).thenReturn(attributes);
        when(context.getObjectMapper()).thenReturn(objectMapper);
        attributes.put(Context.class.getSimpleName(), context);
        final String expectedEnvironment = "test-archura-io";
        when(request.getFirstHeader("X-Archura-Environment")).thenReturn(expectedEnvironment);
        final JsonNode jsonNode = objectMapper.readValue(configurationOnlyHost, JsonNode.class);
        final Map<String, Object> config = objectMapper.convertValue(jsonNode, new TypeReference<>() {
        });
        multiEnvironment.setConfiguration(config);

        multiEnvironment.apply(request);

        String actualEnvironment = String.valueOf(attributes.get(ATTRIBUTE_ENVIRONMENT));
        assertEquals(expectedEnvironment, actualEnvironment);
    }

    @Test
    void should_SetEnvironment_When_HeaderConfigurationMatchesToGroup() throws JsonProcessingException {
        final String configurationOnlyHost = """
                {
                    "header": {
                        "name": "X-Archura-Environment",
                        "groupName": "environment",
                        "regex": "(?<environment>.*)-archura-io"
                    }
                }
                """;
        when(request.getAttributes()).thenReturn(attributes);
        when(context.getObjectMapper()).thenReturn(objectMapper);
        attributes.put(Context.class.getSimpleName(), context);
        final String expectedEnvironment = "test";
        when(request.getFirstHeader("X-Archura-Environment")).thenReturn("test-archura-io");
        final JsonNode jsonNode = objectMapper.readValue(configurationOnlyHost, JsonNode.class);
        final Map<String, Object> config = objectMapper.convertValue(jsonNode, new TypeReference<>() {
        });
        multiEnvironment.setConfiguration(config);

        multiEnvironment.apply(request);

        String actualEnvironment = String.valueOf(attributes.get(ATTRIBUTE_ENVIRONMENT));
        assertEquals(expectedEnvironment, actualEnvironment);
    }

    @Test
    void should_SetEnvironment_When_PathConfigurationMatches() throws JsonProcessingException {
        final String configurationOnlyHost = """
                {
                    "path": {
                        "groupName": "",
                        "regex": "(?:[^\\/]|\\/\\/)+"
                    }
                }
                """;
        when(request.getAttributes()).thenReturn(attributes);
        when(request.getRequestURI()).thenReturn(URI.create("/test/some/other/987/index.html?a=1&b=2"));
        when(context.getObjectMapper()).thenReturn(objectMapper);
        attributes.put(Context.class.getSimpleName(), context);
        final String expectedEnvironment = "test";
        final JsonNode jsonNode = objectMapper.readValue(configurationOnlyHost, JsonNode.class);
        final Map<String, Object> config = objectMapper.convertValue(jsonNode, new TypeReference<>() {
        });
        multiEnvironment.setConfiguration(config);

        multiEnvironment.apply(request);

        String actualEnvironment = String.valueOf(attributes.get(ATTRIBUTE_ENVIRONMENT));
        assertEquals(expectedEnvironment, actualEnvironment);
    }

}