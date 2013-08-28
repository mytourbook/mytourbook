package net.tourbook.common.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

/*
 * This source code was found (1.8.2013) here http://www.eclipse.org/forums/index.php/t/208284/
 */

/**
 * Demonstrates the workaround for displaying a SWT popup menu over swing components under GTK (menu
 * not displayed / visible bug)
 * 
 * @author Samuel Thiriot, INRIA
 */
public class SWTPopupOverAWT {

	private Display				swtDisplay;
//	private Shell				swtShell;

	private Menu				swtPopupMenu;

	private final static int	MAX_RETRIES	= 10;

	public SWTPopupOverAWT(final Display display, final Menu swtContextMenu) {

		swtDisplay = display;
		swtPopupMenu = swtContextMenu;
	}

//	public static void main(final String[] args) {
//
//		final SWTPopupOverAWT test = new SWTPopupOverAWT();
//
//		test.display();
//
//	}
//
//	public void display() {
//
//		// creates a SWT Shell
//		swtDisplay = new Display();
//		swtShell = new Shell(swtDisplay);
//		swtShell.setText("click somewhere !"); //$NON-NLS-1$
//
//		final Composite swtComposite = new Composite(swtShell, SWT.BORDER | SWT.EMBEDDED);
//
//		swtShell.setLayout(new FillLayout());
//
//		// create AWT embedded components into the SWT shell
//		final Frame awtFrame = SWT_AWT.new_Frame(swtComposite);
//		final Panel awtPanel = new Panel(new BorderLayout());
//		awtFrame.add(awtPanel);
//
//		// create the popup menu to display
//		swtPopupMenu = new Menu(swtComposite);
//
//		final MenuItem item1 = new MenuItem(swtPopupMenu, SWT.PUSH);
//		item1.setText("useless item for test"); //$NON-NLS-1$
//		item1.addSelectionListener(new SelectionListener() {
//			@Override
//			public void widgetDefaultSelected(final SelectionEvent arg0) {}
//
//			@Override
//			public void widgetSelected(final SelectionEvent arg0) {
//				System.out.println("The useless popup menu was clicked !"); //$NON-NLS-1$
//			}
//		});
//
//		swtPopupMenu.addMenuListener(new MenuListener() {
//
//			@Override
//			public void menuHidden(final MenuEvent arg0) {
//
//				System.out.println("the SWT menu was hidden (by itself)"); //$NON-NLS-1$
//			}
//
//			@Override
//			public void menuShown(final MenuEvent arg0) {
//
//			}
//		});
//
//		// management of events from awt to swt
//
//		final JPanel jPanel = new JPanel(new BorderLayout());
//		awtPanel.add(jPanel);
//
//		jPanel.addMouseListener(new MouseAdapter() { // maps AWT mouse events to the display of the popup menu
//
//					@Override
//					public void mousePressed(final MouseEvent e) {
//
//						final boolean isLeft = SwingUtilities.isLeftMouseButton(e);
//						final boolean isRight = SwingUtilities.isRightMouseButton(e);
//
//						System.out.println("AWT click detected\tisLeft=" + isLeft + "\tisRight=" + isRight); //$NON-NLS-1$ //$NON-NLS-2$
//
//						swtDisplay.asyncExec(new Runnable() {
//							public void run() {
//								System.out.println("SWT calling menu"); //$NON-NLS-1$
//								swtIndirectShowMenu(e.getXOnScreen(), e.getYOnScreen());
//							}
//						});
//					}
//				});
//
//		// loop for SWT events
//		swtShell.setBounds(10, 10, 300, 300);
//		swtShell.open();
//		while (!swtShell.isDisposed()) {
//			if (!swtDisplay.readAndDispatch()) {
//				swtDisplay.sleep();
//			}
//		}
//
//		swtDisplay.dispose();
//
//	}

	/**
	 * Workaround: due to a GTK problem (Linux and other Unix), popup menus are not always
	 * displayed. This tries several times to display it. see
	 * http://dev.eclipse.org/newslists/news.eclipse.platform.swt/msg33992.html
	 * http://www.eclipsezone.com/eclipse/forums/t95687.html
	 * 
	 * @param menu
	 * @param retriesRemaining
	 */
	protected void retryVisible(final int retriesRemaining) {

		swtDisplay.asyncExec(new Runnable() {

			@Override
			public void run() {

				if (swtPopupMenu.isVisible()) {
//					System.out.println("made visible after " + (MAX_RETRIES - retriesRemaining) + " attempts"); //$NON-NLS-1$ //$NON-NLS-2$

				} else if (retriesRemaining > 0) {

//					System.out.println("retrying (remains " + (retriesRemaining - 1) + ")"); //$NON-NLS-1$ //$NON-NLS-2$

					//swtHost.getShell().forceFocus();
					//swtHost.getShell().forceActive();
					//menu.setVisible(false);
					swtPopupMenu.setVisible(false);

					{
						final Shell shell = new Shell(swtDisplay, SWT.APPLICATION_MODAL | // should lead the window manager to switch another window to the front
								SWT.DIALOG_TRIM // not displayed into taskbars nor in task managers
						);
						shell.setSize(10, 10); // big enough to avoid errors from the gtk layer
						shell.setBackground(swtDisplay.getSystemColor(SWT.COLOR_RED));
						shell.setText("Not visible"); //$NON-NLS-1$
						shell.setVisible(false);
						shell.open();
						shell.dispose();
					}
					swtPopupMenu.getShell().forceActive();

					//forceFocus();
					//forceActive();
					swtPopupMenu.setVisible(true);

					retryVisible(retriesRemaining - 1);

				} else {
					System.err.println("unable to display the menu, sorry :-("); //$NON-NLS-1$
				}
			}
		});
	}

	protected void swtDirectShowMenu(final int x, final int y) {

		if (swtDisplay.isDisposed()) {
			return;
		}

		swtPopupMenu.setLocation(new Point(x, y));

//		System.out.println("Displaying the menu at coordinates " + x + "," + y); //$NON-NLS-1$ //$NON-NLS-2$
		swtPopupMenu.setVisible(true);

		// if GUI not based on GTK, the menu should already be displayed.

		retryVisible(MAX_RETRIES); // but just in case, we ensure this is the case :-)

	}

	/**
	 * May be called from the AWT thread. Just called swtDirectShowMenu with the very same
	 * parameters, but from the right thread.
	 * 
	 * @param x
	 * @param y
	 */
	public void swtIndirectShowMenu(final int x, final int y) {

		swtDisplay.asyncExec(new Runnable() {

			@Override
			public void run() {

				swtDirectShowMenu(x, y);
			}
		});

	}

}
