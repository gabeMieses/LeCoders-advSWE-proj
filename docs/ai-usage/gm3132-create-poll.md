# AI usage: Create poll endpoint (Gabriel Mieses, gm3132)

Task: issue #3, `POST /polls`. The first version reached main through PR #16. The follow-ups (validation and option ids) were merged in PR #17.

## Tool

| | |
|---|---|
| Tool | Claude Code (Anthropic), CLI |
| Model | Claude Opus 5.5 (`claude-opus-5-5`) |
| Where we got it | <https://claude.com/claude-code>, signed in with a personal Claude account |
| Date used | 2026-10-25 to 2026-10-26, one session |

## Primary prompts

1. First version (commit `f5a0902`):

   > Inside of PollController.java, make an HTTP endpoint that creates polls. I want the inputs to require a question, options for the question, an api key, and a counting method specified. Be concise in the implementation. Record the Poll object inside of Poll.java so future endpoints can reuse the definition. Also store newly created polls, placing the logic inside of PollRepository.java.

2. Follow-ups (commits `e21f005` and `1d15c31`, PR #17):

   > Given my PR's follow-ups (Validate the API key actually exists/ should be accepted, Validate the counting method is supported, Blank/Duplicate options not rejected, Return back the correct success message (with option ids)), I want you to implement for these, reuse as much code as possible, be concise. Don't touch any other work

## How I used it

1. **Planning.** I had Claude read our team's handoff doc and asked what the smallest testable version of create poll would be, before it wrote any code.
2. **First version.** Claude wrote the endpoint, the `Poll` record and the in-memory `PollRepository`. I cut it down several times: I removed repository methods the endpoint didn't use (such as delete) and kept only the inputs I asked for. I tested it with curl against the running app.
3. **Merging main.** After the team's Checkstyle PR was merged, Claude fixed the merge conflicts and reformatted my files to pass Checkstyle (4-space indents and Javadoc). I checked that the merge kept main's changes.
4. **Follow-ups.** Claude added the validation from the second prompt and changed the response to return `poll_id` and `option_ids`. The first attempt changed my teammate's cast ballot code, so I had it revert everything and redo the work touching only create poll.
5. **Tests.** Claude wrote the create poll controller tests and the repository tests. It added a test `application.properties` so the keys match my teammate's `MockApiKeys`, which kept their cast ballot tests passing. Claude ran `mvn verify` (tests, Checkstyle and JaCoCo) and curl checks for 201, 400 and 401. I committed the tests and the code separately.

I made the design decisions:
- all four inputs are required
- accepted API keys come from `poll.api-keys` in `application.properties`
- the counting methods are `plurality`, `ranked_choice` and `borda`
- option ids are zero-based positions in the options list, matching cast ballot

## AI-generated code

All code and tests below were AI-generated, then reviewed and tested by me. Each one is marked in the source with a comment or Javadoc line: "AI-generated with Claude Code (Opus 5.5); reviewed and tested by Gabriel Mieses."

| File | What was generated |
|---|---|
| `src/main/java/com/lecoders/poll/PollApplication.java` | scaffold: the Spring Boot main class (Javadoc added later in the tooling PR) |
| `pom.xml` | scaffold: parent, project coordinates, web starter and Spring Boot plugin (Checkstyle, JaCoCo and test dependencies added later in the tooling PR) |
| `src/main/java/com/lecoders/poll/PollController.java` | `createPoll`, `CreatePollRequest`, `COUNTING_METHODS`, and the original API key check (Haoran later moved it into `ApiKeyValidator`) |
| `src/main/java/com/lecoders/poll/Poll.java` | the original `Poll` record (teammates have since extended it) |
| `src/main/java/com/lecoders/poll/PollRepository.java` | the original class and `save` |
| `src/main/resources/application.properties` | `poll.api-keys` |
| `src/test/resources/application.properties` | whole file |
| `src/test/java/com/lecoders/poll/poll/CreatePollControllerTest.java` | whole file (16 tests) |
| `src/test/java/com/lecoders/poll/poll/PollRepositoryTest.java` | the 3 `save` tests |

This doc was also drafted with Claude.

## Tokens used

Counted from Claude Code's session log. Most of each total is cache reads: every reply re-reads the conversation so far.

### Per prompt (estimate)

Each primary prompt took several back-and-forth messages. A prompt's row adds up every message from when I started that work until I moved on to the next task. The boundaries are my best estimate.

| Work | Output | Total |
|---|---|---|
| Prompt 1: first version, including my follow-up questions and the PR description | 17,509 | 2,955,595 |
| Prompt 2: follow-ups, including planning, the reverted first attempt and the tests | 28,400 | 4,918,937 |

### Rest of the session

| Work | Output | Total |
|---|---|---|
| Reading the handoff doc and the project scaffold | 30,618 | 4,088,915 |
| Fixing merge conflicts with main and Checkstyle | 7,500 | 1,302,849 |
| Other questions (MVP status, git account) | 3,629 | 647,842 |
| This doc and the AI markers | 9,391 | 951,721 |

### Whole session

| Token type | Count |
|---|---|
| Input (not cached) | 252 |
| Output | 97,047 |
| Cache write | 299,053 |
| Cache read | 14,469,507 |
| **Total** | **14,865,859** |
