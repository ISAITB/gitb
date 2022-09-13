# Introduction

The current project contains the main application components that are used by the Interoperability Test Bed.
These are:
- The **test engine**, responsible for executing test cases and reporting their progress and outcome.
  In terms of Docker images, this component represents the ``gitb-srv`` image.
- The **frontend**, responsible for the Test Bed's user interface and its test, user, and configuration
  management features. In terms of Docker images, this component represents the ``gitb-ui`` image.

In the subsequent sections each of these components will be refered to using their Docker image names\
(``gitb-srv`` and ``gitb-ui``).

> **Note**  
> This repository is used to build the Test Bed's software components from their source. The simplest approach
> to use the Test Bed is via its **published Docker images** ([gitb-ui](https://hub.docker.com/r/isaitb/gitb-ui) and [gitb-srv](https://hub.docker.com/r/isaitb/gitb-srv)) for which you can follow the installation
> guides [for development](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBed/index.html) and [for production](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html). All other documentation (guides, tutorials, reference documentation) is available [here](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed/documentation-and-resources).

# Technology stack

Both ``gitb-srv`` and ``gitb-ui`` are Java-based applications. Specifically, ``gitb-srv`` is packaged
as a Spring Boot application using Akka as its primary internal framework, whereas ``gitb-ui``
is developed in Scala and uses the Play Framework.

The frontend of the ``gitb-ui`` component is an Angular app developed in TypeScript, and is managed in terms
of scaffolding and build using Angular CLI.

# Prerequisites

To build and run the Test Bed's components you need to have the following tools:
- JDK 11+, used as the base platform for both ``gitb-srv`` and ``gitb-ui``.
- Maven 3+, used to build ``gitb-srv``.
- SBT 1.3.13+, used to build ``gitb-ui``.
- NPM version 6.14+, used to build the frontend app of ``gitb-ui``.

Although not mandatory, the proposed IDE to use is IntelliJ, and VS Code for ``gitb-ui``'s Angular app.  

## Additional Test Bed components

The focus of this README file is the ``gitb-srv`` and ``gitb-ui`` components. To run a complete Test Bed instance 
you will also require at least:
- A MySQL database (version 5.*) for its persistence.
- A REDIS instance for the caching of user sessions.

Both these instances are set up separately (e.g. via Docker) environment. These can be set-up from Docker 
as follows:
- MySQL: ``docker run --name gitb-mysql -p 3306:3306 -d isaitb/gitb-mysql``
- REDIS: ``docker run --name gitb-redis -p 6379:6379 -d redis:6.0.8``

# Configuration properties

The main configuration file of interest is ``application.conf`` from ``gitb-ui`` although this never needs
to be adapted directly. All properties can be set through environment variables, provided either via Docker
or through the IDE used for development.

The full listing and documentation of properties is available at https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#configuration-properties.

# Build for development

Follow these steps to build and run the application components for development purposes.

## Test engine (gitb-srv)

To build and run the ``gitb-srv`` component carry out the following steps:
1. From folder ``gitb`` run ``mvn clean install -DskipTests=true``.
2. Create a run configuration in your IDE as follows (or set up outside your IDE). The run configuration
   can be defined in one of two ways:
   - **As a Java Application (best):** Set class ``Application`` from the ``gitb-testbed-service`` module as
     your entry point. This is the best approach as it allows direct debugging without extra configuration.
   - **Via Maven:** Run ``mvn spring-boot:run`` from the ``gitb-testbed-service`` module.
3. Adapt your run definition's environment variables to add the following:
   - ``remote.testcase.repository.url`` = http://localhost:9000/repository/tests/:test_id/definition
   - ``remote.testresource.repository.url`` = http://localhost:9000/repository/resource/:test_id/:resource_id

Note that defining a run configuration is something specific. If running outside an IDE you can follow either
approach but ensure that the environment variables are correctly set up.

## Frontend (gitb-ui)

To build and run the ``gitb-ui`` component carry out the following steps:
1. From the ``gitb-ui`` folder issue ``sbt compile``.
2. Define in your IDE a run configuration as a sbt application to run ``sbt run``.
3. Set environment variables as needed. For example set ``THEME = ec`` for a EC-themed UI.

The next step is to choose how to build and run the Angular UI application. Its resources are defined in
the ``gitb-ui/ui`` folder and its build is managed by Angular CLI. You have two options on how to work with
the Angular app - choose the preferred approach for you from the two following sections. As a common first
step regardless of the approach you choose you need to issue ``npm install`` from folder ``gitb-ui/ui``. 

**Developing with EU Login enabled:** In case you are developing with EU Login enabled (i.e. against a local EU 
Login test instance), you need to ensure that the following environment variables are set:
- ``SESSION_SECURE`` set to ``false`` 
- ``SESSION_COOKIE_NAME`` set to ``ITB_SESSION``.

Failure to set these properties will result in session cookies never getting served over HTTP, resulting in endless
authentication loops. The alternative is to test over HTTPS via a local SSL-enabled proxy (e.g. a local nginx server
with a self-signed server certificate).

### Option 1: Work on Angular UI through Play application

Using this approach you build the Angular app using Angular CLI but access it through the Play application. To do this:
1. From ``gitb-ui/ui`` issue ``npm run build``. This will build the app and copy it under the Play application's ``assets``
   folder. To rebuild automatically for any changes use ``npm run build:dev``.
2. Access the application by first going to the Play application's welcome page at http://localhost:9000. Once you click
   any of the login options the Angular app will be launched.
3. Once any part of the Angular app is rebuilt you will need to refresh the browser page to see
   changes.     

**When to use this approach:** Using this approach mirrors exactly how the application will run in production. In addition,
you may need to use this approach to effectively test aspects that require interaction with the Play application. These 
include:
- Testing with EU Login enabled (as EU Login's callback returns to the Play application).
- Testing the different login options (e.g. register, demos, confirm role assignment).

### Option 2: Work on Angular UI standalone 

Using this approach you build and serve the Angular app using Angular CLI. In this scenario the Play application
is only used through its REST API which the Angular app proxies and uses. To use this approach:
- Only once from ``gitb-ui/ui`` issue  ``npm run build``. This is needed to put in place static resources used by the Angular app
  (most notably tinymce styles).
- From ``gitb-ui/ui`` issue ``npm start``. This reloads the app upon detected changes.
- Access the application at http://localhost:4200 (this will be automatically opened for you).  

**When to use this approach:** This approach is the most efficient as it allows use of the lightweight CLI server
and provides automatic refresh for build changes. The problem with this approach is that it cannot cover
cases where you need to test interactions with the Play application (e.g. when EU Login is used for authentication).
Having said this however, you can still use this after you have authenticated using the Play application given that
authentication cookies are shared.

# Build for production

Follow these steps to make the components' production builds.

## Test engine (gitb-srv)

1. From the current (root) folder issue ``mvn clean install -DskipTests=true``
2. Copy file ``gitb-testbed-service/target/itbsrv.war`` to a separate folder to build its Docker image.
3. Use Dockerfile ``etc/docker/gitb-srv/Dockerfile`` from the same folder to build the image.

## Frontend (gitb-ui)

1. From the ``gitb-ui`` folder issue ``sbt clean dist``. This builds all Play code and automatically calls the required
   prod build target from its Angular app.
2. Unzip the contents of the ``gitb-ui/target/universal/gitb-1.0-SNAPSHOT.zip`` archive to a separate folder. The name of the resulting unzipped folder should be ``gitb-ui``.
3. Use the Dockerfile ``etc/docker/gitb-ui/Dockerfile`` from the same folder to build the image.

# Using the application

Once a complete Test Bed instance has been setup, either in development or production mode, access using the default
Test Bed administrator account as follows:
1. Go to http://localhost:9000.
2. Click on the login button.
3. Authenticate using ``test@test.com`` with a password of ``test``.