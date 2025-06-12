Test engine callbacks appear to be **working correctly**. This means external test components and systems under test
should be able to call the `gitb-srv` test engine.

Callbacks to the test engine are used in two cases:
* When test cases **receive messages** from systems under test.
* When [custom extension services](https://www.itb.ec.europa.eu/docs/services/latest/) are used and provide input to 
  **test session logs**.

The test engine's endpoints responsible for this, listen under a `CALLBACK_ROOT_URL` defined as a variable
of the `gitb-srv` component (assuming use of Docker Compose):
```
...
services:
  ...
  gitb-srv:
    ...
    environment:
     # The root URL of all callback endpoints to the test engine.
     - CALLBACK_ROOT_URL = https://www.my.org/itbsrv
  ...
```

The currently configured `CALLBACK_ROOT_URL` is `%s`.

If you are expecting calls to the test engine as previously described, ensure this URL is correctly configured. Note that
whether the test engine should be publicly or locally accessible depends on your test cases, and how the test engine is accessed by 
any supporting test services. For more information refer to the [access URL section](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#step-2-determine-the-access-url)
of the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/).