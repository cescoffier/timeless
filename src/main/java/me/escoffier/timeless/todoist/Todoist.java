package me.escoffier.timeless.todoist;

import com.fasterxml.jackson.annotation.*;
import me.escoffier.timeless.model.Label;
import me.escoffier.timeless.model.Project;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RegisterRestClient(baseUri = "https://api.todoist.com")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface Todoist {

    @POST
    @Path("/sync/v9/sync")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    SyncResponse sync(SyncRequest request);

    @POST
    @Path("/rest/v2/tasks")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    void addTask(TaskCreationRequest request);

    @POST
    @Path("/rest/v2/projects")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    Project createProject(ProjectCreationRequest request);

    @POST
    @Path("/rest/v2/labels")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    Label createLabel(LabelCreationRequest request);

    @POST
    @Path("/rest/v2/sections")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    Section createSection(SectionCreationRequest request);

    @POST
    @Path("/rest/v2/tasks/{id}/close")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    void completeTask(@PathParam("id") String id);

    @POST
    @Path("/rest/v2/tasks/{id}/reopen")
    @ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
    void uncompleteTask(@PathParam("id") long id);

    default String lookupAuth() {
        return "Bearer " + token();
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
    }

    record ProjectCreationRequest(String name, String parent_id) {

    }

    record SectionCreationRequest(String name, String project_id) {

        public static SectionCreationRequest create(String name, String project_id) {
            return new SectionCreationRequest(name, project_id);
        }
    }

    record Section(String id, String project_id, String name) {

    }

    record LabelCreationRequest(String name) {
        //todo: order, color, favorite
    }

}
