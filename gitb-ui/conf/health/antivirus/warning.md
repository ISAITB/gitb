The antivirus scanning service is currently **disabled**. Given that you are running in **production mode**
this is considered to be an important vulnerability to be addressed. 

#### What is the impact?

A disabled antivirus scanning service will not raise errors but means that users' file uploads are currently not being
scanned. Such uploads include:

* **Test suites** by community administrators.
* **Configuration properties** at all levels (domain, organisation, system, statement) that are of `binary` type.
* **Administrator uploads** such as community and system resources, badges, and data archives.
* **Test session inputs** for `interact` steps that are file uploads. 

Considering that you are running in **production mode** it is assumed that end-users are accessing your Test Bed instance.
Depending on your tests' configuration you could be vulnerable to malicious files uploaded by non-administrator users
that will go undetected.

#### How to enable the service?

The antivirus service is managed through environment variables. To enable the service you should adapt the configuration
of the `gitb-ui` component as follows (assuming use of Docker Compose):

```
...
services:
  ...
  gitb-ui:
    ...
    environment:
     # Set to true to enable the service.
     - ANTIVIRUS_SERVER_ENABLED = true
     # The host address at which the antivirus service can be reached by gitb-ui.
     - ANTIVIRUS_SERVER_HOST = gitb-antivirus
     # The port at which the antivirus service is listening.
     - ANTIVIRUS_SERVER_PORT = 3310
  ...
```

More information is available in the [antivirus-related section](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#antivirus-scanning)
of the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/).