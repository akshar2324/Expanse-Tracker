# Security Policy

## Supported Versions

Currently, only the latest version of AkSpend on the `main` branch is actively supported with security updates. We recommend keeping your local forks and clones up to date.

## Reporting a Vulnerability

If you discover a security vulnerability in AkSpend, please do not disclose it publicly on the issue tracker.

Instead, please send a direct message or email to the repository owner (or the contact provided by the maintainer). Please provide a description of the issue, steps to reproduce, and any other relevant context.

We will acknowledge your report, investigate the issue, and work on a fix as quickly as possible.

## Secret Handling

- **No Public Credentials:** Do not commit any real passwords, API keys, keystores (`.keystore`, `.jks`), or `google-services.json` files to this repository.
- **Local Configuration:** Use local configuration files like `keystore.properties` or `.env` for development and signing. These files are excluded via `.gitignore`.
- **Key Rotation:** If you suspect any private key, API token, or signing credential has been exposed, rotate it immediately and revoke the exposed key.

## Privacy & Offline First

AkSpend is designed as an offline-first app. It does not send financial data to external servers or backends. Please refer to our [Privacy Model](docs/privacy-model.md) for more details.
