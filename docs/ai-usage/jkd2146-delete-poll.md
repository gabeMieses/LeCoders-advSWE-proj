# AI usage: Delete poll endpoint (Jason Dixon, jkd2146)

Task: issue #5, `DELETE /polls/{pollId}`, merged in PR #18.

## Tool

| | |
|---|---|
| Tool | Claude Code (Anthropic), VS Code extension |
| Model | Claude Opus 5.5 (`claude-opus-5-5`) |
| Where we got it | <https://claude.com/claude-code>, signed in with a personal Claude account |
| Date used | 2026-10-26, one session |

## How I used it

1. **Planning.** I gave Claude the assignment description, the rubric, and our proposal. I asked it to look at the repo's current state and the GitHub issue assigned to me (#5), and to plan the work before writing any code. It summarized what was already on main, listed the open issues and who owned them, and proposed a plan for the delete endpoint: check order, response codes, repository methods and tests.
2. **Updating the plan.** PR #17 (create-poll follow-up) was merged while I was planning. I asked Claude to look at the new commit first. It adjusted the plan to reuse the API key check that #17 added.
3. **Splitting the work into steps.** I asked Claude to implement the task as separate commits, one per step:
   1. Move the API key check into a shared helper (no behavior change)
   2. Add the repository delete methods, with unit tests
   3. Add the `DELETE /polls/{pollId}` endpoint, with controller unit tests that use Mockito mocks
   4. Add end-to-end tests with the real repositories
   5. Make cast ballot reject unknown API keys (a gap I found in the existing code)
   6. Mark the AI-generated code
4. **Working through each step.** For each step, Claude wrote the code and tests and ran `mvn verify` (tests, Checkstyle and JaCoCo). I reviewed the result before it was committed under my name. I made the decisions:
   - only the poll's creator can delete it
   - 401 rather than 403 for a non-owner, to match our proposal
   - each step as its own commit
   - the cast ballot fix kept in a separate commit so the endpoint's owner could review it
5. **PR.** Claude drafted the PR description. Gabe reviewed and approved PR #18 before it was merged.

## Prompts

These are the main prompts I gave Claude:

1. *(with the assignment screenshots and the proposal PDF attached)* "Here is the current project, I don't want you to write code yet, but look at our current status and if you can, check for the task assigned to me on git and let's plan what to do"
2. "there was a new commit, look at it first"
3. "Yeah let's begin the implementation for what was assigned to me please, also remember that I do not want you to allocate yourself as a contributor on here. Let's try and have this into separate commits"
4. "Alright let's go ahead and continue on"
5. "what should be my PR description"
6. "is my part done? I also need to document as to how I used AI ..."

## AI-generated code

All code and tests from this task were AI-generated and then reviewed by me. Each one is marked in the source with a comment or Javadoc line: "AI-generated with Claude Code (Opus 5.5); reviewed and tested by Jason Dixon."

| File | What was generated |
|---|---|
| `src/main/java/com/lecoders/poll/PollController.java` | `deletePoll` endpoint, `requireValidApiKey` helper, cast ballot key check |
| `src/main/java/com/lecoders/poll/PollRepository.java` | `deleteById` |
| `src/main/java/com/lecoders/poll/BallotRepository.java` | `deleteByPollId`, `voteKey` |
| `src/test/java/com/lecoders/poll/delete/DeletePollControllerTest.java` | whole file (8 tests) |
| `src/test/java/com/lecoders/poll/delete/DeletePollFlowTest.java` | whole file (4 tests) |
| `src/test/java/com/lecoders/poll/poll/PollRepositoryTest.java` | 3 `deleteById` tests |
| `src/test/java/com/lecoders/poll/ballot/BallotRepositoryTest.java` | 3 `deleteByPollId` tests |
| `src/test/java/com/lecoders/poll/ballot/CastBallotControllerTest.java` | `castBallot_unknownApiKey_returns401` |

This doc was also drafted with Claude.

## Tokens used

Counted from Claude Code's session log. It covers the whole session: planning, implementation, PR description and this doc.

| Token type | Count |
|---|---|
| Input (not cached) | 62 |
| Output | 29,989 |
| Cache write | 224,150 |
| Cache read | 3,233,729 |
| **Total** | **3,487,930** |

Model calls: 31, all on Claude Opus 5.5. Most of the total is cache reads: on every call the model re-reads the conversation so far, and that reread is billed at a lower cache rate.
