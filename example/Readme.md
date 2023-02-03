# Simple ZIO HTTP Server

This basic HTTP server written in [ZIO](https://zio.dev) and [ZIO-http](https://https://github.com/zio/zio-http) provides a simple "Hello World" in response to a GET in the "/" route.

The idea is to demonstrate how to build a Docker container with the application binary built by GraalVM Native Image and packed into a container image ready for cloud distribution.

To simply run the application locally, use `./mill hello.run`. To run the tests, use `./mill hello.test`.

To build the Docker image (requires [GraalVM JDK](https://www.graalvm.org/) and [Docker](https://www.docker.com/) installed and running) with the binary, use:

```sh
./mill hello.dockerNative.build
```

The image `docker.io/myuser/helloapp` will be created which can be started with:

```sh
docker run -it --rm -p 8080:8080 docker.io/myuser/helloapp
```

and browsed with:

```sh
❯ curl -s http://localhost:8080
Hello World!%
```

Push the image to a container registry like DockerHub (must be authenticated previously with `docker login`) making it available globally:

```sh
./mill hello.dockerNative.push
```
