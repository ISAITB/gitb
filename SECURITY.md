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

## Software Bill of Materials and Vulnerability Reports

To support the transparency and security monitoring needs of downstream users, the Test Bed team publishes the
following reports for the releases of the Solution:

- A **Software Bill of Materials (SBOM)**, listing the components included in the release.
- A **Vulnerability Disclosure Report (VDR)**, listing the known vulnerabilities that relate to these components
  along with the Test Bed team's assessment of each one (e.g. *not affected*, with a justification, or *exploitable*).
  Besides informing users, this allows the report to be used as a VEX document, for example to dismiss the findings
  of vulnerability scanners that do not apply to the Solution.

The Solution is reported as a whole: a single SBOM and VDR is published per release, covering the test engine
(``gitb-srv``) and the frontend (``gitb-ui``). Reports are provided in [CycloneDX](https://cyclonedx.org/) JSON format
and are digitally signed. They are published in the [release-reports](https://github.com/ISAITB/release-reports)
repository, which also includes instructions on how to verify their signatures:

- The reports of the Solution are available under [``reports/itb``](https://github.com/ISAITB/release-reports/tree/master/reports/itb).
- An overview of the vulnerability status of all releases is available in [VULNERABILITY_STATUS.md](https://github.com/ISAITB/release-reports/blob/master/VULNERABILITY_STATUS.md).
- For automated integrations, a mirror of the reports with a machine-readable index is available at
  https://www.itb.ec.europa.eu/release-reports/reports/index.json.

Note that VDRs are living documents: the VDR of a release may be updated after the release is published, as new
vulnerabilities are discovered or assessments are revisited. When consulting the report of a release, always use its
latest version.

If a vulnerability scanner reports an issue that is not covered by the Solution's VDR, or you disagree with an
assessment made in it, please report it as described above.

## Responsible Disclosure

We ask security researchers and users to give the maintainers a reasonable opportunity to investigate and address
security issues before publicly disclosing them.

We appreciate responsible security research and reports that help improve the security of the Solution.
