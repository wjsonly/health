package com.health.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "health.upload")
public class UploadProperties {
    private String directory = "./data/uploads";
    private String publicPrefix = "/uploads/";

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getPublicPrefix() {
        return publicPrefix;
    }

    public void setPublicPrefix(String publicPrefix) {
        this.publicPrefix = publicPrefix;
    }

    public String normalizedPublicPrefix() {
        String normalized = publicPrefix.startsWith("/") ? publicPrefix : "/" + publicPrefix;
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }
}
