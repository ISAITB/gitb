The antivirus scanning service is enabled but not working correctly. The service check reported the following error:
```
%s
```

#### What is the impact?

An enabled but non-functioning antivirus service will cause any file uploads by users **to fail**. Such uploads include:
* **Test suites** by community administrators.
* **Configuration properties** at all levels (domain, organisation, system, statement) that are of `binary` type.
* **Administrator uploads** such as community and system resources, badges, and data archives.
* **Test session inputs** for `interact` steps that are file uploads. 

Actions that do not involve file uploads will continue to work. Note also that the execution of test sessions that do
not request user uploads will also work.

#### How to fix this?

The antivirus service is enabled because the environment variable `ANTIVIRUS_SERVER_ENABLED` is set to `true`.
A quick workaround is to set this to `false` to temporarily deactivate the antivirus scans.

To address this in full however, you should investigate why the configured service is offline or not accessible 
by the `gitb-ui` component. The current configuration (defined via environment variables) is as follows:
* `ANTIVIRUS_SERVER_HOST`: `%s`
* `ANTIVIRUS_SERVER_PORT`: `%s`
* `ANTIVIRUS_SERVER_TIMEOUT`: `%s`

Ensure that your antivirus service is running and accessible to `gitb-ui` based on the provided environment
variables. You can also refer to the [antivirus-related section](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#antivirus-scanning)
of the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/).