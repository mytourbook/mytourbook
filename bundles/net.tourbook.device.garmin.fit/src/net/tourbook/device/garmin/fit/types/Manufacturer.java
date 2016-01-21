package net.tourbook.device.garmin.fit.types;

import com.garmin.fit.Fit;

/**
 * This is from {@link com.garmin.fit.Manufacturer} and converted into an enum.
 */
public enum Manufacturer {

	GARMIN(1), //
	GARMIN_FR405_ANTFS(2), // 		// Do not use.  Used by FR405 for ANTFS man id.
	ZEPHYR(3), //
	DAYTON(4), //
	IDT(5), //
	SRM(6), //
	QUARQ(7), //
	IBIKE(8), //
	SARIS(9), //
	SPARK_HK(10), //
	TANITA(11), //
	ECHOWELL(12), //
	DYNASTREAM_OEM(13), //
	NAUTILUS(14), //
	DYNASTREAM(15), //
	TIMEX(16), //
	METRIGEAR(17), //
	XELIC(18), //
	BEURER(19), //
	CARDIOSPORT(20), //
	A_AND_D(21), //
	HMM(22), //
	SUUNTO(23), //
	THITA_ELEKTRONIK(24), //
	GPULSE(25), //
	CLEAN_MOBILE(26), //
	PEDAL_BRAIN(27), //
	PEAKSWARE(28), //
	SAXONAR(29), //
	LEMOND_FITNESS(30), //
	DEXCOM(31), //
	WAHOO_FITNESS(32), //
	OCTANE_FITNESS(33), //
	ARCHINOETICS(34), //
	THE_HURT_BOX(35), //
	CITIZEN_SYSTEMS(36), //
	MAGELLAN(37), //
	OSYNCE(38), //
	HOLUX(39), //
	CONCEPT2(40), //
	ONE_GIANT_LEAP(42), //
	ACE_SENSOR(43), //
	BRIM_BROTHERS(44), //
	XPLOVA(45), //
	PERCEPTION_DIGITAL(46), //
	BF1SYSTEMS(47), //
	PIONEER(48), //
	SPANTEC(49), //
	METALOGICS(50), //
	_4IIIIS(51), //
	SEIKO_EPSON(52), //
	SEIKO_EPSON_OEM(53), //
	IFOR_POWELL(54), //
	MAXWELL_GUIDER(55), //
	STAR_TRAC(56), //
	BREAKAWAY(57), //
	ALATECH_TECHNOLOGY_LTD(58), //
	MIO_TECHNOLOGY_EUROPE(59), //
	ROTOR(60), //
	GEONAUTE(61), //
	ID_BIKE(62), //
	SPECIALIZED(63), //
	WTEK(64), //
	PHYSICAL_ENTERPRISES(65), //
	NORTH_POLE_ENGINEERING(66), //
	BKOOL(67), //
	CATEYE(68), //
	STAGES_CYCLING(69), //
	SIGMASPORT(70), //
	TOMTOM(71), //
	PERIPEDAL(72), //
	WATTBIKE(73), //
	MOXY(76), //
	CICLOSPORT(77), //
	POWERBAHN(78), //
	ACORN_PROJECTS_APS(79), //
	LIFEBEAM(80), //
	BONTRAGER(81), //
	WELLGO(82), //
	SCOSCHE(83), //
	MAGURA(84), //
	WOODWAY(85), //
	ELITE(86), //
	NIELSEN_KELLERMAN(87), //
	DK_CITY(88), //
	TACX(89), //
	DIRECTION_TECHNOLOGY(90), //
	MAGTONIC(91), //
	DEVELOPMENT(255), //
	ACTIGRAPHCORP(5759), //
	INVALID(Fit.UINT16_INVALID), //

	;

	private final Integer	value;

	private Manufacturer(final Integer value) {
		this.value = value;
	}

	public static Manufacturer valueOf(final Integer value) {
		Manufacturer result = null;
		for (final Manufacturer manufacturer : Manufacturer.values()) {
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
