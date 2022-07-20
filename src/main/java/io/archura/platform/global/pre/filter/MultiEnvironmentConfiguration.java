package io.archura.platform.global.pre.filter;

import lombok.Data;

@Data
public class MultiEnvironmentConfiguration {
    private Host host;
    private Header header;
    private Path path;

    @Data
    public static class Host {
        private String groupName;
        private String regex;
    }

    @Data
    public static class Header {
        private String name;
        private String groupName;
        private String regex;
    }

    @Data
    public static class Path {
        private String groupName;
        private String regex;
    }
}
