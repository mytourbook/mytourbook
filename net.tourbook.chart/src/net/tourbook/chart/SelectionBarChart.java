package net.tourbook.chart;

import org.eclipse.jface.viewers.ISelection;

public class SelectionBarChart implements ISelection {

	public int	serieIndex;
	public int	valueIndex;

	public SelectionBarChart(int serieIndex, int valueIndex) {
		this.serieIndex = serieIndex;
		this.valueIndex = valueIndex;
	}

	public boolean isEmpty() {
		return false;
	}

}
