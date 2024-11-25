# Interoperability Test Bed Helm chart

This is a [Helm](https://helm.sh/) chart for the installation of the
[Interoperability Test Bed (ITB)](https://interoperable-europe.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed) on [Kubernetes](https://kubernetes.io/).

## How to use this chart?

To set up the ITB on your cluster you need to complete the following steps:

1. Install a **Kubernetes cluster**.
2. Install **Helm** to manage your deployments.
3. Install an **Ingress Controller** of your choice.
4. Install the **ITB** using `helm install` or `helm upgrade --install`.

Note that depending on the Ingress Controller you use you will likely need
to adapt the chart's ingress definition ([nginx](https://kubernetes.github.io/ingress-nginx/) is considered by default). 

## Production use

The default configuration of the chart results in an instance for development
and experimentation. When preparing an ITB instance for production please
refer to the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/).

## Development use

When using to create a development instance, you may find it more suitable to have an instance
pointed to the Test Bed's "nightly" tags, and also to have TLS disabled. To set up such a development 
instance you can use a value override file when installing or upgrading, as follows:
```yaml
mysql:
  # Switch the image to point to the nightly tag and ensure it is always pulled.
  image: isaitb/gitb-mysql:nightly
  imagePullPolicy: Always
ui:
  # Switch the image to point to the nightly tag and ensure it is always pulled.
  image: isaitb/gitb-ui:nightly
  imagePullPolicy: Always
  env:
    # Adapt the home link to use "http" as opposed to "https".
    TESTBED_HOME_LINK: http://localhost/itb
srv:
  # Switch the image to point to the nightly tag and ensure it is always pulled.
  image: isaitb/gitb-srv:nightly
  imagePullPolicy: Always
  env:
    # Adapt the callback link to use "http" as opposed to "https".
    CALLBACK_ROOT_URL: http://localhost/itbsrv
ingress:
  annotations:
    # Configure the default nginx ingress to not redirect "http" to "https".
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
```