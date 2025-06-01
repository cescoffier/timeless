package me.escoffier.timeless.inboxes.instapaper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.escoffier.timeless.model.NewTaskRequest;

import static me.escoffier.timeless.inboxes.pocket.Item.READING_LIST_PROJECT;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Bookmark(
        String hash,
        String description,
        @JsonProperty("bookmark_id") long bookmarkId,
        @JsonProperty("private_source") String privateSource,
        String title,
        String url,
        @JsonProperty("progress_timestamp") long progressTimestamp,
        long time,
        double progress,
        String starred,
        String type
) {

    public boolean isStarred() {
        return "1".equals(starred);
    }

    public String name() {
        return ! isBlank(title) ? title : url;
    }

    public String link() {
        return "https://www.instapaper.com/read/%s".formatted(bookmarkId);
    }

    public NewTaskRequest asNewTaskRequest() {

        var x = new NewTaskRequest(
                getTaskTitle(),
                link(),
                READING_LIST_PROJECT,
                null
        );
        x.addLabels("timeless/instapaper");
        x.description = """
                Source: %s
                URL: %s
                Description: %s
                """.formatted(link(), url(), !isBlank(description) ? description : "No description");

        return x;
    }

    private boolean isBlank(String description) {
        return description == null || description.isBlank();
    }

    public String getTaskTitle() {
        String t = name();
        return "To read - " + t;
    }

}