package de.byteholder.geoclipse.map.event;

public class ZoomEvent {

	private int	newZoom;

	public ZoomEvent(int newZoom) {
		this.newZoom = newZoom;
	}

	public int getZoom() {
		return newZoom;
	}
}
