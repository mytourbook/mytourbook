/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.map3.view;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.util.PerformanceStatistic;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;

import net.tourbook.common.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

public class Map3StatisticsView extends ViewPart {

	public static final String					ID					= "net.tourbook.map3.Map3StatisticsView";	//$NON-NLS-1$

	private static final int					UI_UPDATE_INTERVAL	= 500;

	private static final WorldWindowGLCanvas	_wwCanvas			= Map3Manager.getWWCanvas();

	private RenderingListener					_renderingListener;

	private Collection<PerformanceStatistic>	_perFrameStatistics	= new ArrayList<PerformanceStatistic>();

	private long								_lastUIUpdateTime;
	private boolean								_isUIUpdateScheduled;

	/*
	 * UI controls
	 */
	private FormToolkit							_tk;

	private ScrolledComposite					_scrolledContainer;
	private Composite							_scrolledContent;

	private Composite							_containerStatistics;

	public Map3StatisticsView() {}

	private void addRenderingListener() {

		_wwCanvas.setPerFrameStatisticsKeys(PerformanceStatistic.ALL_STATISTICS_SET);

		_renderingListener = new RenderingListener() {
			public void stageChanged(final RenderingEvent event) {

				if (_isUIUpdateScheduled) {

					// UI update is scheduled but not yet done

					return;
				}

				final String stage = event.getStage();
				final Object source = event.getSource();

//				System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//						+ ("\tstage: " + stage)
//						+ ("\tsource: " + source.getClass()));
//				// TODO remove SYSTEM.OUT.PRINTLN

				final long now = System.currentTimeMillis();

				if (now - _lastUIUpdateTime > UI_UPDATE_INTERVAL
						&& stage.equals(RenderingEvent.AFTER_BUFFER_SWAP)
						&& source instanceof WorldWindow) {

					// check if statistic data are available
					final Collection<PerformanceStatistic> perFrameStatistics = _wwCanvas
							.getSceneController()
							.getPerFrameStatistics();

					if (perFrameStatistics.size() < 1) {
						return;
					}

					// schedule a new UI update

					_perFrameStatistics.clear();
					_perFrameStatistics.addAll(perFrameStatistics);

					_isUIUpdateScheduled = true;

					EventQueue.invokeLater(new Runnable() {
						public void run() {

							// run in SWT thread

							Display.getDefault().asyncExec(new Runnable() {
								public void run() {

									updateUI_Container();

									_isUIUpdateScheduled = false;

									_lastUIUpdateTime = System.currentTimeMillis();
								}
							});
						}
					});

				}
			}
		};

		_wwCanvas.addRenderingListener(_renderingListener);
	}

	private void createActions(final Composite parent) {

	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		addRenderingListener();

		createActions(parent);
		fillActionBars();
	}

	private void createUI(final Composite parent) {

		_tk = new FormToolkit(parent.getDisplay());

		_scrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		{
			_scrolledContent = _tk.createComposite(_scrolledContainer);
			GridDataFactory.fillDefaults().applyTo(_scrolledContent);
			GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_scrolledContent);
			{
				createUI_10_StatisticsContainer(_scrolledContent);
			}

			// setup scrolled container
			_scrolledContainer.setExpandVertical(true);
			_scrolledContainer.setExpandHorizontal(true);
			_scrolledContainer.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(final ControlEvent e) {
					onResizeScrolledContainer(_scrolledContent);
				}
			});

			_scrolledContainer.setContent(_scrolledContent);
		}
	}

	private void createUI_10_StatisticsContainer(final Composite parent) {

		_containerStatistics = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().applyTo(_containerStatistics);
		GridLayoutFactory.fillDefaults().applyTo(_containerStatistics);
	}

	private void createUI_50_StatisticChildren(	final Composite parent,
												final Collection<PerformanceStatistic> perFrameStatistics) {

		PerformanceStatistic[] pfs = new PerformanceStatistic[perFrameStatistics.size()];
		pfs = perFrameStatistics.toArray(pfs);
//		Arrays.sort(pfs);

		final Composite container = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.spacing(7, 3)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			for (final PerformanceStatistic stat : pfs) {

				final Object value = stat.getValue();
				String uiValue;
				if (value instanceof Integer) {
					uiValue = ((Integer) value).intValue() == 0 ? UI.EMPTY_STRING : value.toString();
				} else if (value instanceof Long) {
					uiValue = ((Long) value).longValue() == 0 ? UI.EMPTY_STRING : value.toString();
				} else {
					uiValue = value.toString();
				}

				final Label label = _tk.createLabel(container, uiValue, SWT.TRAIL);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

				_tk.createLabel(container, stat.getDisplayString());
			}
		}
	}

	@Override
	public void dispose() {

		_tk.dispose();

		_wwCanvas.removeRenderingListener(_renderingListener);

		super.dispose();
	}

	private void fillActionBars() {

	}

	private void onResizeScrolledContainer(final Composite container) {
		_scrolledContainer.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	@Override
	public void setFocus() {}

	private void updateUI_Container() {

		if (_containerStatistics == null || _containerStatistics.isDisposed()) {
			return;
		}

		_containerStatistics.setRedraw(false);
		{
			// dispose previous statistic controls
			for (final Control statsChildren : _containerStatistics.getChildren()) {
				statsChildren.dispose();
			}

			// create new statistic controls with content
			createUI_50_StatisticChildren(_containerStatistics, _perFrameStatistics);

//			_containerStatistics.layout();
			UI.updateScrolledContent(_containerStatistics);
		}
		_containerStatistics.setRedraw(true);
	}

}
