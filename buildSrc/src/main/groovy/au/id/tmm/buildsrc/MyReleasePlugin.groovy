package au.id.tmm.buildsrc

import io.codearte.gradle.nexus.NexusStagingExtension
import io.codearte.gradle.nexus.NexusStagingPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployment
import org.gradle.api.artifacts.maven.MavenPom
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.ShowStacktrace
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.util.GFileUtils

final class MyReleasePlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        if (!target.rootProject.plugins.hasPlugin(NexusStagingPlugin.class)) {
            throw new GradleException("Nexus staging plugin required")
        }
        if (!target.rootProject.plugins.hasPlugin(MyVersionPlugin.class)) {
            throw new GradleException("My version plugin required")
        }

        target.apply(plugin: 'maven')
        target.apply(plugin: 'signing')

        target.ext.'signing.keyId' = property(target, 'signing.keyId', 'GNU_KEY_ID')
        target.ext.'signing.secretKeyRingFile' = target.rootProject.file('secring.gpg').absolutePath
        target.ext.'signing.password' = property(target, 'signing.password', 'GNU_KEY_PASSWORD')

        MyReleasePluginExtension extension = target.extensions.findByType(MyReleasePluginExtension.class)

        if (extension == null) {
            throw new GradleException("Must provide a release extension before the release plugin is applied")
        }

        NexusStagingExtension nexusStaging = target.rootProject.extensions.getByName("nexusStaging")

        nexusStaging.username = property(target, 'ossrhUser', 'OSSRH_USER')
        nexusStaging.password = property(target, 'ossrhPassword', 'OSSRH_PASSWORD')

        target.task(type: Jar, 'sourcesJar') {
            classifier 'sources'
            from target.sourceSets.main.allScala
        }

        target.task('writeJavadocReadme') {
            doLast {
                def readmeFile = new File(target.tasks.javadoc.destinationDir, 'README.txt')

                GFileUtils.mkdirs(readmeFile.parentFile)
                readmeFile.createNewFile()
                readmeFile.text = 'Empty file to pass publishing requirements'
            }
        }

        target.task(type: Jar, dependsOn: [target.tasks.javadoc, target.tasks.writeJavadocReadme], 'javadocJar') {
            classifier = 'javadoc'
            from target.tasks.javadoc.destinationDir
        }

        target.task(type: Jar, dependsOn: target.tasks.scaladoc, 'scaladocJar') {
            shouldRunAfter('test')
            classifier = 'scaladoc'
            from target.tasks.scaladoc.destinationDir
        }

        target.tasks.build.dependsOn(target.tasks.scaladocJar)

        target.artifacts {
            archives target.tasks.javadocJar, target.tasks.sourcesJar, target.tasks.scaladocJar
        }

        SigningExtension signing = target.extensions.getByName('signing')

        signing.required = { target.gradle.taskGraph.hasTask(target.tasks.uploadArchives) }

        signing.sign(target.configurations.archives)

        target.tasks.withType(Sign).each { Sign it ->
            it.onlyIf = {
                target.gradle.taskGraph.hasTask(target.tasks.uploadArchives) &&
                        (it.isRequired() || it.getSignatory() != null)
            }
        }

        target.install {
            repositories {
                mavenInstaller {
                    configurePom(target, extension, pom)
                }
            }
        }

        target.uploadArchives {
            repositories {
                mavenDeployer {
                    beforeDeployment { MavenDeployment deployment -> signing.signPom(deployment) }

                    repository(url: "https://oss.sonatype.org/service/local/staging/deploy/maven2/") {
                        authentication(userName: nexusStaging.username, password: nexusStaging.password)
                    }

                    configurePom(target, extension, pom)
                }
            }

            onlyIf { target.rootProject.ext.versionIsFinal }
        }

        target.task(type: GradleBuild, 'releaseIfFinalVersion') {
            onlyIf {
                target.rootProject.ext.versionIsFinal &&
                        (System.getenv().TRAVIS == "true" ? System.getenv().TRAVIS_TAG != null : true)
            }

            startParameter.logLevel = LogLevel.INFO
            startParameter.showStacktrace = ShowStacktrace.ALWAYS_FULL

            tasks = ['assemble', 'uploadArchives', ':closeAndReleaseRepository']
        }
    }

    def configurePom(Project target, MyReleasePluginExtension extension, MavenPom pom) {
        pom.project {
            artifactId "${target.name}${target.ext.s}"
            name "$target.group:${target.name}"
            description target.description
            packaging 'jar'
            url "https://github.com/$extension.githubUser/$extension.githubRepoName"
            licenses {
                license {
                    name extension.licenceName
                    url extension.licenceUrl
                    distribution 'repo'
                }
            }
            scm {
                url "https://github.com/$extension.githubUser/$extension.githubRepoName"
                connection "git@github.com:$extension.githubUser/${extension.githubRepoName}.git"
                developerConnection "git@github.com:$extension.githubUser/${extension.githubRepoName}.git"
            }
            developers {
                developer {
                    name extension.developerName
                    email extension.developerEmail
                }
            }
        }
    }

    private static String property(Project target, String propertyName, String envVar) {
        target.findProperty(propertyName) ?: System.getenv().get(envVar) ?: ''
    }

    static final class MyReleasePluginExtension {
        String githubUser
        String githubRepoName

        String licenceName
        String licenceUrl

        String developerName
        String developerEmail
    }
}
