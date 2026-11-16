The trusted timestamp service is **enabled** and **working**, but the timestamps it provides **do not include the
certificate of the service**. At the same time the Test Bed is configured to include long-term validation data in PDF
signatures (the environment variable `SIGNATURE_LT_ENABLED` is set to `true`).

#### What is the impact?

Long-term validation data (certificates and revocation information, resulting in signatures of the PAdES baseline
**B-LT** level) is also collected for the certificate of the service that issued the trusted timestamp. As the service
does not provide its certificate, **no validation data can be added for the timestamp**. PDF reports configured to
include digital signatures will still be generated and signed, with a trusted timestamp and with the validation data of
the signing certificate. However, validators that cannot retrieve the certificate of the timestamp service otherwise may
be unable to verify the timestamp. Digital signatures are optional, so this does not impact in any way Test Bed
operations.

#### How to fix this?

The timestamp requests always ask the service to include its certificate, so this is a choice of the service that you
are using. You should either use a service that includes its certificate in the timestamps it issues or, if long-term
validation data is not needed, set `SIGNATURE_LT_ENABLED` to `false`. The service is currently configured as follows
(all settings are environment variables of the `gitb-ui` component):

* `TSA_SERVER_URL`: `%1$s`
* `TSA_SERVER_TIMEOUT`: %2$s seconds
* `TSA_SERVER_AUTH_ENABLED`: %3$s
* `TSA_SERVER_POLICY_OID`: %4$s

For more setup information you can also refer to the [timestamp-related section](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#conformance-certificate-timestamps)
of the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/).