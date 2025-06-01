package me.escoffier.timeless.inboxes.instapaper;

import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.model.*;

public class InstapaperApi extends DefaultApi10a {
    private static final InstapaperApi INSTANCE = new InstapaperApi();

    private InstapaperApi() {}

    public static InstapaperApi instance() {
        return INSTANCE;
    }

    @Override
    public String getRequestTokenEndpoint() {
        throw new UnsupportedOperationException("Not supported by Instapaper");
    }

    @Override
    public String getAccessTokenEndpoint() {
        return "https://www.instapaper.com/api/1/oauth/access_token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return "https://www.instapaper.com/api/1/oauth/authorize";
    }

    @Override
    public String getAuthorizationUrl(OAuth1RequestToken requestToken) {
        throw new UnsupportedOperationException("Not supported by Instapaper");
    }
}