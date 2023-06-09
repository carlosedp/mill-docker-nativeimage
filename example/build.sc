import mill._, mill.scalalib._, mill.scalalib.scalafmt._
import $ivy.`com.carlosedp::mill-docker-nativeimage::0.6.0`
import com.carlosedp.milldockernative.DockerNative

object hello extends ScalaModule with DockerNative {
  def scalaVersion = "3.3.0"
  def ivyDeps = Agg(
    ivy"dev.zio::zio:2.0.6",
    ivy"dev.zio::zio-http:0.0.3",
  )

  object dockerNative extends DockerNativeConfig {
    // Native Image parameters
    def nativeImageName         = "hello"
    def nativeImageGraalVmJvmId = T("graalvm-java17:22.3.1")
    def nativeImageClassPath    = runClasspath()
    def nativeImageMainClass    = "com.domain.Main.MainApp"
    // GraalVM parameters needed by ZIO and ZIO-http
    def nativeImageOptions = Seq(
      "--no-fallback",
      "--enable-http",
      "--enable-url-protocols=http,https",
      "--install-exit-handlers",
      "-Djdk.http.auth.tunneling.disabledSchemes=",
      "--initialize-at-run-time=io.netty.channel.DefaultFileRegion",
      "--initialize-at-run-time=io.netty.channel.epoll.Native",
      "--initialize-at-run-time=io.netty.channel.epoll.Epoll",
      "--initialize-at-run-time=io.netty.channel.epoll.EpollEventLoop",
      "--initialize-at-run-time=io.netty.channel.epoll.EpollEventArray",
      "--initialize-at-run-time=io.netty.channel.kqueue.KQueue",
      "--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventLoop",
      "--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventArray",
      "--initialize-at-run-time=io.netty.channel.kqueue.Native",
      "--initialize-at-run-time=io.netty.channel.unix.Limits",
      "--initialize-at-run-time=io.netty.channel.unix.Errors",
      "--initialize-at-run-time=io.netty.channel.unix.IovArray",
      "--initialize-at-run-time=io.netty.handler.ssl.BouncyCastleAlpnSslUtils",
      "--initialize-at-run-time=io.netty.handler.codec.compression.ZstdOptions",
      "--initialize-at-run-time=io.netty.incubator.channel.uring.Native",
      "--initialize-at-run-time=io.netty.incubator.channel.uring.IOUring",
      "--initialize-at-run-time=io.netty.incubator.channel.uring.IOUringEventLoopGroup",
    ) ++ (if (sys.props.get("os.name").contains("Linux")) Seq("--static") else Seq.empty)

    // Docker image parameters
    def baseImage    = "ubuntu:22.04"
    def tags         = List("docker.io/myuser/helloapp")
    def exposedPorts = Seq(8080)
  }

  object test extends Tests {
    def ivyDeps = Agg(
      ivy"dev.zio::zio-test:2.0.6",
      ivy"dev.zio::zio-test-sbt:2.0.6",
    )
    def testFramework = T("zio.test.sbt.ZTestFramework")
  }
}
