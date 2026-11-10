# Install and backup guide

> For: operator of the customer (reads) · Delivered with the product (NFR-15, DoD-P04) · Source: `environments.md`, `docker-compose.prod.yml`, `scripts/backup.sh`, `scripts/restore.sh` · Agent: writes

This guide shows how to start Contact Groups (Skupine kontaktov) on your own server, how to keep it running, and how to back up and restore its data. Nobody is on call (NFR-05), so the setup restarts itself and the checks below are the only monitoring you need.

## 1. What you need

| Item | Notes |
|---|---|
| A server with Docker and Docker Compose v2 | Sized for up to 100 registered Users, 20 of them at the same time, and Groups of up to 10,000 Contacts (NFR-04). |
| A reverse proxy or load balancer with a TLS certificate | The product answers plain HTTP only inside your network and **refuses plain HTTP from outside** (`REQUIRE_HTTPS`). Your proxy ends TLS and forwards to the two local ports below (R-018-12). |
| A Firebase project | Sign-in (e-mail with password, Google) is Firebase Authentication. You need the project id, the web API key, the auth domain, and a service-account file (section 3). |
| The two images | `contact-groups-backend` and `contact-groups-frontend` (version 1.0.0), built as in section 2. |

The ports bind to `127.0.0.1` only: the backend on **8080**, the web application on **8081**. Point your proxy to `http://127.0.0.1:8081` for the page and `http://127.0.0.1:8080` for `/api/`, or serve both under one domain; set `CORS_ALLOWED_ORIGIN` to the address of the page (section 3).

## 2. Build the images

From the folder `02_output` of the delivered source:

```bash
docker build -t contact-groups-backend:1.0.0 backend
docker build -t contact-groups-frontend:1.0.0 frontend
```

The frontend image is the same for every environment: its address and Firebase values are read from the environment when the container starts.

## 3. Configure

Copy the values into a file `production.env` next to `docker-compose.prod.yml`. Keep it readable only by the operator (`chmod 600`), never commit it, never send it by e-mail.

| Name | Meaning |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Database name, its owner and the owner's password. The owner runs the schema migrations. |
| `DB_APP_PASSWORD` | Password of the restricted database user the application uses at run time. It must differ from `POSTGRES_PASSWORD`. |
| `FIREBASE_PROJECT_ID` | Id of your Firebase project. |
| `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN` | Web API key and auth domain of the same project (shown in the Firebase console, "Project settings"). |
| `CORS_ALLOWED_ORIGIN` | The address people type to open the page, for example `https://contacts.example.com`, with no path and no trailing slash. |
| `VITE_API_BASE_URL` | The address of the interface as the browser sees it, for example `https://contacts.example.com/api/v1`. |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path **on the host** of the Firebase service-account file (role "Firebase Authentication Admin"). It is needed to delete accounts and to read them (US-021). The backend refuses to start without it. The file must be readable by user id 10001, for example `chmod 0440` and group 10001. |

Optional names with their defaults: `RATE_LIMIT_PER_MINUTE` (100 requests per User and minute), `MAX_REQUEST_SIZE` (1 MB), `TRASH_PURGE_CRON` (the hourly clean-up of the Trash), `LOG_LEVEL` (`INFO`), `DB_APP_USER`. Leave `FIREBASE_AUTH_EMULATOR_HOST` empty: it is for tests only, and the product refuses to start in production when it is set.

## 4. Start

```bash
docker compose -f docker-compose.prod.yml --env-file production.env up -d
```

The first start creates the database schema and the restricted database user (Flyway). All three services are healthy after about ten seconds.

Check:

```bash
docker compose -f docker-compose.prod.yml --env-file production.env ps
curl -fsS http://127.0.0.1:8080/health
```

The second command must print `{"status":"UP"}`. It is the only unauthenticated address and shows nothing else.

## 5. Availability (NFR-05)

The target is 99 % per month. What keeps it there:

- `restart: unless-stopped` on every service: a crash or a server reboot restarts the product by itself (Docker must start at boot: `systemctl enable docker`).
- A health check in both application images: `docker compose ps` shows `healthy` or `unhealthy`. A backend that lost its database shows `unhealthy`; `docker compose -f docker-compose.prod.yml --env-file production.env restart backend` repairs most causes.
- Announce planned maintenance to your Users beforehand; an update (section 8) takes a minute or two.

Look at the logs when something is wrong: `docker compose -f docker-compose.prod.yml --env-file production.env logs --tail 200 backend`. They contain no passwords, tokens or personal data.

## 6. Backup (NFR-06)

The data lives in the Docker volume `contact-groups_pgdata`. The script `scripts/backup.sh` writes one dump of the database and removes dumps older than 14 days:

```bash
./scripts/backup.sh /var/backups/contact-groups
```

Run it once a day, for example with `crontab -e` (as the user that may run Docker):

```
15 2 * * *  /opt/contact-groups/scripts/backup.sh /var/backups/contact-groups >> /var/log/contact-groups-backup.log 2>&1
```

What the script does and promises:

- one file `contact-groups-<UTC date and time>.dump` per run, readable only by its owner;
- a failed run leaves no file and exits with a non-zero status; old dumps are removed only after a new one succeeded;
- the retention is 14 days (`RETENTION_DAYS` changes it, but see the note on personal data below).

Copy the folder to a second place (another server, object storage) with a tool of your choice; a backup on the same disk does not survive the loss of the disk. Look at the log or the newest file once a week.

**Personal data in backups.** A Contact that a User deletes for good, and an account that is deleted, disappear from the database at once but stay in the backups until those are removed. That is why backups are kept 14 days and not longer. Keep copies no longer than that too.

## 7. Restore (NFR-06, DoD-P07)

**Test a restore at least once before you rely on the backups and after every change of the server.** The test does not touch production data:

```bash
./scripts/restore.sh /var/backups/contact-groups/contact-groups-2026-11-10T021500Z.dump
```

It creates a second database `<name>_restore`, loads the dump and prints the number of rows of every table. Compare them with the production database (`psql … -c "SELECT count(*) FROM contact"`); they are equal if nothing changed since the backup. The script prints the command that removes the copy.

To **replace the production data** with a dump (for example after a disk failure or a wrong mass change):

```bash
./scripts/backup.sh /var/backups/contact-groups      # keep what is there now, first
./scripts/restore.sh <dump-file> --replace
```

`--replace` stops the backend and the web application, deletes the production database, loads the dump, and starts both again. Everything after the time of the dump is lost; tell your Users. A file that is not a dump of `backup.sh` is refused before anything is deleted.

When you restore on a new server: install it as in sections 1 to 4 first, then run `restore.sh … --replace` on it.

## 8. Update to a new version

1. Run the backup (section 6).
2. Build the new images (section 2) with a new version tag, and set `BACKEND_IMAGE` and `FRONTEND_IMAGE` in `production.env` to them.
3. `docker compose -f docker-compose.prod.yml --env-file production.env up -d`. The database migrations run on the start of the backend and only move forward; to go back to the old version, restore the backup of step 1 with the old images.
4. Check as in section 4.

## 9. Checks after the start (short list)

- `docker compose … ps` shows three services `healthy` or `running`.
- `/health` prints `{"status":"UP"}`.
- You can sign in at the address of the page, create a Group and a Contact, and sign out.
- Plain HTTP from outside answers 403 (the proxy must use HTTPS).
- A backup was written and a test restore showed equal row counts.
