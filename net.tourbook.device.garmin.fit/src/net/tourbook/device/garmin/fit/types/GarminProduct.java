package net.tourbook.device.garmin.fit.types;

import com.garmin.fit.Fit;

public enum GarminProduct {

	// source: com.garmin.fit.GarminProduct

	HRM1(1), //
	AXH01(2), // 							// AXH01 HRM chipset
	AXB01(3), //
	AXB02(4), //
	HRM2SS(5), //
	DSI_ALF02(6), //
	HRM3SS(7), //
	HRM_RUN_SINGLE_BYTE_PRODUCT_ID(8), //	// hrm_run model for HRM ANT+ messaging
	BSM(9), // 								// BSM model for ANT+ messaging
	BCM(10), // 							// BCM model for ANT+ messaging
	FR301_CHINA(473), //
	FR301_JAPAN(474), //
	FR301_KOREA(475), //
	FR301_TAIWAN(494), //
	FR405(717), // 							// Forerunner 405
	FR50(782), // 							// Forerunner 50
	FR405_JAPAN(987), //
	FR60(988), // 							// Forerunner 60
	DSI_ALF01(1011), //
	FR310XT(1018), // 						// Forerunner 310
	EDGE500(1036), //
	FR110(1124), // 						// Forerunner 110
	EDGE800(1169), //
	EDGE500_TAIWAN(1199), //
	EDGE500_JAPAN(1213), //
	CHIRP(1253), //
	FR110_JAPAN(1274), //
	EDGE200(1325), //
	FR910XT(1328), //
	EDGE800_TAIWAN(1333), //
	EDGE800_JAPAN(1334), //
	ALF04(1341), //
	FR610(1345), //
	FR210_JAPAN(1360), //
	VECTOR_SS(1380), //
	VECTOR_CP(1381), //
	EDGE800_CHINA(1386), //
	EDGE500_CHINA(1387), //
	FR610_JAPAN(1410), //
	EDGE500_KOREA(1422), //
	FR70(1436), //
	FR310XT_4T(1446), //
	AMX(1461), //
	FR10(1482), //
	EDGE800_KOREA(1497), //
	SWIM(1499), //
	FR910XT_CHINA(1537), //
	FENIX(1551), //
	EDGE200_TAIWAN(1555), //
	EDGE510(1561), //
	EDGE810(1567), //
	TEMPE(1570), //
	FR910XT_JAPAN(1600), //
	FR620(1623), //
	FR220(1632), //
	FR910XT_KOREA(1664), //
	FR10_JAPAN(1688), //
	EDGE810_JAPAN(1721), //
	VIRB_ELITE(1735), //
	EDGE_TOURING(1736), // 					// Also Edge Touring Plus
	EDGE510_JAPAN(1742), //
	HRM_RUN(1752), //
	EDGE510_ASIA(1821), //
	EDGE810_CHINA(1822), //
	EDGE810_TAIWAN(1823), //
	EDGE1000(1836), //
	VIVO_FIT(1837), //
	VIRB_REMOTE(1853), //
	VIVO_KI(1885), //
	EDGE510_KOREA(1918), //
	FR620_JAPAN(1928), //
	FR620_CHINA(1929), //
	FR220_JAPAN(1930), //
	FR220_CHINA(1931), //
	SDM4(10007), // 						// SDM4 footpod
	EDGE_REMOTE(10014), //
	TRAINING_CENTER(20119), //
	ANDROID_ANTPLUS_PLUGIN(65532), //
	CONNECT(65534), // 						// Garmin Connect website
	INVALID(Fit.UINT16_INVALID), //

	;

	private final Integer	value;

	private GarminProduct(final Integer value) {
		this.value = value;
	}

	public static GarminProduct valueOf(final Integer value) {

		GarminProduct result = null;

		for (final GarminProduct garminProduct : GarminProduct.values()) {

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
