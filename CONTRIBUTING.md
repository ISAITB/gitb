# Contributing

These are the guidelines for contributors to the **Interoperability Test Bed's GITB Test Bed software**, subsequently referred to
as the **Solution**. In the scope of this document the terms **You**, **Your**, and **Yourself** refer to yourself
acting in the role of a prospective contributor. 

## Introduction

Thank You for Your interest in contributing to this Solution.

The Solution is developed and maintained primarily by the European Commission. Contributions from external developers
are welcome, although the project is not currently operated as a community-driven project.

The primary type of contribution that we encourage is feedback raised as issues in the current repository.
The reason for this is to ensure the Solution always remains reusable for the widest set of users and use cases, while
remaining true to its design principles. Nonetheless, we do also accept limited community contributions in the form of
code, configuration and documentation, to be merged following an appropriate review and approval process by the
Solution maintainers.

## Types of contribution

We distinguish the following types of contribution to the project:

1. **Contribution of feedback** 

Providing feedback is the primary means of contribution that we encourage. Such feedback relates to feature requests,
suggested improvements, and bug reports. Any user, including Yourself, is free to [raise a ticket](#issues) to provide
such feedback.

2. **Minor code and configuration contributions**

We consider as minor contributions those that apply targeted corrections or improvements, affecting a limited set
of resources. In these cases we prefer that You create a relevant [issue](#issues) first, and then link it
to a [pull request](#pull-requests), to facilitate exchanges, traceability, and discoverability by other users.
You are not required to sign a particular document, however You are encouraged to sign off commits with
a GPG key. Please refer also to the section on [Minor Contributions](#minor-contributions) for additional information.

3. **Documentation contributions**

Contributions to documentation do not require You to raise a relevant [issue](#issues) first, only create a 
[pull request](#pull-requests) with the proposed changes. You are not required to sign a particular document, however
You are encouraged to sign off commits with a GPG key.

4. **All other code and configuration contributions**

Code and configuration contributions that affect significant changes are typically not preferred. In all such cases
You should raise an [issue](#issues) in advance to discuss with the Solution's maintainers. In case it is
agreed to proceed with the contribution, please refer to the section on [Substantial Contributions](#substantial-contributions)
for additional steps. In the end Your resulting contribution should be submitted as a [pull request](#pull-requests)
for review by the Solution maintainers.

## Issues

You should use GitHub Issues for:

- Bug reports
- Feature requests
- Documentation issues
- Raising questions
- Other actionable project issues

In particular when creating a bug report, please provide extra information on the Solution's version that was used, the
expected behaviour, test data to replicate, log extracts, screenshots, and any other pertinent information. Ensure You
also check the history of issues to ensure that this has not already been raised and potentially addressed in a 
subsequent release or development build.

For security vulnerabilities, please follow the process described in [SECURITY.md](SECURITY.md) instead of opening a
public issue.

## Development

See the project [README.md](README.md) for instructions on setting up the development environment, building the project,
and testing Your changes. In doing so please:

- Follow the existing coding conventions.
- Keep changes focused and avoid unrelated modifications.
- Add or update tests where appropriate.
- Update documentation when necessary.
- Make sure the project builds and the relevant tests pass before submitting a pull request.

## Pull Requests

In the contribution scenarios where You expect to share code, configuration or documentation updates, please do so
using a pull request. Pull requests should:

- Clearly describe what has been changed and why.
- Include tests where appropriate.
- Reference an existing issue when applicable.
- Explain any relevant compatibility or behavioural changes.

All pull requests are subject to review and approval by the Solution maintainers. Submitting a pull request does not
imply that the proposed change will be accepted.

Maintainers may request changes, propose an alternative implementation, or decline a contribution if it does not fit
the Solution's scope, architecture, roadmap, or maintenance requirements.

## Code of Conduct

Please read and follow [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) when participating in the project.

## Minor Contributions

Minor contributions to code and configuration resources may involve resources that are subject to intellectual property
rights and copyright. In the case of such contributions, You agree that by submitting Your contributions You waive any
intellectual property rights and copyright claims to Your contribution.

Contributions that involve third-party software libraries that are not already included in the Solution are never
considered minor. The same applies for existing third-party software libraries for which the contribution changes the
Solution's included version numbers. In such cases You are invited to raise a relevant [issue](#issues).

## Substantial Contributions

When making a substantial code contribution, additional steps are needed from You. Please identify Yourself in one
of the following sections and follow the procedure described there.

1. **EU Officials (from the European Commission, European Parliament, European Council, the Council of the European,
   Court of Justice of the European Union, European Court of Auditors, European Economic and Social Committee, 
   Committee of the Regions)**

The rights in Your contribution are the property of the European Union. When sending a contribution You need to
sign off the commits You make with a GPG key. By signing off Your commits, You indicate that You have read
[these terms](etc/contributions/DCO.md) and that You agree with these.

2. **MS Administrations, other EU Institutions and Bodies with separate
   legal personality (e.g. Executive, Decentralized Agencies, European
   Central Bank), universities, legal entities (NGOs, private
   companies)**

If the contribution comes from an administration of a Member State in the EU or other public administrations outside
the EU, from an EU institution or body with legal personality, from a university or in general from a legal entity
(NGO, private company etc.), the copyright belongs to the respective entity and hence we need to receive from You
the right to use the contribution. For this to happen, please have Your legal representative sign the
[Contributor Licence Agreement](etc/contributions/CLA.md) and send it to us by email at 
[DIGIT-ITB@ec.europa.eu](mailto:DIGIT-ITB@ec.europa.eu).

Any employee submitting a contribution on Your behalf needs to sign off the commits with a GPG key. By signing off
the commit, the employee (individual) who submits a contribution indicates that he/she has read the terms of the
[Contributor Licence Agreement](etc/contributions/CLA.md) and agrees with these.

3. **Individuals acting outside the performance of their duties or self-employed**

If You are an individual (natural person), either self-employed or working outside the performance of Your duties
with Your employer, You own the rights in Your contribution and we need to receive from You the right to use such
contribution. For this to happen, please sign the [Contributor Licence Agreement](etc/contributions/CLA.md) and send
it to us by email at [DIGIT-ITB@ec.europa.eu](mailto:DIGIT-ITB@ec.europa.eu).

Additionally, please sign off on the commits You make with a GPG key.

4. **Individuals acting in connection with the performance of their duties**

If You are an employed individual (natural person) and the contribution can be considered as within Your authorised
scope of work or line of duty, it is Your employer who holds the copyright in the contribution and hence You are in
a position to give us a right to use it. For this reason, we kindly ask You to address Your employer and follow
section 2 above.

To this end we need to receive from Your employer the signed [Contributor Licence Agreement](etc/contributions/CLA.md).

Additionally, please sign off on the commits You make with a GPG key.

### How to handle Your contribution

**Legal compliance:** Please ensure that, if Your contribution is based upon previous third-party work, such work that
is covered under an appropriate (open source) licence that would not prevent it from being used in the project, under
its established outbound licence. To this end, You need to ensure that there is no incompatibility between (i) any
of the licences which cover the third-party work that You use or (ii) the licence of the project and the licences of
any third-party work You may use within Your contribution (such incompatibilities would arise if You use GPL code in
Your contribution for a project which is distributed under EUPL or MIT or if Your contribution is based both on Apache
and GPL 2.0 only code). Please ensure that You are not violating the terms of any licence by using it within Your
contribution. You are guaranteeing compliance with applicable (open source) licences under article 3 in the CLA. If You
have doubts on the correct use of third-party software, You may address the Solution maintainers before committing code.

For any third-party work that You use within the contribution You submit us, You must retain all copyright and licence
information (as available for instance in a specific 'Licence' file, 'Notice' file, in the headers of the sources of 
the used third-party work etc.).

We additionally have to request You to provide us with a list of third-party software (dependencies) You are using
within Your contribution (i.e. a software bill of materials as required under good development practices for open
source projects). Please provide it together with Your commit, in any format convenient for You. The software bill of
materials can be generated using a code scanning tool or a package manager; however You must double-check the generated
information and complement it if there is a need to do so.

Please make sure You read closely article 3 in the [Contributor Licence Agreement](etc/contributions/CLA.md) which
specifically refers to these obligations.

We reserve the right to refuse Your contribution for non-compliance reasons or for not providing us with the required
list of dependencies.

**Signing of the CLA:** Please note that the CLA You sign for this project does not apply to other projects of the EC.
If You are contributing to several projects, You need to send a signed CLA for each of these.

We accept the following types of signatures for the CLA You return:

- electronic signatures using trusted service providers from the list available 
  here: https://eidas.ec.europa.eu/efda/tl-browser/#/screen/home
- wet signatures (handed signed) or
- other digital solutions so long they are eIDAS compliant (e.g. open-pdf-sign)
