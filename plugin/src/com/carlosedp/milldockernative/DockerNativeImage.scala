package com.carlosedp
package milldockernative

import scala.collection.immutable._

import io.github.alexarchambault.millnativeimage.NativeImage
import mill._
import mill.scalalib.JavaModule
import os.Shellable.IterableShellable

trait DockerNative { outer: JavaModule =>

  trait DockerNativeConfig extends mill.Module with NativeImage {

    /**
     * Tags that should be applied to the built image In the standard
     * registry/repository:tag format
     */
    def tags:            T[Seq[String]]         = T(List(outer.artifactName()))
    def labels:          T[Map[String, String]] = Map.empty[String, String]
    def baseImage:       T[String]              = "docker.io/redhat/ubi8"
    def pullBaseImage:   T[Boolean]             = T(baseImage().endsWith(":latest"))
    def coursierVersion: T[String]              = "v2.1.0-RC5"
    def baseDockerImage: T[String]              = "nativeimagebase:graalvm"
    def baseDockerFile: T[String] =
      s"""FROM ubuntu:22.04
          RUN apt-get update -q -y && apt-get install -q -y build-essential libz-dev locales --no-install-recommends
          RUN locale-gen en_US.UTF-8
          ENV LANG en_US.UTF-8
          ENV LANGUAGE en_US:en
          ENV LC_ALL en_US.UTF-8""".stripMargin

    /**
     * TCP Ports the container will listen to at runtime.
     *
     * See also the Docker docs on
     * [[https://docs.docker.com/engine/reference/builder/#expose ports]] for
     * more information.
     */
    def exposedPorts: T[Seq[Int]] = Seq.empty[Int]

    /**
     * UDP Ports the container will listen to at runtime.
     *
     * See also the Docker docs on
     * [[https://docs.docker.com/engine/reference/builder/#expose ports]] for
     * more information.
     */
    def exposedUdpPorts: T[Seq[Int]] = Seq.empty[Int]

    /**
     * The names of mount points.
     *
     * See also the Docker docs on
     * [[https://docs.docker.com/engine/reference/builder/#volume volumes]] for
     * more information.
     */
    def volumes: T[Seq[String]] = Seq.empty[String]

    /**
     * Environment variables to be set in the container.
     *
     * See also the Docker docs on
     * [[https://docs.docker.com/engine/reference/builder/#env ENV]] for more
     * information.
     */
    def envVars: T[Map[String, String]] = Map.empty[String, String]

    /**
     * Commands to add as RUN instructions.
     *
     * See also the Docker docs on
     * [[https://docs.docker.com/engine/reference/builder/#run RUN]] for more
     * information.
     */
    def run: T[Seq[String]] = Seq.empty[String]

    /**
     * Command line arguments to be passed to the executable
     */
    def commandArgs: T[Seq[String]] = Seq.empty[String]

    /**
     * Any applicable string to the USER instruction.
     *
     * An empty string will be ignored and will result in USER not being
     * specified. See also the Docker docs on
     * [[https://docs.docker.com/engine/reference/builder/#user USER]] for more
     * information.
     */
    def user: T[String] = ""

    /**
     * The name of the executable to use, the default is "docker".
     */
    def executable: T[String] = "docker"

    private def baseImageCacheBuster: T[
      (Boolean,
        Double,
      )
    ] = T.input {
      val pull = pullBaseImage()
      if (pull) (pull, Math.random()) else (pull, 0d)
    }

    def dockerfile: T[String] = T {
      val nativeBinName = nativeImage().path.last
      val labelRhs = labels().map { case (k, v) =>
        val lineBrokenValue = v
          .replace("\r\n", "\\\r\n")
          .replace("\n", "\\\n")
          .replace("\r", "\\\r")
        s""""$k"="$lineBrokenValue""""
      }
        .mkString(" ")

      val lines = List(
        if (labels().isEmpty) "" else s"LABEL $labelRhs",
        if (exposedPorts().isEmpty) ""
        else
          exposedPorts()
            .map(port => s"$port/tcp")
            .mkString("EXPOSE ", " ", ""),
        if (exposedUdpPorts().isEmpty) ""
        else
          exposedUdpPorts()
            .map(port => s"$port/udp")
            .mkString("EXPOSE ", " ", ""),
        envVars().map { case (env, value) =>
          s"ENV $env=$value"
        }
          .mkString("\n"),
        if (volumes().isEmpty) ""
        else volumes().map(v => s"\"$v\"").mkString("VOLUME [", ", ", "]"),
        run().map(c => s"RUN $c").mkString("\n"),
        if (user().isEmpty) "" else s"USER ${user()}",
      ).filter(_.nonEmpty).mkString(sys.props("line.separator"))

      val cmdArgs =
        if (commandArgs().isEmpty) ""
        else commandArgs().map(v => s"\"$v\"").mkString("CMD [", ", ", "]")

      s"""
         |FROM ${baseImage()}
         |$lines
         |COPY $nativeBinName /$nativeBinName
         |ENTRYPOINT ["/$nativeBinName"]
         |$cmdArgs""".stripMargin
    }

    //  The image that will be generated to build the native image
    def buildBaseDockerImage = T.input {
      os.proc("docker", "build", "-t", baseDockerImage(), ".", "-f", writeDockerFile()).call(check = false)
      baseDockerImage()
    }
    def writeDockerFile = T {
      val filename = "Dockerfile.nativeimagebase"
      os.write.over(
        T.dest / filename,
        baseDockerFile(),
      )
      T.dest / filename.toString()
    }

    // Build native image on a Docker container if not running on Linux
    override def nativeImageDockerParams = T {
      if (sys.props.get("os.name").contains("Linux") == false) {
        Some(
          NativeImage.DockerParams(
            imageName = buildBaseDockerImage(),
            prepareCommand = "",
            csUrl =
              s"https://github.com/coursier/coursier/releases/download/${coursierVersion()}/cs-${sys.props.get("os.arch").get}-pc-linux.gz",
            extraNativeImageArgs = Nil,
          )
        )
      } else { Option.empty[NativeImage.DockerParams] }
    }

    /**
     * Convenience task to build the Linux Native Image binary of the
     * application in a Docker container
     */
    final def buildBin = T {
      val asmPath = nativeImage().path
      asmPath
    }

    /**
     * Builds a Docker Image containing the Native Image binary of the
     * application The native image is built in a Docker container if host
     * operating system is not Linux
     */
    final def build = T {
      val dest = T.dest

      val asmPath = nativeImage().path
      os.copy(asmPath, dest / asmPath.last)

      os.write(dest / "Dockerfile", dockerfile())

      val log = T.log

      val tagArgs = tags().flatMap(t => List("-t", t))

      val (pull, _)      = baseImageCacheBuster()
      val pullLatestBase = IterableShellable(if (pull) Some("--pull") else None)

      val result = os
        .proc(executable(), "build", tagArgs, pullLatestBase, dest)
        .call(stdout = os.Inherit, stderr = os.Inherit)

      log.info(
        s"Docker build completed ${if (result.exitCode == 0) "successfully"
          else "unsuccessfully"} with ${result.exitCode}"
      )
      tags()
    }

    /**
     * Push the generated Docker image to a registry
     */
    final def push(
    ) = T.command {
      val tags = build()
      tags.foreach(t =>
        os.proc(executable(), "push", t)
          .call(stdout = os.Inherit, stderr = os.Inherit)
      )
    }
  }
}
