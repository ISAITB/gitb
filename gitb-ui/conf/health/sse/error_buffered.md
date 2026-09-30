User interface communications are **being delayed**. This means that the `gitb-ui` component is able to open a
server-sent events connection with its frontend (running in the user's browser), but the events it sends are not
delivered immediately.

#### What is the impact?

Updates from running test sessions (e.g. test step progress, or requests for user input) will reach the user interface
late, or in bursts. When tests are executed interactively through the user interface they may appear to hang or
to progress with long delays. Note that it is still possible to execute tests without relying on this communication:
* Through the user interface, selecting to launch tests in the **background**.
* Through the **REST API** (if enabled).

#### How to fix this?

This is typically due to a **reverse proxy** (or other network component) situated between users and the `gitb-ui`
component that buffers responses. You need to disable response buffering for the Test Bed's paths, for example:
* For **nginx**, set `proxy_buffering off;` (the Test Bed also sends an `X-Accel-Buffering: no` response header that
  turns off buffering for nginx by default, unless the proxy is configured to ignore it via `proxy_ignore_headers`).
* For **Apache HTTP Server**, use `flushpackets=on` in the `ProxyPass` definition.
* For **HAProxy**, do not use `option http-buffer-request` and ensure no compression is applied to `text/event-stream`
  responses.
* For **Kubernetes ingress controllers**, apply the equivalent annotations to disable proxy buffering.

Also ensure that any compression (e.g. gzip) applied by the proxy is not enabled for `text/event-stream` responses.

For more information you can refer to the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/), and specifically the section on
[configuring a reverse proxy](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#step-7-configure-reverse-proxy).