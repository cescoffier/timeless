# Timeless

Opinionated, flexible but pragmatic way to try conquer the many inboxes in a Red Hatter's life.

## Setup

- Get a todoist account and grab API token
  - pre-create projects: Talks, Reading List
- Get a github account and grab API token
- Setup a google project on google cloud platform (public google not red hat as it won't have access to public gmail)
  - Enable GMail API, Google Calendar Api and Google Drive API
  - Download credentials.json from https://console.cloud.google.com/apis/api/people.googleapis.com/credentials into root of timeless and rename it into `credentials-<name-of-account>, for example `credentials-redhat.json` or `credentials-personal.json`
- Get pocket account and create an app to get consumer token and use https://reader.fxneumann.de/plugins/oneclickpocket/auth.php to get access token
  

## Build and run timeless

`mvn package`

Copy `timeless-example.yaml` to `timeless.yaml` (put it next to credentials-<name-of-account>.json)

`java -Dquarkus.config.locations=timeless.yaml -jar target/timeless-0.1.0-SNAPSHOT.jar`


