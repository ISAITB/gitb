Callbacks from custom test services are **disabled**. Such callbacks would be made during test sessions to:

- Notify the test engine to complete pending `receive` steps (or handler-enabled `interact` steps).
- Send messages to add to the **test session log**.

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
