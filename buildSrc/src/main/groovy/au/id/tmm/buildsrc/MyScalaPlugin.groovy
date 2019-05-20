package au.id.tmm.buildsrc

import com.github.maiflai.ScalaTestPlugin
import com.hierynomus.gradle.license.LicenseBasePlugin
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import com.mycila.maven.plugin.license.header.HeaderType
import nl.javadude.gradle.plugins.license.LicenseExtension
import nl.javadude.gradle.plugins.license.header.HeaderDefinitionBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.scala.ScalaCompile
import org.scoverage.ScoveragePlugin

class MyScalaPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {

        def s = target.ext.s

        def zincVersion = target.ext.zincVersion

        def scalaVersion = target.ext.scalaVersion
        def scalaTestVersion = target.ext.scalaTestVersion

        def tmmTestUtilsVersion = target.ext.tmmTestUtilsVersion

        target.plugins.apply(ScalaPlugin.class)
        target.plugins.apply(ScalaTestPlugin.class)
        if (target.findProperty('noScoverage') != true ) {
            applyScoverage(target)
        }

        target.repositories {
            mavenLocal()
            mavenCentral()
        }

        target.configurations {
            scalaCompilerPlugin
        }

        target.dependencies {
            zinc "com.typesafe.zinc:zinc:$zincVersion"

            compile "org.scala-lang:scala-library:$scalaVersion"

            testCompile "org.scalatest:scalatest${s}:$scalaTestVersion"
            testCompile "au.id.tmm:tmm-test-utils${s}:$tmmTestUtilsVersion"

            // Needed to produce scalatest report
            testRuntime 'org.pegdown:pegdown:1.4.2'

            scalaCompilerPlugin "org.spire-math:kind-projector${s}:0.9.7"
        }

        target.tasks.withType(ScalaCompile) {
            scalaCompileOptions.with {
                additionalParameters = [
                    '-deprecation',                      // Emit warning and location for usages of deprecated APIs.
                    '-encoding', 'utf-8',                // Specify character encoding used by source files.
                    '-explaintypes',                     // Explain type errors in more detail.
                    '-feature',                          // Emit warning and location for usages of features that should be imported explicitly.
                    '-language:existentials',            // Existential types (besides wildcard types) can be written and inferred
                    '-language:higherKinds',             // Allow higher-kinded types
                    '-language:implicitConversions',     // Allow implicit conversions
                    '-unchecked',                        // Enable additional warnings where generated code depends on assumptions.
                    '-Xcheckinit',                       // Wrap field accessors to throw an exception on uninitialized access.
                    '-Xfatal-warnings',                  // Fail the compilation if there are any warnings.
                    '-Xfuture',                          // Turn on future language features.
                    '-Xlint:by-name-right-associative',  // By-name parameter of right associative operator.
                    '-Xlint:constant',                   // Evaluation of a constant arithmetic expression results in an error.
                    '-Xlint:delayedinit-select',         // Selecting member of DelayedInit.
                    '-Xlint:doc-detached',               // A Scaladoc comment appears to be detached from its element.
                    '-Xlint:inaccessible',               // Warn about inaccessible types in method signatures.
                    '-Xlint:infer-any',                  // Warn when a type argument is inferred to be `Any`.
                    '-Xlint:nullary-override',           // Warn when non-nullary `def f()' overrides nullary `def f'.
                    '-Xlint:nullary-unit',               // Warn when nullary methods return Unit.
                    '-Xlint:option-implicit',            // Option.apply used implicit view.
                    '-Xlint:package-object-classes',     // Class or object defined in package object.
                    '-Xlint:poly-implicit-overload',     // Parameterized overloaded implicit methods are not visible as view bounds.
                    '-Xlint:private-shadow',             // A private field (or class parameter) shadows a superclass field.
                    '-Xlint:stars-align',                // Pattern sequence wildcard must align with sequence component.
                    '-Xlint:type-parameter-shadow',      // A local type parameter shadows a type already in scope.
                    '-Xlint:unsound-match',              // Pattern match may not be typesafe.
                    '-Ypartial-unification',             // Enable partial unification in type constructor inference
                    '-Ywarn-extra-implicit',             // Warn when more than one implicit parameter section is defined.
                    '-Ywarn-inaccessible',               // Warn about inaccessible types in method signatures.
                    '-Ywarn-infer-any',                  // Warn when a type argument is inferred to be `Any`.
                    '-Ywarn-nullary-override',           // Warn when non-nullary `def f()' overrides nullary `def f'.
                    '-Ywarn-nullary-unit',               // Warn when nullary methods return Unit.
                    '-Ywarn-unused:imports',             // Warn if an import selector is not referenced.
                    '-Ywarn-unused:locals',              // Warn if a local definition is unused.
                    '-Ywarn-unused:privates',            // Warn if a private member is unused.
                    '-Ypatmat-exhaust-depth', '80',      // Increase max exhaustion depth

                    "-Xplugin:" + target.configurations.scalaCompilerPlugin.asPath,
                ]
            }
        }

