package de.byteholder.geoclipse.map;

import org.eclipse.swt.graphics.GC;

public interface Painter<T> {

	public void paint(GC gc, T parameter);
}
