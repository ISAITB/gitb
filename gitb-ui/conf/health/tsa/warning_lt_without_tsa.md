The trusted timestamp service is currently **disabled**, but the Test Bed is configured to include long-term validation
data in PDF signatures (the environment variable `SIGNATURE_LT_ENABLED` is set to `true`).

#### What is the impact?

Long-term validation data (certificates and revocation information, resulting in signatures of the PAdES baseline
**B-LT** level) can only be added to signatures that include a trusted timestamp. As no timestamp service is enabled, the
configured B-LT level **will not be reached**. PDF reports configured to include digital signatures will still be
generated and signed, but only with the basic **B-B** level and without any validation data. Digital signatures are
optional, so this does not impact in any way Test Bed operations.

#### How to fix this?

You should either enable the trusted timestamp service or, if long-term validation data is not needed, set
`SIGNATURE_LT_ENABLED` to `false`. The trusted timestamp service is managed through environment variables. To enable it
you should adapt the configuration of the `gitb-ui` component as follows (assuming use of Docker Compose):

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

More information is available in the [timestamp-related section](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#conformance-certificate-timestamps)
of the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/).
