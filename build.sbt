import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._
enablePlugins(ScalaJSPlugin, WorkbenchPlugin/*,ScalaJSBundlerPlugin*/)

//resolvers += Resolver.file("releases", file("C:/Workspace/Data/.m2/repository"))

name := "Client Dev"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
 //shared
 "io.circe" %%% "circe-core" % "0.8.0",
 "io.circe" %%% "circe-generic" % "0.8.0",
 "io.circe" %%% "circe-parser" % "0.8.0",
 "com.lihaoyi" %%% "autowire" % "0.2.6",
 "com.lihaoyi" %%% "scalatags" % "0.6.5",
 "com.typesafe.slick" %% "slick" % "3.2.0",
 //client
 "org.scala-js" %%% "scalajs-dom" % "0.9.2-SNAPSHOT",
 "be.doeraene" %%% "scalajs-jquery" % "0.9.1",
 "org.scala-js" %%% "scalajs-java-time" % "0.2.1",
 // client test
 "com.lihaoyi" %%% "utest" % "0.4.4" % "test"
)

jsDependencies += RuntimeDOM


scalaJSUseMainModuleInitializer := true
//npmDependencies in Compile += "sourcemapped-stacktrace" -> "1.1.6"
mainClass in Compile := Some("vintur.Main")
//mainClass in Compile := Some("ezilay.EziTest")
scalaJSUseMainModuleInitializer in Test := false


testFrameworks += new TestFramework("utest.runner.Framework")
lazy val elideOptions = settingKey[Seq[String]]("Set limit for elidable functions")

elideOptions := Seq()

scalacOptions ++= elideOptions.value

skip in packageJSDependencies := false

//TODO ElementQueries in public/scripts contains these files while I await https://github.com/marcj/css-element-queries/pull/116
//jsDependencies += "org.webjars.bower" % "css-element-queries" % "0.3.2" / "0.3.2/src/ElementQueries.js"
//jsDependencies += "org.webjars.bower" % "css-element-queries" % "0.3.2" / "0.3.2/src/ResizeSensor.js"
jsDependencies += "org.webjars" % "log4javascript" % "1.4.13-1" / "js/log4javascript_uncompressed.js" minified "js/log4javascript.js"
//jsDependencies += "org.webjars" % "jquery" % "2.1.3" / "2.1.3/jquery.js"
jsDependencies += "org.webjars" % "jquery" % "3.2.1" / "3.2.1/jquery.js"
//scalaJSOptimizerOptions ~= {_.withDisableOptimizer(true)}

lazy val copyOver = TaskKey[Unit]("copyOver")

copyOver := {
 println("Started copyOver")
 val wbRoot: File = baseDirectory.value
 val vinturRoot = new File(wbRoot, "\\..").getCanonicalFile
 val playRoot = new File(vinturRoot, "\\play").getCanonicalFile
 val wbStylesheetsDir = new File(wbRoot, "\\src\\main\\resources\\stylesheets").getCanonicalFile
 val wbShared = new File(wbRoot, "\\src\\main\\scala\\shared").getCanonicalFile
 val wbClient = new File(wbRoot, "\\src\\main\\scala").getCanonicalFile
 val playStylesheetsDir = new File(playRoot, "\\server\\public\\stylesheets").getCanonicalFile
 val playShared = new File(playRoot, "\\shared\\\\src\\main\\scala\\shared").getCanonicalFile
 val playClient = new File(playRoot, "\\client\\src\\main\\scala").getCanonicalFile
 println("vinturRoot = " + vinturRoot.toPath)
 println("wbRoot = " + wbRoot.toPath)
 println("playRoot = " + playRoot.toPath)
 println("wbStylesheetsDir = " + wbStylesheetsDir.toPath)
 println("playStylesheetsDir = " + playStylesheetsDir.toPath)
 println("reltest: " + vinturRoot.toPath.relativize(wbStylesheetsDir.toPath))
 //TODO put these visitors in a public library
 class DeleteContentsVisitor(root: Path) extends java.nio.file.SimpleFileVisitor[Path] {
  private def delete(path: Path): Unit = if (!path.toFile.delete()) throw new IOException("Could not delete file \"" + path + "\"")
  override def visitFile(filePath: Path, attrs: BasicFileAttributes): FileVisitResult = {
   delete(filePath)
   java.nio.file.FileVisitResult.CONTINUE
  }
  override def postVisitDirectory(dirPath: Path, exc: IOException): FileVisitResult = {
   if (dirPath != root) {
    delete(dirPath)
   }
   java.nio.file.FileVisitResult.CONTINUE
  }
 }
 class ClearAndCopyVisitor(srcRoot: File, destRoot: File, opSkipDir: Option[Path]) extends java.nio.file.SimpleFileVisitor[Path] {
  private val srcRootPath = srcRoot.toPath
  private def toTarget(srcPath: Path): Path = {
   println("srcRootPath=" + srcRootPath)
   println("srcPath=" + srcPath)
   val rel = srcRootPath.relativize(srcPath).normalize().toString
   println("rel=" + rel)
   new File(destRoot, rel).getAbsoluteFile.toPath
  }
  override def preVisitDirectory(dirPath: Path, attrs: BasicFileAttributes): FileVisitResult = {
   if (opSkipDir.exists(_ == dirPath)) {
    java.nio.file.FileVisitResult.SKIP_SUBTREE
   } else {
    val dest = toTarget(dirPath)
    if (dirPath == srcRootPath) {
     Files.walkFileTree(dest, new DeleteContentsVisitor(dest))
    } else {
     println("dirPath=" + dirPath)
     println("dest=" + dest)
     Files.copy(dirPath, dest)
    }
    java.nio.file.FileVisitResult.CONTINUE
   }
  }
  override def visitFile(filePath: Path, attrs: BasicFileAttributes): FileVisitResult = {
   Files.copy(filePath, toTarget(filePath))
   java.nio.file.FileVisitResult.CONTINUE
  }
 }
 def copyOver(sourceDir: File, targetDir: File, opSkipDir: Option[File] = None) = Files.walkFileTree(sourceDir.toPath, new ClearAndCopyVisitor(sourceDir, targetDir, opSkipDir.map(_.toPath)))
 copyOver(wbStylesheetsDir, playStylesheetsDir)
 copyOver(wbShared, playShared)
 copyOver(wbClient, playClient, Some(wbShared))
 println("Completed copyOver")
}
