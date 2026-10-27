# AI usage: GET results and counting algorithms (Haoran)

Task: issue #6, `GET /polls/{pollId}/results` and plurality, ranked-choice, and Borda counting. Implementation submitted in PR #19; source attribution comments added in PR #21. Both PRs were merged.

## Tool

| | |
|---|---|
| Tool | OpenAI Codex desktop |
| Model | `gpt-6-astra`, as recorded in the available local session metadata |
| Where we got it | https://openai.com/codex/, used through the desktop application |
| Date used | 2026-10-26, one continuing project conversation |

## How I used it

1. **Understanding the task.** I asked Codex to inspect the assignment, project proposal, Issue #6, existing repository code, and teammates' contributions. It helped distinguish my GET/results responsibilities from the create, cast, and delete endpoints owned by other teammates.

2. **Counting rules and implementation.** I asked it to build on decisions already made by the team and propose rules where the requirements were incomplete. It generated the results endpoint, service, response types, and three counting algorithms. The implemented choices include:
   - plurality counts first choices only;
   - Borda awards N-1 through 0 points using the full poll option count, with zero for unranked options;
   - plurality/Borda score ties favor the smallest option ID;
   - ranked choice eliminates one lowest-count option per round, eliminating the largest ID when lowest counts tie;
   - ranked choice requires strictly more than half of non-exhausted ballots to win and records each round;
   - empty polls return no winner.
   
   I authorized provisional choices and included them in the PR for team review rather than presenting unspecified rules as original assignment requirements.

3. **Integration with teammates' work.** Codex adapted the results module to read from the shared ballot repository, aligned option IDs to the team's zero-based convention, and introduced a shared configured API key validator. I asked it to align with PR #17. Later, it resolved conflicts with the delete endpoint from PR #18 while preserving teammates' functionality and attribution. The original create, cast, delete, and build-tool implementations remain team contributions.

4. **Testing and debugging.** Codex generated algorithm tests, Mockito service tests, real HTTP integration tests, and storage/authentication tests. It ran Maven verification and fixed integration problems. I also ran the checks in IntelliJ and manually exercised create, cast, all three result methods, and 400/401/404 cases. The combined local project passed 87 test executions, Checkstyle with zero violations, and 103/106 branches covered (97.17%). Those totals include teammates' tests; they are not all my own tests or a final tagged-release report.

5. **Review and submission.** I inspected test outputs and API responses and asked questions about the implementation. Codex helped draft commit/PR descriptions and guided me through staging, committing, pushing, and resolving conflicts. I performed the GitHub submission steps. Gabe approved PR #19. The AI attribution follow-up was merged through PR #21. This describes my actual review process; it does not claim that I independently reviewed every line of generated code.

## AI-generated code

The independent results classes use the following annotation:

“AI-generated with OpenAI Codex; integration tested by Haoran (@lek1ma).”

Paths below are relative to `src/main/java/com/lecoders/poll/` unless otherwise specified.

| File | What was generated or adapted |
|---|---|
| `results/CountingAlgorithms.java` | Three algorithms, ranking validation, deterministic tie-breaking, round and exhaustion handling |
| `results/CountingMethod.java` | Supported method values and parsing |
| `results/CountResult.java` | Immutable counting and round result records |
| `results/ResultsService.java` | Poll lookup, counting orchestration, response structure, and error mapping |
| `results/ResultsController.java` | GET endpoint and authentication integration |
| `results/RankedBallotStore.java` | Ballot-reading interface |
| `results/RepositoryRankedBallotStore.java` | Adapter reading the team's repository without renumbering option IDs |
| `ApiKeyValidator.java` | Shared configured-key validation |
| `Poll.java` | Defensive copy of options and zero-based `optionIds()` |
| `Ballot.java` | Defensive copy of rankings |
| `BallotRepository.java` | Snapshot reading and synchronization changes to save/delete methods |
| `PollController.java` | Shared-key wiring, option-ID response adaptation, and validation integration |
| `src/test/java/com/lecoders/poll/results/CountingAlgorithmsTest.java` | Algorithm tests |
| `src/test/java/com/lecoders/poll/results/ResultsServiceTest.java` | Mocked service tests |
| `src/test/java/com/lecoders/poll/results/ResultsHttpTest.java` | HTTP create/cast/count tests, errors, and delete/results isolation |
| `src/test/java/com/lecoders/poll/results/StorageAndAuthTest.java` | Snapshot, option-order, and key-validation tests |
| `src/test/java/com/lecoders/poll/ballot/CastBallotControllerTest.java` | Adaptation of the team's test configuration to shared key validation |
| `src/test/java/com/lecoders/poll/poll/CreatePollControllerTest.java` | Shared-validator import; original test scenarios came from PR #17 |
| `src/test/java/com/lecoders/poll/delete/DeletePollControllerTest.java` and `DeletePollFlowTest.java` | Shared-validator imports; original delete tests came from PR #18 |

An initial temporary `InMemoryRankedBallotStore` was also generated during development and removed when results were connected to the shared repository.

This report was drafted with Codex.

## Tokens used

| Token type | Count |
|---|---:|
| Input, not cached | 457,750 |
| Cached input | 29,514,752 |
| Output | 81,788 |
| **Total** | **30,054,290** |



