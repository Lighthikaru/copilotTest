# Project Navigator

Project Navigator is a self-contained Spring Boot web app with embedded Tomcat.

## MVP Scope

- Bind an existing local project folder
- Build a local project map
- Use GitHub Copilot through the local `copilot` CLI login on the same machine

## What To Deliver For Internal Testing

Use the executable JAR generated at:

```text
target/project-navigator-0.1.0-SNAPSHOT.jar
```

Or hand over this bundled package:

```text
target/project-navigator-bundle.zip
```

The bundle contains:

- `project-navigator-0.1.0-SNAPSHOT.jar`
- `scripts/start-project-navigator.sh`
- `scripts/start-project-navigator.bat`

## Prerequisites On The Test Machine

- Java 17+
- Optional but required for chat: official `copilot` CLI

## Local Project + Copilot Flow

1. Start the app.
2. Open `http://localhost:8080`.
3. In a terminal on the same machine, run:

```bash
copilot login
```

4. Finish browser authorization.
5. Back in the app, click `Refresh`.
6. Bind an existing local project folder.
7. Wait for indexing to finish, then start chatting.

## Run

### macOS / Linux

```bash
./scripts/start-project-navigator.sh
```

### Windows

```bat
scripts\start-project-navigator.bat
```

### Direct Java Command

```bash
java -jar project-navigator-0.1.0-SNAPSHOT.jar
```

If you are running from the source repository instead of the bundle, this also works:

```bash
java -jar target/project-navigator-0.1.0-SNAPSHOT.jar
```

Default URL:

```text
http://localhost:8080
```

## Common Environment Variables

- `SERVER_PORT`: override web port
- `NAVIGATOR_DATA_ROOT`: override default data root if you do not want to use the user home directory

## Notes

- This MVP does not clone from GitLab.
- This MVP does not use GitHub OAuth.
- Project data, SQLite state, and chat summaries are stored under the local user profile by default.
- This package does not require an external Tomcat server.
# copilotTest
# copilotTest
# copilotTest
# copilotTest
