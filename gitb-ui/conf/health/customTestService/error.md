The custom %s service with identifier `%s` is **not working correctly**. The service check reported the following error:
```
%s
```

#### What is the impact?

If this test service is actively used in test cases as the handler for relevant test steps, these steps will result
in failures. To determine where this service is used look for %s steps, that define `$DOMAIN{%s}` as the `handler`
attribute.

#### How to fix this?

To address this issue:
1. Make sure that the service is reachable at `%s`.
2. Ensure that the service's `getModuleDefinition()` operation is implemented. 

In case this service is not used in tests or is expected to be offline, you should set it as **not monitored**. This
can be done when [editing the service's information](https://www.itb.ec.europa.eu/docs/itb-ta/latest/domainDashboard/index.html#test-services).
For more information on the `getModuleDefinition()` operation used for the status check, refer to the
[GITB test services documentation](https://www.itb.ec.europa.eu/docs/services/latest/index.html).