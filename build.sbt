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

val zioVersion  = "1.0.3"
val catsVersion = "2.2.0"
val fs2Version  = "2.4.5"

lazy val core = project
  .in(file("core"))
  .settings(settingsHelper.settingsForSubprojectCalled("core"))
  .settings(
    skip in publish := true,
  )

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
  .settings(
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsVersion,
  )
  .dependsOn(core, core % "test->test")

lazy val interopFs2 = project
  .in(file("interop/fs2"))
  .settings(settingsHelper.settingsForSubprojectCalled("interop-fs2"))
  .settings(
    libraryDependencies += "co.fs2" %% "fs2-core" % fs2Version,
  )
  .dependsOn(interopCats)

lazy val interopZio = project
  .in(file("interop/zio"))
  .settings(settingsHelper.settingsForSubprojectCalled("interop-zio"))
  .settings(
    libraryDependencies += "dev.zio" %% "zio" % zioVersion,
  )
  .dependsOn(core)

addCommandAlias("check", ";+test;scalafmtCheckAll")
