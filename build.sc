import mill._
import mill.scalalib._
import mill.scalalib.scalafmt._
import mill.scalalib.publish._
import mill.scalalib.api.ZincWorkerUtil._

import $ivy.`com.goyeau::mill-scalafix::0.5.1`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.1`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import $ivy.`io.chris-kipp::mill-ci-release::0.1.10`
import io.kipp.mill.ci.release.{CiReleaseModule, SonatypeHost}
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.1`
import de.tobiasroeser.mill.integrationtest._
import $ivy.`com.carlosedp::mill-aliases::0.5.0`
import com.carlosedp.aliases._

val millVersions           = Seq("0.10.12", "0.11.0") // scala-steward:off
val scala213               = "2.13.15"
val millnativeimage_plugin = "0.1.30"
val pluginName             = "mill-docker-nativeimage"

object plugin extends Cross[Plugin](millVersions)
trait Plugin  extends Cross.Module[String]
    with ScalaModule
    with Publish
    with ScalafixModule
    with ScalafmtModule {
    val millVersion   = crossValue
    def scalaVersion  = scala213
    def artifactName  = s"${pluginName}_mill${scalaNativeBinaryVersion(millVersion)}"
    def scalacOptions = super.scalacOptions() ++ Seq("-Ywarn-unused", "-deprecation", "-feature")

    def compileIvyDeps = super.compileIvyDeps() ++ Agg(
        ivy"com.lihaoyi::mill-scalalib:${millVersion}"
    )
    def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"io.github.alexarchambault.mill::mill-native-image_mill${scalaNativeBinaryVersion(millVersion)}::${millnativeimage_plugin}"
    )

    def sources = T.sources {
        super.sources() ++ Seq(
            millSourcePath / s"src-mill${scalaNativeBinaryVersion(millVersion)}"
        ).map(PathRef(_))
    }
}

trait Publish extends CiReleaseModule {
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

// object itest extends Cross[itestCross](millVersions)
// trait itestCross extends Cross.Module[String] with MillIntegrationTestModule {
//   override def millSourcePath =
//     super.millSourcePath / os.up / crossValue.split('.').take(2).mkString(".")
//   def millTestVersion = crossValue
//   def pluginsUnderTest = Seq(
//     `plugin`(crossValue)
//   )
//   def testBase = millSourcePath / "src"
// }

object MyAliases extends Aliases {
    def fmt      = alias("mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources")
    def checkfmt = alias("mill.scalalib.scalafmt.ScalafmtModule/checkFormatAll __.sources")
    def lint     = alias("mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources", "__.fix")
    def deps     = alias("mill.scalalib.Dependency/showUpdates")
    def pub      = alias("io.kipp.mill.ci.release.ReleaseModule/publishAll")
    def publocal = alias("__.publishLocal")
    def testall  = alias("__.test")
}
