import mill._
import mill.scalalib._
import mill.scalalib.scalafmt._
import mill.scalalib.publish._
import mill.scalalib.api.ZincWorkerUtil.{scalaBinaryVersion, scalaNativeBinaryVersion}
import coursier.Repositories
import os.Path

import $ivy.`com.goyeau::mill-scalafix::0.2.11`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`
import io.kipp.mill.ci.release.{CiReleaseModule, SonatypeHost}
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.1`
import de.tobiasroeser.mill.integrationtest._

// Used versions
object versions {
  val millVersions           = Seq("0.10.12", "0.11.1")
  val millBinaryVersions     = millVersions.map(scalaNativeBinaryVersion)
  val scala213               = "2.13.11"
  val millnativeimage_plugin = "0.1.25"
  val organizeimports        = "0.6.0"
  val utest                  = "0.8.1"
}

def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(millVersion)
def millVersion(binaryVersion:     String) = versions.millVersions.find(v => millBinaryVersion(v) == binaryVersion).get

object plugin extends Cross[MillcrossplatformCross](versions.millBinaryVersions: _*)
class MillcrossplatformCross(millBinaryVersion: String)
  extends ScalaModule
  with CiReleaseModule
  with ScalafixModule
  with ScalafmtModule {
  def scalaVersion            = versions.scala213
  override def millSourcePath = super.millSourcePath / os.up
  override def scalacOptions  = super.scalacOptions() ++ Seq("-Ywarn-unused", "-deprecation", "-feature")
  override def artifactName   = s"mill-docker-nativeimage_mill$millBinaryVersion"

  def scalafixIvyDeps                     = Agg(ivy"com.github.liancheng::organize-imports:${versions.organizeimports}")
  override def scalafixScalaBinaryVersion = scalaBinaryVersion(versions.scala213)

  def repositoriesTask = T.task {
    super.repositoriesTask() ++ Seq(Repositories.sonatype("snapshots"), Repositories.sonatypeS01("snapshots"))
  }
  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-scalalib:${millVersion(millBinaryVersion)}",
    ivy"io.github.alexarchambault.mill::mill-native-image_mill$millBinaryVersion::${versions.millnativeimage_plugin}",
  )

  def pomSettings = PomSettings(
    description = "A Mill plugin to generate Docker images with Native Image executable (GraalVM binary).",
    organization = "com.carlosedp",
    url = "https://github.com/carlosedp/mill-docker-nativeimage",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("carlosedp", "mill-docker-nativeimage"),
    developers = Seq(
      Developer(
        "carlosedp",
        "Carlos Eduardo de Paula",
        "https://github.com/carlosedp",
      )
    ),
  )

  def publishVersion: T[String] = T {
    val isTag = T.ctx().env.get("GITHUB_REF").exists(_.startsWith("refs/tags"))
    val state = VcsVersion.vcsState()
    if (state.commitsSinceLastTag == 0 && isTag) {
      state.stripV(state.lastTag.get)
    } else {
      val v = state.stripV(state.lastTag.get).split('.')
      s"${v(0)}.${(v(1).toInt) + 1}-SNAPSHOT"
    }
  }
  override def sonatypeHost = Some(SonatypeHost.s01)
}

object itest extends Cross[itestCross]("0.10.12", "0.11.0-M8")
class itestCross(millVersion: String) extends MillIntegrationTestModule {
  override def millSourcePath: Path =
    super.millSourcePath / os.up / millVersion.split('.').take(2).mkString(".")
  def millTestVersion = millVersion
  def pluginsUnderTest = Seq(
    `plugin`(millBinaryVersion(millVersion))
  )
  def testBase = millSourcePath / "src"
}

// -----------------------------------------------------------------------------
// Command Aliases
// -----------------------------------------------------------------------------
// Alias commands are run like `./mill run [alias]`
// Define the alias as a map element containing the alias name and a Seq with the tasks to be executed
val aliases: Map[String, Seq[String]] = Map(
  "lint"     -> Seq("mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources", "__.fix"),
  "fmt"      -> Seq("mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources"),
  "checkfmt" -> Seq("mill.scalalib.scalafmt.ScalafmtModule/checkFormatAll __.sources"),
  "deps"     -> Seq("mill.scalalib.Dependency/showUpdates"),
  "testall"  -> Seq("__.test"),
  "pub"      -> Seq("io.kipp.mill.ci.release.ReleaseModule/publishAll"),
)

def run(ev: eval.Evaluator, alias: String = "") = T.command {
  aliases.get(alias) match {
    case Some(t) =>
      mill.main.MainModule.evaluateTasks(
        ev,
        t.flatMap(x => Seq(x, "+")).flatMap(_.split("\\s+")).init,
        mill.define.SelectMode.Single,
      )(identity)
    case None =>
      Console.err.println("Use './mill run [alias]'."); Console.out.println("Available aliases:")
      aliases.foreach(x => Console.out.println(s"${x._1.padTo(15, ' ')} - Commands: (${x._2.mkString(", ")})"));
      sys.exit(1)
  }
}
