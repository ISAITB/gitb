import scala.sys.process.Process

/*
 * UI Build hook Scripts
 */

// Execution status success.
val Success = 0

// Execution status failure.
val Error = 1

// True if build running operating system is windows.
val isWindows = System.getProperty("os.name").toLowerCase().contains("win")

// Execute on commandline, depending on the operating system. Used to execute npm commands.
def runOnCommandline(script: String)(implicit dir: File): Int = {
  if(isWindows){ Process("cmd /c " + script, dir) } else { Process(script, dir) } }!

// Execute `npm install` command to install all node module dependencies. Return Success if already installed.
def runNpmInstall(implicit dir: File): Int =
  runOnCommandline(FrontendCommands.dependencyInstall)

// Execute frontend prod build task. Update to change the frontend prod build task.
def executeProdBuild(implicit dir: File): Int = {
  if (runNpmInstall == Success) {
    runOnCommandline(FrontendCommands.buildProd)
  } else {
    Error
  }
}

// Create frontend build tasks for prod execution.

lazy val `ui-prod-build` = taskKey[Unit]("Run UI build when packaging the application.")

`ui-prod-build` := {
  implicit val userInterfaceRoot: File = baseDirectory.value / "ui"
  if (executeProdBuild != Success) throw new Exception("Oops! UI Build crashed.")
}

// Execute frontend prod build task prior to play dist execution.
dist := (dist dependsOn `ui-prod-build`).value

// Execute frontend prod build task prior to play stage execution.
stage := (stage dependsOn `ui-prod-build`).value
