package me.escoffier.timeless.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NewTaskRequest {

    public final String content;
    public final String project;
    public final String due;
    public final List<String> labels = new ArrayList<>();
    public String section;
    public int priority = -1;
    public final String description;

    public NewTaskRequest(String content, String project, String due) {
        this.content = content;
        this.project = project;
        this.due = due;
        this.description = "";
        this.section = null;
    }

    public NewTaskRequest(String content, String link, String project, String due) {
        this.content = String.format("[%s](%s)", content, link);
        this.project = project;
        this.due = due;
        this.description = "Source: " + link;
        this.section = null;
    }

    public NewTaskRequest(String content, String link, String project, String section, String due) {
        this.content = String.format("[%s](%s)", content, link);
        this.project = project;
        if (section != null  && section.length() > 0) {
            this.section = section;
        } else {
            this.section = null;
        }
        this.due = due;
        this.description = "Source: " + link;
    }

    public void addLabels(String... labels) {
        this.labels.addAll(Arrays.asList(labels));
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getIdentifier() {
        return content;
    }
}
