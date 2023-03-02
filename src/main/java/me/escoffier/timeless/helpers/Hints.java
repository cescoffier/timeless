package me.escoffier.timeless.helpers;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

import javax.ws.rs.DefaultValue;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "hints")
public interface Hints {

    @WithParentName
    List<Hint> hints();

    default Hints.Hint lookup(String content) {
        for (Hints.Hint hint : hints()) {
            if (hint.match(content)) {
                return hint;
            }
        }
        return NO_HINT;
    }


    interface Hint {
        String keywords();

        String project();

        Optional<String> section();

        default boolean match(String content) {
            var kw = Arrays.stream(keywords().split(",")).map(String::trim).toList();
            return kw.stream().anyMatch(s -> content.toLowerCase().contains(s.toLowerCase()));
        }

    }

    public static final Hints.Hint NO_HINT = new Hints.Hint() {
        @Override
        public String keywords() {
            return null;
        }

        @Override
        public String project() {
            return null;
        }

        @Override
        public Optional<String> section() {
            return Optional.empty();
        }
    };
}
