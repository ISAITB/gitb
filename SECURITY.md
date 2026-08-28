# Security Policy

This is the security policy related to the **Interoperability Test Bed's GITB Test Bed software**, subsequently referred to as 
the **Solution**.

## Introduction

This Solution is developed and maintained primarily by the European Commission. It is used by European Commission
services, as well as by various other parties. The security of the Solution regardless of the use case for which it
is being used, is of utmost importance to us.

## Reporting a Vulnerability

Please do not report security vulnerabilities through public GitHub issues, pull requests, or discussions. If you
believe you have found a security vulnerability in the Solution, please report it privately by sending an
email to:

- [EC-DIGIT-SECURITY-ASSURANCE@ec.europa.eu](mailto:EC-DIGIT-SECURITY-ASSURANCE@ec.europa.eu): DIGIT's security assurance team.
- [DIGIT-ITB@ec.europa.eu](mailto:DIGIT-ITB@ec.europa.eu): DIGIT's ITB support team.
 
Please include as much information as possible, including:

- A description of the vulnerability.
- The affected version(s).
- Steps to reproduce the issue.
- A proof of concept or minimal reproduction, where available.
- The potential impact.
- Any relevant logs or error messages.
- A suggested mitigation or fix, if available.

Please do not publicly disclose the vulnerability until the maintainers have had an opportunity to investigate it.

## Vulnerabilities in European Commission Services

This repository contains the Solution's software itself.

If you have identified a vulnerability in an internet-facing service operated by the European Commission, rather than
in the software contained in this repository, please follow the European Commission's Vulnerability Disclosure Policy:

https://commission.europa.eu/legal-notice/vulnerability-disclosure-policy_en

## Security Updates

Security fixes will be released as appropriate to the affected versions. When reported vulnerabilities are found to
be exploitable a patch fix shall be released as soon as possible.

Users are encouraged to keep their copy of the Solution software up to date and to monitor the repository's releases and
security advisories.

## Third-Party Dependencies

Security vulnerabilities in third-party dependencies should normally be reported to the maintainers of the affected
dependency.

The Test Bed team continuously monitors the security health of the Solution's third-party dependencies and proactively
issues patch updates for the Solution where vulnerable dependencies are found to be exploitable. The Test Bed team
may also choose to release patch updates addressing high-severity vulnerabilities in third-party libraries that are
not exploitable, to facilitate automated security monitoring processes of downstream users.

If you find that a vulnerability in a third-party dependency is not sufficiently addressed, or leads to unexpected
implications, please report it as described above.

## Responsible Disclosure

We ask security researchers and users to give the maintainers a reasonable opportunity to investigate and address
security issues before publicly disclosing them.

We appreciate responsible security research and reports that help improve the security of the Solution.
