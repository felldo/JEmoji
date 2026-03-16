package net.fellbaum.jemoji.internal;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

public interface ResourceFilesProvider {

    default Map<String, String> readLanguageFile(final String filePathName) {
        final ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(this.getClass().getResourceAsStream(filePathName), new TypeReference<>() {
        });
    }

}