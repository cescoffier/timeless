package me.escoffier.timeless.todoist;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.escoffier.timeless.helpers.Hints;
import me.escoffier.timeless.model.Backend;
import me.escoffier.timeless.model.NewTaskRequest;
import me.escoffier.timeless.model.Project;
import me.escoffier.timeless.model.Section;
import me.escoffier.timeless.model.Task;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ApplicationScoped
public class TodoistService implements Backend {

    private static final Logger LOGGER = Logger.getLogger("Todoist");

    private Project inbox;
    private List<Project> projects;
    private List<Task> tasks;

    private Map<Project, List<Section>> sectionsPerProject;


    private List<Project> roots;

    @Inject
    @RestClient
    Todoist todoist;


    @ConfigProperty(defaultValue = "false", name = "todoist.cleanup-enabled")
    boolean cleanupEnabled;

    private List<Task> completed;

    @PostConstruct
    void fetch() {
        roots = new ArrayList<>();
        sectionsPerProject = new HashMap<>();

        SyncResponse response = todoist.sync(SyncRequest.INSTANCE);

        ZonedDateTime time = Instant.now().minus(Duration.ofDays(7))
                .atZone(ZoneOffset.UTC).withHour(0).withMinute(0);
        String since = DateTimeFormatter.ISO_INSTANT.format(time);
        Todoist.CompletedTasksResponse completedTasks = todoist.getCompletedTasks(200, since);

        completed = completedTasks.toTasks();
        inbox = response.projects().stream().filter(p -> p.name().equalsIgnoreCase("Inbox"))
                .findFirst()
                .orElseThrow();

        projects = response.projects();
        response.items().forEach(t -> t.project = getProjectPerId(t.project_id));

        for (Project project : projects) {
            if (project.parent() == null) {
                roots.add(project);
            }
        }

        for (Section section : response.sections()) {
            var p = getProjectPerId(section.project_id());
            sectionsPerProject.computeIfAbsent(p, x -> new ArrayList<>()).add(section);
        }

        this.tasks = response.items();
    }

    public void addTask(String content, String deadline, Project project, String section, int priority, List<String> labels, String description) {
        Todoist.TaskCreationRequest request = Todoist.TaskCreationRequest.create(content, null).withDescription(description);

        if (project != null) {
            request = request.withProject(project.id());
        }
        if (section != null) {
            request = request.withSection(section);
        }
        if (deadline != null) {
            request = request.withDue(deadline);
        }
        if (priority != -1) {
            request = request.withPriority(priority);
        }
        if (!labels.isEmpty()) {
            request = request.withLabels(labels);
        }
        todoist.addTask(request);
    }

    public Project getProjectByName(String name) {
        return projects.stream().filter(p -> p.name().equalsIgnoreCase(name)).findFirst()
                .orElseThrow(() -> new RuntimeException("No project named " + name + " in " + projects.stream().map(Project::name).toList()));
    }

    public Project getProjectPerId(String id) {
        return projects.stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }

    @Override
    public List<Section> getSection(Project project) {
        var l = sectionsPerProject.get(project);
        if (l == null) {
            return Collections.emptyList();
        }
        return l;
    }

    @Override
    public List<Task> getAllTasks() {
        return Collections.unmodifiableList(tasks);
    }

    @Override
    public List<Project> getProjects() {
        return Collections.unmodifiableList(projects);
    }

    @Override
    public List<Project> getProjectRoots() {
        return roots;
    }

    @Override
    public List<Project> getSubProject(Project project) {
        return projects.stream().filter(p -> project.id().equalsIgnoreCase(p.parent())).collect(Collectors.toList());
    }

    @Override
    public List<Task> getMatchingTasks(Predicate<Task> predicate) {
        return tasks.stream().filter(predicate).collect(Collectors.toList());
    }

    @Override
    public List<Task> getAllMatchingTasks(Predicate<Task> predicate) {
        List<Task> active = getMatchingTasks(predicate);
        List<Task> completed = this.completed.stream().filter(predicate).toList();

        List<Task> result = new ArrayList<>();
        result.addAll(active);
        result.addAll(completed);
        return result;
    }

    @Override
    public Optional<Task> getMatchingTask(Predicate<Task> predicate) {
        return tasks.stream().filter(predicate).findAny();
    }

    @Override
    public Optional<Task> getTaskMatchingRequest(NewTaskRequest request) {
        return tasks.stream().filter(t -> t.content.equalsIgnoreCase(request.content)).findFirst();
    }

    @Override
    public void create(NewTaskRequest request) {
        LOGGER.infof("\uD83D\uDD04 Creating new task: %s", request.content + ": " + request.getDescription());
        Project project = inbox;
        String section = null;
        if (request.project != null) {
            project = getProjectByName(request.project);
        }
        if (request.section != null && project != null) {
            var l = getSection(project);
            section = l.stream().filter(s -> s.name().contains(request.section)).map(Section::id).findFirst()
                    .orElseThrow(() -> new NoSuchElementException("No section " + request.section + " in " + request.project));
        }
        addTask(request.content, request.due, project, section, request.priority, request.labels, request.description);
    }

    @Override
    public void complete(Task task) {
        LOGGER.infof("\uD83D\uDD04 Completing task: %s (%s)", task.content, task.id);
        todoist.completeTask(task.id);
    }

    @Override
    public Project getProject(String name) {
        return projects.stream().filter(p -> p.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    @Inject
    Hints hints;

    @Override
    public void cleanupInbox() {
        if (!cleanupEnabled) {
            return;
        }

        // Retrieve all the tasks in the inbox
        List<Task> tasks = getMatchingTasks(t -> t.project != null && t.project.name().equalsIgnoreCase("Inbox"));
        // For each task, check if we have hints
        for (Task task : tasks) {
            // Check if we have hints
            var hint = hints.lookup(task.content);
            if (hint == null || hint.project() == null) {
                continue;
            }
            // Check if the task is already in the project
            Project project = getProjectByName(hint.project());
            if (project.name().equalsIgnoreCase("Inbox")) {
                continue;
            }
            // Move the task to the associated project
            String sectionId = null;
            if (hint.section().isPresent()) {
                // We need to find the section id
                var sections = getSection(project);
                String name = hint.section().get();
                var section = sections.stream().filter(s -> s.name().equalsIgnoreCase(name)).findFirst();
                sectionId = section.map(Section::id).orElse(null);
            }

            Todoist.MoveRequest request = new Todoist.MoveRequest(project.id(), sectionId);
            Log.infof("\uD83D\uDD04 Moving task %s to project/section %s", task.content, request);
            todoist.moveTask(task.id, request);
        }
    }
}
