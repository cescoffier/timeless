package me.escoffier.timeless;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.escoffier.timeless.model.Project;
import me.escoffier.timeless.todoist.SyncRequest;
import me.escoffier.timeless.todoist.Todoist;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
@CommandLine.Command(name = "report", description = "List completed tasks over the past 8 days")
@Unremovable
public class ReportCompletedCommand implements Runnable {

    @Inject
    @RestClient
    Todoist todoist;

    @Inject
    @ConfigProperty(name = "report.excluded_projects")
    Optional<List<String>> excludedProjects;

    @Inject
    @ConfigProperty(name = "report.excluded_tasks")
    Optional<List<String>> excludedTasks;

    @Override
    public void run() {
        ZonedDateTime time = Instant.now().minus(Duration.ofDays(7))
                .atZone(ZoneOffset.UTC).withHour(0).withMinute(0);
        String since = DateTimeFormatter.ISO_INSTANT.format(time);
        Todoist.CompletedTasksResponse resp = todoist.getCompletedTasks(200, since);
        List<Todoist.CompletedItem> items = resp.items;

        List<Project> projects = todoist.sync(SyncRequest.INSTANCE).projects();

        ListMultimap<String, String> report = MultimapBuilder.treeKeys((Comparator<String>) (o1, o2) -> {
            if (o1.equalsIgnoreCase("inbox") && o2.equalsIgnoreCase("inbox")) {
                return 0;
            }
            if (o1.equalsIgnoreCase("inbox")) {
                return -1;
            }
            if (o2.equalsIgnoreCase("inbox")) {
                return 1;
            }
            return o1.compareTo(o2);
        }).arrayListValues().build();

        for (Todoist.CompletedItem completed : items) {
            Project project = null;
            if (completed.project_id != null) {
                project = getProjectPerId(projects, completed.project_id);
            }

            if (isExcluded(completed)) {
                continue;
            }

            if (project == null) {
                report.put("inbox", completed.title());
            } else if (!excludedProjects.orElse(List.of()).contains(project.name())) {
                String pn = getProjectName(projects, project);
                report.put(pn, completed.title());
            }
        }

        Set<String> added = new HashSet<>();
        report.asMap().forEach((key, value) -> {
            System.out.println("== " + key);
            value.forEach(s -> {
                String extracted = extract(s);
                if (added.add(extracted)) {
                    System.out.println("    * " + extracted);
                }
            });
            System.out.println();
        });
    }

    private boolean isExcluded(Todoist.CompletedItem completed) {
        for (String prefix : excludedTasks.orElse(List.of())) {
            if (completed.title().toLowerCase().startsWith(prefix.toLowerCase())) {
                return true;
            }
        }
        return false;
    }


    private String extract(String s) {
        if (s.startsWith("[") && s.lastIndexOf("]") != -1) {
            return s.substring(1, s.lastIndexOf(("]")));
        }
        return s;
    }

    private String getProjectName(List<Project> projects, Project project) {
        if (project.parent() == null) {
            return project.name();
        }
        Project parent = projects.stream().filter(p -> p.id().equalsIgnoreCase(project.parent())).findAny()
                .orElse(null);
        if (parent == null) {
            return project.name();
        }
        return parent.name() + "/" + project.name();
    }

    private Project getProjectPerId(List<Project> projects, String projectId) {
        return projects.stream().filter(p -> projectId.equalsIgnoreCase(p.id())).findFirst().orElse(null);
    }

}
