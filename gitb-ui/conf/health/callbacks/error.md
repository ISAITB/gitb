Test engine callbacks are **not working correctly**. The `gitb-srv` test engine check reported the following error:
```
%s
```

#### What is the impact?

While the `gitb-srv` component is down:
* Users will be unable to **launch test sessions**.
* Administrators will be unable to **view test case definitions**.

#### How to fix this?

To resolve this issue go through the following steps in sequence:
1. Ensure that `gitb-srv` is running.
2. Check your internal networking setup to ensure nothing blocks access between `gitb-ui` and `gitb-srv`.

For more information you can refer to the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/).