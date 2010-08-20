package net.tourbook.device.garmin.fit.types;

public enum GarminProduct {

	FR405(717), FR50(782), FR60(988), FR310XT(1018), EDGE500(1036), FR110(1124);

	private final Integer	value;

	private GarminProduct(Integer value) {
		this.value = value;
	}

	public static GarminProduct valueOf(Integer value) {
		GarminProduct result = null;
		for (GarminProduct garminProduct : GarminProduct.values()) {
			if (garminProduct.getValue().equals(value)) {
				result = garminProduct;
				break;
			}
		}

		return result;
	}

	public Integer getValue() {
		return value;
	}

}
