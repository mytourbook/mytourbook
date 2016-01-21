package cop.swt.widgets.viewers.table.celleditors;

public final class RangeContent
{
	private final int minimum;
	private final int maximum;
	private final int increment;
	private final int multiplier;

	public RangeContent(double minimum, double maximum, double increment, int multiplier)
	{
		this.minimum = (int)(minimum * multiplier);
		this.maximum = (int)(maximum * multiplier);
		this.increment = (int)(increment * multiplier);
		this.multiplier = multiplier;
	}

	public RangeContent(int minimum, int maximum, int increment)
	{
		this(minimum, maximum, increment, 1);
	}

	public int getMinimum()
	{
		return minimum;
	}

	public int getMaximum()
	{
		return maximum;
	}

	public int getIncrement()
	{
		return increment;
	}

	public int getMultiplier()
	{
		return multiplier;
	}

	/*
	 * Object
	 */

	@Override
	public String toString()
	{
		return "[" + minimum + "," + maximum + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
