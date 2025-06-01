package me.escoffier.timeless.inboxes.instapaper;

import com.github.scribejava.core.model.OAuth1AccessToken;

public class InstapaperOAuthToken {
    public String token;
    public String tokenSecret;

    public InstapaperOAuthToken() {}

    public InstapaperOAuthToken(String token, String tokenSecret) {
        this.token = token;
        this.tokenSecret = tokenSecret;
    }

    public OAuth1AccessToken toAccessToken() {
        return new OAuth1AccessToken(token, tokenSecret);
    }

    public static InstapaperOAuthToken fromAccessToken(OAuth1AccessToken accessToken) {
        return new InstapaperOAuthToken(accessToken.getToken(), accessToken.getTokenSecret());
    }


}