        target.tasks.clean {
            delete target.file('out')
        }

        applyLicensePlugin(target)
    }

    private void applyScoverage(Project target) {
        def s = target.ext.s
        def scoverageVersion = target.ext.scoverageVersion

        target.plugins.apply(ScoveragePlugin.class)

        target.dependencies {
            scoverage "org.scoverage:scalac-scoverage-plugin${s}:$scoverageVersion"
            scoverage "org.scoverage:scalac-scoverage-runtime${s}:$scoverageVersion"
        }

        target.tasks.compileScoverageScala.shouldRunAfter(target.tasks.test)

        target.scoverage {
            coverageOutputCobertura = false
        }

        target.tasks.checkScoverage {
            minimumRate = 0
        }
    }

    private static void applyLicensePlugin(Project target) {
        target.plugins.apply(LicenseBasePlugin.class)

        LicenseExtension extension = target.extensions.getByName('license') as LicenseExtension

        extension.header = target.rootProject.file('HEADER')
        extension.exclude('*.gitkeep')
        extension.strictCheck = true

        def scalaHeaderDefinition = new HeaderDefinitionBuilder("scala")
            .withFirstLine(HeaderType.JAVADOC_STYLE.definition.firstLine)
            .withBeforeEachLine("  * ")
            .withAfterEachLine(HeaderType.JAVADOC_STYLE.definition.afterEachLine)
            .withEndLine("  */")
            .withNoBlankLines()
            .withSkipLinePattern(HeaderType.JAVADOC_STYLE.definition.skipLinePattern.toString())
            .withFirstLineDetectionDetectionPattern(HeaderType.JAVADOC_STYLE.definition.firstLineDetectionPattern.toString())
            .withLastLineDetectionDetectionPattern(HeaderType.JAVADOC_STYLE.definition.lastLineDetectionPattern.toString())
            .multiline()
            .padLines()

        extension.headerDefinition(scalaHeaderDefinition)

        extension.mapping("scala", "scala")

        target.tasks.withType(LicenseCheck.class).each { licenseCheckTask ->
            def sourceSetName = licenseCheckTask.name.drop("${LicenseBasePlugin.FORMAT_TASK_BASE_NAME}".length()).uncapitalize()

            if (sourceSetName == 'main') {
                target.tasks.getByName('compileScala').dependsOn(licenseCheckTask)
            } else if (sourceSetName == 'test') {
                target.tasks.getByName('compileTestScala').dependsOn(licenseCheckTask)
            } else {
                target.tasks.findByName("compile${sourceSetName.capitalize()}Scala")?.dependsOn(licenseCheckTask)
            }
        }

        def updateLicensesTask = target.tasks.create(name: 'updateLicenses', group: 'verification')
        target.tasks.withType(LicenseFormat.class).each { updateLicensesTask.dependsOn(it) }

    }
}
