/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tourbook.map25.layer.marker.algorithm.distance;

/**
 * Original: {@link com.google.android.gms.maps.model.LatLng}
 */
public final class LatLng {

	public final double	latitude;
	public final double	longitude;

	public LatLng(final double var1, final double var3) {
		this(1, var1, var3);
	}

	LatLng(final int var1, final double var2, final double var4) {

		if (-180.0D <= var4 && var4 < 180.0D) {
			this.longitude = var4;
		} else {
			this.longitude = ((var4 - 180.0D) % 360.0D + 360.0D) % 360.0D - 180.0D;
		}

		this.latitude = Math.max(-90.0D, Math.min(90.0D, var2));
	}

	@Override
	public boolean equals(final Object otherLatLng) {

		if (this == otherLatLng) {
			return true;
		} else if (!(otherLatLng instanceof LatLng)) {
			return false;
		} else {

			final LatLng latLng2 = (LatLng) otherLatLng;

			return Double.doubleToLongBits(this.latitude) == Double.doubleToLongBits(latLng2.latitude) && Double
					.doubleToLongBits(this.longitude) == Double.doubleToLongBits(latLng2.longitude);
		}
	}

	@Override
	public int hashCode() {

		long var3 = Double.doubleToLongBits(this.latitude);
		int var5 = 31 + (int) (var3 ^ var3 >>> 32);

		var3 = Double.doubleToLongBits(this.longitude);
		var5 = 31 * var5 + (int) (var3 ^ var3 >>> 32);

		return var5;
	}

	@Override
	public String toString() {

		final double var1 = this.latitude;
		final double var3 = this.longitude;

		return (new StringBuilder(60))
				.append("lat/lng: (") //$NON-NLS-1$
				.append(var1)
				.append(",") //$NON-NLS-1$
				.append(var3)
				.append(")") //$NON-NLS-1$
				.toString();
	}
}
