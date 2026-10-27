# AI usage: Build tooling, docs and API tests (Aliasgar Sakarwala, as8199)

Two tools were used for this work. **Do not replace section 1 with section 2** — keep both.

1. **Claude Code** — Checkstyle / JaCoCo / test-dependency tooling (and related README
   Development notes from the tooling PR).
2. **Cursor Agent** — iteration-1 docs (`README.md`, `docs/API.md`), Postman/Newman, error
   message config, AI markers, and reports.

---

## 1. Claude Code (tooling)

> **Restore needed.** When the Cursor section below was written, the on-disk copy of this
> file was empty, so a full-file write replaced whatever Claude Code write-up was already
> here (likely still open / unsaved in the editor). That draft was not in git and no Cursor
> Local History entry was found for this path.
>
> **Please paste your Claude Code section back here** (or use the editor Timeline /
> Local History / Undo on this file if you still have it). Expected contents, matching the
> other `docs/ai-usage/` files:
>
> - Tool table (Claude Code, model, date)
> - Primary prompts
> - How I used it
> - AI-generated code table (`pom.xml` Checkstyle/JaCoCo blocks, `checkstyle.xml`,
>   Development README bits, etc.)
> - Tokens used (from the Claude Code session log)

### Tool

| | |
|---|---|
| Tool | Claude Code (Anthropic) — TODO: CLI or VS Code extension |
| Model | TODO |
| Where we got it | TODO |
| Date used | TODO |

### Primary prompts

TODO — paste the Claude Code prompts that were here before.

### How I used it

TODO — paste the Claude Code workflow that was here before.

### AI-generated code

TODO — paste the Claude Code file table that was here before. Typical paths for this task:

| Path | What was generated |
|---|---|
| `pom.xml` | Checkstyle plugin, JaCoCo plugin, `jacoco.branch.minimum`, `spring-boot-starter-test` |
| `config/checkstyle/checkstyle.xml` | LeCoders adaptation of `google_checks.xml` (4-space Indentation) |
| `README.md` | Development section (commands, Checkstyle, JaCoCo) from the tooling PR |

### Tokens used

TODO — paste totals from the Claude Code session log (same shape as `gm3132-create-poll.md`
/ `jkd2146-delete-poll.md`).

| Token type | Count |
|---|---|
| Input (not cached) | TODO |
| Output | TODO |
| Cache write | TODO |
| Cache read | TODO |
| **Total** | **TODO** |

---

## 2. Cursor Agent (docs, API tests and follow-ups)

This section is the Cursor-only part that was meant to fill the TODOs for documentation and
API testing. It does **not** replace section 1.

### Tool

| | |
|---|---|
| Tool | Cursor Agent (Cursor IDE) |
| Model | Claude Opus 5 (Composer / agent router in Cursor) |
| Where we got it | Cursor IDE, agent chat on the project workspace |
| Date used | 2026-10-26, continuing conversation on branch `Docreview1` |

### Primary prompts

1. README (iteration-1 documentation):

   > Document ONLY what the code actually does. Exact paths, HTTP methods, header names,
   > JSON field names (exact spelling/casing), status codes and error messages must come
   > from the code. Never guess. … Write README.md … Do not modify any Java code.
   > Do not commit or push.

2. Split API docs, then Postman and follow-ups:

   > Split the docs: move Authentication, Error format, API reference, Call order rules,
   > Example usage scenarios and Misuse examples into docs/API.md …
   >
   > One folder per endpoint. Cover the success path and every misuse case in docs/API.md.
   > Each request has pm.test scripts asserting status code and key response fields. …
   > Add server.error.include-message=always … Re-run every error example in docs/API.md …

(Pom / Checkstyle prompts that also ran in this Cursor chat belong with the tooling story
in section 1 once that section is restored; they are not repeated here so Claude Code
credit is not overwritten.)

### How I used it

1. **README and accuracy.** Required the agent to read the codebase and document only
   behaviour proven by the code or live curls. Kept the existing Development section and
   merged rather than rewriting it.
2. **Docs split.** Moved Authentication through Misuse examples into `docs/API.md` with its
   own table of contents; README kept a short endpoint table plus a link.
3. **Postman / Newman.** Collection v2.1 that passes top to bottom on a fresh server:
   `postman/LeCoders-poll-service.postman_collection.json`,
   `postman/local.postman_environment.json`, report at
   `reports/postman/newman-report.json` (72 requests, 137 assertions, 0 failures).
4. **Error messages and markers.** `server.error.include-message=always`, re-captured error
   bodies in `docs/API.md`, AI markers on unmarked results/auth classes, tool names on the
   four results test classes, `PollController` Javadoc cleanup.
5. **Reports.** `mvn verify` + `mvn checkstyle:checkstyle`, then copy into `reports/`
   (103/106 branches = 97.17%).

### AI-generated code and docs

| Path | What was generated or updated |
|---|---|
| `README.md` | Iteration-1 project docs (team, proposal diffs, API summary, testing, reports, AI usage) |
| `docs/API.md` | Full HTTP API reference split out of the README |
| `postman/LeCoders-poll-service.postman_collection.json` | Collection v2.1; description credits Cursor Agent |
| `postman/local.postman_environment.json` | `baseUrl`, `apiKey`, `otherApiKey` |
| `src/main/resources/application.properties` | `server.error.include-message=always` |
| `src/main/java/com/lecoders/poll/ApiKeyValidator.java` | In-source AI marker only |
| `src/main/java/com/lecoders/poll/results/CountingAlgorithms.java` | In-source AI marker only |
| `src/main/java/com/lecoders/poll/results/CountingMethod.java` | In-source AI marker only |
| `src/main/java/com/lecoders/poll/results/CountResult.java` | In-source AI marker only |
| `src/main/java/com/lecoders/poll/results/ResultsController.java` | In-source AI marker only |
| `src/main/java/com/lecoders/poll/PollController.java` | Class Javadoc cleanup and `@param apiKeys` |
| `src/test/java/com/lecoders/poll/results/*Test.java` (4 files) | Marker text updated to name OpenAI Codex |
| `reports/checkstyle/` | Copied Checkstyle HTML + XML after a clean verify |
| `reports/jacoco/` | Copied JaCoCo site after a clean verify |
| `reports/postman/newman-report.json` | Newman JSON export of the passing run |

In-source XML comments on `pom.xml` / `checkstyle.xml` currently name Cursor Agent for the
tooling blocks; once section 1 is restored, align those markers with whichever tool actually
authored each block.

### Tokens used

Cursor Agent did not expose a Claude-Code-style session total for this chat.

| Token type | Count |
|---|---|
| Input (not cached) | TODO — not recorded |
| Cached input | TODO — not recorded |
| Output | TODO — not recorded |
| **Total** | **TODO — not recorded** |
