import com.github.pshirshov.izumi.sbt.deps.IzumiDeps._
import com.github.pshirshov.izumi.sbt.ConvenienceTasksPlugin.Keys.defaultStubPackage
import com.github.pshirshov.izumi.sbt.IzumiScopesPlugin.ProjectReferenceEx
import com.github.pshirshov.izumi.sbt.IzumiSettingsGroups.autoImport.SettingsGroupId._
import com.lightbend.paradox.sbt.ParadoxPlugin.autoImport.paradoxTheme
import com.typesafe.sbt.pgp.PgpSettings
import coursier.ShadingPlugin.autoImport.shadingNamespace
import sbt.Keys.{baseDirectory, pomExtra, publishMavenStyle, sourceDirectory}
import sbtassembly.Assembly
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._


enablePlugins(IzumiEnvironmentPlugin)
enablePlugins(IzumiDslPlugin)
enablePlugins(GitStampPlugin)
disablePlugins(AssemblyPlugin)

name := "izumi-r2"

val AppSettings = SettingsGroupId()
val LibSettings = SettingsGroupId()
val SbtSettings = SettingsGroupId()
val ShadingSettings = SettingsGroupId()
val WithoutBadPlugins = SettingsGroupId()

scalacOptions in ThisBuild ++= CompilerOptionsPlugin.dynamicSettings(scalaOrganization.value, scalaVersion.value, isSnapshot.value)
defaultStubPackage := Some("com.github.pshirshov.izumi")

val baseSettings = new GlobalSettings {
  override protected val settings: Map[SettingsGroupId, ProjectSettings] = Map(
    GlobalSettingsGroup -> new ProjectSettings {
      override val settings: Seq[sbt.Setting[_]] = Seq(
        organization := "com.github.pshirshov.izumi.r2"
        , crossScalaVersions := Seq(
          V.scala_212
        )
        , publishMavenStyle in Global := true
        , sonatypeProfileName := "com.github.pshirshov"
        , publishTo := Some(
          if (isSnapshot.value)
            Opts.resolver.sonatypeSnapshots
          else
            Opts.resolver.sonatypeStaging
        )
        , credentials in Global ++= Seq(new File("credentials.sonatype-nexus.properties")).filter(_.exists()).map(Credentials(_))
        , pomExtra in Global := <url>https://bitbucket.org/pshirshov/izumi-r2</url>
          <licenses>
            <license>
              <name>BSD-style</name>
              <url>http://www.opensource.org/licenses/bsd-license.php</url>
              <distribution>repo</distribution>
            </license>
          </licenses>
          <developers>
            <developer>
              <id>pshirshov</id>
              <name>Pavel Shirshov</name>
              <url>https://github.com/pshirshov</url>
            </developer>
          </developers>

        , releaseProcess := Seq[ReleaseStep](
          checkSnapshotDependencies, // : ReleaseStep
          inquireVersions, // : ReleaseStep
          runClean, // : ReleaseStep
          runTest, // : ReleaseStep
          setReleaseVersion, // : ReleaseStep
          commitReleaseVersion, // : ReleaseStep, performs the initial git checks
          tagRelease, // : ReleaseStep
          //publishArtifacts,                       // : ReleaseStep, checks whether `publishTo` is properly set up
          setNextVersion, // : ReleaseStep
          commitNextVersion, // : ReleaseStep
          pushChanges // : ReleaseStep, also checks that an upstream branch is properly configured
        )
        , addCompilerPlugin(R.kind_projector)
      )
    }
    , LibSettings -> new ProjectSettings {
      override val settings: Seq[sbt.Setting[_]] = Seq(
        Seq(
          libraryDependencies ++= R.essentials
          , libraryDependencies ++= T.essentials
        )
      ).flatten
    }
    , ShadingSettings -> new ProjectSettings {
      override val plugins: Set[Plugins] = Set(ShadingPlugin)

      override val settings: Seq[sbt.Setting[_]] = Seq(
        inConfig(_root_.coursier.ShadingPlugin.Shading)(PgpSettings.projectSettings ++ PublishingPlugin.projectSettings) ++
          _root_.coursier.ShadingPlugin.projectSettings ++
          Seq(
            publish := publish.in(Shading).value
            , publishLocal := publishLocal.in(Shading).value
            , PgpKeys.publishSigned := PgpKeys.publishSigned.in(Shading).value
            , PgpKeys.publishLocalSigned := PgpKeys.publishLocalSigned.in(Shading).value
            , shadingNamespace := "izumi.shaded"
            , shadeNamespaces ++= Set(
              "fastparse"
              , "sourcecode"
              //            , "net.sf.cglib"
              //            , "org.json4s"
            )
          )
      ).flatten
    }
    , SbtSettings -> new ProjectSettings {
      override val plugins: Set[Plugins] = Set(ScriptedPlugin)

      override val settings: Seq[sbt.Setting[_]] = Seq(
        Seq(
          target ~= { t => t.toPath.resolve("primary").toFile }
          , scriptedLaunchOpts := {
            scriptedLaunchOpts.value ++
              Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
          }
          , scriptedBufferLog := false

          , crossScalaVersions := Seq(
            V.scala_212
          )
          , libraryDependencies ++= Seq(
            "org.scala-sbt" % "sbt" % sbtVersion.value
          )
          , sbtPlugin := true
        )
      ).flatten
    }
    , AppSettings -> new ProjectSettings {
      override val disabledPlugins: Set[AutoPlugin] = Set(SitePlugin)
      override val plugins = Set(AssemblyPlugin)
    }
    , WithoutBadPlugins -> new ProjectSettings {
      override val disabledPlugins: Set[AutoPlugin] = Set(AssemblyPlugin, SitePlugin)
    }
  )
}
// --------------------------------------------

