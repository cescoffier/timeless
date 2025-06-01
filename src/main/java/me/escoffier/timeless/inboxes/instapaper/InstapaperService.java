package me.escoffier.timeless.inboxes.instapaper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Inbox;
import me.escoffier.timeless.model.NewTaskRequest;
import me.escoffier.timeless.model.Task;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class InstapaperService implements Inbox {

    private static final Logger LOGGER = Logger.getLogger("Instapaper");
    private static final String TOO_MANY_ARTICLES = "Review your reading list";

    private List<Bookmark> bookmarks;

    @Inject
    InstapaperClient client;

    @ConfigProperty(name = "instapaper.limit", defaultValue = "100")
    int limit;

    @ConfigProperty(name = "instapaper.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "instapaper.reading-list-project", defaultValue =  "Reading List") String project;

    public void fetch() {
        LOGGER.info("\uD83D\uDEB6 Retrieving reading list from Instapaper...");
        bookmarks = client.retrieve();
        LOGGER.infof("\uD83D\uDEB6  Read List size: %d", bookmarks.size());
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<Runnable> getPlan(Backend backend) {
        if (bookmarks == null) {
            fetch();
        }

        List<Task> existingReadTasks = backend.getMatchingTasks(this::isFromReadingList);

        // 1 - If fetched contains a bookmark without an associated task -> create new task
        // 2 - If fetched contains a bookmark with an uncompleted associated task -> do nothing
        // 3 - If backend contains "read" tasks (existingReadTasks) without an associated bookmark in fetched -> complete task

        List<Runnable> actions = new ArrayList<>();
        for (Bookmark bk : bookmarks) {
            NewTaskRequest request = bk.asNewTaskRequest();
            Optional<Task> maybe = backend.getTaskMatchingRequest(request);
            if (maybe.isEmpty()) {
                // Case 1
                actions.add(() -> backend.create(request));
            }
        }

        for (Task task : existingReadTasks) {
            Optional<Bookmark> thread = bookmarks.stream()
                    .filter(s -> task.content.startsWith("[" + s.getTaskTitle() + "]"))
                    .findFirst();
            if (thread.isEmpty()) {
                // 4 - complete the task
                actions.add(() -> backend.complete(task));
            }
        }

        if (bookmarks.size() >= limit && backend.getMatchingTasks(t -> t.content.equalsIgnoreCase(TOO_MANY_ARTICLES)).isEmpty()) {
            actions.add(() -> backend.create(new NewTaskRequest(TOO_MANY_ARTICLES, project, "sunday")));
        }

        return actions;
    }

    private boolean isFromReadingList(Task task) {
        return task.content.contains("](https://www.instapaper.com/read/");
    }
}
