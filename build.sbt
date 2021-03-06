import sbt.Project.projectToRef
import sbt.Project.projectToRef
name := "Main Play Project"
version := "1.0-SNAPSHOT"

updateOptions := updateOptions.value.withCachedResolution(true)
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature")
scalaVersion in ThisBuild := "2.12.1"

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// use eliding to drop some debug code in the production build
lazy val elideOptions = settingKey[Seq[String]]("Set limit for elidable functions")

lazy val clients = Seq(client)

val dbDeps = Seq("mysql" % "mysql-connector-java" % "6.0.6")

val slickSharedDeps = Seq("com.typesafe.slick" %% "slick" % "3.2.0")

val slickServerDeps = Seq(
 "com.typesafe.play" %% "play-slick" % "3.0.0"
) ++ slickSharedDeps ++ dbDeps

val poiDeps = Seq(
 "org.apache.poi" % "poi" % "3.17",
 "org.apache.poi" % "poi-ooxml" % "3.17"
)

lazy val services = (project in file("services")).settings(
 libraryDependencies += filters,
 libraryDependencies += guice
)

lazy val server = (project in file("server")).settings(
 //PlayKeys.playRunHooks += HttpRequestOnStartPlayRunHook(baseDirectory.value),
 scalaJSProjects := clients,
 pipelineStages := Seq(scalaJSProd, gzip),
 libraryDependencies += guice,
 libraryDependencies ++= dbDeps,
 libraryDependencies ++= slickServerDeps,
 libraryDependencies ++= poiDeps,
 libraryDependencies ++= Seq(
  // https://mvnrepository.com/artifact/javax.xml.stream/stax-api
  //"javax.xml.stream" % "stax-api" % "1.0-2",
  "com.vmunier" %% "scalajs-scripts" % "1.1.0",
  //  "com.vmunier" %% "play-scalajs-scripts" % "0.4.0",
  "org.webjars" % "jquery" % "3.2.1"
 )
).enablePlugins(PlayScala)
 .aggregate(clients.map(projectToRef): _*)
 .dependsOn(services, sharedJvm /*, macros*/)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared")).
 settings(
  libraryDependencies ++= (Seq(
   "io.circe" %%% "circe-core" % "0.8.0",
   "io.circe" %%% "circe-generic" % "0.8.0",
   "io.circe" %%% "circe-parser" % "0.8.0",
   "com.lihaoyi" %%% "autowire" % "0.2.6",
   "com.lihaoyi" %%% "scalatags" % "0.6.5"
  ) ++ slickSharedDeps)
 ).jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val client = (project in file("client")).settings(
 mainClass in Compile := Some("vintur.Main"),
 scalaJSUseMainModuleInitializer := true,
 scalaJSUseMainModuleInitializer in Test := false,
 elideOptions := Seq(),
 scalacOptions ++= elideOptions.value,
 skip in packageJSDependencies := false,
 //TODO ElementQueries in public/scripts contains these files while I await https://github.com/marcj/css-element-queries/pull/116
 jsDependencies += "org.webjars" % "log4javascript" % "1.4.13-1" / "js/log4javascript_uncompressed.js" minified "js/log4javascript.js",
 jsDependencies += "org.webjars" % "jquery" % "2.1.3" / "2.1.3/jquery.js",
 libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.2-SNAPSHOT",
  "be.doeraene" %%% "scalajs-jquery" % "0.9.1",
  "org.scala-js" %%% "scalajs-java-time" % "0.2.1",
  // test
  "com.lihaoyi" %%% "utest" % "0.4.7" % "test"
 )
 //jsDependencies += "org.webjars.bower" % "css-element-queries" % "0.3.2" / "0.3.2/src/ElementQueries.js",
 // jsDependencies += "org.webjars.bower" % "css-element-queries" % "0.3.2" / "0.3.2/src/ResizeSensor.js",
).enablePlugins(ScalaJSPlugin, ScalaJSPlay).
 dependsOn(sharedJs)

//TODO Macros / Makroz
/*
lazy val macros = (project in file("macros")).settings(
 libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
).dependsOn(services)
*/
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value

// See https://github.com/ochrons/scalajs-spa-tutorial/blob/master/build.sbt for release
