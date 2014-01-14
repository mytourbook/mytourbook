/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
/**
 * @author Alfred Barten
 */
package net.tourbook.common.color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.swt.graphics.Image;

/**
 * Contains the profile image and all rgb vertices.
 */
public abstract class ProfileImage implements Cloneable {

	protected static final int			IMAGE_MIN_WIDTH		= 20;
	protected static final int			IMAGE_MIN_HEIGHT	= 10;

	private int							_imageWidth;
	private int							_imageHeight;

	private boolean						_isHorizontal		= true;

	private static final RGBVertex[]	DEFAULT_VERTICES	= new RGBVertex[] {
			new RGBVertex(0, 0xff, 0, 0),
			new RGBVertex(10, 0, 0xff, 0),
			new RGBVertex(20, 0, 0, 0xff)					};
	/**
	 * Contains all vertices.
	 */
	private ArrayList<RGBVertex>		_rgbVertices		= new ArrayList<RGBVertex>(Arrays.asList(DEFAULT_VERTICES));

	private RGBVertex[]					_cachedVertices;

	/*
	 * UI controls
	 */
	private Image						_profileImage;

	public void addVertex(final int vertexPosition, final RGBVertex rgbVertex) {

		_rgbVertices.add(vertexPosition, rgbVertex);

		// sort vertices by value
		Collections.sort(_rgbVertices);

		invalidateVertices();
	}

	public void addVertex(final RGBVertex rgbVertex) {

		_rgbVertices.add(rgbVertex);

		invalidateVertices();
	}

	@Override
	public ProfileImage clone() {

		ProfileImage clonedObject = null;

		try {

			clonedObject = (ProfileImage) super.clone();

			clonedObject._rgbVertices = new ArrayList<RGBVertex>();

			for (final RGBVertex rgbVertex : _rgbVertices) {
				clonedObject._rgbVertices.add(rgbVertex.clone());
			}

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
	}

	public abstract Image createImage(int width, int height, final boolean isHorizontal);

	public void disposeImage() {
		UI.disposeResource(_profileImage);
	}

	public Image getImage() {
		return _profileImage;
	}

	public abstract int getRGB(final long value);

	/**
	 * @return Return rgb verticies, this list should not be modified, use
	 *         {@link #setVertices(ArrayList)} to modify this list.
	 */
	public ArrayList<RGBVertex> getRgbVertices() {
		return _rgbVertices;
	}

	public RGBVertex[] getRgbVerticesArray() {

		// ensure array is valid
		if (_cachedVertices == null) {
			_cachedVertices = _rgbVertices.toArray(new RGBVertex[_rgbVertices.size()]);
		}

		return _cachedVertices;
	}

	/**
	 * @param width
	 * @param height
	 * @param isHorizontal
	 * @return Returns cached rgb vertex image, the image will be created when size or orientation
	 *         is different.
	 */
	public Image getValidatedImage(final int width, final int height, final boolean isHorizontal) {

		/*
		 * create image when the requested image size/orientation is different than the previous
		 * created image
		 */
		if (_profileImage == null
				|| _profileImage.isDisposed()
				|| _isHorizontal != isHorizontal
				|| _imageWidth != width
				|| _imageHeight != height) {

			_isHorizontal = isHorizontal;
			_imageWidth = width;
			_imageHeight = height;

			// dispose previous image
			disposeImage();

			_profileImage = createImage(width, height, isHorizontal);
		}

		return _profileImage;
	}

	public String getVertexKey() {

		final StringBuilder sb = new StringBuilder();
		for (final RGBVertex vertex : _rgbVertices) {
			sb.append(vertex.toString());
		}
		return sb.toString();
	}

	public void invalidateCachedColors() {

		invalidateVertices();
	}

	private void invalidateVertices() {

		_cachedVertices = null;
	}

	/**
	 * Removes vertices.
	 * 
	 * @param removedVertex
	 */
	public void removeVertex(final RGBVertex removedVertex) {

		_rgbVertices.remove(removedVertex);

		invalidateVertices();
	}

	/**
	 * Removes vertices by index.
	 * 
	 * @param vertexRemoveIndex
	 */
	public void removeVerticesByIndex(final ArrayList<Integer> vertexRemoveIndex) {

		final ArrayList<RGBVertex> removedVertices = new ArrayList<RGBVertex>();

		for (final Integer integer : vertexRemoveIndex) {
			removedVertices.add(_rgbVertices.get(integer));
		}

		_rgbVertices.removeAll(removedVertices);

		invalidateVertices();
	}

	public void setVertices(final ArrayList<RGBVertex> rgbVertices) {

		_rgbVertices.clear();
		_rgbVertices.addAll(rgbVertices);

		invalidateVertices();
	}

	public void setVertices(final RGBVertex[] rgbVertices) {

		_rgbVertices.clear();
		_rgbVertices.addAll(Arrays.asList(rgbVertices));

		invalidateVertices();
	}

	@Override
	public String toString() {
		final int maxLen = 10;
		return String.format(
				"RGBVertices [_rgbVertices=%s]", //$NON-NLS-1$
				_rgbVertices != null ? _rgbVertices.subList(0, Math.min(_rgbVertices.size(), maxLen)) : null);
	}

}
