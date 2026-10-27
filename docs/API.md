# API documentation — LeCoders poll-service

Complete reference for the poll service's HTTP API: authentication, the error body, every
endpoint, the order calls must be made in, and worked examples of correct use and misuse.

Every example below is real output captured from a locally started service. To start one,
see [Development](../README.md#development) in the README; the base URL is then
`http://localhost:8080`.

## Contents

- [Authentication](#authentication)
  - [Where valid keys are configured](#where-valid-keys-are-configured)
  - [Adding a key](#adding-a-key)
  - [Missing or invalid keys](#missing-or-invalid-keys)
  - [Which endpoints require a key, and what ownership means](#which-endpoints-require-a-key-and-what-ownership-means)
- [Error format](#error-format)
  - [Where `message` comes from](#where-message-comes-from)
- [API reference](#api-reference)
  - [1. Create a poll](#1-create-a-poll) — `POST /polls`
  - [2. Cast a ballot](#2-cast-a-ballot) — `POST /polls/{pollId}/ballots`
  - [3. Get results](#3-get-results) — `GET /polls/{pollId}/results`
    - [The three counting methods](#the-three-counting-methods)
    - [Ties](#ties)
    - [When there are no ballots](#when-there-are-no-ballots)
  - [4. Delete a poll](#4-delete-a-poll) — `DELETE /polls/{pollId}`
- [Call order rules](#call-order-rules)
- [Example usage scenarios](#example-usage-scenarios)
  - [Scenario A — a lunch poll counted three ways, then deleted](#scenario-a--a-lunch-poll-counted-three-ways-then-deleted)
  - [Scenario B — a ranked-choice poll that needs two rounds](#scenario-b--a-ranked-choice-poll-that-needs-two-rounds)
- [Misuse examples](#misuse-examples)
  - [1. No API key — `401`](#1-no-api-key--401)
  - [2. Unrecognised API key — `401`](#2-unrecognised-api-key--401)
  - [3. A poll with fewer than two options — `400`](#3-a-poll-with-fewer-than-two-options--400)
  - [4. An option id belonging to a different poll — `400`](#4-an-option-id-belonging-to-a-different-poll--400)
  - [5. The same option ranked twice — `400`](#5-the-same-option-ranked-twice--400)
  - [6. The same `voter_id` voting twice — `400`](#6-the-same-voter_id-voting-twice--400)
  - [7. An unsupported counting method — `400`](#7-an-unsupported-counting-method--400)
  - [8. Deleting another client's poll — `401`](#8-deleting-another-clients-poll--401)
  - [9. Using a deleted poll — `404`](#9-using-a-deleted-poll--404)
  - [10. A non-numeric poll id — `400`](#10-a-non-numeric-poll-id--400)
  - [11. A body with no `Content-Type` — `415`](#11-a-body-with-no-content-type--415)
  - [12. The wrong HTTP method — `405`](#12-the-wrong-http-method--405)
  - [Not covered: voting on a closed poll](#not-covered-voting-on-a-closed-poll)

---

## Authentication

Every endpoint is authenticated with an API key sent in the **`X-API-Key`** request
header. The key identifies the **client program** calling the service, not a person.
Who is voting is a separate, client-supplied value: the `voter_id` field in a ballot body.
Two different people voting through the same client program send the same `X-API-Key`
but different `voter_id`s.

### Where valid keys are configured

Accepted keys come from the **`poll.api-keys`** property, a comma-separated list, defined in
`src/main/resources/application.properties`:

```properties
poll.api-keys=demo-key-1,demo-key-2
```

`ApiKeyValidator` splits that value on commas, trims each entry and drops empty ones.
Tests use their own list in `src/test/resources/application.properties`
(`test-key-client-a,test-key-client-b`), which must stay in sync with
`src/test/java/com/lecoders/poll/support/MockApiKeys.java`.

### Adding a key

Either add it to the list in `application.properties`, or override the whole list at run
time without editing the file:

```bash
POLL_API_KEYS=key-for-team-a,key-for-team-b mvn spring-boot:run
# or
java -Dpoll.api-keys=key-for-team-a,key-for-team-b -jar target/poll-service-0.1.0.jar
```

The override **replaces** the list in the properties file rather than adding to it: started
with `POLL_API_KEYS=env-key-only`, the service accepts `env-key-only` and rejects
`demo-key-1` with 401.

### Missing or invalid keys

The check is exact string matching against the configured list, and it **fails closed**:

| Request | Result |
|---|---|
| No `X-API-Key` header | 401 |
| `X-API-Key` that is not in the configured list | 401 |
| `X-API-Key` that is only whitespace | 401 |
| Any key at all, when `poll.api-keys` is empty or unset | 401 — no client can do anything |

### Which endpoints require a key, and what ownership means

All four do: `POST /polls`, `POST /polls/{pollId}/ballots`,
`GET /polls/{pollId}/results` and `DELETE /polls/{pollId}`.

The key that creates a poll **owns** it, and that ownership only restricts deletion:

| Action | Who may do it |
|---|---|
| Create a poll | any configured key |
| Cast a ballot on a poll | any configured key, including keys that did not create the poll |
| Read a poll's results | any configured key |
| Delete a poll | **only the key that created it**; any other valid key gets 401 |

The key check runs **before** anything else, so an invalid key never reveals whether a poll
exists and never changes stored data.

---

## Error format

Every error is the Spring Boot default error body: five fields, including a
human-readable `message` explaining what was wrong.

```json
{"timestamp":"2026-10-27T01:35:55.739+00:00","status":401,"error":"Unauthorized","message":"Invalid or missing API key","path":"/polls"}
```

| Field | Meaning |
|---|---|
| `timestamp` | When the error was produced, ISO-8601 with offset |
| `status` | The HTTP status code, repeated in the body |
| `error` | The standard reason phrase for that status |
| `message` | Why the request was rejected. See the table below for where the text comes from |
| `path` | The request path |

The `message` field only appears because `src/main/resources/application.properties` sets
`server.error.include-message=always`. Spring Boot hides it by default, which would leave
several distinct causes of the same status code indistinguishable.

### Where `message` comes from

| Source | Example text | Notes |
|---|---|---|
| The reason passed to a `ResponseStatusException` in the service's own code | `voter_id is required`, `voter dup has already voted on this poll`, `unknown option id 4`, `only the client program that created the poll may delete it` | Written for clients, and the most useful case to branch on |
| Spring's request handling, before any handler runs | `Method 'PUT' is not supported.`, `Content-Type 'application/x-www-form-urlencoded;charset=UTF-8' is not supported.`, `JSON parse error: Unexpected end-of-input within/between Object entries` | Framework wording, so it can change when Spring Boot is upgraded |
| Spring's path-variable binding | `Method parameter 'pollId': Failed to convert value of type 'java.lang.String' to required type 'long'; For input string: "abc"` | Mentions internal types; do not show it to end users |

Two cautions when reading `message`:

- **It is not a stable API.** Nothing pins the wording, and the framework-generated
  messages in particular are tied to the Spring Boot version. Branch on `status` plus the
  field you sent; treat `message` as diagnostic text for logs and humans.
- **The same condition can be worded differently by different endpoints.** An unknown poll
  is `Unknown poll_id` from `GET /polls/{pollId}/results` but `unknown poll_id 4` from the
  cast-ballot and delete endpoints, and a missing `ranked_option_ids` gives the same
  `ranked_option_ids must contain at least one option` as an empty one.

---

## API reference

Base URL for a locally started service: `http://localhost:8080`.

Examples below use `demo-key-1` and `demo-key-2`, the defaults in
`application.properties`. The ids in the responses are real output from a local run;
your ids will differ because they are assigned sequentially from `1` on each start.

---

### 1. Create a poll

`POST /polls`

Creates a poll owned by the calling client program and returns the ids a client needs
for every later call.

**Headers**

| Header | Required | Value |
|---|---|---|
| `X-API-Key` | yes | A configured API key |
| `Content-Type` | yes | `application/json` (415 without it) |

**Path and query parameters:** none.

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `question` | string | yes | Must not be missing, `null`, empty or only whitespace |
| `options` | array of string | yes | At least 2 entries; every entry non-blank; no duplicates (exact string match) |
| `counting_method` | string | yes | Exactly one of `plurality`, `ranked_choice`, `borda` (case-sensitive) |

Unknown fields are ignored.

**Example request**

```bash
curl -s -X POST http://localhost:8080/polls \
  -H 'X-API-Key: demo-key-1' \
  -H 'Content-Type: application/json' \
  -d '{"question":"What should we eat for the team lunch?","options":["Tacos","Pizza","Sushi"],"counting_method":"plurality"}'
```

**Example success response** — `201 Created`

```json
{"option_ids":[0,1,2],"poll_id":2}
```

`option_ids` are the zero-based positions of `options`, so `0` is `Tacos`, `1` is `Pizza`
and `2` is `Sushi`. These are the only option identifiers the rest of the API accepts.

**Side effects**

- Stores one poll holding the question, the option labels, the calling API key as owner,
  and the requested counting method. Nothing else is written.
- The poll id comes from a counter that starts at `1` per service start and is never reused.

**Status codes**

| Code | When |
|---|---|
| 201 | Poll created |
| 400 | `question` missing or blank; `options` missing, fewer than 2 entries, containing a blank or `null` entry, or containing duplicates; `counting_method` missing, blank or not one of the three supported values; or the body is not parseable JSON |
| 401 | `X-API-Key` missing, whitespace-only or not in the configured list |
| 405 | Another method used on `/polls`, for example `GET` |
| 415 | Body sent without `Content-Type: application/json` |

---

### 2. Cast a ballot

`POST /polls/{pollId}/ballots`

Records one voter's ranked preferences on a poll. A ranking may be **partial**: it does
not have to list every option.

**Headers**

| Header | Required | Value |
|---|---|---|
| `X-API-Key` | yes | Any configured key; it does not have to be the poll's creator |
| `Content-Type` | yes | `application/json` (415 without it) |

**Path and query parameters**

| Name | Type | Required | Rules |
|---|---|---|---|
| `pollId` | integer (path) | yes | Must parse as a number (400 otherwise) and name an existing poll (404 otherwise) |

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `voter_id` | string | yes | Must not be missing, `null`, empty or only whitespace. Must not have already voted on **this** poll |
| `ranked_option_ids` | array of integer | yes | At least one entry, ordered most preferred first. Every entry must be non-`null` and in `0 .. optionCount-1` for this poll. No entry may repeat. May omit options (partial ranking allowed) |

**Example request**

```bash
curl -s -X POST http://localhost:8080/polls/4/ballots \
  -H 'X-API-Key: demo-key-1' \
  -H 'Content-Type: application/json' \
  -d '{"voter_id":"alice","ranked_option_ids":[0,1,2]}'
```

**Example success response** — `201 Created`

```json
{"ballot_id":1}
```

**Side effects**

- Stores one ballot with a new id, the poll id, the `voter_id` and a frozen copy of the ranking.
- Records the pair `(pollId, voter_id)` so the same voter cannot vote on this poll again.
- A **rejected** ballot stores nothing, so a voter whose ballot was refused can still cast a
  valid one afterwards.

**Status codes**

| Code | When |
|---|---|
| 201 | Ballot stored |
| 400 | `pollId` is not a number; `voter_id` missing or blank; `ranked_option_ids` missing or empty; any entry `null`, negative, or `>=` the poll's option count; an option id listed twice; this `voter_id` already voted on this poll; or the body is not parseable JSON |
| 401 | `X-API-Key` missing, whitespace-only or not in the configured list |
| 404 | No poll has this `pollId`, including a poll that was deleted |
| 405 | Another method used on the ballots path, for example `DELETE` |
| 415 | Body sent without `Content-Type: application/json` |

---

### 3. Get results

`GET /polls/{pollId}/results`

Counts the ballots stored for a poll using the method named in the query string. This is a
pure read: it never changes stored data, and it can be called while voting is still open.

**Headers**

| Header | Required | Value |
|---|---|---|
| `X-API-Key` | yes | Any configured key; it does not have to be the poll's creator |

**Path and query parameters**

| Name | Type | Required | Rules |
|---|---|---|---|
| `pollId` | integer (path) | yes | Must parse as a number (400 otherwise) and name an existing poll (404 otherwise) |
| `counting_method` | string (query) | yes | Exactly one of `plurality`, `ranked_choice`, `borda`. Case-sensitive; there is no default. Validated **before** the poll is looked up |

The poll's own stored `counting_method` is ignored here; only this query parameter decides
how ballots are counted.

**Request body:** None.

**Example request**

```bash
curl -s -X GET 'http://localhost:8080/polls/4/results?counting_method=borda' \
  -H 'X-API-Key: demo-key-1'
```

**Example success response** — `200 OK`

```json
{"poll_id":4,"counting_method":"borda","winning_option_id":1,"counts":{"0":4,"1":8,"2":3},"total_ballots":5,"rounds":[]}
```

| Field | Meaning |
|---|---|
| `poll_id` | The poll that was counted |
| `counting_method` | The method actually applied, echoed back |
| `winning_option_id` | The winning option id, or `null` when there is no winner |
| `counts` | Option id to score. **Votes** for plurality and ranked choice, **points** for Borda. JSON object keys are strings |
| `total_ballots` | How many ballots were counted, including exhausted ones |
| `rounds` | Ranked-choice rounds, in order. Always `[]` for plurality and Borda |

The key order inside `counts` is not significant and varies between runs of the service, so
read it as a map rather than relying on the order shown in the examples below.

Each entry in `rounds` has `round` (1-based), `counts` for the options still active at the
start of that round, `active_ballots`, `exhausted_ballots`, and `eliminated_option_id`
(`null` in the final round).

#### The three counting methods

**Plurality.** Only each ballot's first choice is counted; everything ranked below it is
ignored. Every option in the poll appears in `counts`, with `0` for options that no ballot
ranked first. The option with the most first-choice votes wins.

**Ranked choice (instant runoff).** Counting proceeds in rounds. In each round every ballot
is assigned to its highest-ranked option that is still active. A ballot whose every ranked
option has been eliminated is *exhausted*: it is reported in `exhausted_ballots`, drops out
of `active_ballots`, and stops affecting the majority threshold. An option wins as soon as
its count is **strictly more than half** of `active_ballots`; otherwise the option with the
fewest votes is eliminated and another round runs. Because eliminated options are dropped,
the top-level `counts` is the **final round's** counts and omits options eliminated earlier
— use `rounds` to see the full picture.

**Borda count.** Every ballot awards `N - 1 - position` points to each option it ranks,
where `N` is the number of options **in the poll** and `position` is the option's 0-based
place on that ballot. With 3 options a first choice earns 2 points, a second 1, a third 0.
Options a ballot leaves unranked get nothing from that ballot, so partial rankings award
fewer total points. The option with the most points wins.

#### Ties

Ties are broken deterministically by option id, never by input order or insertion order:

- **Winning a round or a count:** among options with the equal highest score, the **lowest**
  option id wins. With one vote for option `0` and one for option `1`, plurality reports
  `{"0":1,"1":1}` and `winning_option_id: 0`.
- **Being eliminated in ranked choice:** among options with the equal lowest count, the
  **highest** option id is eliminated, which preserves the lower-id preference above.

#### When there are no ballots

For all three methods, a poll with no ballots returns `200` with no winner rather than an
error:

```json
{"poll_id":6,"counting_method":"ranked_choice","winning_option_id":null,"counts":{"0":0,"1":0},"total_ballots":0,"rounds":[]}
```

`winning_option_id` is `null`, every option is present in `counts` with `0`,
`total_ballots` is `0`, and `rounds` is empty even for ranked choice.

**Side effects:** none. Nothing is stored, updated or deleted.

**Status codes**

| Code | When |
|---|---|
| 200 | Results computed, including for a poll with no ballots |
| 400 | `counting_method` missing, empty or not one of the three supported values (checked first, so this wins even when the poll does not exist); or `pollId` is not a number |
| 401 | `X-API-Key` missing, whitespace-only or not in the configured list |
| 404 | No poll has this `pollId`, including a poll that was deleted |
| 405 | Another method used on the results path, for example `POST` |
| 500 | Stored ballot data is inconsistent with the poll's options. Not reachable through the public API in iteration 1, because cast ballot validates every ranking; covered by a mocked unit test |

---

### 4. Delete a poll

`DELETE /polls/{pollId}`

Deletes a poll and every ballot cast on it. Only the client program that created the poll
may delete it.

**Headers**

| Header | Required | Value |
|---|---|---|
| `X-API-Key` | yes | Must be the **same key that created the poll** |

**Path and query parameters**

| Name | Type | Required | Rules |
|---|---|---|---|
| `pollId` | integer (path) | yes | Must parse as a number (400 otherwise) and name an existing poll (404 otherwise) |

**Request body:** None.

**Example request**

```bash
curl -s -X DELETE http://localhost:8080/polls/4 \
  -H 'X-API-Key: demo-key-1'
```

**Example success response** — `200 OK`

```json
{"poll_id":4,"ballots_deleted":5}
```

**Side effects**

- Removes the poll, then removes every ballot cast on it, reporting the count as
  `ballots_deleted`. The poll is removed **first**, so a concurrent cast-ballot call gets
  404 while the ballots are still being cleared.
- Also forgets which voters voted on that poll.
- Other polls and their ballots are untouched.
- The deletion is permanent; poll ids are never reused.
- A rejected delete (401 or 404) removes nothing.

**Status codes**

| Code | When |
|---|---|
| 200 | Poll and its ballots deleted |
| 400 | `pollId` is not a number |
| 401 | `X-API-Key` missing, whitespace-only or not in the configured list, **or** a valid key that did not create this poll |
| 404 | No poll has this `pollId`, including one already deleted |
| 405 | Another method used on this path, for example `PUT` |

---

## Call order rules

1. **Create the poll first.** `POST /polls` is the only call that needs no existing poll, and
   its response is the only place a client learns the `poll_id` and `option_ids`.
2. **Keep the `option_ids`.** Option ids are positions within *that poll's* `options` array.
   Nothing echoes the option labels back, and there is no endpoint to read a poll, so a
   client that discards the create response cannot recover which label `0` meant.
3. **Ballots only after the poll exists.** `POST /polls/{pollId}/ballots` returns 404 for an
   unknown `pollId`.
4. **Option ids must be in range for the poll being voted on.** An id that is valid for a
   different, larger poll is rejected with 400 only because it is out of range here. An id
   that happens to be in range for both polls is accepted silently, as a vote for whatever
   that position means in *this* poll.
5. **One ballot per `voter_id` per poll.** A second ballot from the same `voter_id` on the
   same poll is 400. The same `voter_id` may vote on a *different* poll. A ballot that was
   rejected does not consume the voter's single vote.
6. **Results may be read at any time** — before any ballots are cast, during voting, and by
   any valid key. There is no need to close or finalise a poll first, and there is currently
   no way to close one.
7. **Delete last.** After a successful `DELETE`, every later call on that poll returns 404:
   casting a ballot, reading results under any counting method, and deleting it again.
8. **Only the creating key can delete.** A delete attempted by another valid key returns 401
   and leaves the poll fully usable, so ballots can still be cast on it afterwards.
9. **Restarting the service clears everything.** Storage is in memory, so all polls and
   ballots disappear and ids restart at `1`.

---

## Example usage scenarios

Both scenarios are real runs against a locally started service; the responses are copied
from that run.

### Scenario A — a lunch poll counted three ways, then deleted

**Step 1. Create the poll.**

```bash
curl -s -X POST http://localhost:8080/polls \
  -H 'X-API-Key: demo-key-1' -H 'Content-Type: application/json' \
  -d '{"question":"What should we eat for the team lunch?","options":["Tacos","Pizza","Sushi"],"counting_method":"plurality"}'
```

```json
{"option_ids":[0,1,2],"poll_id":4}
```

So `0` = Tacos, `1` = Pizza, `2` = Sushi.

**Step 2. Cast five ballots.** Each returns `201` with a new `ballot_id`.

```bash
for b in '{"voter_id":"alice","ranked_option_ids":[0,1,2]}' \
         '{"voter_id":"bob","ranked_option_ids":[1,0,2]}' \
         '{"voter_id":"carol","ranked_option_ids":[1,2,0]}' \
         '{"voter_id":"dave","ranked_option_ids":[2,1,0]}' \
         '{"voter_id":"erin","ranked_option_ids":[1,0,2]}'; do
  curl -s -X POST http://localhost:8080/polls/4/ballots \
    -H 'X-API-Key: demo-key-1' -H 'Content-Type: application/json' -d "$b"
  echo
done
```

```json
{"ballot_id":1}
{"ballot_id":2}
{"ballot_id":3}
{"ballot_id":4}
{"ballot_id":5}
```

**Step 3. Read results under each method.**

```bash
curl -s -H 'X-API-Key: demo-key-1' 'http://localhost:8080/polls/4/results?counting_method=plurality'
curl -s -H 'X-API-Key: demo-key-1' 'http://localhost:8080/polls/4/results?counting_method=borda'
curl -s -H 'X-API-Key: demo-key-1' 'http://localhost:8080/polls/4/results?counting_method=ranked_choice'
```

```json
{"poll_id":4,"counting_method":"plurality","winning_option_id":1,"counts":{"0":1,"1":3,"2":1},"total_ballots":5,"rounds":[]}
{"poll_id":4,"counting_method":"borda","winning_option_id":1,"counts":{"0":4,"1":8,"2":3},"total_ballots":5,"rounds":[]}
{"poll_id":4,"counting_method":"ranked_choice","winning_option_id":1,"counts":{"0":1,"1":3,"2":1},"total_ballots":5,"rounds":[{"round":1,"counts":{"0":1,"1":3,"2":1},"active_ballots":5,"exhausted_ballots":0,"eliminated_option_id":null}]}
```

Pizza (`1`) wins under all three. Plurality counts only first choices (3 of 5). Borda gives
Pizza 8 points of the 15 available (5 ballots × 2 points for a first choice). Ranked choice
needs just one round, because 3 of 5 active ballots is already more than half, so
`eliminated_option_id` is `null`.

**Step 4. Delete the poll.**

```bash
curl -s -X DELETE http://localhost:8080/polls/4 -H 'X-API-Key: demo-key-1'
```

```json
{"poll_id":4,"ballots_deleted":5}
```

**Step 5. Confirm it is gone.** Results, a new ballot, and a second delete all return 404:

```json
{"timestamp":"2026-10-27T01:35:56.030+00:00","status":404,"error":"Not Found","message":"Unknown poll_id","path":"/polls/4/results"}
{"timestamp":"2026-10-27T01:35:56.041+00:00","status":404,"error":"Not Found","message":"unknown poll_id 4","path":"/polls/4/ballots"}
{"timestamp":"2026-10-27T01:35:56.050+00:00","status":404,"error":"Not Found","message":"unknown poll_id 4","path":"/polls/4"}
```

### Scenario B — a ranked-choice poll that needs two rounds

**Step 1. Create the poll.**

```bash
curl -s -X POST http://localhost:8080/polls \
  -H 'X-API-Key: demo-key-1' -H 'Content-Type: application/json' \
  -d '{"question":"Which office snack should we stock?","options":["Chips","Fruit","Cookies"],"counting_method":"ranked_choice"}'
```

Returned `{"option_ids":[0,1,2],"poll_id":5}`, so `0` = Chips, `1` = Fruit, `2` = Cookies.

**Step 2. Cast five ballots, two of them partial rankings.**

```bash
for b in '{"voter_id":"v1","ranked_option_ids":[0,1]}' \
         '{"voter_id":"v2","ranked_option_ids":[0]}' \
         '{"voter_id":"v3","ranked_option_ids":[1]}' \
         '{"voter_id":"v4","ranked_option_ids":[1]}' \
         '{"voter_id":"v5","ranked_option_ids":[2,1]}'; do
  curl -s -X POST http://localhost:8080/polls/5/ballots \
    -H 'X-API-Key: demo-key-1' -H 'Content-Type: application/json' -d "$b"
  echo
done
```

Each returned `201` (`ballot_id` 6 through 10).

**Step 3. Read the ranked-choice result.**

```bash
curl -s -H 'X-API-Key: demo-key-1' 'http://localhost:8080/polls/5/results?counting_method=ranked_choice'
```

```json
{"poll_id":5,"counting_method":"ranked_choice","winning_option_id":1,"counts":{"0":2,"1":3},"total_ballots":5,"rounds":[{"round":1,"counts":{"0":2,"1":2,"2":1},"active_ballots":5,"exhausted_ballots":0,"eliminated_option_id":2},{"round":2,"counts":{"0":2,"1":3},"active_ballots":5,"exhausted_ballots":0,"eliminated_option_id":null}]}
```

Round 1 splits 2–2–1 with no option above half of 5, so Cookies (`2`), the lowest, is
eliminated. In round 2 `v5`'s second preference transfers to Fruit (`1`), giving it 3 of 5 —
more than half — so Fruit wins. Note the top-level `counts` shows only `0` and `1`, because
`2` was eliminated; round 1 in `rounds` still shows all three.

**Step 4. Compare with plurality on the same ballots.**

```bash
curl -s -H 'X-API-Key: demo-key-1' 'http://localhost:8080/polls/5/results?counting_method=plurality'
```

```json
{"poll_id":5,"counting_method":"plurality","winning_option_id":0,"counts":{"0":2,"1":2,"2":1},"total_ballots":5,"rounds":[]}
```

Plurality gives a different winner: Chips (`0`) and Fruit (`1`) tie on first choices at 2
each, and the tie goes to the lower option id. This is why `counting_method` is a required,
explicit parameter.

---

## Misuse examples

Every case below was run against the service; the status is what it actually returned.

### 1. No API key — `401`

```bash
curl -s -X POST http://localhost:8080/polls -H 'Content-Type: application/json' \
  -d '{"question":"Q?","options":["A","B"],"counting_method":"plurality"}'
```

```json
{"timestamp":"2026-10-27T01:35:55.739+00:00","status":401,"error":"Unauthorized","message":"Invalid or missing API key","path":"/polls"}
```

**Why:** `X-API-Key` is required on every endpoint, and the validator rejects a `null` key
before any other work happens.

### 2. Unrecognised API key — `401`

```bash
curl -s -X POST http://localhost:8080/polls -H 'X-API-Key: not-a-real-key' \
  -H 'Content-Type: application/json' \
  -d '{"question":"Q?","options":["A","B"],"counting_method":"plurality"}'
```

```json
{"timestamp":"2026-10-27T01:35:55.754+00:00","status":401,"error":"Unauthorized","message":"Invalid or missing API key","path":"/polls"}
```

**Why:** keys must match an entry in `poll.api-keys` exactly. A whitespace-only key fails the
same way.

### 3. A poll with fewer than two options — `400`

```bash
curl -s -X POST http://localhost:8080/polls -H 'X-API-Key: demo-key-1' \
  -H 'Content-Type: application/json' \
  -d '{"question":"Q?","options":["Only one"],"counting_method":"plurality"}'
```

```json
{"timestamp":"2026-10-27T01:35:55.776+00:00","status":400,"error":"Bad Request","message":"question, at least 2 distinct non-blank options, and a counting_method of [borda, plurality, ranked_choice] are required","path":"/polls"}
```

**Why:** an election needs at least two choices, so `options.size() < 2` is refused. Duplicate
options (`["Tacos","Tacos"]`) and blank options (`["A"," "]`) are refused for the same reason:
they would produce two option ids that mean the same thing, or one that means nothing.

### 4. An option id belonging to a different poll — `400`

Poll `8` has two options (ids `0`, `1`); a separate poll has five (ids `0`–`4`). Sending `4`
to poll `8`:

```bash
curl -s -X POST http://localhost:8080/polls/8/ballots -H 'X-API-Key: demo-key-1' \
  -H 'Content-Type: application/json' \
  -d '{"voter_id":"v1","ranked_option_ids":[4]}'
```

```json
{"timestamp":"2026-10-27T01:35:56.285+00:00","status":400,"error":"Bad Request","message":"unknown option id 4","path":"/polls/8/ballots"}
```

**Why:** option ids are validated against the target poll's own option count, so `4` is out of
range for a two-option poll. Note this is caught because the id is out of range, not because
the service knows it came from elsewhere: an id such as `1`, which is in range for both
polls, would be accepted as a vote for this poll's second option.

### 5. The same option ranked twice — `400`

```bash
curl -s -X POST http://localhost:8080/polls/8/ballots -H 'X-API-Key: demo-key-1' \
  -H 'Content-Type: application/json' \
  -d '{"voter_id":"v1","ranked_option_ids":[0,1,0]}'
```

```json
{"timestamp":"2026-10-27T01:35:56.296+00:00","status":400,"error":"Bad Request","message":"option id 0 is ranked more than once","path":"/polls/8/ballots"}
```

**Why:** a ranking is an ordering of distinct preferences. Ranking one option both first and
third is contradictory, and would let a single ballot count twice under Borda.

### 6. The same `voter_id` voting twice — `400`

```bash
# first ballot succeeds
curl -s -X POST http://localhost:8080/polls/8/ballots -H 'X-API-Key: demo-key-1' \
  -H 'Content-Type: application/json' -d '{"voter_id":"dup","ranked_option_ids":[0]}'
# second ballot from the same voter on the same poll
curl -s -X POST http://localhost:8080/polls/8/ballots -H 'X-API-Key: demo-key-1' \
  -H 'Content-Type: application/json' -d '{"voter_id":"dup","ranked_option_ids":[1]}'
```

```json
{"ballot_id":13}
{"timestamp":"2026-10-27T01:35:56.368+00:00","status":400,"error":"Bad Request","message":"voter dup has already voted on this poll","path":"/polls/8/ballots"}
```

**Why:** the repository records each `(pollId, voter_id)` pair and refuses a second ballot, so
one person cannot vote twice. The same `voter_id` *can* vote on a different poll.

### 7. An unsupported counting method — `400`

On create:

```bash
curl -s -X POST http://localhost:8080/polls -H 'X-API-Key: demo-key-1' \
  -H 'Content-Type: application/json' \
  -d '{"question":"Q?","options":["A","B"],"counting_method":"approval"}'
```

On results, including the right method in the wrong case:

```bash
curl -s -H 'X-API-Key: demo-key-1' 'http://localhost:8080/polls/8/results?counting_method=approval'
curl -s -H 'X-API-Key: demo-key-1' 'http://localhost:8080/polls/8/results?counting_method=PLURALITY'
curl -s -H 'X-API-Key: demo-key-1' 'http://localhost:8080/polls/8/results'
```

All four return `400`, for example:

```json
{"timestamp":"2026-10-27T01:35:56.415+00:00","status":400,"error":"Bad Request","message":"Unsupported counting_method","path":"/polls/8/results"}
```

**Why:** only `plurality`, `ranked_choice` and `borda` are implemented, matching is
case-sensitive, and the service refuses to guess a default when the parameter is missing.

### 8. Deleting another client's poll — `401`

Poll `10` was created with `demo-key-1`:

```bash
curl -s -X DELETE http://localhost:8080/polls/10 -H 'X-API-Key: demo-key-2'
```

```json
{"timestamp":"2026-10-27T01:35:56.527+00:00","status":401,"error":"Unauthorized","message":"only the client program that created the poll may delete it","path":"/polls/10"}
```

**Why:** `demo-key-2` is a valid key, so it passes authentication, but it is not the poll's
owner. Nothing is deleted and the poll stays usable. (`demo-key-2` *can* still cast ballots on
poll `10` and read its results — ownership only restricts deletion.)

### 9. Using a deleted poll — `404`

After `DELETE /polls/4` succeeded:

```bash
curl -s -X POST http://localhost:8080/polls/4/ballots -H 'X-API-Key: demo-key-1' \
  -H 'Content-Type: application/json' -d '{"voter_id":"frank","ranked_option_ids":[0]}'
curl -s -H 'X-API-Key: demo-key-1' 'http://localhost:8080/polls/4/results?counting_method=plurality'
curl -s -X DELETE http://localhost:8080/polls/4 -H 'X-API-Key: demo-key-1'
```

```json
{"timestamp":"2026-10-27T01:35:56.041+00:00","status":404,"error":"Not Found","message":"unknown poll_id 4","path":"/polls/4/ballots"}
{"timestamp":"2026-10-27T01:35:56.030+00:00","status":404,"error":"Not Found","message":"Unknown poll_id","path":"/polls/4/results"}
{"timestamp":"2026-10-27T01:35:56.050+00:00","status":404,"error":"Not Found","message":"unknown poll_id 4","path":"/polls/4"}
```

**Why:** deletion removes the poll from storage, and every endpoint looks the poll up before
doing anything. Ids are never reused, so a deleted poll is permanently 404.

### 10. A non-numeric poll id — `400`

```bash
curl -s -X DELETE http://localhost:8080/polls/abc -H 'X-API-Key: demo-key-1'
```

```json
{"timestamp":"2026-10-27T01:35:56.576+00:00","status":400,"error":"Bad Request","message":"Method parameter 'pollId': Failed to convert value of type 'java.lang.String' to required type 'long'; For input string: \"abc\"","path":"/polls/abc"}
```

**Why:** `pollId` is declared as a `long`, so Spring cannot bind `abc` and rejects the request
before the handler runs. The same applies to the ballots and results paths.

### 11. A body with no `Content-Type` — `415`

```bash
curl -s -X POST http://localhost:8080/polls -H 'X-API-Key: demo-key-1' \
  -d '{"question":"Q?","options":["A","B"],"counting_method":"borda"}'
```

```json
{"timestamp":"2026-10-27T01:35:55.860+00:00","status":415,"error":"Unsupported Media Type","message":"Content-Type 'application/x-www-form-urlencoded;charset=UTF-8' is not supported.","path":"/polls"}
```

**Why:** both POST endpoints consume JSON only, so the body cannot be parsed without
`Content-Type: application/json`.

### 12. The wrong HTTP method — `405`

```bash
curl -s -X GET  http://localhost:8080/polls     -H 'X-API-Key: demo-key-1'
curl -s -X PUT  http://localhost:8080/polls/1   -H 'X-API-Key: demo-key-1'
```

```json
{"timestamp":"2026-10-27T01:35:55.870+00:00","status":405,"error":"Method Not Allowed","message":"Method 'GET' is not supported.","path":"/polls"}
{"timestamp":"2026-10-27T01:35:56.589+00:00","status":405,"error":"Method Not Allowed","message":"Method 'PUT' is not supported.","path":"/polls/1"}
```

**Why:** only the four documented method-and-path combinations are mapped. There is no way to
list or update polls.

### Not covered: voting on a closed poll

The proposal's "no ballots after the closing time" misuse cannot be demonstrated, because
closing times are not implemented — see
[Changes from the proposal](../README.md#changes-from-the-proposal) in the README. A ballot
is accepted for as long as the poll exists.

