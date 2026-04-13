# Node Manager

The node manager is part of the Mosaic platform. It is expected to run on a local node and manage Mosaic runtime instances (as well as other runtimes integrated into node manager).

### Node manager API

The node manager is a Micronaut application exposing an HTTP endpoint. Optionally, it can also run a socket server along with the HTTP server (this option is used by the top-level scheduler).

The node manager HTTP API is implemented in the [Controller](src/main/java/org/graalvm/argo/lambda_manager/LambdaManagerController.java). The socket server API is similar and is implemented in its [Utils](src/main/java/org/graalvm/argo/lambda_manager/socketserver/RequestUtils.java) class.

### Configuring the node manager

The node manager accepts two main JSON configuration files: "config" and "variables". You can find the default versions of these files here:

- [`run/configs/manager/default-lambda-manager.json`](../run/configs/manager/default-lambda-manager.json)
- [`run/configs/manager/default-variables.json`](../run/configs/manager/default-variables.json)

### Build and deploy

Run the `build.sh` script to build the node manager.

Run the `deploy.sh` script to deploy the node manager locally. Run the script with the `-h` or `--help` option to get usage instructions.

The easiest way to get started with the node manager is to inspect and run one of the simple [tests](tests). These tests were created to test the node manager but they can also guide you through the deployment process of the node manager as they contain example commands to deploy the node manager, upload functions, invoke them, and terminate the deployment.
