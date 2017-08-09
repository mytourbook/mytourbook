package net.tourbook.map25.layer.labeling;

import org.oscim.core.MapPosition;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.TextBucket;
import org.oscim.renderer.bucket.TextureBucket;

/**
 * Original: {@link org.oscim.layers.tile.vector.labeling.LabelTask}
 */
final class LabelTask {

    final TextureBucket layers;
    final TextBucket textLayer;
    final SymbolBucket symbolLayer;

    final MapPosition pos;

    LabelTask() {
        pos = new MapPosition();

        symbolLayer = new SymbolBucket();
        textLayer = new TextBucket();

        layers = symbolLayer;
        symbolLayer.next = textLayer;
    }

}
