package me.escoffier.timeless.inboxes.google;

import java.util.Map;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "google")
public interface GAccounts {
@WithParentName 
    Map<String, GAccount> accounts();
}