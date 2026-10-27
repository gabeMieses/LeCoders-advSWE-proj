# LeCoders poll-service

A Spring Boot REST service for ranked-choice polling. It has **no user interface**: every
feature is reached over HTTP by client programs, which create a poll, submit ranked ballots
on behalf of their users, and ask for results computed by **plurality**, **ranked choice**
(instant runoff) or **Borda count**. All four endpoints are authenticated with an API key
that identifies the calling client program.

Storage is in memory only (`ConcurrentHashMap`), so every poll and ballot is lost when the
service restarts, and poll and ballot ids restart at `1`.

- **Repository:** <https://github.com/gabeMieses/LeCoders-advSWE-proj>
- **Iteration:** 1 (Advanced Software Engineering)

---

## Team

Roles below are taken from the git history and the per-task AI usage docs in
`docs/ai-usage/`.

| Member | GitHub | Built |
|---|---|---|
| Gabriel Mieses (gm3132) | [@gabeMieses](https://github.com/gabeMieses) | Project skeleton; `POST /polls` (create poll) and its validation, `Poll`, `PollRepository.save`, create-poll and repository tests (issue #3, PRs #16 and #17) |
| Jean-Brandon "JB" Diei (jed2221) | [@jbdiei](https://github.com/jbdiei) | `POST /polls/{pollId}/ballots` (cast ballot), `Ballot`, `BallotRepository.saveIfFirstVote`, the shared `MockApiKeys` test helper and cast-ballot tests |
| Haoran Guo (hg2816) | [@lek1ma](https://github.com/lek1ma) | `GET /polls/{pollId}/results`, the three counting algorithms, `CountingMethod`, `CountResult`, `RankedBallotStore`, the shared `ApiKeyValidator`, and the results/storage/HTTP tests (issue #6, PRs #19 and #21) |
| Jason Dixon (jkd2146) | [@jkd2146](https://github.com/jkd2146) | `DELETE /polls/{pollId}`, `PollRepository.deleteById`, `BallotRepository.deleteByPollId`, the unknown-API-key check on cast ballot, and the delete tests (issue #5, PR #18) |
| Aliasgar Sakarwala (as8199) | [@AliasgarSakarwala](https://github.com/AliasgarSakarwala) | Build tooling: Checkstyle and JaCoCo wiring in `pom.xml`, `config/checkstyle/checkstyle.xml`, the Spring Boot test dependency; the Postman API test collection, this README and `docs/API.md` |

---

## Changes from the proposal

| # | Proposal said | The code does | Reason |
|---|---|---|---|
| 1 | Create poll takes an **optional closing time** | No closing time is accepted or stored anywhere. `Poll` has no such field, and an unknown `closing_time` JSON field is silently ignored (the request still returns 201). | Deferred. `PollController.castBallot` carries the comment `// TODO: reject ballots once the poll is closed, after polls store a closing time.` |
| 2 | Cast ballot **rejects closed polls** | Not implemented, because there is no closing time to compare against. Ballots are accepted for as long as the poll exists. | Follows from change 1. |
| 3 | Create poll takes question, options and API key | It **also requires `counting_method`**, which must be exactly `plurality`, `ranked_choice` or `borda`. | The create-poll AI usage doc records the decision that all four inputs are required. |
| 4 | — | Options must be **at least 2, distinct, and non-blank**; the question must be non-blank. | Added by the create-poll follow-up (PR #17), whose task list included "Blank/Duplicate options not rejected". |
| 5 | Get results takes poll_id and counting method; returns 200/400/404 | It **also requires a valid API key** and returns **401** without one. | The results work adopted the shared `ApiKeyValidator` during integration, so all four endpoints authenticate the same way. |
| 6 | Get results takes "counting method" | `counting_method` is a **required query parameter with no default**, and is **case-sensitive**. A poll's own stored `counting_method` is never used to pick the method. | `CountingMethod.parse` is documented as rejecting "missing or unsupported methods instead of choosing an implicit default". |
| 7 | Cast ballot returns 201/400/401 | It also returns **404** for an unknown or deleted `pollId`. | The proposal's status list left out 404 for this endpoint. A poll that doesn't exist (or was deleted) is a missing resource, not a malformed request, so it gets 404 instead of 400. |
| 8 | Delete poll returns 200/401 | It also returns **404** for an unknown or already-deleted poll. | Same reason as change 7. It also lets a client tell "already deleted" (404) apart from "not your poll" (401). |
| 9 | — | Results are computed **while voting is still open**; there is no "poll closed" precondition for reading them. | Follows from change 1. |
| 10 | — | Any valid API key may **cast ballots on** and **read results of** any poll. Only `DELETE` is restricted to the key that created the poll. | Matches the proposal, which scoped ownership to deletion only. |
| 11 | — | Results can return **500** when stored ballot data is inconsistent with the poll's options. | Inconsistent stored data is a fault in the service, not in the client's request, so `ResultsService` maps that `IllegalArgumentException` to 500 rather than 400. Not reachable through the public API in iteration 1; covered only by a mocked unit test. |

### Not implemented yet

- **Closing times**, and therefore the proposal's rule that ballots are refused after a poll closes.
- **No endpoint reads a poll back.** There is no `GET /polls` or `GET /polls/{pollId}`, so a
  client cannot recover a poll's question or option labels after the `POST /polls` response.
  Only `option_ids` (the numeric positions) can be rediscovered, via a results call.

---

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
| `mvn spring-boot:run` | Builds and starts the service on <http://localhost:8080> |
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
  `jacoco.branch.minimum` property, currently `0.55`.
- **Report:** after `mvn test`, open `target/site/jacoco/index.html` in a browser.
  The "Missed Branches" column shows which `if`/`switch` paths no test reaches.

---

## API documentation

The service exposes four endpoints, all of which require an `X-API-Key` header:

| Method | Path | What it does | Auth |
|---|---|---|---|
| `POST` | `/polls` | Creates a poll from a question, at least 2 distinct options and a counting method; returns the `poll_id` and zero-based `option_ids` | Any configured API key; that key becomes the poll's owner |
| `POST` | `/polls/{pollId}/ballots` | Records one voter's ranked option ids on a poll, at most one ballot per `voter_id` per poll | Any configured API key, including keys that did not create the poll |
| `GET` | `/polls/{pollId}/results` | Counts the poll's ballots by `plurality`, `ranked_choice` or `borda` (required query parameter) and returns the winner, per-option scores and ranked-choice rounds | Any configured API key |
| `DELETE` | `/polls/{pollId}` | Deletes the poll and every ballot cast on it | **Only the API key that created the poll** |

There is no endpoint to list or read back a poll, and no way to close one.

**See [docs/API.md](docs/API.md)** for the full reference: authentication and key
configuration, the exact error body, per-endpoint parameters, validation rules, status
codes and real request/response examples, the order calls must be made in, two end-to-end
usage scenarios, and twelve worked misuse examples.

---

## Testing

### Unit and integration tests

- **Framework:** JUnit 5 (Jupiter), including `@ParameterizedTest` with `@EnumSource`
- **Mocking:** Mockito — `@Mock` with `MockitoExtension` in plain unit tests, and
  `@MockitoBean` to replace repositories in Spring slice tests
- **Assertions:** AssertJ (`assertThat`), plus MockMvc's `jsonPath` and `status` matchers
- **HTTP:** `@WebMvcTest` with MockMvc for controller slices, and `@SpringBootTest` with
  `TestRestTemplate` on a random port for real end-to-end HTTP
- **Location:** `src/test/java/com/lecoders/poll/`, in packages that mirror the feature they
  cover (`poll`, `ballot`, `delete`, `results`, `support`)
- **Test configuration:** `src/test/resources/application.properties` sets the API keys tests
  use; `support/MockApiKeys.java` holds the header name and the fake keys shared across tests

**Run them:**

```bash
mvn test     # tests plus the coverage report
mvn verify   # tests, then Checkstyle and the branch-coverage gate
```

The suite currently has **87 tests**, grouped as follows:

| Test class | Tests | What it covers |
|---|---|---|
| `poll/CreatePollControllerTest` | 16 | `POST /polls` over MockMvc: 201 with `poll_id` and zero-based `option_ids`, all three counting methods, both client keys, and every 400/401 validation path (missing/blank question, missing/one/blank/`null`/duplicate options, missing/blank/unsupported counting method) |
| `poll/PollRepositoryTest` | 6 | `PollRepository` directly: `save` assigns increasing ids and keeps fields, respects a supplied id, and `deleteById` removes only the target poll and reports whether it existed |
| `ballot/CastBallotControllerTest` | 17 | `POST /polls/{pollId}/ballots` over MockMvc: full and partial rankings, voting with another client's key, 401 cases, 404 for an unknown poll, and the 400 cases (missing/blank `voter_id`, missing/empty ranking, out-of-range/negative/`null`/duplicate option ids, repeat voter), plus that a rejected ballot does not block a later valid one |
| `ballot/BallotRepositoryTest` | 8 | `BallotRepository` directly: `saveIfFirstVote` id assignment and one-vote-per-poll enforcement, the same voter on different polls, and `deleteByPollId` removing only that poll's ballots and forgetting its voters |
| `delete/DeletePollControllerTest` | 8 | `DELETE /polls/{pollId}` with both repositories replaced by Mockito mocks: the 200 response body, that the poll is deleted *before* its ballots, 401 for a non-owner/missing/blank/unknown key, 404 for an unknown poll, 400 for a non-numeric id, and that rejected calls delete nothing |
| `delete/DeletePollFlowTest` | 4 | Delete against the real in-memory repositories: `ballots_deleted` counts, and that after deletion casting a ballot and deleting again both 404 while a non-owner's failed delete leaves the poll usable |
| `results/CountingAlgorithmsTest` | 13 | `CountingAlgorithms` as pure functions: hand-calculated plurality, Borda and multi-round ranked-choice elections, exhausted ballots, immediate majorities, id-based tie-breaking, and rejection of invalid options, rankings and methods |
| `results/ResultsServiceTest` | 4 | `ResultsService` with a mocked repository and ballot store: method validated before storage is touched, 404 for an unknown poll, the requested method honoured over the stored one, and corrupt stored data mapped to 500 |
| `results/ResultsHttpTest` | 8 | Full-stack HTTP: create, cast and count over a real port for all three methods, ranked-choice rounds, an empty poll, 400/401/404 handling, that rejected ballots do not change results, and that deleting one poll does not affect another |
| `results/StorageAndAuthTest` | 3 | Storage and auth invariants: ballot snapshots are immutable and isolated per poll, a caller cannot mutate a saved poll's option order, and an empty key list denies everyone |

### API tests

- **Tool:** Postman (collection format v2.1)
- **Collection:** `postman/LeCoders-poll-service.postman_collection.json`
- **Environment:** `postman/local.postman_environment.json`, which sets `baseUrl` to
  `http://localhost:8080` and `apiKey`/`otherApiKey` to the two demo keys in
  `src/main/resources/application.properties`
- **Coverage:** one folder per endpoint — 72 requests and 137 assertions covering each
  success path and every misuse case documented in [docs/API.md](docs/API.md)
- **How to run:** start the service with `mvn spring-boot:run`, then either import both
  files in the Postman app (**File → Import**), select the *poll-service local* environment
  and use **Run collection**, or run it headless with
  [Newman](https://www.npmjs.com/package/newman):

  ```bash
  npx newman run postman/LeCoders-poll-service.postman_collection.json \
    -e postman/local.postman_environment.json --reporters cli,json \
    --reporter-json-export reports/postman/newman-report.json
  ```

  The last run's machine-readable output is committed at
  `reports/postman/newman-report.json`.

Run the collection **top to bottom against a freshly restarted service**. The create-poll
folder stores each new `poll_id` in a collection variable that later folders depend on, the
results assertions expect exactly the ballots cast earlier in the run, and the delete folder
removes a poll permanently, so individual requests do not pass in isolation or on a second
run against the same server.

---

## Reports

`target/` is git-ignored, so each report is copied out of it into `reports/` after a clean
run of the full build.

| Report | Committed path | Regenerate with |
|---|---|---|
| Checkstyle, HTML | `reports/checkstyle/checkstyle.html` | `mvn checkstyle:checkstyle`, then copy `target/reports/checkstyle.html` |
| Checkstyle, XML | `reports/checkstyle/checkstyle-result.xml` | the same run; copy `target/checkstyle-result.xml` |
| JaCoCo coverage | `reports/jacoco/index.html` (plus `jacoco.xml` and `jacoco.csv` beside it) | `mvn verify`, then copy the whole `target/site/jacoco/` directory |
| Newman API tests | `reports/postman/newman-report.json` | the Newman command in [API tests](#api-tests) above |

These were produced by the most recent local run of `mvn verify` followed by
`mvn checkstyle:checkstyle`, which reported:

- **87 tests, 0 failures** and `BUILD SUCCESS`
- **0 Checkstyle violations** across `src/main/java` and `src/test/java`
- **Branch coverage 103 of 106 = 97.17%**, against the 55% gate in `pom.xml`
  (instructions 1128/1135 = 99.38%, lines 207/209 = 99.04%)
- **72 Postman requests, 137 assertions, 0 failures**

Open `reports/jacoco/index.html` in a browser for the per-class breakdown; the three
uncovered branches are the only gaps left.

---

## Third-party code

- **`config/checkstyle/checkstyle.xml`** is adapted from Checkstyle 10.21.1's
  `google_checks.xml`
  ([source](https://github.com/checkstyle/checkstyle/blob/checkstyle-10.21.1/src/main/resources/google_checks.xml)),
  licensed **LGPL-2.1** by the Checkstyle project (authors Max Vetrenko, Mauryan Kansara,
  Ruslan Diachenko, Roman Ivanov). The only change is the `Indentation` module, set to
  4 spaces instead of 2. The attribution is repeated in the file header.

No other copied or vendored third-party code was found in this repository. Everything else
under `src/` was written for this project.

**All libraries are fetched by Maven from Maven Central at build time and none are vendored**
— there is no `lib/` directory and no committed `.jar` files. The dependencies are the
Spring Boot 3.5.6 starters (`spring-boot-starter-web` and `spring-boot-starter-test`, the
latter supplying JUnit 5, Mockito, AssertJ, JSONPath and MockMvc), with Checkstyle 10.21.1
and JaCoCo 0.8.13 pulled in by their Maven plugins.

---

## Project management

- **Tracking tool:** GitHub Issues, used instead of the Jira named in the proposal. Each task
  is an issue assigned to its owner and labelled by type (`endpoint`, `testing`, `tooling`,
  `docs`) and iteration (`iteration-1`); there is no separate project board.
- **Issues:** <https://github.com/gabeMieses/LeCoders-advSWE-proj/issues>
- **Pull requests:** <https://github.com/gabeMieses/LeCoders-advSWE-proj/pulls>

Iteration 1 was tracked as one issue per endpoint, each delivered in its own branch and pull
request and reviewed by another team member before merging: issue #3 create poll (PRs #16,
#17), issue #5 delete poll (PR #18), issue #6 results and counting (PRs #19, #21), plus the
cast-ballot and build-tooling branches.

---

## AI usage

| Tool | Used for | Example prompts | Tokens |
|---|---|---|---|
| **Claude Code** (Anthropic), model Claude Opus 5.5, CLI — Gabriel Mieses | Project skeleton (`PollApplication`, `pom.xml` coordinates and web starter); `POST /polls` with `CreatePollRequest`, `COUNTING_METHODS` and the original API-key check; `Poll`; `PollRepository.save`; `application.properties` for both main and test; `CreatePollControllerTest`; the three `save` tests in `PollRepositoryTest` | *"Inside of PollController.java, make an HTTP endpoint that creates polls. I want the inputs to require a question, options for the question, an api key, and a counting method specified… Record the Poll object inside of Poll.java so future endpoints can reuse the definition. Also store newly created polls, placing the logic inside of PollRepository.java."* and *"Given my PR's follow-ups (Validate the API key actually exists/ should be accepted, Validate the counting method is supported, Blank/Duplicate options not rejected, Return back the correct success message (with option ids)), I want you to implement for these, reuse as much code as possible, be concise. Don't touch any other work"* | **14,865,859** total for the whole Claude Code session (252 input, 97,047 output, 299,053 cache write, 14,469,507 cache read). The two create-poll prompts account for 7,874,532 of it |
| **Claude Code** (Anthropic), model Claude Opus 5.5, VS Code extension — Jason Dixon | `deletePoll` and the extracted API-key helper in `PollController`; `PollRepository.deleteById`; `BallotRepository.deleteByPollId` and `voteKey`; `DeletePollControllerTest`; `DeletePollFlowTest`; the `deleteById` and `deleteByPollId` repository tests; the unknown-key check on cast ballot | *"Here is the current project, I don't want you to write code yet, but look at our current status and if you can, check for the task assigned to me on git and let's plan what to do"* (with our proposal attached), *"there was a new commit, look at it first"*, *"Yeah let's begin the implementation for what was assigned to me please, also remember that I do not want you to allocate yourself as a contributor on here. Let's try and have this into separate commits"*, *"Alright let's go ahead and continue on"* and *"what should be my PR description"* | **3,487,930** total (62 input, 29,989 output, 224,150 cache write, 3,233,729 cache read) |
| **Claude Code** (Anthropic), model Claude Opus 5.5 — JB Diei | `castBallot`, `CastBallotRequest` and `validateRanking` in `PollController`; the original `Ballot` record; `BallotRepository` and `saveIfFirstVote`; `PollRepository.findById`; `MockApiKeys`; `CastBallotControllerTest` and the `saveIfFirstVote` tests | *"just fill in the casting ballots fuction stuff, the other ones are meant fo rother team members"* and *"yes create tests, and create a specifc test folder with just ballot stuff. and make a separte file or folder or whatever would be optimal with mock api keys"* | **3,880,206** total (100 input, 41,273 output, 160,679 cache write, 3,678,154 cache read) |
| **OpenAI Codex** desktop, model recorded as `gpt-6-astra` — Haoran Guo | The whole `results` package (`CountingAlgorithms`, `CountingMethod`, `CountResult`, `ResultsService`, `ResultsController`, `RankedBallotStore`, `RepositoryRankedBallotStore`); the shared `ApiKeyValidator`; defensive copies in `Poll` and `Ballot`; snapshot reads and `synchronized` changes in `BallotRepository`; shared-key wiring and option-id adaptation in `PollController`; `CountingAlgorithmsTest`, `ResultsServiceTest`, `ResultsHttpTest`, `StorageAndAuthTest` | Summarized in `Haoran-ai-usage.md` rather than quoted: inspect the assignment, issue and teammates' code; build on existing team decisions and propose rules where requirements were incomplete; adapt to the shared repository and resolve conflicts with the delete endpoint | **30,054,290** total (457,750 input not cached, 29,514,752 cached input, 81,788 output) |
| **Cursor Agent**, model Claude Opus 5 — Aliasgar Sakarwala | The Checkstyle, JaCoCo and test-dependency wiring in `pom.xml`; the Checkstyle violations fixed in `PollApplication`; this README and `docs/API.md`; the Postman collection and environment under `postman/`; `server.error.include-message=always`; the Checkstyle, JaCoCo and Newman reports under `reports/` | *"I want to add some new additions to the current pom xml file, for it to look like … Is this good? if so make the changes"*, *"Fix all the checkstyle errrors being caused"*, *"Document ONLY what the code actually does. Exact paths, HTTP methods, header names, JSON field names (exact spelling/casing), status codes and error messages must come from the code."* and *"One folder per endpoint. Cover the success path and every misuse case in docs/API.md. Each request has pm.test scripts asserting status code and key response fields."* | Not recorded: Cursor did not expose a session token total (see `as8199-Ai-usage.md`) |

Per-task write-ups, including each author's own recorded session token totals, are in
`docs/ai-usage/`: `gm3132-create-poll.md`, `jbdiei-cast-ballot.md`, `jkd2146-delete-poll.md`,
`Haoran-ai-usage.md` and `as8199-Ai-usage.md`. The counts above are each author's **whole session**, not just the
task named in the row, and they are not directly comparable: the Claude Code totals count
cache reads (which dominate them) while the Codex total counts cached input separately.

### Marking of AI-generated code

AI-generated or AI-debugged code is marked in the source with a Javadoc line or comment
naming the tool and the reviewer, in one of these forms:

- `AI-generated with Claude Code (Opus 5.5); reviewed and tested by <name>.`
- `AI-generated with OpenAI Codex; integration tested by Haoran (@lek1ma).`
- `AI-generated with Cursor Agent (Claude Opus 5); reviewed and tested by Aliasgar Sakarwala.`
  (XML comments in `pom.xml` and `config/checkstyle/checkstyle.xml`)

Every file listed as AI-generated in `docs/ai-usage/` carries one of these markers, naming
the tool that produced it.
