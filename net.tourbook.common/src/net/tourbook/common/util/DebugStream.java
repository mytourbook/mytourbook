package net.tourbook.common.util;

import java.io.PrintStream;
import java.text.MessageFormat;

/**
 * @author Jeeeyul 2011. 11. 1. 4:36:51
 * @since M1.10
 * 
 *        <pre>
 * 
 * Just call DebugStream.activate() when your application start.
 * 
 * It converts messages in console view from:
 * 
 * Hello World.
 * 
 * into:
 * 
 * (MyHelloWorld.java:10) : Hello World.
 * 
 * Yes, you can click and jump to your System.out.println() code in Eclipse console view. It makes debugging so easy.
 * </pre>
 */
public class DebugStream extends PrintStream {

	private static final DebugStream	INSTANCE	= new DebugStream();

	private DebugStream() {
		super(System.out);
	}

	public static void activate() {
		System.setOut(INSTANCE);
	}

	@Override
	public void println(final Object x) {
		showLocation();
		super.println(x);
	}

	@Override
	public void println(final String x) {
		showLocation();
		super.println(x);
	}

	private void showLocation() {
		final StackTraceElement element = Thread.currentThread().getStackTrace()[3];
		super.print(MessageFormat.format("({0}:{1, number,#}) : ", element.getFileName(), element.getLineNumber())); //$NON-NLS-1$
	}
}
