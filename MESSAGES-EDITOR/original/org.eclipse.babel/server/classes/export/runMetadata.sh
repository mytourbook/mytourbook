#!/bin/sh

$1/eclipse -application org.eclipse.equinox.p2.metadata.generator.EclipseGenerator -updateSite $2 -site file:$2/site.xml -metadataRepositoryName "Babel language packs update site" -append -reusePack200Files -vmargs -Xmx256m