The trusted timestamp service is currently **disabled**. This is not a problem as trusted timestamps are fully 
optional.

Enabling the service will include trusted timestamps to all generated PDF reports including:
* Conformance reports and certificates.
* Test case and test step reports.

When running in **production mode**, including trusted timestamps as part of your (optional) signatures would be a good
practice. Note that timestamps, and digital signatures as a whole, are entirely optional and skipping them will not impact
in any way Test Bed operations.

#### How to enable the service?

The trusted timestamp service is managed through environment variables. To enable the service you should adapt the
configuration of the `gitb-ui` component as follows (assuming use of Docker Compose):

```
...
services:
  ...
  gitb-ui:
    ...
    environment:
     # Set to true to enable the service.
     - TSA_SERVER_ENABLED = true
     # The host address at which the timestamp service can be reached by gitb-ui.
     - TSA_SERVER_URL = https://freetsa.org/tsr
  ...
```

More information is available in  the [timestamp-related section](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#conformance-certificate-timestamps)
of the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/).
