package net.tourbook.map25.layer.labeling;

import org.oscim.renderer.bucket.TextItem;
import org.oscim.utils.pool.Pool;

/**
 * Original: {@link org.oscim.layers.tile.vector.labeling.LabelPool}
 */
final class LabelPool extends Pool<TextItem> {
    @Override
    protected Label createItem() {
        return new Label();
    }

    Label releaseAndGetNext(final Label l) {
        if (l.item != null) {
			l.item = TextItem.pool.release(l.item);
		}

        // drop references
        l.item = null;
		l.label = null;
        final Label ret = (Label) l.next;

        // ignore warning
        super.release(l);
        return ret;
    }
}
