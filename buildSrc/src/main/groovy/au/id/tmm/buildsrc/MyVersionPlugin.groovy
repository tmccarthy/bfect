package au.id.tmm.buildsrc

import org.ajoberstar.grgit.gradle.GrgitPlugin
import org.ajoberstar.reckon.core.Scope
import org.ajoberstar.reckon.core.VcsInventory
import org.ajoberstar.reckon.core.strategy.ScopeNormalStrategy
import org.ajoberstar.reckon.gradle.ReckonExtension
import org.ajoberstar.reckon.gradle.ReckonPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.function.Function

final class MyVersionPlugin implements Plugin<Project> {

    @Override
    void apply(Project target) {
        target.plugins.apply(GrgitPlugin.class)
        target.plugins.apply(ReckonPlugin.class)

        target.reckon {
            normal = new ScopeNormalStrategy(normalScopeCalc(target))
            preRelease = stageFromProp("final", "rc")
        }

        target.ext.versionIsFinal = !target.version.toString().contains('rc')
    }

    private static Function<VcsInventory, Optional<String>> normalScopeCalc(Project project) {
        new Function<VcsInventory, Optional<String>>() {
            @Override
            Optional<String> apply(VcsInventory vcsInventory) {
                def fromProp = Optional.ofNullable(project.findProperty(ReckonExtension.SCOPE_PROP)?.toString())

                if (fromProp.isPresent()) {
                    fromProp
                } else {
                    Optional.of(Scope.PATCH.toString())
                }
            }
        }
    }
}
