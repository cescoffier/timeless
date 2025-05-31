# Timeless

Opinionated, flexible but pragmatic way to try conquer the many inboxes in a Red Hatter's life.

## Setup

- Get a todoist account and grab API token
    - pre-create projects: Talks, Reading List
- Get a github account and grab API token
- Setup a google project on google cloud platform (public google not red hat as it won't have access to public gmail)
    - Enable GMail API, Google Calendar Api and Google Drive API
    - Download credentials.json from https://console.cloud.google.com/apis/api/people.googleapis.com/credentials into
      root of timeless and rename it into `credentials-<name-of-account>, for example `credentials-redhat.json` or `
      credentials-personal.json`
- Get pocket account and create an app to get consumer token and
  use https://reader.fxneumann.de/plugins/oneclickpocket/auth.php to get access token

## Build and run timeless

`mvn package`

Copy `timeless-example.yaml` to `timeless.yaml` (put it next to credentials-<name-of-account>.json)

`java -Dquarkus.config.locations=timeless.yaml -jar target/timeless-0.1.0-SNAPSHOT.jar`

## Hints

For each task created by timeless, you can provide a _hint_ to automatically configure the project and the section for
the task.

Hints are configured as follows:

```yaml
hints:
  # Keywords: to match against the task content (title), comma separated list
  # Project: the name of the project to assign the task to, must exist in Todoist
  # Section: the name of the section to assign the task to, must exist in Todoist (optional)
  - keywords: Clement / Thomas Sync, Quarkus Team call
    project: "☕ - Quarkus"
    section: "📆 - Meetings"

  - keywords: https://github.com/smallrye/smallrye-reactive-messaging
    project: "🚧 -  Messaging"
```

If inbox cleanup is enabled (using `todoist.cleanup-enabled: true`), hints are applied to each task from the inbox. 
If a task matches a hint, it is assigned to the project and section specified in the hint.