val inRoot = In(".")

val inShade = In("shade")
  .withModuleSettings(WithoutBadPlugins)

val inSbt = In("sbt")
  .withModuleSettings(SbtSettings)
  .withModuleSettings(WithoutBadPlugins)
val inDiStage = In("distage")
  .withModuleSettings(LibSettings)
  .withModuleSettings(WithoutBadPlugins)
val inLogStage = In("logstage")
  .withModuleSettings(LibSettings)
  .withModuleSettings(WithoutBadPlugins)
val inFundamentals = In("fundamentals")
  .withModuleSettings(LibSettings)
  .withModuleSettings(WithoutBadPlugins)
val inIdealinguaBase = In("idealingua")
val inIdealingua = inIdealinguaBase
  .withModuleSettings(LibSettings)
  .withModuleSettings(WithoutBadPlugins)


// --------------------------------------------

lazy val fundamentalsCollections = inFundamentals.as.module
lazy val fundamentalsPlatform = inFundamentals.as.module
lazy val fundamentalsFunctional = inFundamentals.as.module

lazy val fundamentals: Seq[ProjectReferenceEx] = Seq(
  fundamentalsCollections
  , fundamentalsPlatform
  , fundamentalsFunctional
)
// --------------------------------------------
val globalDefs = setup(baseSettings)
  .withSharedLibs(fundamentals: _*)
// --------------------------------------------

lazy val fundamentalsReflection = inFundamentals.as.module
  .dependsOn(fundamentalsPlatform)
  .settings(
    libraryDependencies ++= Seq(
      R.scala_reflect
    )
  )

lazy val distageModel = inDiStage.as.module
  .depends(fundamentalsReflection)

lazy val distageProxyCglib = inDiStage.as.module
  .depends(distageModel)
  .settings(
    libraryDependencies ++= Seq(
      R.scala_reflect
      , R.cglib_nodep
    )
  )

lazy val distagePlugins = inDiStage.as.module
  .depends(distageCore)
  .settings(
    libraryDependencies ++= Seq(R.fast_classpath_scanner)
  )

lazy val distageApp = inDiStage.as.module
  .depends(distageCore, distagePlugins, distageConfig, logstageDi)


lazy val distageConfig = inDiStage.as.module
  .depends(distageCore)
  .settings(
    libraryDependencies ++= Seq(
      R.typesafe_config
    )
  )


lazy val distageCore = inDiStage.as.module
  .depends(fundamentalsFunctional, distageModel, distageProxyCglib)
  .settings(
    libraryDependencies ++= Seq(
      R.scala_reflect
    )
  )

lazy val distageStatic = inDiStage.as.module
  .depends(distageCore)
  .settings(
    libraryDependencies += R.shapeless
  )

lazy val logstageApiBase = inLogStage.as.module

lazy val distageCats = inDiStage.as.module
  .depends(distageModel, distageCore.testOnlyRef)
  .settings(
    libraryDependencies += R.cats_kernel
    , libraryDependencies ++= T.cats_all
  )


lazy val logstageApiBaseMacro = inLogStage.as.module
  .depends(logstageApiBase)
  .settings(
    libraryDependencies ++= Seq(
      R.scala_reflect
    )
  )

lazy val logstageApiLogger = inLogStage.as.module
  .depends(logstageApiBaseMacro)


lazy val logstageSinkConsole = inLogStage.as.module
  .depends(logstageApiBase)
  .depends(Seq(
    logstageApiLogger
  ).map(_.testOnlyRef): _*)

lazy val logstageAdapterSlf4j = inLogStage.as.module
  .depends(logstageApiLogger)
  .settings(
    libraryDependencies += R.slf4j_api
    , compileOrder in Compile := CompileOrder.Mixed
    , compileOrder in Test := CompileOrder.Mixed
  )

lazy val logstageDi = inLogStage.as.module
  .depends(
    logstageApiLogger
    , distageModel
  )
  .depends(Seq(
    distageCore
  ).map(_.testOnlyRef): _*)

lazy val logstageRenderingJson4s = inLogStage.as.module
  .depends(logstageApiLogger)
  .depends(Seq(
    logstageSinkConsole
  ).map(_.testOnlyRef): _*)
  .settings(libraryDependencies ++= Seq(R.json4s_native))

