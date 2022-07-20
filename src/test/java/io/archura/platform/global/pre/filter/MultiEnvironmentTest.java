package io.archura.platform.global.pre.filter;

import io.archura.platform.context.Context;
import io.archura.platform.logging.Logger;
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
    private Map<String, Object> configuration;
    private HashMap<String, Object> attributes;

    @BeforeEach
    void setup() {
        multiEnvironment = new MultiEnvironment();
        configuration = Collections.emptyMap();
        attributes = new HashMap<>();
        attributes.put(Context.class.getSimpleName(), context);
        multiEnvironment.setConfiguration(configuration);
        when(request.attributes()).thenReturn(attributes);
        when(request.headers()).thenReturn(headers);
        when(context.getLogger()).thenReturn(Logger.consoleLogger());
    }

    @Test
    void should_SetEnvironmentToDefault_When_ConfigurationIsEmpty() {
        final String expectedEnvironment = "DEFAULT";
        when(headers.firstHeader("host")).thenReturn("UNKNOWN_HOST_NAME");

        multiEnvironment.accept(request);

        String actualEnvironment = String.valueOf(attributes.get("ARCHURA_REQUEST_ENVIRONMENT"));
        assertEquals(expectedEnvironment, actualEnvironment);
    }

}