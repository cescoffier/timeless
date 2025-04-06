package me.escoffier.timeless;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Inbox;
import me.escoffier.timeless.model.Task;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
@CommandLine.Command(name = "sync", description = "Sync Todoist tasks with Gmail, Github, Pocket...")
@Unremovable
public class SyncCommand implements Runnable {

    private static final Logger LOGGER = Logger.getLogger("Timeless");

    @Inject
    Backend backend;

    @Inject
    Instance<Inbox> inboxes;

    @Override
    public void run() {
        List<Runnable> plan = new ArrayList<>();
        AtomicInteger boxCount = new AtomicInteger();

        LOGGER.infof("Found %d inboxes", inboxes.stream().count());

        CountDownLatch latch = new CountDownLatch((int) inboxes.stream().count());

        inboxes.stream().forEach(i -> {
            Thread.ofVirtual().start(() -> {
                try {
                    boxCount.getAndIncrement();
                    plan.addAll(i.getPlan(backend));
                } finally {
                    latch.countDown();
                }
            });
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            LOGGER.error("\uD83D\uDC7F Unable to wait for inboxes", e);
        }

        LOGGER.infof("\uD83D\uDEB6? Executing plan containing %d actions from %d inboxes", plan.size(), boxCount.get());
        plan.forEach(r -> {
            try {
                r.run();
            } catch (RuntimeException e) {
                LOGGER.error("\uD83D\uDC7F Unable to execute an action", e);
            }
        });

        // Find duplicates
        List<Task> tasks = backend.getAllTasks();
        for (Task t : tasks) {
            String content = t.content;
            Optional<Task> optional = tasks.stream()
                    .filter(other -> other != t && other.content.equalsIgnoreCase(content))
                    .findFirst();
            optional.ifPresent(task -> LOGGER.warnf("\uD83E\uDD14 Duplicate tasks found: %s (%s)", task.content,
                    task.project == null ? "inbox" : task.project.name()));
        }

    }

}
