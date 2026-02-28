package org.sitenetsoft.quarkus.vdp.deployment;

import org.sitenetsoft.quarkus.vdp.VdpResponseFilter;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerResponseFilterBuildItem;

class VdpProcessor {

    private static final String FEATURE = "vdp";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerFilter() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(VdpResponseFilter.class)
                .setDefaultScope(DotNames.SINGLETON)
                .setUnremovable()
                .build();
    }

    @BuildStep
    CustomContainerResponseFilterBuildItem registerResponseFilter() {
        return new CustomContainerResponseFilterBuildItem(VdpResponseFilter.class.getName());
    }
}
