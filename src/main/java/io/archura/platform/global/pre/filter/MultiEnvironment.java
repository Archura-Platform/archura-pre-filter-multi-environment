package io.archura.platform.global.pre.filter;

import io.archura.platform.context.Context;
import io.archura.platform.function.Configurable;
import io.archura.platform.logging.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class MultiEnvironment implements Consumer<ServerRequest>, Configurable {

    private Map<String, Object> configuration;

    @Override
    public void accept(ServerRequest request) {
        final Map<String, Object> attributes = request.attributes();
        final Context context = (Context) attributes.get(Context.class.getSimpleName());
        final Logger logger = context.getLogger();
        final ServerRequest.Headers headers = request.headers();
        final String host = headers.firstHeader("host");

        attributes.put("ARCHURA_REQUEST_ENVIRONMENT", "DEFAULT");

        logger.info("request = " + request + " configuration = " + configuration);
    }

    @Override
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

}
