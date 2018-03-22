# GITB docker images

The resources in this folder are used to build the GITB docker images.

## gitb-ui

To build the UI application `gitb-ui`:
1. Extract the built archive `gitb-ui\target\universal\gitb-x-y-z.zip` to folder `etc\docker\gitb-ui\`
2. Rename the extracted folder to `gitb-ui`
3. Build using `docker build -t isaitb/gitb-ui:latest .` (or replacing "latest" with the desired version)

## gitb-srv

To build the backend application `gitb-srv`:
1. Copy the built archive `gitb-testbed-service\target\itbsrv.war` to folder `etc\docker\gitb-srv\`
2. Build using `docker build -t isaitb/gitb-srv:latest .` (or replacing "latest" with the desired version)

## gitb-mysql

To build the DB `gitb-mysql`:
1. Build using `docker build -t isaitb/gitb-mysql:latest .` (or replacing "latest" with the desired version)