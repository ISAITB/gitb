Callbacks from custom test services are **enabled** but without requiring valid **API keys** to be provided. Such callbacks are made during test sessions to:

- Notify the test engine to complete pending `receive` steps (or handler-enabled `interact` steps).
- Send messages to add to the **test session log**.

The current configuration means that:

- REST callbacks are %s.
- SOAP callbacks are %s.
- Test services are not required to provide a valid API key to make callbacks.

Having disabled API keys is an expected setting given that you are currently running in **development mode**. For a production instance it is strongly advised, although not mandatory, that you disable unused callback APIs and require API keys if callbacks are expected.

#### How to manage callback settings?

To view and manage test service callback settings go to the [system administration screen](https://www.itb.ec.europa.eu/docs/itb-ta/latest/systemAdministration/index.html#manage-configuration-settings).

Alternatively you can also manage callbacks through environment variables set on the `gitb-ui` component as follows:

```
...
services:
  ...
  gitb-ui:
    ...
    environment:
     - TEST_SERVICE_CALLBACKS_ENABLED=true
     - TEST_SERVICE_CALLBACKS_SOAP_ENABLED=false
     - TEST_SERVICE_CALLBACKS_REST_ENABLED=true
     - TEST_SERVICE_CALLBACKS_API_KEYS_ENABLED=true
  ...
```

More information regarding the available environment variables is available in the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#configuration-properties).
