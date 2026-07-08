Callbacks from custom test services are **enabled** but without requiring valid **API keys** to be provided. Such callbacks are made during test sessions to:

- Notify the test engine to complete pending `receive` steps (or handler-enabled `interact` steps).
- Send messages to add to the **test session log**.

The current configuration means that:

- REST callbacks are %s.
- SOAP callbacks are %s.
- Test services are not required to provide a valid API key to make callbacks.

Given that you are running in **production mode** this is considered as a potential vulnerability to be addressed.

#### What is the impact?

When test service callback APIs are enabled but API keys are not required, an external service could send a notification for any active test session as long as it knows its test session identifier. In addition, even if specific test sessions are not affected, the test engine will process incoming requests on its callback APIs before rejecting them, leading to unnecessary resource usage.

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
