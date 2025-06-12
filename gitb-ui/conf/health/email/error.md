The email (SMTP) service is enabled but not working correctly. The service check reported the following error:
```
%s
```

#### What is the impact?

Test Bed operations will continue to function regardless of whether related email sending fails. Nonetheless, there are 
cases where this may cause issues:

* Administrator emails such as self-registration notifications and reminders for test session input will not be sent.
* If contacting support is enabled (currently **%s**), users will get errors when submitting the contact form.   

#### How to fix this?

Go to the [system administration screen](https://www.itb.ec.europa.eu/docs/itb-ta/latest/systemAdministration/index.html#manage-configuration-settings) and correct the current email settings. Emails can also
be disabled as a temporary fix. In doing so, notifications will not be sent and users will no longer see the 
**contact support** link.