The email (SMTP) service is currently **disabled**. This is an expected setting given that you are currently running in
**development mode**.

When switching to **production mode**, enabling emails can be desired especially to allow users to use the Test Bed's
**contact support** feature.

#### How to enable the service?

To view, manage and test your email settings go to the [system administration screen](https://www.itb.ec.europa.eu/docs/itb-ta/latest/systemAdministration/index.html#manage-configuration-settings).

Alternatively you can also set email settings through environment variables set on the `gitb-ui` component as follows:

```
...
services:
  ...
  gitb-ui:
    ...
    environment:
     - EMAIL_ENABLED=true
     - EMAIL_SMTP_HOST=mail.my.org
     - EMAIL_SMTP_PORT=25
     - EMAIL_SMTP_AUTH_ENABLED=true
     - EMAIL_SMTP_AUTH_USERNAME=a_username
     - EMAIL_SMTP_AUTH_PASSWORD=a_password
     - EMAIL_FROM=contact@my.org
     - EMAIL_TO=support@my.org
  ...
```

More information regarding the available environment variables is available in the [email-related section](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#email-notifications-and-support)
of the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/).