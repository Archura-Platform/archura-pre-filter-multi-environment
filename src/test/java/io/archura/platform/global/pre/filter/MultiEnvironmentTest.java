package io.archura.platform.global.pre.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.archura.platform.context.Context;
import io.archura.platform.exception.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiEnvironmentTest {

    private MultiEnvironment multiEnvironment;

    @Mock
    private ServerRequest request;

    @Mock
    private ServerRequest.Headers headers;

    @Mock
    private Context context;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> attributes = new HashMap<>();

    @BeforeEach
    void setup() {
        multiEnvironment = new MultiEnvironment();
        attributes.clear();
    }

    @Test
    void should_SetEnvironmentToDefault_When_NoConfigurationProvided() {
        final String expectedEnvironment = "DEFAULT";
        when(request.attributes()).thenReturn(attributes);
        attributes.put(Context.class.getSimpleName(), context);

        multiEnvironment.accept(request);

        String actualEnvironment = String.valueOf(attributes.get("ARCHURA_REQUEST_ENVIRONMENT"));
        assertEquals(expectedEnvironment, actualEnvironment);
    }

    @Test
    void should_SetEnvironmentToDefault_When_UnreadableConfigurationProvided() throws JsonProcessingException {
        final ObjectMapper objectMapperMock = mock(ObjectMapper.class);
        when(context.getObjectMapper()).thenReturn(objectMapperMock);
        when(objectMapperMock.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        when(request.attributes()).thenReturn(attributes);
        attributes.put(Context.class.getSimpleName(), context);
        multiEnvironment.setConfiguration(Collections.emptyMap());

        assertThrows(ConfigurationException.class, () -> multiEnvironment.accept(request));
    }

    @Test
    void should_SetEnvironmentToDefault_When_EmptyConfigurationProvided() {
        when(request.attributes()).thenReturn(attributes);
        when(context.getObjectMapper()).thenReturn(objectMapper);
        attributes.put(Context.class.getSimpleName(), context);
        final String expectedEnvironment = "DEFAULT";
        multiEnvironment.setConfiguration(Collections.emptyMap());

        multiEnvironment.accept(request);

        String actualEnvironment = String.valueOf(attributes.get("ARCHURA_REQUEST_ENVIRONMENT"));
        assertEquals(expectedEnvironment, actualEnvironment);
    }

    @Test
    void should_SetEnvironmentToDefault_When_HostConfigurationMatches() throws JsonProcessingException {
        final String configurationOnlyHost = """
                {
                    "host": {
                        "groupName": "",
                        "regex": ".*"
                    }
                }
                """;
        when(request.attributes()).thenReturn(attributes);
        when(request.headers()).thenReturn(headers);
        when(context.getObjectMapper()).thenReturn(objectMapper);
        attributes.put(Context.class.getSimpleName(), context);
        final String expectedEnvironment = "test.archura.io";
        when(headers.firstHeader("host")).thenReturn(expectedEnvironment);
        final JsonNode jsonNode = objectMapper.readValue(configurationOnlyHost, JsonNode.class);
        final Map<String, Object> config = objectMapper.convertValue(jsonNode, new TypeReference<>() {
        });
        multiEnvironment.setConfiguration(config);

        multiEnvironment.accept(request);

        String actualEnvironment = String.valueOf(attributes.get("ARCHURA_REQUEST_ENVIRONMENT"));
        assertEquals(expectedEnvironment, actualEnvironment);
    }

    @Test
    void should_SetEnvironmentToDefault_When_HostConfigurationMatchesToGroup() throws JsonProcessingException {
        final String configurationOnlyHostWithGroup = """
                {
                    "host": {
                        "groupName": "environment",
                        "regex": "(?<environment>.*).archura.io"
                    }
                }
                """;
        when(request.attributes()).thenReturn(attributes);
        when(request.headers()).thenReturn(headers);
        when(context.getObjectMapper()).thenReturn(objectMapper);
        attributes.put(Context.class.getSimpleName(), context);
        final String expectedEnvironment = "test";
        final String hostname = "test.archura.io";
        when(headers.firstHeader("host")).thenReturn(hostname);
        final JsonNode jsonNode = objectMapper.readValue(configurationOnlyHostWithGroup, JsonNode.class);
        final Map<String, Object> config = objectMapper.convertValue(jsonNode, new TypeReference<>() {
        });
        multiEnvironment.setConfiguration(config);

        multiEnvironment.accept(request);

        String actualEnvironment = String.valueOf(attributes.get("ARCHURA_REQUEST_ENVIRONMENT"));
        assertEquals(expectedEnvironment, actualEnvironment);
    }

}