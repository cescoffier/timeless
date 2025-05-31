package me.escoffier.timeless.helpers;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import static me.escoffier.timeless.helpers.Hints.NO_HINT;

@Singleton
public class HintManager {

    @Inject
    Instance<Hints> hints;

    public Hints.Hint lookup(String content) {
        if (hints.isUnsatisfied()) {
            return NO_HINT;
        }

        for (Hints.Hint hint : hints.get().hints()) {
            if (hint.match(content)) {
                return hint;
            }
        }
        return NO_HINT;
    }
}
