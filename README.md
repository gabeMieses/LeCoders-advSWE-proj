# LeCoders-advSWE-proj

## Development

### Prerequisites

- Java 17 or newer (`java -version`)
- Maven 3.9+ (`mvn -version`)

All libraries and tools (Spring Boot, JUnit 5, Mockito, Checkstyle, JaCoCo) are
declared in `pom.xml` and downloaded automatically by Maven on the first build.

### Commands

Run these from the project root (the folder containing `pom.xml`).

| Command | What it does |
|---|---|
| `mvn spring-boot:run` | Builds and starts the service |
| `mvn test` | Runs all unit tests and writes the coverage report |
| `mvn verify` | Runs tests, then **fails** if there is any style violation or branch coverage is below 55% |
| `mvn checkstyle:checkstyle` | Writes a full HTML style report without failing the build |

**Run `mvn verify` before opening a pull request.** If it prints `BUILD SUCCESS`,
your code passes the style check and the coverage threshold.

### Style checking (Checkstyle)

- **Tool:** Checkstyle 10.21.1 via `maven-checkstyle-plugin` 3.6.0
- **Ruleset:** Google Java Style (`google_checks.xml`), modified to use 4-space
  indentation instead of 2
- **Config:** `config/checkstyle/checkstyle.xml`
- **Scope:** both `src/main/java` and `src/test/java`

When a check fails, Maven prints the file, line, column and rule name:

```
[WARN] src/main/java/com/lecoders/PollService.java:42:9: 'if' construct must use '{}'s. [NeedBraces]
```

Look up the rule in brackets at <https://checkstyle.org/checks.html> if the
message is unclear.

Common violations:

- Missing Javadoc on a public class or method (test methods marked `@Test` are exempt)
- `if`/`else`/`for` without braces
- Lines over 100 characters
- Wildcard imports (`import java.util.*;`)
- Field names whose second letter is uppercase (`pId` fails, `pollId` passes)

Indentation is 4 spaces, which matches the default settings of VS Code and
IntelliJ, so no editor changes are needed. If Checkstyle reports an
`Indentation` or `FileTabCharacter` violation, run Format Document on that file.

### Test coverage (JaCoCo)

- **Tool:** JaCoCo 0.8.13 via `jacoco-maven-plugin`
- **Config:** `pom.xml` (the `jacoco-maven-plugin` block). The threshold is the
  `jacoco.branch.minimum` property.
- **Report:** after `mvn test`, open `target/site/jacoco/index.html` in a browser.
  The "Missed Branches" column shows which `if`/`switch` paths no test reaches.