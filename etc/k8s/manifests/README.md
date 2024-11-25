# Interoperability Test Bed k8s manifests

This folder holds [Kubernetes](https://kubernetes.io/) manifests for the installation
of the [Interoperability Test Bed (ITB)](https://interoperable-europe.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed).

## How to use these manifests?

To set up the ITB on your cluster you need to complete the following steps:

1. Install a **Kubernetes cluster**.
2. Install an **Ingress Controller** of your choice.
3. Apply the ITB's **manifests** in sequence.

Note that depending on the Ingress Controller you use you will likely need
to adapt the ingress manifest ([nginx](https://kubernetes.github.io/ingress-nginx/) is considered by default).

The sequence with which the manifest files should be executed is defined as a prefix
of the manifest files names. Following this sequence you will:

1. Create the persistent volume claim for the database data volume.
2. Create the persistent volume claim for the file repository volume.
3. Create the deployment for the ITB's MySQL database used to persist all data.
4. Create the service for the ITB's MySQL database.
5. Create the deployment for the ITB's Redis cache instance for user session persistence.
6. Create the service for the ITB's Redis cache.
7. Create the deployment for the ITB's test engine used to execute tests.
8. Create the service for the ITB's test engine.
9. Create the deployment for the ITB's frontend responsible for the user interface and REST API.
10. Create the service for the ITB's frontend.
11. Create the ingress to expose the ITB's frontend and test engine outside the cluster.

## Production use

The default configuration of the manifests result in an instance for development
and experimentation. When preparing an ITB instance for production please
refer to the [production installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/).