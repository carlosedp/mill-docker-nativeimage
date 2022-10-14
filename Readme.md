# mill-docker-nativeimage

This is a [Mill][mill] plugin modeled after the [contrib][docker-plugin] Docker plugin which allows building Docker images with Native Image binaries built by the amazing plugin [mill-native-image][mill-native-image] by Alex Archambault.

The inspiration came from Quarkus framework which generates applications that are cloud native and "Container-ready" with fast startup times and small size not depending on the JVM.

The plugin allows building very small containers and the application packed as a binary produces blazing fast startup times.

## Getting Started

The plugin provides a trait to configure the native build and container image, in addition there are two commands, one for building the Docker image and another to push the image to the registry. The runtime must be already authenticated for this.

### Installing the Plugin

To start using this plugin you'll want to include the following import in your build file:

```scala
import $ivy.`com.carlosedp::mill-docker-nativeimage::0.1.0`  //ReleaseVerMill
import com.carlosedp.milldockernative.DockerNative
```

Under the hood, the plugin uses [mill-native-image][mill-native-image] to build your application Native Image binary which gets copied into the base container image which can be customized with some parameters as listed below. To generate the Native Image, you need an installed version of [GraalVM][graalvm-install] and the native-image utility.

### Usage

Sample configuration:

```scala
object myApp extends ScalaModule with DockerNative {
  // def ivyDeps = ...
  object dockerNative extends DockerNativeConfig {
    // Some Native Image parameters
    def nativeImageName = "myAppName"
    def nativeImageGraalVmJvmId = T {
      sys.env.getOrElse("GRAALVM_ID", "graalvm-java17:22.2.0")
    }
    def nativeImageClassPath = runClasspath()
    def nativeImageMainClass = "com.domain.myClass"
    def nativeImageOptions = Seq( // Some parameters, depending on your application
      "--no-fallback",
      "--enable-url-protocols=http,https",
      "-Djdk.http.auth.tunneling.disabledSchemes=",
      "--allow-incomplete-classpath",
    )

    // Some Docker image parameters
    def tags                 = List("docker.io/myuser/myApp")
    def exposedPorts         = Seq(8080)
  }

  object test extends Tests {
    // ...
  }
}
```

Build and Push with:

```sh
mill myApp.dockerNative.build
mill myApp.dockerNative.push

docker run docker.io/myuser/myApp
```

A more detailed build for a ZIO-http sample application with Native, Docker and DockerNative builds can be seen at the [zio-scalajs-stack][zio-scalajs-stack-build] project. Also there are some bugs running a Native Image binary for a Scala 3 project as seen [here][nativeimage-bug].

### Configuration

Docker image configuration parameters:

```scala
// Override tags to set the output image name
def tags = List("docker.io/myuser/myApp")
// Overrides base container image, default value below
def baseImage = "redhat/ubi8"
// Configure whether the docker build should check the remote registry for a new version of the base image before building.
// By default this is true if the base image is using a latest tag
def pullBaseImage = true
// Add container metadata via the LABEL instruction
def labels = Map("version" -> "1.0")
// TCP ports the container will listen to
def exposedPorts = Seq(8080, 443)
// UDP ports the container will listen to
def exposedUdpPorts = Seq(80)
// The names of mount points, these will be translated to VOLUME instructions
def volumes = Seq("/v1", "/v2")
// Environment variables to be set in the container (ENV instructions)
def envVars = Map("foo" -> "bar", "foobar" -> "barfoo")
// Command line arguments to be passed to the executable
def commandArgs = Seq("--port=80", "-v")
// Add RUN instructions
def run = Seq(
  "/bin/bash -c 'echo Hello World!'",
  "useradd -ms /bin/bash new-user"
)
// User to use when running the image
def user = "nobody"
// Optionally override the docker executable to use something else
def executable = "podman"
```

Native Image parameters:

```scala
// Define the output binary name
def nativeImageName = "myAppName"
// Set the GraalVM version
def nativeImageGraalVmJvmId = T {sys.env.getOrElse("GRAALVM_ID", "graalvm-java17:22.2.0")}
// Define the classpath
def nativeImageClassPath = runClasspath()
// Define your application main class
def nativeImageMainClass = "com.domain.myClass"
// Sets GraalVM Native Image options, depends on your application uses
def nativeImageOptions = Seq(
  "--no-fallback",
)
```

For more details, check the [mill-native-image][mill-native-image-src] source code where all available options is shown.

## Acknowledgements

This plugin would not be possible without the amazing work in the [mill-docker-plugin][docker-plugin] and [mill-native-image][mill-native-image] plugins.


[mill]: https://com-lihaoyi.github.io/mill/mill/Intro_to_Mill.html
[docker-plugin]: https://com-lihaoyi.github.io/mill/mill/Plugin_Docker.html
[mill-native-image]: https://github.com/alexarchambault/mill-native-image
[mill-native-image-src]: https://github.com/alexarchambault/mill-native-image/blob/master/plugin/src/io/github/alexarchambault/millnativeimage/NativeImage.scala
[graalvm-install]: https://www.graalvm.org/22.1/reference-manual/native-image/
[zio-scalajs-stack-build]: https://github.com/carlosedp/zio-scalajs-stack/blob/5c9e2817480ba7ef263770108197a36ff493dea7/build.sc#L51
[nativeimage-bug]: https://github.com/carlosedp/zio-scalajs-stack/issues/8
