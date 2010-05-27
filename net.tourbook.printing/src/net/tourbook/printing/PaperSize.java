package net.tourbook.printing;

public enum PaperSize {

	A4(0), LETTER(1);

	private int code;

	private PaperSize(int c) {
		code = c;
	}

	public int getCode() {
		return code;
	}
	
	@Override
	public String toString() {
		// only capitalize the first letter
		String s = super.toString();
		return s.substring(0, 1) + s.substring(1).toLowerCase();
	}
}