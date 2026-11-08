# Run guide

> For: human · Owner: Team lead

Every step from the customer's requirements to the released product. You talk and answer; the agent does the rest.

## Prerequisites

- **Claude Code CLI**, installed and signed in: `claude --version` must work in a terminal. The runner starts every session through it. (Using another agent instead, for example Codex: set `Agent` in `01_input/01_project/00_setup/run-config.md` and install and sign in to that CLI.)
- **Node 18 or newer** (`node --version`) and **git** (`git --version`).
- **Docker** or the other runtimes the project will use. `/preflight` tells you exactly which tools and versions are missing; install them when it asks.
- **Internet access** during the run (dependency downloads, vulnerability databases, the browser used for screenshots).
- **Everything the customer gave you**: documents, images, e-mails, notes (PDF, images, Markdown or plain text; convert Word files to PDF first).

## Steps

### 1. Create the project folder

1. Copy the whole `00_Template` folder to `01_Konkretne_Implementacije_Projektov/<nn_Project-name>`. Never work in `00_Template` itself.
2. Put everything the customer gave you into `<project>/01_input/01_project/00_intake/customer_requirement/`. Only customer material goes there; the agent treats every file in it as coming from the customer.

### 2. Interview: `/grill-me`

1. Open a terminal in the project folder and start Claude Code (`claude`), or open a new session on that folder in the Claude app.
2. Type `/grill-me`. It reads the whole `customer_requirement/` folder first.
3. Answer one question at a time. The agent suggests an answer each time:
   - answer what you know;
   - "you decide" accepts its suggestion (not for price, rights, personal data, security, legal or cancellation topics);
   - "I don't know" leaves the question open.
4. At the end it lists the open questions. Send them to the customer, then open a new session and type `/grill-me resume` to add the answers.
5. The interview is done when the agent says the next command is `/to-spec`. Close the session.

### 3. Start the run: `/run`

Open a **new** session in the project folder and type `/run`. Keep this session open until the run ends. It first prepares the run with you:

1. It checks the prerequisites and stops with a message if one is missing.
2. It creates the git repository (if there is none) and generates all project inputs from the interview.
3. **Review questions**, five at a time, each with a suggested answer. Reply per question, or "ok" to accept all five.
4. **Run settings**: run name, design round (`ask`: you design the look with the agent later; `skip`: a default modern design), and the commands the agent may run without asking. Confirm or correct them.
5. **Secrets**: it lists the keys. Copy `01_input/01_project/00_setup/secrets.env.example` to `.env` in the project folder, fill in the values **in an editor**, never in the chat, and reply "done".
6. It commits the inputs. From now on, do not edit any file in the project folder.

### 4. During the run

The agent works through the sessions on its own and reports each one in one line. You only act when it asks:

- **Decisions**: numbered options with a suggested one. Reply with the numbers, for example `D-03: 2, D-04: 1`, or "ok" for the suggestions. You are asked only about business consequences, missing tools or versions, and approvals.
- **Missing tools**: install what it names, then reply "done".
- **Wireframes**: grey drawings of every screen for mobile, tablet and desktop in `02_output/docs/wireframes/` (the `*_mobile.png`, `*_tablet.png`, `*_desktop.png` files). Show them to the customer if you can, then reply `1` (approved with the customer), `2` (approved by you only) or `3` with the changes per screen, for example `3: WF-03 move filters to the left`.
- **After the first version (MVP)**: whether the customer has seen the grey application, and their remarks.
- **Design** (only with design round `ask`): when every story is built, the application looks like the wireframes, complete but grey. Open a **second** session in the project folder, type `/design`, and answer its questions (colours, typeface, mobile, tablet and desktop layout, motion); after each change it shows before and after and asks if it is right. When it is finished, reply "done" in the `/run` session.
- **A session failed**: it shows the error. Reply "again" to retry or "stop".
- **Verification rounds**: after three rounds of fixes it asks you to accept, change or inspect the remaining problem.

Answer within about an hour. If you must stop, close the session; nothing is lost.

### 5. Continue after a stop

Open a new session in the project folder and type `/run resume`. It continues exactly where the run stopped.

### 6. After the run

When the run reports that it is complete:

1. Read `02_output/docs/release-notes.md` and run every manual test it lists.
2. Check every decision marked "pending review" in `02_output/docs/decisions-log.md`; tell the customer about those that affect them.
3. Read `02_output/docs/07_retrospective.md` (the run's numbers and what to improve next time).
4. The product is in `02_output/`; how to build, run and deploy it is in `02_output/README.md`.
5. Optional: push the repository to a remote, on a branch of its own:
   ```bash
   git remote add origin <repository URL>
   git push -u origin <branch>
   ```
   Before pushing, check that `git status` does not list `.env`.

## Rules for the whole run

- One `/run` session per project at a time; the only second session is `/design` when asked.
- Never paste a secret into the chat; secrets go only into `.env`.
- Do not edit inputs, tests or code by hand during the run; changes go through your answers.
- To change the agent, model or effort of a session, edit `01_input/01_project/00_setup/run-config.md` before step 3.6 (the commit of the inputs).
