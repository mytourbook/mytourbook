package net.tourbook.common.tooltip;

public interface ICloseOpenedDialogs {

	/**
	 * Close all opened dialogs except the opening dialog.
	 * 
	 * @param openingDialog
	 */
	void closeOpenedDialogs(IOpeningDialog openingDialog);

}