lazy val logstageSinkFile = inLogStage.as.module
  .depends(logstageApiBase)
  .depends(Seq(
    logstageApiLogger
  ).map(_.testOnlyRef): _*)

lazy val logstageSinkSlf4j = inLogStage.as.module
  .depends(logstageApiBase)
  .depends(Seq(
    logstageApiLogger
  ).map(_.testOnlyRef): _*)
  .settings(libraryDependencies ++= Seq(R.slf4j_api, T.slf4j_simple))



lazy val idealinguaModel = inIdealingua.as.module
  .settings()

lazy val idealinguaRuntimeRpc = inIdealingua.as.module

lazy val idealinguaTestDefs = inIdealingua.as.module.dependsOn(idealinguaRuntimeRpc, idealinguaRuntimeRpcCirce)

lazy val idealinguaRuntimeRpcCirce = inIdealingua.as.module
  .depends(idealinguaRuntimeRpc)
  .settings(libraryDependencies ++= R.circe)

lazy val idealinguaRuntimeRpcCats = inIdealingua.as.module
  .depends(idealinguaRuntimeRpc)
  .settings(libraryDependencies ++= R.cats_all)


lazy val idealinguaRuntimeRpcHttp4s = inIdealingua.as.module
  .depends(idealinguaRuntimeRpcCirce, idealinguaRuntimeRpcCats)
  .depends(Seq(idealinguaTestDefs).map(_.testOnlyRef): _*)
  .settings(libraryDependencies ++= R.http4s_all)


lazy val fastparseShaded = inShade.as.module
  .settings(libraryDependencies ++= Seq(R.fastparse))
  .settings(ShadingSettings)

lazy val idealinguaCore = inIdealingua.as.module
  .settings(libraryDependencies ++= Seq(R.scala_reflect, R.scalameta) ++ Seq(T.scala_compiler))
  .depends(idealinguaModel, idealinguaRuntimeRpc, fastparseShaded)
  .depends(Seq(idealinguaTestDefs).map(_.testOnlyRef): _*)
  .settings(ShadingSettings)

lazy val idealinguaExtensionRpcFormatCirce = inIdealingua.as.module
  .depends(idealinguaCore, idealinguaRuntimeRpcCirce)
  .depends(Seq(idealinguaTestDefs).map(_.testOnlyRef): _*)


lazy val idealinguaCompiler = inIdealinguaBase.as.module
  .depends(idealinguaCore, idealinguaExtensionRpcFormatCirce)
  .settings(AppSettings)
  .settings(
    libraryDependencies ++= Seq(R.scallop)
    , mainClass in assembly := Some("com.github.pshirshov.izumi.idealingua.compiler.CliIdlCompiler")
  )
  .settings(addArtifact(artifact in(Compile, assembly), assembly))


lazy val sbtIzumi = inSbt.as
  .module

lazy val sbtIzumiDeps = inSbt.as
  .module
  .settings(withBuildInfo("com.github.pshirshov.izumi.sbt.deps", "Izumi"))

lazy val sbtIdealingua = inSbt.as
  .module
  .depends(idealinguaCore, idealinguaExtensionRpcFormatCirce)

lazy val sbtTests = inSbt.as
  .module
  .depends(sbtIzumiDeps, sbtIzumi, sbtIdealingua)

lazy val logstage: Seq[ProjectReference] = Seq(
  logstageDi
  , logstageApiLogger
  , logstageSinkConsole
  , logstageSinkFile
  , logstageSinkSlf4j
  , logstageAdapterSlf4j
)
lazy val distage: Seq[ProjectReference] = Seq(
  distageCore
  , distageApp
)
lazy val idealingua: Seq[ProjectReference] = Seq(
  idealinguaCore
  , idealinguaRuntimeRpc
  , idealinguaRuntimeRpcHttp4s
  , idealinguaRuntimeRpcCats
  , idealinguaRuntimeRpcCirce
  , idealinguaExtensionRpcFormatCirce
  , idealinguaCompiler
)
lazy val izsbt: Seq[ProjectReference] = Seq(
  sbtIzumi, sbtIdealingua, sbtTests
)

lazy val allProjects = distage ++ logstage ++ idealingua ++ izsbt

lazy val `izumi-r2` = inRoot.as
  .root
  .transitiveAggregate(allProjects: _*)
  .enablePlugins(ParadoxSitePlugin, SitePlugin, GhpagesPlugin)
  .settings(
    paradoxTheme := Some(builtinParadoxTheme("generic"))
    , sourceDirectory in Paradox := baseDirectory.value / "doc" / "paradox"
    , previewFixedPort := Some(9999)
    , scmInfo := Some(ScmInfo(url("https://github.com/pshirshov/izumi-r2"), "git@github.com:pshirshov/izumi-r2.git"))
    , git.remoteRepo := scmInfo.value.get.connection
  )
