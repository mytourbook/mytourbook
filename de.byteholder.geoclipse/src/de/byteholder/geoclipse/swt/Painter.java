package de.byteholder.geoclipse.swt;

import org.eclipse.swt.graphics.GC;

public interface Painter<T> {

	public void paint(GC gc, T parameter);
}
