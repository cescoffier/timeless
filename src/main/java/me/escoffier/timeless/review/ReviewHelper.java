package me.escoffier.timeless.review;

import me.escoffier.timeless.model.Project;
import me.escoffier.timeless.todoist.Todoist;
import org.apache.commons.io.output.StringBuilderWriter;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class ReviewHelper {

    public static void prepareWeeklyReview(String reviewProjectName, Todoist todoist,
                                           List<Project> projects) {
        Project weeklyReviewProject = null;
        for (Project project : projects) {
            if (project.name().equalsIgnoreCase(reviewProjectName)) {
                weeklyReviewProject = project;
                break;
            }
        }
        if (weeklyReviewProject == null) {
            throw new NoSuchElementException("Unable to find weekly review project: " + reviewProjectName);
        }

        List<Todoist.CompletedItem> subTasks = new ArrayList<>();
        ZonedDateTime time = Instant.now().minus(Duration.ofDays(10))
                .atZone(ZoneOffset.UTC).withHour(0).withMinute(0);
        String since = DateTimeFormatter.ISO_INSTANT.format(time);
        Todoist.CompletedTasksResponse tasks = todoist
                .getCompletedTasks(200, since);
        for (Todoist.CompletedItem item : tasks.items) {
            if (item.project_id.equals(weeklyReviewProject.id()) && item.completed_date != null) {
                subTasks.add(item);
            }
        }

        subTasks.forEach(ci -> todoist.reopenTask(ci.task_id));

    }

    public static void toHtml(StringBuilderWriter writer) throws IOException {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(writer.toString());
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        StringBuilderWriter report = new StringBuilderWriter();
        renderer.render(document, report);
        Files.writeString(new File("weekly.html").toPath(), report.toString());
    }
}
