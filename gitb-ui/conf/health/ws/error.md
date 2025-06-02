User interface communications are not working correctly. This means that the `gitb-ui` component is unable to
establish web socket connections with its frontend (running in the user's browser).

#### What is the impact?

When web sockets cannot be established, test sessions will be unable to start when executed interactively through the
user interface (tests will appear to hang and never proceed). Note that although test sessions cannot be launched
interactively, it is still possible to do so:

* Through the user interface, selecting to launch tests in the **background**.
* Through the **REST API** (if enabled).

#### How to fix this?

Web socket problems are typically due to either:
* Misconfigured **environment variables** in the Test Bed's setup regarding its public path exposure.
* A misconfigured **firewall** situated between users accessing the Test Bed and the `gitb-ui` component.

Starting from the **environment variables**, you need to ensure that the `gitb-ui` component defines correctly:
* Variable `TESTBED_HOME_LINK` that defines the full public path users access the Test Bed at. This is currently set 
  to `%s`.
* Variable `AUTHENTICATION_COOKIE_PATH` that defines the path at which the users' session cookie is mapped to. This is 
  currently set to `%s`.
* (Optional) variable `WEB_CONTEXT_ROOT` that defines the internal web context root of the `gitb-ui` component. This is
  current set to `%s`.

Regarding these variables ensure that:
1. All variables are set to the values you expect.
2. Variable `TESTBED_HOME_LINK` ends with the `AUTHENTICATION_COOKIE_PATH`.
3. If you define a `WEB_CONTEXT_ROOT` and [use a reverse proxy](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#step-7-configure-reverse-proxy),
   ensure that the proxy correctly maps between the public path and the internal one.
4. There are no extra trailing slashes in any of these properties that would make them inconsistent.

If you are certain that these variables are correctly set up, the remaining cause would be a **misconfigured firewall**.
You need to ensure that there is no firewall in place with rules blocking the `ws` or `wss` protocols. Depending on the
firewall configuration you may also be able to find relevant errors in your browser's network monitoring tab.

For more information you can refer to the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/), and specifically the sections on
[determining the access URL](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#step-2-determine-the-access-url), 
[preparing the basic configuration](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#step-3-prepare-basic-configuration), 
[configuring a reverse proxy](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#step-7-configure-reverse-proxy), and 
[configuring your firewall](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#step-8-firewall-configuration).