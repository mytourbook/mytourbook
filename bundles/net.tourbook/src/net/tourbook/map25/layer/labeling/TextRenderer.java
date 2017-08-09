/*
 * Copyright 2012 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.tourbook.map25.layer.labeling;

import net.tourbook.map25.layer.labeling.LabelLayer.Worker;

import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.RenderBucket;
import org.oscim.renderer.bucket.TextureBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Original: {@link org.oscim.layers.tile.vector.labeling.TextRenderer}
 */
class TextRenderer extends BucketRenderer {

	static final Logger		log			= LoggerFactory.getLogger(TextRenderer.class);
	static final boolean	dbg			= false;

	private final Worker	mWorker;

	long					lastDraw	= 0;

	public TextRenderer(final Worker worker) {
		mWorker = worker;
	}

	@Override
	public synchronized void render(final GLViewport v) {

		GLState.test(false, false);
		//Debug.draw(pos, layers);

		buckets.vbo.bind();

		final float scale = (float) (v.pos.scale / mMapPosition.scale);

		setMatrix(v, false);

		for (RenderBucket l = buckets.get(); l != null;) {
			l = TextureBucket.Renderer.draw(l, v, scale);
		}
	}

	@Override
	public synchronized void update(final GLViewport v) {

		LabelTask t;

		synchronized (mWorker) {
			t = mWorker.poll();
			if (t == null) {
				if (!mWorker.isRunning()) {
					mWorker.submit(50);
				}
				return;
			}
			buckets.clear();
		}

		// set new TextLayer to be uploaded and rendered
		buckets.set(t.layers);
		mMapPosition = t.pos;
		compile();
	}

}
