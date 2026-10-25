# Review of the uploaded ZIP

The uploaded ZIP contained both Maven projects and a README. It did not contain built executable JARs, a video, or a repository URL. Diagrams were written in README code blocks; viewable diagram files were not included. A repository/video may exist separately, but neither was provided for verification.

## Corrections and additions

- Read-only users can now issue allUsers/end/bye while ordinary chat remains blocked.
- SEND/RUN and LEAVE have sufficient column width for readable labels.
- Added short TCPServer/TCPClient launcher classes matching the assignment; retained the original packaged classes and protocol.
- Added host/port and argument-count validation, and a visible server startup error for invalid settings.
- Added an identification timeout, output-error checking and a ready flag so WELCOME precedes ordinary broadcasts.
- Ordered active-user snapshot callbacks during simultaneous updates.
- Added external properties, Windows launch scripts, and a root Maven aggregator.
- Added rendered class/deployment diagrams with SVG and editable PUML versions matching the actual class relationships.
- Corrected build commands and the explanation of external configuration locations.
- Added submission instructions, a timed demo script, a link placeholder file and a real-socket verification program.

## Verified and remaining checks

- Both Maven projects compile with Java 21.
- 24 real-socket checks passed, including 60 concurrently sent messages, Unicode broadcast, private lists, read-only restrictions, duplicate names, client parsing and disconnection cleanup.
- Actual JavaFX windows were exercised on Linux with named users and a guest: button/Enter send, timestamps, private lists, guest rejection, guest inquiry, end and LEAVE, and user-list cleanup.
- Windows x64 JARs are packaged separately. Windows graphical execution remains the student's final local check.
- The class and deployment diagrams were rendered and visually inspected.
- No GitHub repository was published and no personal demonstration was recorded by this preparation. The student must provide both links and submit through the course portal.

See START_HERE.md for the remaining tasks in order. The blog post and extra UML diagrams are optional.
