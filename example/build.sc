import mill._, mill.scalalib._, mill.scalalib.scalafmt._
import $ivy.`com.carlosedp::mill-docker-nativeimage::0.4-SNAPSHOT`
import com.carlosedp.milldockernative.DockerNative

object hello extends ScalaModule with DockerNative {
  def scalaVersion = "3.3.0-RC2"
  // def ivyDeps = ...
//   def nativeImageClassPath = runClasspath()
  object dockerNative extends DockerNativeConfig {
    // Native Image parameters
    def nativeImageName         = "hello"
    def nativeImageGraalVmJvmId = T("graalvm-java17:22.3.1")
    def nativeImageClassPath    = runClasspath()
    def nativeImageMainClass    = "com.domain.Hello.Hello"
    // GraalVM parameters depending on your application needs
    def nativeImageOptions = Seq(
      "--no-fallback",
      "--enable-url-protocols=http,https",
      "-Djdk.http.auth.tunneling.disabledSchemes=",
    ) ++ (if (sys.props.get("os.name").contains("Linux")) Seq("--static") else Seq.empty)

    // Docker image parameters
    def baseImage    = "ubuntu:22.04"
    def tags         = List("docker.io/myuser/helloapp")
    def exposedPorts = Seq(8080)
  }

  object test extends Tests {
    // ...
  }
}
