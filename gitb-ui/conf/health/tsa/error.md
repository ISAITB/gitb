The trusted timestamp service is enabled but not working correctly. The service check reported the following error:
```
%s
```

#### What is the impact?

An enabled but non-functioning trusted timestamp service means that creating PDF reports that are configured to include
digital signatures **will fail**. This affects:

* Conformance reports and certificates.
* Test case and test step reports.

While this problem persists there will be no impact regarding executing tests and other administrative actions, however
all PDF reporting will be effectively disabled.

#### How to fix this?

The trusted timestamp service is enabled because the environment variable `TSA_SERVER_ENABLED` is set to `true`.
A quick workaround is to set this to `false` to temporarily deactivate the timestamp requests.

To address this in full however, you should investigate why the configured service is not accessible by the `gitb-ui`
component. Note that the currently configured `TSA_SERVER_URL` is `%s`. For more setup information you can also refer
to the [timestamp-related section](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#conformance-certificate-timestamps)
of the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/).