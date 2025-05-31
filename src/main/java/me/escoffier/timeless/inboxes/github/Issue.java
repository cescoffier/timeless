package me.escoffier.timeless.inboxes.github;

import me.escoffier.timeless.helpers.HintManager;
import me.escoffier.timeless.helpers.Hints;
import me.escoffier.timeless.model.NewTaskRequest;

import static me.escoffier.timeless.helpers.DueDates.todayOrTomorrow;

public class Issue {

    public String html_url;
    public String title;
    public Repository repository;
    public int number;
    public String state;

    public String url() {
        return html_url;
    }

    public String title() {
        return title;
    }

    public String project() {
        return repository.full_name;
    }

    public boolean isOpen() {
        return "open".equalsIgnoreCase(state);
    }

    public NewTaskRequest asNewTaskRequest(HintManager hints) {
        String content = getTaskName();
        Hints.Hint hint = hints.lookup(html_url);
        var request = new NewTaskRequest(
                content,
                html_url,
                hint.project(),
                hint.section().orElse(null),
                todayOrTomorrow()
        );
        request.addLabels("Devel", "timeless/github");
        return request;
    }

    public String getTaskName() {
        return "Fix " + title + " " + project() + "#" + number;
    }

    public static class Repository {
        public String full_name;
    }
}
