package me.escoffier.timeless.inboxes.instapaper;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class InstapaperClient {

    private final OAuth10aService service;

    InstapaperOAuthTokenStore store;
    private final OAuth1AccessToken accessToken;

    public InstapaperClient(InstapaperOAuthTokenStore store,
                            @ConfigProperty(name = "instapaper.consumer-id") String consumerId,
                            @ConfigProperty(name = "instapaper.consumer-secret") String consumerSecret,
                            @ConfigProperty(name = "instapaper.username") String username,
                            @ConfigProperty(name = "instapaper.password") String password
    ) throws Exception {
        this.store = store;
        this.service = new ServiceBuilder(consumerId)
                .apiSecret(consumerSecret)
                .build(InstapaperApi.instance());

        System.out.println("Instapaper - Initializing OAuth...");
        InstapaperOAuthToken stored = store.load();
        if (stored != null) {
            accessToken = stored.toAccessToken();
        } else {
            accessToken = authenticate(username, password);
            store.save(InstapaperOAuthToken.fromAccessToken(accessToken));
        }
    }

    private OAuth1AccessToken authenticate(String username, String password) throws Exception {
        OAuthRequest request = new OAuthRequest(Verb.POST, InstapaperApi.instance().getAccessTokenEndpoint());
        request.addParameter("x_auth_mode", "client_auth");
        request.addParameter("x_auth_username", username);
        request.addParameter("x_auth_password", password);

        service.signRequest(new OAuth1AccessToken("", ""), request);
        Response response = service.execute(request);

        if (response.getCode() != 200) {
            throw new RuntimeException("Authentication failed: " + response.getBody());
        }

        Map<String, String> map = Arrays.stream(response.getBody().split("&"))
                .map(pair -> pair.split("="))
                .collect(Collectors.toMap(p -> p[0], p -> p[1]));

        return new OAuth1AccessToken(map.get("oauth_token"), map.get("oauth_token_secret"));
    }

    public List<Bookmark> retrieve() {
        OAuthRequest request = new OAuthRequest(Verb.POST, "https://www.instapaper.com/api/1/bookmarks/list");
        request.addParameter("limit", "500");

        service.signRequest(accessToken, request);
        Response response;
        try {
            response = service.execute(request);
            if (response.getCode() == 200) {
                return parse(response.getBody());
            } else {
                throw new RuntimeException("Instapaper API returned an error: " + response.getCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve bookmarks from Instapaper", e);
        }
    }

    private List<Bookmark> parse(String body) {
        JsonArray json = new JsonArray(body);
        // We have multiple types of objects, the "type" field indicates the type
        List<Bookmark> bookmarks = new ArrayList<>();
        for (int i = 0; i < json.size(); i++) {
            JsonObject obj = json.getJsonObject(i);
            if (obj.getString("type").equals("bookmark")) {
                bookmarks.add(obj.mapTo(Bookmark.class));
            }
        }
        return bookmarks;
    }

}
