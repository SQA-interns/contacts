# Security baseline

> For: agent · Owner: Security officer · Agent: read-only

Applies to every project. Project requirements (`SR-nn`) and the chosen standard level are in `project/02_design/security-requirements.md`.

## Verification standard by component type

| Component type | Standard | Default level |
|---|---|---|
| Web app, API, service | OWASP ASVS (latest) | Level 1; Level 2 when handling authentication, payments or sensitive personal data |
| Mobile app | OWASP MASVS (latest) | L1 |
| Desktop app, CLI, library | Applicable OWASP ASVS chapters | Level 1 |

## Controls for every project

| ID | Control | Why |
|---|---|---|
| SB-01 | All input is validated on the trusted side (server or core), whatever the client does. | A client can be bypassed; only the trusted side can enforce a rule. |
| SB-02 | Every non-public operation checks authentication and authorization. | Hiding a link is not access control; every operation can be called directly. |
| SB-03 | Credentials are stored only as salted, slow hashes; secrets come from configuration (ES-01, ES-02). | A leaked database must not reveal passwords; configuration keeps secrets out of code. |
| SB-04 | Data in transit uses TLS outside the local machine. | Anyone on the network path can read or change unencrypted traffic. |
| SB-05 | Output is encoded for its context; queries are parameterised. | Unencoded output and built-up queries are the source of injection attacks. |
| SB-06 | Public and authentication endpoints are rate limited. | Without limits, guessing passwords and flooding public forms cost an attacker nothing. |
| SB-07 | Errors and logs expose no internals, secrets or personal data (ES-07). | Internals in errors help an attacker; personal data in logs is a data breach. |
| SB-08 | Dependencies are scanned; no known Critical or High vulnerability ships. | Most exploited vulnerabilities are in dependencies, not in the code written for the project. |
| SB-09 | Source is scanned with static analysis and a secret scanner. | Tools find classes of defects and leaked secrets that reviews miss. |
| SB-10 | Web responses carry security headers (content security policy, framing, content-type options). | Headers switch on browser protections against script injection and clickjacking at almost no cost. |
| SB-11 | Deployed processes run with least privilege (e.g. non-root containers). | A compromised process can only do what its privileges allow. |

## Personal data (privacy)

| ID | Control | Why |
|---|---|---|
| SB-12 | Collect only the personal data a requirement needs; the project lists it. | Data not collected cannot leak (data minimisation). |
| SB-13 | Each personal-data item has a stated purpose and retention period. | Personal data needs a lawful purpose and must be deleted when it is no longer needed. |
| SB-14 | Consent, where required, is explicit and never preselected. | Only an explicit, informed choice is valid consent. |
