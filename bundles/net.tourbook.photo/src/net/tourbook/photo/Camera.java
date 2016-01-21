/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

public class Camera implements Comparable<Camera> {

	public String	cameraName;

	/**
	 * Time adjustment for this camera in ms.
	 */
	public long		timeAdjustment;

	public Camera(final String cameraName) {
		this.cameraName = cameraName;
	}

	@Override
	public int compareTo(final Camera otherCamera) {
		return cameraName.compareTo(otherCamera.cameraName);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Camera)) {
			return false;
		}
		final Camera other = (Camera) obj;
		if (cameraName == null) {
			if (other.cameraName != null) {
				return false;
			}
		} else if (!cameraName.equals(other.cameraName)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cameraName == null) ? 0 : cameraName.hashCode());
		return result;
	}

	public void setTimeAdjustment(final int hours, final int minutes, final int seconds) {
		timeAdjustment = (hours * 60 * 60 * 1000) + (minutes * 60 * 1000) + (seconds * 1000);
	}

	@Override
	public String toString() {
		return "Camera [cameraName=" + cameraName + "{)}, timeAdjustment=" + timeAdjustment + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
