package me.escoffier.timeless.inboxes.instapaper;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Singleton
public class InstapaperOAuthTokenStore {

    private static final Path FILE = Paths.get("token-instapaper/instapaper_token.json");

    @Inject
    ObjectMapper mapper;

    public void save(InstapaperOAuthToken token) throws Exception {
        FILE.getParent().toFile().mkdirs();
        mapper.writeValue(FILE.toFile(), token);
    }

    public InstapaperOAuthToken load() throws Exception {
        if (!Files.exists(FILE)) {
            return null;
        }
        return mapper.readValue(FILE.toFile(), InstapaperOAuthToken.class);
    }
}