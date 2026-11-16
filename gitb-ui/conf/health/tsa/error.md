The trusted timestamp service is enabled but not working correctly. The service check reported the following error:
```
%1$s
```

#### What is the impact?

An enabled but non-functioning trusted timestamp service means that PDF reports that are configured to include digital
signatures will be generated **without a trusted timestamp** in their signatures. This also means that long-term
validation data (if `SIGNATURE_LT_ENABLED` is set) cannot be added to these signatures. This affects:

* Conformance reports and certificates.
* Test case and test step reports.

While this problem persists there will be no impact regarding executing tests and other administrative actions.

#### How to fix this?

The trusted timestamp service is enabled because the environment variable `TSA_SERVER_ENABLED` is set to `true`.
A quick workaround is to set this to `false` to temporarily deactivate the timestamp requests.

To address this in full however, you should investigate why the configured service is not accessible by the `gitb-ui`
component or does not accept its requests. The service is currently configured as follows (all settings are
environment variables of the `gitb-ui` component):

* `TSA_SERVER_URL`: `%2$s`. Check that this address is correct and reachable by `gitb-ui` (taking into account any
  configured proxy).
* `TSA_SERVER_TIMEOUT`: %3$s seconds. This is the maximum time to wait for the service (to connect and to respond).
  Increase it if the service is slow to respond.
* `TSA_SERVER_AUTH_ENABLED`: %4$s. If the service requires authentication, set this to `true` and provide the
  credentials (HTTP basic authentication) through `TSA_SERVER_USERNAME` and `TSA_SERVER_PASSWORD`.
* `TSA_SERVER_POLICY_OID`: %5$s. If the service requires a specific timestamp policy, set its object identifier
  (e.g. `1.2.3.4`) here. Otherwise leave it empty.

For more setup information you can also refer to the [timestamp-related section](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#conformance-certificate-timestamps)
of the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/).