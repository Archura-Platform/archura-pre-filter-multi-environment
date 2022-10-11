package io.archura.platform.imperativeshell.global.pre.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.archura.platform.api.context.Context;
import io.archura.platform.api.http.HttpServerRequest;
import io.archura.platform.api.logger.Logger;
import io.archura.platform.api.type.Configurable;
import io.archura.platform.imperativeshell.global.pre.filter.exception.ConfigurationException;

import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.nonNull;

public class MultiEnvironment implements UnaryOperator<HttpServerRequest>, Configurable {

    public static final String ATTRIBUTE_ENVIRONMENT = "ARCHURA_REQUEST_ENVIRONMENT";
    public static final String DEFAULT_ENVIRONMENT = "default";
    public static final String HOST_HEADER_NAME = "host";
    private Map<String, Object> configuration;
    private Logger logger;

    @Override
    public HttpServerRequest apply(HttpServerRequest request) {
        final Map<String, Object> attributes = request.getAttributes();
        final Context context = (Context) attributes.get(Context.class.getSimpleName());
        logger = context.getLogger();
        logger.debug("configuration: %s", configuration);
        attributes.put(ATTRIBUTE_ENVIRONMENT, DEFAULT_ENVIRONMENT);
        if (nonNull(configuration)) {
            final ObjectMapper objectMapper = context.getObjectMapper();
            final MultiEnvironmentConfiguration config = getConfig(objectMapper);
            logger.debug("MultiEnvironmentConfiguration config: %s", config);
            if (isHostConfigValid(config.getHost())) {
                logger.debug("Host configuration is valid.");
                final MultiEnvironmentConfiguration.Host hostConfig = config.getHost();
                final String input = request.getFirstHeader(HOST_HEADER_NAME);
                final String regex = hostConfig.getRegex();
                final String groupName = hostConfig.getGroupName();
                handleEnvironment(attributes, regex, groupName, input);
            }
            if (isHeaderConfigValid(config.getHeader())) {
                logger.debug("Header configuration is valid.");
                final MultiEnvironmentConfiguration.Header headerConfig = config.getHeader();
                final String input = request.getFirstHeader(headerConfig.getName());
                final String regex = headerConfig.getRegex();
                final String groupName = headerConfig.getGroupName();
                handleEnvironment(attributes, regex, groupName, input);
            }
            if (isPathConfigValid(config.getPath())) {
                logger.debug("Path configuration is valid.");
                final MultiEnvironmentConfiguration.Path pathConfig = config.getPath();
                final String input = request.getRequestURI().getPath();
                final String regex = pathConfig.getRegex();
                final String groupName = pathConfig.getGroupName();
                handleEnvironment(attributes, regex, groupName, input);
            }
        }
        logger.debug("Filter set the environment value to: %s", attributes.get(ATTRIBUTE_ENVIRONMENT));
        return request;
    }

    private MultiEnvironmentConfiguration getConfig(ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(configuration), MultiEnvironmentConfiguration.class);
        } catch (JsonProcessingException e) {
            throw new ConfigurationException(e);
        }
    }

    private boolean isPathConfigValid(MultiEnvironmentConfiguration.Path path) {
        return nonNull(path) && nonNull(path.getRegex()) && nonNull(path.getGroupName()) && notEmpty(path.getRegex());
    }

    private boolean isHeaderConfigValid(MultiEnvironmentConfiguration.Header header) {
        return nonNull(header) && nonNull(header.getName()) && nonNull(header.getRegex()) && nonNull(header.getGroupName())
                && notEmpty(header.getName()) && notEmpty(header.getRegex());
    }

    private boolean isHostConfigValid(MultiEnvironmentConfiguration.Host host) {
        return nonNull(host) && nonNull(host.getGroupName()) && nonNull(host.getRegex()) && notEmpty(host.getRegex());
    }

    private void handleEnvironment(Map<String, Object> attributes, String regex, String groupName, String input) {
        if (nonNull(input)) {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(input);
            boolean matchFound = matcher.find();
            if (matchFound) {
                if (notEmpty(groupName)) {
                    final String environmentName = matcher.group(groupName);
                    attributes.put(ATTRIBUTE_ENVIRONMENT, environmentName);
                } else {
                    final String environmentName = matcher.group(0);
                    attributes.put(ATTRIBUTE_ENVIRONMENT, environmentName);
                }
                logger.debug("Set environment value to: %s", attributes.get(ATTRIBUTE_ENVIRONMENT));
            }
        }
    }

    private boolean notEmpty(String value) {
        return !value.trim().isEmpty();
    }

    @Override
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
}
