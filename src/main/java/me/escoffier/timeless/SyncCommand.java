package me.escoffier.timeless;

import io.quarkus.logging.Log;
import me.escoffier.timeless.helpers.GoogleDriveSync;
import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.Inbox;
import me.escoffier.timeless.model.Project;
import me.escoffier.timeless.model.Task;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ApplicationScoped
@CommandLine.Command(name = "sync", description = "Sync Todoist tasks with Gmail, Github, Pocket...")
public class SyncCommand implements Runnable {

    private static final Logger LOGGER = Logger.getLogger("Timeless");

    @Inject
    Backend backend;

    @ConfigProperty(name = "todoist.exclude-areas-under")
    List<String> exclusions;

    @Inject
    Instance<Inbox> inboxes;

    @Inject
    GoogleDriveSync gds;

    @Override
    public void run() {
        List<Runnable> plan = new ArrayList<>();
        AtomicInteger boxCount = new AtomicInteger();

        LOGGER.infof("Found %d inboxes", inboxes.stream().count());

        inboxes.stream().forEach(i -> { boxCount.getAndIncrement(); plan.addAll(i.getPlan(backend)); });

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


//        // Report
//        LOGGER.info("Sanitization Report:");
//        for (Project root : backend.getProjectRoots()) {
//            List<Project> actualProjects = new ArrayList<>();
//            traverse(backend, root, actualProjects);
//
//            actualProjects.forEach(project -> {
//                List<Task> list = backend.getMatchingTasks(t -> t.project == project);
//                if (!backend.isArea(project)) {
//                    if (list.isEmpty()) {
//                        LOGGER.warnf("The project %s has no more tasks, should it be archived?", project.name());
//                    } else {
//                        List<Task> next = list.stream().filter(t -> t.hasLabel("next")).toList();
//                        if (next.isEmpty()) {
//                            LOGGER.warnf("The project %s has no `next` task", project.name());
//                        } else if (next.size() > 1) {
//                            LOGGER.warnf("The project %s has multiple `next` tasks", project.name());
//                        }
//                    }
//                } else {
//                    for (Task task : list) {
//                        if (task.section_id == null) {
//                            LOGGER.warnf("The area of responsibility %s has tasks not organized in a section: %s", project.name(), task.content);
//                        }
//                    }
//                }
//            });
//        }
//        LOGGER.info("----------------------------------");

        // Rocket sync.
//        try {
//            gds.sync();
//        } catch (Exception e) {
//            Log.errorf("Unable to sync with Google Drive", e);
//        }

    }

    private void traverse(Backend backend, Project root, List<Project> collector) {
        List<Project> projects = backend.getSubProject(root);
        boolean area = backend.isArea(root);
        boolean isRoot = root.parent() == null;

        if (exclusions.contains(root.name())) {
            return;
        }

        if (isRoot) { // Skip the roots
            for (Project project : projects) {
                if (!exclusions.contains(project.name())) {
                    traverse(backend, project, collector);
                }
            }
            return;
        }

        if (projects.isEmpty() && area) {
            LOGGER.warnf("The area of responsibility %s has no active project", root.name());
            return;
        }
        if (!area && !projects.isEmpty()) {
            LOGGER.warnf("The project %s (parent: %s) is not an area of responsibility but has sub-projects: %s",
                    root.name(), root.parent(), projects.stream().map(Project::name).collect(Collectors.joining(", ")));
        }

        if (area) {
            collector.add(root);
            for (Project project : projects) {
                if (!exclusions.contains(project.name())) {
                    traverse(backend, project, collector);
                }
            }
        }

    }

}
