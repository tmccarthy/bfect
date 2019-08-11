import DependencySettings._

val settingsHelper = ProjectSettingsHelper("au.id.tmm","bfect")()

settingsHelper.settingsForBuild

lazy val root = project
  .in(file("."))
  .settings(settingsHelper.settingsForRootProject)
  .settings(console := (console in Compile in core).value)
  .aggregate(
    core,
    testing,
    io,
    interopCats,
    interopFs2,
    interopZio,
  )

lazy val core = project
  .in(file("core"))
  .settings(settingsHelper.settingsForSubprojectCalled("core"))

lazy val testing = project
  .in(file("testing"))
  .settings(settingsHelper.settingsForSubprojectCalled("testing"))
  .dependsOn(core)

lazy val io = project
  .in(file("io"))
  .settings(settingsHelper.settingsForSubprojectCalled("io"))
  .dependsOn(core)

lazy val interopCats = project
  .in(file("interop/cats"))
  .settings(settingsHelper.settingsForSubprojectCalled("interop-cats"))
  .settings(catsEffectDependency)
  .dependsOn(core)

lazy val interopFs2 = project
  .in(file("interop/fs2"))
  .settings(settingsHelper.settingsForSubprojectCalled("interop-fs2"))
  .settings(fs2Dependency)
  .dependsOn(interopCats)

lazy val interopZio = project
  .in(file("interop/zio"))
  .settings(settingsHelper.settingsForSubprojectCalled("interop-zio"))
  .settings(
    libraryDependencies += "dev.zio" %% "zio" % "1.0.0-RC11-1",
  )
  .dependsOn(core)

addCommandAlias("check", ";+test;scalafmtCheckAll")
