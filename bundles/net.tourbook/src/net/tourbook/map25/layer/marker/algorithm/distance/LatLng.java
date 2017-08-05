/*
 * Original: com.google.android.gms.maps.model.LatLng
 */
package net.tourbook.map25.layer.marker.algorithm.distance;

public final class LatLng {

	private final int	mVersionCode;

	public final double	latitude;
	public final double	longitude;

	public LatLng(final double var1, final double var3) {
		this(1, var1, var3);
	}

	LatLng(final int var1, final double var2, final double var4) {

		this.mVersionCode = var1;

		if (-180.0D <= var4 && var4 < 180.0D) {
			this.longitude = var4;
		} else {
			this.longitude = ((var4 - 180.0D) % 360.0D + 360.0D) % 360.0D - 180.0D;
		}

		this.latitude = Math.max(-90.0D, Math.min(90.0D, var2));
	}

	@Override
	public boolean equals(final Object var1) {

		if (this == var1) {
			return true;
		} else if (!(var1 instanceof LatLng)) {
			return false;
		} else {
			final LatLng var2 = (LatLng) var1;
			return Double.doubleToLongBits(this.latitude) == Double.doubleToLongBits(var2.latitude) && Double
					.doubleToLongBits(this.longitude) == Double.doubleToLongBits(var2.longitude);
		}
	}

	int getVersionCode() {
		return this.mVersionCode;
	}

	@Override
	public int hashCode() {

		final boolean var1 = true;
		final boolean var2 = true;
		long var3 = Double.doubleToLongBits(this.latitude);
		final boolean var10000 = true;
		final boolean var10001 = true;

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
				.append("lat/lng: (")
				.append(var1)
				.append(",")
				.append(var3)
				.append(")")
				.toString();
	}
}
