package me.escoffier.timeless.inboxes.google;

import io.smallrye.config.WithDefault;

public interface GAccount {

    @WithDefault("0")
    int inboxid();

}
