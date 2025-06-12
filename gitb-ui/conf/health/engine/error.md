Test engine communications are **not working correctly**. This means that the `gitb-ui` and `gitb-srv` components are unable
to communicate with each other. The tests carried out and reported errors are listed below.

**Test 1:** Access from `gitb-ui` to `gitb-srv` - **%s**
```
%s
```

**Test 2:** Access from `gitb-srv` to `gitb-ui` (session status updates) - **%s**
```
%s
```

**Test 3:** Access from `gitb-srv` to `gitb-ui` (resource lookup) - **%s**
```
%s
```

#### What is the impact?

If communication between `gitb-ui` and `gitb-srv` is down, this means that:
* Users will be unable to **launch test sessions**.
* Administrators will be unable to **view test case definitions**.

#### How to fix this?

To resolve this issue go through the following steps in sequence:
1. Ensure that `gitb-srv` is running.
2. In case you have set a [custom context root](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#using-a-custom-context-root-for-itb-ui)
   for `gitb-ui` via the `WEB_CONTEXT_ROOT` variable, ensure that the `REPOSITORY_ROOT_URL` of `gitb-srv` is
   set accordingly. These are currently respectively set as `%s` and `%s`.
3. Ensure the `HMAC_KEY` variables for `gitb-ui` and `gitb-srv` match. These currently %s.
4. Check your internal networking setup to ensure nothing blocks access between `gitb-ui` and `gitb-srv`.

For more information you can refer to the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/).