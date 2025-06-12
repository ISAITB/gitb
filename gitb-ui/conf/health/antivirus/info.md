The antivirus scanning service is currently **disabled**. This is not considered a problem given that you are currently
running in **development mode**. When enabled, the service will scan all file uploads such as:

* **Test suites** by community administrators.
* **Configuration properties** at all levels (domain, organisation, system, statement) that are of `binary` type.
* **Administrator uploads** such as community and system resources, badges, and data archives.
* **Test session inputs** for `interact` steps that are file uploads. 

A Test Bed instance that is exposed to end users should be operating in **production mode**. For a production instance
it is strongly advised, although not mandatory, that you enable the antivirus scanning service.

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
