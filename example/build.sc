import mill._, mill.scalalib._, mill.scalalib.scalafmt._
import $ivy.`com.carlosedp::mill-docker-nativeimage::0.6.0`
import com.carlosedp.milldockernative.DockerNative

object hello extends ScalaModule with DockerNative {
  def scalaVersion = "3.3.0"
  def ivyDeps = Agg(
    ivy"dev.zio::zio:2.0.15",
    ivy"dev.zio::zio-http:3.0.0-RC2",
  )
  // GraalVM parameters needed by ZIO and ZIO-http
  def useNativeConfig = T.input(T.env.get("NATIVECONFIG_GEN").contains("true"))
  def forkArgs = T {
    if (useNativeConfig())
      Seq(s"-agentlib:native-image-agent=config-merge-dir=${resources().head.path}/META-INF/native-image")
    else Seq.empty
  }

  object dockerNative extends DockerNativeConfig {
    // Native Image parameters
    def nativeImageName         = "hello"
    def nativeImageGraalVmJvmId = T("graalvm-java17:22.3.2")
    def nativeImageClassPath    = runClasspath()
    def nativeImageMainClass    = "com.domain.Main.MainApp"
    def nativeImageOptions = super.nativeImageOptions() ++
      // GraalVM initializes all classes at runtime, so lets ignore all configs from jars since some change this behavior
      Seq("--exclude-config", "/.*.jar", ".*.properties") ++
      (if (sys.props.get("os.name").contains("Linux")) Seq("--static") else Seq.empty)

    // Docker image parameters
    def baseImage    = "ubuntu:22.04"
    def tags         = List("docker.io/myuser/helloapp")
    def exposedPorts = Seq(8080)
  }

  object test extends ScalaTests with TestModule.ZioTest {
    def ivyDeps = Agg(
      ivy"dev.zio::zio-test:2.0.15",
      ivy"dev.zio::zio-test-sbt:2.0.15",
    )
  }
}
