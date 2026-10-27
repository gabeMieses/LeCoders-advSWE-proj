# AI usage: Cast ballot endpoint (JB Diei)

Task: issue #4, `POST /polls/{pollId}/ballots`. The endpoint and its tests were merged in PR #16, and the first AI usage comments were merged in PR #20.

## Tool

| | |
|---|---|
| Tool | Claude Code (Anthropic), VS Code extension |
| Model | Claude Opus 5.5 (`claude-opus-5-5`) |
| Where we got it | <https://claude.com/claude-code>, signed in with a personal Claude account |
| Date used | 2026-10-26, one session |

## Primary prompts

1. Skeleton (I pasted our project proposal above this prompt):

   > Following these requirements, create skelton code for each of the api calls within @src/main/java/com/lecoders/poll/ApiController.java make sure it ollows proper java style and will pass mvn style checker and what not

2. Cast ballot implementation:

   > just fill in the casting ballots fuction stuff, the other ones are meant fo rother team members

3. Tests:

   > yes create tests, and create a specifc test folder with just ballot stuff. and make a separte file or folder or whatever would be optimal with mock api keys

## How I used it

1. **Setting up the branch.** My local repo hadn't fetched my teammates' branches. Claude fetched them and found Gabe's create-poll branch and the Checkstyle/JaCoCo tooling branch. I had it base my branch on create-poll and merge in the tooling branch, so my code would be checked against the team's real Checkstyle rules.
2. **Skeleton.** Claude added stub methods for cast ballot, results and delete to `PollController` (create-poll had replaced `ApiController`). It also reformatted the existing create-poll files to pass Checkstyle. I then deleted the results and delete stubs myself, because those endpoints belong to teammates.
3. **Cast ballot.** Claude implemented the endpoint, a `Ballot` record, an in-memory `BallotRepository`, and a `findById` method on `PollRepository`. It checks for:
   - 401 if the API key is missing
   - 404 if the poll doesn't exist
   - 400 for a missing voter, an empty ranking, unknown or duplicate option ids, or a voter who already voted

   Saving the ballot and the "already voted" check happen in one step, so two requests from the same voter can't both get through. Claude ran the app and checked each of these cases with curl.
4. **Tests.** Claude wrote the tests in `src/test/java/com/lecoders/poll/ballot/`. The fake API keys went in a shared `support/MockApiKeys.java` so teammates' tests could reuse them. Claude ran `mvn verify` (tests, Checkstyle and JaCoCo) until it passed.
5. **Build and git help.** JaCoCo printed errors on my JDK 25, so Claude installed JDK 17 (the project's Java version) and verified the build on it. I also used Claude to understand where my branch would merge to, and to fix diverged branches. In one case I had amended the wrong commit after a `git reset`; Claude rebuilt my branch on the latest `main` with my changes as one commit and kept a backup branch.

I made the design decisions:
- implement only cast ballot and leave the other endpoints to their owners
- ballot tests in their own `ballot/` folder, with mock API keys in a shared file
- option ids are zero-based positions in the poll's options list, matching create poll
- a voter can rank some options and leave others out
- build on JDK 17

Teammates later changed some of this code. Jason made cast ballot reject unknown API keys (PR #18), and Haoran moved the key check into `ApiKeyValidator` and made `Ballot` copy its ranking (PR #19). The "closed poll" check is still a TODO, because polls don't store a closing time yet.

## AI-generated code

All code and tests below were AI-generated, then reviewed and tested by me. In `PollController.java`, `Ballot.java` and the two test classes, the code is marked in the source with a comment or Javadoc line: "AI-generated with Claude Code (Opus 5.5); reviewed and tested by JB Diei." `saveIfFirstVote`, `findById` and `MockApiKeys` are not marked in the source yet.

| File | What was generated |
|---|---|
| `src/main/java/com/lecoders/poll/PollController.java` | `castBallot`, `CastBallotRequest`, `validateRanking` |
| `src/main/java/com/lecoders/poll/Ballot.java` | the original `Ballot` record (Haoran later added the defensive copy) |
| `src/main/java/com/lecoders/poll/BallotRepository.java` | the original class and `saveIfFirstVote` |
| `src/main/java/com/lecoders/poll/PollRepository.java` | `findById` |
| `src/test/java/com/lecoders/poll/support/MockApiKeys.java` | whole file |
| `src/test/java/com/lecoders/poll/ballot/BallotRepositoryTest.java` | the 5 `saveIfFirstVote` tests |
| `src/test/java/com/lecoders/poll/ballot/CastBallotControllerTest.java` | whole file except `castBallot_unknownApiKey_returns401` (Jason) and the shared-key test setup (Haoran) |

This doc was also drafted with Claude.

## Tokens used

Counted from Claude Code's session log. Most of each total is cache reads: every reply re-reads the conversation so far.

### Per prompt

| Work | Output | Total |
|---|---|---|
| Prompt 1: skeleton, including fetching branches and merging the tooling branch | 8,797 | 620,570 |
| Prompt 2: cast ballot implementation and curl checks | 7,305 | 269,790 |
| Prompt 3: tests | 7,973 | 394,407 |

### Rest of the session

| Work | Output | Total |
|---|---|---|
| Switching the build to JDK 17 | 2,564 | 422,989 |
| Git help: upstream branches, diverged branches, `.gitignore`, fixing the amended commit | 9,763 | 1,705,747 |
| This doc | 4,871 | 466,703 |

### Whole session

Counted up to when this doc was written.

| Token type | Count |
|---|---|
| Input (not cached) | 100 |
| Output | 41,273 |
| Cache write | 160,679 |
| Cache read | 3,678,154 |
| **Total** | **3,880,206** |
