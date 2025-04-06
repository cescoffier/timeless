package me.escoffier.timeless.todoist;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.rest.client.reactive.ClientQueryParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import me.escoffier.timeless.helpers.Markdown;
import me.escoffier.timeless.model.Label;
import me.escoffier.timeless.model.Project;
import me.escoffier.timeless.model.Section;
import me.escoffier.timeless.model.Task;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RegisterRestClient(baseUri = "https://todoist.com/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface Todoist {

    @POST
    @Path("/v1/sync")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    @Produces("application/x-www-form-urlencoded")
    SyncResponse sync(SyncRequest request); // TODO

    @POST
    @Path("/v1.0/tasks")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    void addTask(TaskCreationRequest request);

    /**
     * Creates a new project.
     *
     * @param request the request
     * @return the created project
     */
    @POST
    @Path("/v1/projects")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    Project createProject(ProjectCreationRequest request);

    /**
     * Creates a new label
     *
     * @param request the request
     * @return the created label
     */
    @POST
    @Path("/v1/labels")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    Label createLabel(LabelCreationRequest request);

    @POST
    @Path("/v1.0/sections")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    Section createSection(SectionCreationRequest request);


    @POST
        @Path("/v1/tasks/{id}/close")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    void completeTask(@PathParam("id") String id);

    @POST
    @Path("/v1/tasks/{id}/reopen")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    void reopenTask(@PathParam("id") long id);

    @GET
    @Path("/v1/tasks/completed/by_completion_date")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    @ClientQueryParam(name = "until", value = "{today}")
    CompletedTasksResponse getCompletedTasks(@QueryParam("limit") int limit, @QueryParam("since") String since);

    default String lookupAuth() {
        return "Bearer " + token();
    }

    default String today() {
        ZonedDateTime time = Instant.now().atZone(ZoneOffset.UTC).withHour(0).withMinute(0);
        return DateTimeFormatter.ISO_INSTANT.format(time);
    }

    static String token() {
        return ConfigProvider.getConfig().getValue("todoist.token", String.class);
    }

    record TaskCreationRequest(
            String content,
            @JsonProperty("due_string") String due,
            int priority,
            @JsonProperty("project_id") @JsonInclude(JsonInclude.Include.NON_DEFAULT) String project,
            @JsonProperty("labels") @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> labels,
            @JsonProperty("section_id") @JsonInclude(JsonInclude.Include.NON_DEFAULT) String section,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) String description
    ) {

        public static TaskCreationRequest create(String content, String project, String section) {
            return new TaskCreationRequest(content, null, 1, project, Collections.emptyList(), section, null);
        }

        public static TaskCreationRequest create(String content, String project) {
            return new TaskCreationRequest(content, null, 1, project, Collections.emptyList(), null, null);
        }

        public TaskCreationRequest withDue(String due) {
            return new TaskCreationRequest(content(), due, priority(), project(), labels(), section(), description());
        }

        public TaskCreationRequest withPriority(int priority) {
            return new TaskCreationRequest(content(), due(), priority, project(), labels(), section(), description());
        }

        public TaskCreationRequest withContent(String content) {
            return new TaskCreationRequest(content, due(), priority(), project(), labels(), section(), description());
        }

        public TaskCreationRequest withDescription(String desc) {
            return new TaskCreationRequest(content(), due(), priority(), project(), labels(), section(), desc);
        }

        public TaskCreationRequest withProject(String projectId) {
            return new TaskCreationRequest(content(), due(), priority(), projectId, labels(), section(), description());
        }

        public TaskCreationRequest withLabels(List<String> labels) {
            return new TaskCreationRequest(content(), due(), priority(), project(), labels, section(), description());
        }

        public TaskCreationRequest withSection(String section) {
            return new TaskCreationRequest(content(), due(), priority(), project(), labels, section, description());
        }
    }

    record ProjectCreationRequest(String name, String parent_id) {

    }

    record SectionCreationRequest(String name, String project_id) {

        public static SectionCreationRequest create(String name, String project_id) {
            return new SectionCreationRequest(name, project_id);
        }
    }

    record LabelCreationRequest(String name) {
    }

    class CompletedTasksResponse {

        public List<CompletedItem> items;

        public List<Task> toTasks() {
            return items.stream().map(CompletedItem::toTask).collect(Collectors.toList());
        }
    }

    class CompletedItem {
        public String completed_date;
        public String content;
        public String id;
        public String project_id;
        public long task_id;
        public String parent_id;


        public String title() {
            return Markdown.getText(content);
        }

        public TemporalAccessor getCompletionDate() {
            return DateTimeFormatter.ISO_INSTANT.parse(completed_date);
        }

        public Task toTask() {
            Task task = new Task();
            task.content = title();
            task.id = id;
            task.checked = true;
            if (parent_id != null) {
                task.parentTaskId = parent_id;
            }
            if (project_id != null) {
                task.project_id = project_id;
            }
            return task;
        }
    }

}
