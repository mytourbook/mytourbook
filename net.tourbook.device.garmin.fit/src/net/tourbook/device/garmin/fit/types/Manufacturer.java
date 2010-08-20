package net.tourbook.device.garmin.fit.types;

public enum Manufacturer {

	GARMIN(1);

	private final Integer	value;

	private Manufacturer(Integer value) {
		this.value = value;
	}

	public static Manufacturer valueOf(Integer value) {
		Manufacturer result = null;
		for (Manufacturer manufacturer : Manufacturer.values()) {
			if (manufacturer.getValue().equals(value)) {
				result = manufacturer;
				break;
			}
		}

		return result;
	}

	public Integer getValue() {
		return value;
	}

}
