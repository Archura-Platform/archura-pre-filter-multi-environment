package io.archura.platform.global.pre.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.archura.platform.context.Context;
import io.archura.platform.exception.ConfigurationException;
import io.archura.platform.function.Configurable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.nonNull;

@Component
public class MultiEnvironment implements Consumer<ServerRequest>, Configurable {

    public static final String ATTRIBUTE_ENVIRONMENT = "ARCHURA_REQUEST_ENVIRONMENT";
    public static final String DEFAULT_ENVIRONMENT = "DEFAULT";
    public static final String HOST_HEADER_NAME = "host";
    private Map<String, Object> configuration;

    @Override
    public void accept(ServerRequest request) {
        final Map<String, Object> attributes = request.attributes();
        attributes.put(ATTRIBUTE_ENVIRONMENT, DEFAULT_ENVIRONMENT);
        if (nonNull(configuration)) {
            final Context context = (Context) attributes.get(Context.class.getSimpleName());
            final ObjectMapper objectMapper = context.getObjectMapper();
            final MultiEnvironmentConfiguration config = getConfig(objectMapper);
            if (isHostConfigValid(config.getHost())) {
                handleHostNameBasedEnvironment(request, attributes, config);
            }
        }

    }

    private void handleHostNameBasedEnvironment(ServerRequest request, Map<String, Object> attributes, MultiEnvironmentConfiguration config) {
        final String host = request.headers().firstHeader(HOST_HEADER_NAME);
        if (nonNull(host)) {
            final String regex = config.getHost().getRegex();
            final String groupName = config.getHost().getGroupName();
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(host);
            boolean matchFound = matcher.find();
            if (matchFound) {
                if (notEmpty(groupName)) {
                    final String environmentName = matcher.group(groupName);
                    attributes.put(ATTRIBUTE_ENVIRONMENT, environmentName);
                } else {
                    final String environmentName = matcher.group(0);
                    attributes.put(ATTRIBUTE_ENVIRONMENT, environmentName);
                }
            }
        }
    }

    private MultiEnvironmentConfiguration getConfig(ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(configuration), MultiEnvironmentConfiguration.class);
        } catch (JsonProcessingException e) {
            throw new ConfigurationException(e);
        }
    }

    private boolean isHostConfigValid(MultiEnvironmentConfiguration.Host host) {
        return nonNull(host) && nonNull(host.getGroupName()) && nonNull(host.getRegex()) && notEmpty(host.getRegex());
    }

    private boolean notEmpty(String value) {
        return !value.trim().isEmpty();
    }

    @Override
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
}
