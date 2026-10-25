# Finish the submission

This is a reviewed copy of your uploaded FatimaZahrae_project1 source project. It keeps your original class structure and JOIN/MSG/LIST/LEAVE text protocol. It adds built Windows JARs, diagrams, external settings, short launchers, checks, and submission instructions.

## First: run the prepared applications

1. Extract the entire ZIP.
2. Run `java -version` in Command Prompt. Use Java 21 or newer.
3. Open the `dist` folder. Double-click `start-server.bat`.
4. Double-click `start-client.bat`. Connect as Fatima Zahrae.
5. Open `start-client.bat` twice more: connect as Amine, then with a blank username.
6. Test SEND, Enter, allUsers, read-only blocking, end, bye and LEAVE.

If double-clicking is inconvenient, open separate terminals in `dist`:

```bat
java -jar TCPServer.jar
```

```bat
java -jar TCPClient.jar localhost 3000
```

These are separate terminal commands; the server stays running while clients chat. Keep `server.properties` and `client.properties` in the current working directory. The supplied JARs target Windows x64. Rebuild from source on Linux/macOS or a different CPU architecture.

## Then: source and diagrams

Open the root `pom.xml` in IntelliJ, select JDK 21 and reload Maven. The root POM builds the two projects together; each individual POM also works independently. Read README and `docs/DEMO_SCRIPT.md` to match your explanation to this exact code.

Open `docs/Class_Diagram.png` and `docs/Deployment_Diagram.png`. Include these two diagrams in your submission. SVG and editable PUML versions are also supplied.

## Then: GitHub

If you already have a repository, update it with this reviewed project, commit your actual changes and push. If you do not have one, create an empty repository on GitHub, then run these commands in Git Bash from this project folder:

```sh
git init
git add .gitignore pom.xml TCPServer TCPClient client.properties server.properties
git commit -m "Add TCP chat server and JavaFX client"
git add README.md START_HERE.md docs verification SUBMISSION_LINKS.txt
git commit -m "Add UML, documentation and project checks"
git add dist
git commit -m "Add executable Windows JARs and launch scripts"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
git push -u origin main
```

Replace the repository URL with the real one. These commits describe adding files now; do not invent earlier development history. Do not initialize another repository or add origin again if you already have a Git repository. Ensure your instructor can open the repository.

## Then: the video

Record approximately three minutes showing both source and a live demo. Use `docs/DEMO_SCRIPT.md`. Screen and voice are sufficient; the assignment does not require showing your face.

Upload the recording where your course instructions allow, or submit the video file directly. If using a link, check instructor access. Add the real GitHub and video links to `SUBMISSION_LINKS.txt` and to your course submission.

## Final checklist

- [ ] Both Maven source folders are included.
- [ ] Both executable JARs launch on your computer.
- [ ] Class diagram and deployment diagram are included.
- [ ] README describes building, launching, configuration and commands.
- [ ] Your GitHub repository has the source, README, diagrams, JARs and actual commits.
- [ ] Three-minute video shows source and working features.
- [ ] GitHub/video links work for the instructor.
- [ ] Student details and any actual teammates are correct.
- [ ] The final files/links are uploaded to the course portal before its deadline.

The supplied brief does not name a deadline, submission portal or required video host. Follow the course page for those details. The technical blog post and extra diagrams are optional; prioritize the required items above.
