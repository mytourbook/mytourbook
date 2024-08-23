/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.common;

import static org.eclipse.swt.events.ControlListener.controlResizedAdapter;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Random;

import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.measurement_system.MeasurementSystem;
import net.tourbook.common.measurement_system.MeasurementSystem_Manager;
import net.tourbook.common.measurement_system.Unit_Distance;
import net.tourbook.common.measurement_system.Unit_Elevation;
import net.tourbook.common.measurement_system.Unit_Length;
import net.tourbook.common.measurement_system.Unit_Length_Small;
import net.tourbook.common.measurement_system.Unit_Pace;
import net.tourbook.common.measurement_system.Unit_Pressure_Atmosphere;
import net.tourbook.common.measurement_system.Unit_Temperature;
import net.tourbook.common.measurement_system.Unit_Weight;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.common.weather.IWeather;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.menus.UIElement;
import org.epics.css.dal.Timestamp;
import org.epics.css.dal.Timestamp.Format;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class UI {

   public static final String       SYSTEM_NEW_LINE                      = System.lineSeparator();
   public static final String       SYSTEM_NEW_LINE2                     = SYSTEM_NEW_LINE + SYSTEM_NEW_LINE;

   public static final int          SHELL_MARGIN                         = 5;

   public static final char         SPACE                                = ' ';
   public static final char         NEW_LINE                             = '\n';
   public static final char         TAB                                  = '\t';

   public static final char         SYMBOL_BRACKET_LEFT                  = '(';
   public static final char         SYMBOL_BRACKET_RIGHT                 = ')';

   public static final String       COLON_SPACE                          = ": ";                                        //$NON-NLS-1$
   public static final String       COMMA_SPACE                          = ", ";                                        //$NON-NLS-1$
   /** This is not a real dash it's the negative sign character */
   public static final String       DASH                                 = "-";                                         //$NON-NLS-1$
   public static final String       DASH_WITH_SPACE                      = " - ";                                       //$NON-NLS-1$
   public static final String       DASH_WITH_DOUBLE_SPACE               = "   -   ";                                   //$NON-NLS-1$
   public static final String       DIMENSION                            = " x ";                                       //$NON-NLS-1$
   public static final String       EMPTY_STRING                         = "";                                          //$NON-NLS-1$
   public static final String       MNEMONIC                             = "&";                                         //$NON-NLS-1$
   public static final String       NEW_LINE_TEXT_WIDGET                 = "\r\n";                                      //$NON-NLS-1$
   public static final String       NEW_LINE1                            = "\n";                                        //$NON-NLS-1$
   public static final String       NEW_LINE2                            = "\n\n";                                      //$NON-NLS-1$
   public static final String       NEW_LINE3                            = "\n\n\n";                                    //$NON-NLS-1$
   public static final String       NULL                                 = "null";                                      //$NON-NLS-1$
   public static final String       RESET_LABEL                          = " X ";                                       //$NON-NLS-1$
   public static final String       SLASH                                = "/";                                         //$NON-NLS-1$
   public static final String       SLASH_WITH_SPACE                     = " / ";                                       //$NON-NLS-1$
   public static final String       SPACE1                               = " ";                                         //$NON-NLS-1$
   public static final String       SPACE2                               = "  ";                                        //$NON-NLS-1$
   public static final String       SPACE3                               = "   ";                                       //$NON-NLS-1$
   public static final String       SPACE4                               = "    ";                                      //$NON-NLS-1$
   public static final String       SPACE8                               = "        ";                                  //$NON-NLS-1$
   public static final String       TAB1                                 = "\t";                                        //$NON-NLS-1$
   public static final String       ZERO                                 = "0";                                         //$NON-NLS-1$

   private static final String      JS_APOSTROPHE                        = "'";                                         //$NON-NLS-1$
   private static final String      JS_APOSTROPHE_REPLACEMENT            = "\\'";                                       //$NON-NLS-1$

   /**
    * Suddenly JS_QUOTA_MARK causes this Eclipse exception when opening the string externalization
    * dialog
    *
    * <pre>
    *
    *  java.lang.StringIndexOutOfBoundsException: begin 3, end 0, length 3
    *          at java.base/java.lang.String.checkBoundsBeginEnd(String.java:4606)
    *          at java.base/java.lang.String.substring(String.java:2709)
    *          at org.eclipse.jdt.internal.corext.refactoring.nls.NLSHint.stripQuotes(NLSHint.java:266)
    *          at org.eclipse.jdt.internal.corext.refactoring.nls.NLSHint.createSubstitutions(NLSHint.java:221)
    *          at org.eclipse.jdt.internal.corext.refactoring.nls.NLSHint.<init>(NLSHint.java:106)
    *          at org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring.<init>(NLSRefactoring.java:92)
    *          at org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring.create(NLSRefactoring.java:113)
    *          at org.eclipse.jdt.internal.ui.refactoring.nls.ExternalizeWizard.lambda$0(ExternalizeWizard.java:84)
    *          at org.eclipse.swt.custom.BusyIndicator.showWhile(BusyIndicator.java:67)
    *          at org.eclipse.jdt.internal.ui.refactoring.nls.ExternalizeWizard.open(ExternalizeWizard.java:81)
    *          at org.eclipse.jdt.ui.actions.ExternalizeStringsAction.run(ExternalizeStringsAction.java:191)
    *          at org.eclipse.jdt.ui.actions.ExternalizeStringsAction.run(ExternalizeStringsAction.java:156)
    *          at org.eclipse.jdt.ui.actions.SelectionDispatchAction.dispatchRun(SelectionDispatchAction.java:278)
    * </pre>
    */
// private static final String      JS_QUOTA_MARK                        = "\"";                                        //$NON-NLS-1$
   private static String            JS_QUOTA_MARK                        = new StringBuilder().append('"').toString();
   private static final String      JS_QUOTA_MARK_REPLACEMENT            = "\\\"";                                      //$NON-NLS-1$
   private static final String      JS_BACKSLASH_REPLACEMENT             = "\\\\";                                      //$NON-NLS-1$
   private static final String      HTML_NEW_LINE                        = "\\n";                                       //$NON-NLS-1$

   public static final String       SYMBOL_AMPERSAND                     = "&";                                         //$NON-NLS-1$
   public static final String       SYMBOL_AMPERSAND_AMPERSAND           = "&&";                                        //$NON-NLS-1$
   public static final String       SYMBOL_ARROW_UP                      = "\u2191";                                    //$NON-NLS-1$
   public static final String       SYMBOL_ARROW_DOWN                    = "\u2193";                                    //$NON-NLS-1$
   public static final String       SYMBOL_ARROW_LEFT                    = "\u2190";                                    //$NON-NLS-1$
   public static final String       SYMBOL_ARROW_RIGHT                   = "\u2192";                                    //$NON-NLS-1$
   public static final String       SYMBOL_ARROW_LEFT_RIGHT              = "\u2194";                                    //$NON-NLS-1$
   public static final String       SYMBOL_ARROW_UP_DOWN                 = "\u2195";                                    //$NON-NLS-1$
   public static final String       SYMBOL_ARROW_UP_DOWN_II              = "\u21c5";                                    //$NON-NLS-1$
   public static final String       SYMBOL_AVERAGE                       = "\u00f8";                                    //$NON-NLS-1$
   public static final String       SYMBOL_AVERAGE_WITH_SPACE            = "\u00f8 ";                                   //$NON-NLS-1$
   public static final String       SYMBOL_BLACK_LARGE_CIRCLE            = "\u2B24";                                    //$NON-NLS-1$
   public static final String       SYMBOL_BOX                           = "\u25a0";                                    //$NON-NLS-1$
   public static final String       SYMBOL_BULLET                        = "\u2022";                                    //$NON-NLS-1$
   public static final String       SYMBOL_CROSS_MARK                    = "\u274C";                                    //$NON-NLS-1$
   /** This is the real dash and not the negative sign character */
   public static final String       SYMBOL_DASH                          = "\u2212";                                    //$NON-NLS-1$
   public static final String       SYMBOL_DEGREE                        = "\u00B0";                                    //$NON-NLS-1$
   public static final String       SYMBOL_DBL_ANGLE_QMARK_LEFT          = "\u00AB";                                    //$NON-NLS-1$
   public static final String       SYMBOL_DBL_ANGLE_QMARK_RIGHT         = "\u00BB";                                    //$NON-NLS-1$
   public static final String       SYMBOL_DIFFERENCE                    = "\u0394";                                    //$NON-NLS-1$
   public static final String       SYMBOL_DIFFERENCE_WITH_SPACE         = "\u0394 ";                                   //$NON-NLS-1$
   public static final String       SYMBOL_DOUBLE_HORIZONTAL             = "\u2550";                                    //$NON-NLS-1$
   public static final String       SYMBOL_ELLIPSIS                      = "\u2026";                                    //$NON-NLS-1$
   public static final String       SYMBOL_FIGURE_DASH                   = "\u2012";                                    //$NON-NLS-1$
   public static final String       SYMBOL_FLOPPY_DISK                   = "\ue222";                                    //$NON-NLS-1$
   public static final String       SYMBOL_FOOT_NOTE                     = "\u20F0";                                    //$NON-NLS-1$
   public static final String       SYMBOL_FULL_BLOCK                    = "\u2588";                                    //$NON-NLS-1$
   public static final String       SYMBOL_HEAVY_CHECK_MARK              = "\u2714";                                    //$NON-NLS-1$
   public static final String       SYMBOL_HOURGLASS_WITH_FLOWING_SAND   = "\u231B";                                    //$NON-NLS-1$
   public static final String       SYMBOL_IDENTICAL_TO                  = "\u2261";                                    //$NON-NLS-1$
   public static final String       SYMBOL_INFINITY_MAX                  = "\u221E";                                    //$NON-NLS-1$
   public static final String       SYMBOL_INFINITY_MIN                  = "-\u221E";                                   //$NON-NLS-1$
   public static final String       SYMBOL_MIN                           = "\u1D5B";                                    //$NON-NLS-1$
   public static final String       SYMBOL_MAX                           = "^";                                         //$NON-NLS-1$
   public static final String       SYMBOL_PLUS_MINUS                    = "\u00B1";                                    //$NON-NLS-1$
   public static final String       SYMBOL_SUM_WITH_SPACE                = "\u2211 ";                                   //$NON-NLS-1$
   public static final String       SYMBOL_SUMMARIZED_AVERAGE            = "\u2211 \u00D8";                             //$NON-NLS-1$
   public static final String       SYMBOL_SUN                           = "\u263C";                                    //$NON-NLS-1$
   public static final String       SYMBOL_TAU                           = "\u03c4";                                    //$NON-NLS-1$
   public static final String       SYMBOL_TILDE                         = "\u007e";                                    //$NON-NLS-1$
   public static final String       SYMBOL_WHITE_RIGHT_POINTING_TRIANGLE = "\u25B7";                                    //$NON-NLS-1$
   public static final String       SYMBOL_WHITE_HEAVY_CHECK_MARK        = "\u2705";                                    //$NON-NLS-1$
   public static final String       SYMBOL_WHITE_HARD_SHELL_FLOPPY_DISK  = convertUnicodeCodepointToSurrogate("1F5AB"); //$NON-NLS-1$
   public static final String       SYMBOL_SOFT_SHELL_FLOPPY_DISK        = convertUnicodeCodepointToSurrogate("1F5AC"); //$NON-NLS-1$

   public static final CharSequence SYMBOL_BACKSLASH                     = "\\";                                        //$NON-NLS-1$
   public static final String       SYMBOL_COLON                         = ":";                                         //$NON-NLS-1$
   public static final String       SYMBOL_COMMA                         = ",";                                         //$NON-NLS-1$
   public static final String       SYMBOL_DOT                           = ".";                                         //$NON-NLS-1$
   public static final String       SYMBOL_DOUBLE_VERTICAL               = "||";                                        //$NON-NLS-1$   // this looks ugly "\u2551";
   public static final String       SYMBOL_EQUAL                         = "=";                                         //$NON-NLS-1$
   public static final String       SYMBOL_EXCLAMATION_POINT             = "!";                                         //$NON-NLS-1$
   public static final String       SYMBOL_GREATER_THAN                  = ">";                                         //$NON-NLS-1$
   public static final String       SYMBOL_LESS_THAN                     = "<";                                         //$NON-NLS-1$
   public static final String       SYMBOL_MIDDLE_DOT                    = "·";                                         //$NON-NLS-1$
   public static final String       SYMBOL_MINUS                         = "-";                                         //$NON-NLS-1$
   public static final String       SYMBOL_MNEMONIC                      = "&";                                         //$NON-NLS-1$
   public static final String       SYMBOL_NUMBER_SIGN                   = "#";                                         //$NON-NLS-1$
   public static final String       SYMBOL_PERCENTAGE                    = "%";                                         //$NON-NLS-1$
   public static final String       SYMBOL_PLUS                          = "+";                                         //$NON-NLS-1$
   public static final String       SYMBOL_QUESTION_MARK                 = "?";                                         //$NON-NLS-1$
   public static final char         SYMBOL_SEMICOLON                     = ';';
   public static final String       SYMBOL_STAR                          = "*";                                         //$NON-NLS-1$
   public static final String       SYMBOL_TEMPERATURE_CELSIUS           = "\u00b0C";                                   //$NON-NLS-1$
   public static final String       SYMBOL_TEMPERATURE_FAHRENHEIT        = "\u00b0F";                                   //$NON-NLS-1$
   public static final String       SYMBOL_UNDERSCORE                    = "_";                                         //$NON-NLS-1$
   public static final String       SYMBOL_WIND_WITH_SPACE               = "W ";                                        //$NON-NLS-1$

   public static final CharSequence SYMBOL_HTML_BACKSLASH                = "&#92;";                                     //$NON-NLS-1$

   public static final String       LINK_TAG_START                       = "<a>";                                       //$NON-NLS-1$
   public static final String       LINK_TAG_END                         = "</a>";                                      //$NON-NLS-1$

   public static final int          FORM_FIRST_COLUMN_INDENT             = 16;

   private static final String      Format_TimeDuration_mmss             = "% 03d:%02d";                                //$NON-NLS-1$

   /**
    * The ellipsis is the string that is used to represent shortened text.
    *
    * @since 3.0
    */
   public static final String       ELLIPSIS                             = "...";                                       //$NON-NLS-1$
   public static final String       ELLIPSIS_WITH_SPACE                  = " ... ";                                     //$NON-NLS-1$

   public static final String       INCREMENTER_0_1                      = "0.1";                                       //$NON-NLS-1$
   public static final String       INCREMENTER_0_01                     = "0.01";                                      //$NON-NLS-1$
   public static final String       INCREMENTER_1                        = "1";                                         //$NON-NLS-1$
   public static final String       INCREMENTER_10                       = "10";                                        //$NON-NLS-1$
   public static final String       INCREMENTER_100                      = "100";                                       //$NON-NLS-1$
   public static final String       INCREMENTER_1_000                    = FormatManager.formatNumber_0(1_000);
   public static final String       INCREMENTER_10_000                   = FormatManager.formatNumber_0(10_000);
   public static final String       INCREMENTER_100_000                  = FormatManager.formatNumber_0(100_000);

   private static final char[]      INVALID_FILENAME_CHARS               = new char[] {
         '\\',
         '/',
         ':',
         '*',
         '?',
         '"',
         '<',
         '>',
         '|', };
   private static final char[]      INVALID_FILEPATH_CHARS               = new char[] {
         '*',
         '?',
         '"',
         '<',
         '>',
         '|', };

// SET_FORMATTING_OFF

   public static final boolean   IS_LINUX    = "gtk".equals(SWT.getPlatform());                                                                  //$NON-NLS-1$
   public static final boolean   IS_OSX      = "carbon".equals(SWT.getPlatform())   || "cocoa".equals(SWT.getPlatform());                        //$NON-NLS-1$ //$NON-NLS-2$
   public static final boolean   IS_WIN      = "win32".equals(SWT.getPlatform())    || "wpf".equals(SWT.getPlatform());                           //$NON-NLS-1$ //$NON-NLS-2$

// SET_FORMATTING_ON

   public static final String  TRUE                           = Boolean.toString(true);
   public static final String  FALSE                          = Boolean.toString(false);

   /**
    * Is <code>true</code> when the dark theme in the UI is selected
    */
   public static boolean       IS_DARK_THEME;

   /**
    * Is <code>true</code> when the bright theme in the UI is selected
    */
   public static boolean       IS_BRIGHT_THEME;

   /**
    * Is <code>true</code> when a 4k display is used
    */
   public static boolean       IS_4K_DISPLAY;

   /**
    * On Linux an async selection event is fired since e4
    */
   public static final String  FIX_LINUX_ASYNC_EVENT_1        = "FIX_LINUX_ASYNC_EVENT_1";   //$NON-NLS-1$
   public static final String  FIX_LINUX_ASYNC_EVENT_2        = "FIX_LINUX_ASYNC_EVENT_2";   //$NON-NLS-1$

   public static final String  BROWSER_TYPE_MOZILLA           = "mozilla";                   //$NON-NLS-1$

   public static final String  TIME_ZONE_UTC                  = "UTC";                       //$NON-NLS-1$

   public static final String  UTF_8                          = "UTF-8";                     //$NON-NLS-1$
   public static final String  UTF_16                         = "UTF-16";                    //$NON-NLS-1$
   public static final String  ISO_8859_1                     = "ISO-8859-1";                //$NON-NLS-1$

   public static final Charset UTF8_CHARSET                   = Charset.forName(UTF_8);

   public static final String  MENU_SEPARATOR_ADDITIONS       = "additions";                 //$NON-NLS-1$

   private static final String NUMBER_FORMAT_1F               = "%.1f";                      //$NON-NLS-1$
   private static final String SUB_TASK_PROGRESS              = "{0} / {1} - {2} % - {3} Δ"; //$NON-NLS-1$

   /**
    * Layout hint for a description field
    */
   public static final int     DEFAULT_DESCRIPTION_WIDTH      = 350;
   public static final int     DEFAULT_FIELD_WIDTH            = 40;

   /**
    * Convert Joule in Calorie
    * <p>
    * 1 cal = 4.1868 J<br>
    * 1 J = 0.238846 cal
    */
   public static final float   UNIT_CALORIE_2_JOULE           = 4.1868f;

   /**
    * Convert Calorie to Joule
    * <p>
    * 1 cal = 4.1868 J<br>
    * 1 J = 0.238846 cal
    */
   public static final float   UNIT_JOULE_2_CALORY            = 1.0f / 4.1868f;

   /**
    * Convert Mile into Nautical mile.
    * <p>
    * Multiply miles with this value to get nautical miles
    */
   public static final float   UNIT_MILE_2_NAUTICAL_MILE      = 0.868976f;

   /**
    * Imperial system for distance
    */
   public static final float   UNIT_MILE                      = 1.609344f;

   /**
    * Nautical mile is exact 1852 meter
    */
   public static final float   UNIT_NAUTICAL_MILE             = 1.852f;

   /**
    * Imperial system for small distance, 1 yard = 3 feet = 36 inches = 0,9144 Meter
    */
   public static final float   UNIT_YARD                      = 0.9144f;

   /**
    * Imperial system for very small distance, 1 mm = 0.03937008 inches, 1 inch = 25.4 mm
    */
   public static final float   UNIT_INCH                      = 0.03937008f;

   /**
    * Imperial system for height
    */
   public static final float   UNIT_FOOT                      = 0.3048f;

   /**
    * Imperial system for weight
    */
   public static final float   UNIT_POUND                     = 2.204623f;

   /**
    * Imperial system for temperature
    * <p>
    * (Celsius * 9/5) + 32 = Fahrenheit
    */
   public static final float   UNIT_FAHRENHEIT_MULTI          = 1.8f;
   public static final float   UNIT_FAHRENHEIT_ADD            = 32;

   public static final float   UNIT_METER_TO_INCHES           = 39.37007874f;

   public static final float   UNIT_KILOGRAM_TO_POUND         = 2.204623f;

   /**
    * Hash code including all system measurement data. This can be used to easily find out if the
    * system has changed.
    */
   public static int           UNIT_HASH_CODE;

   /**
    * Distance could be km (metric), mile or nautical mile
    */
   public static boolean       UNIT_IS_DISTANCE_KILOMETER;

   /**
    * Distance could be km (metric), mile or nautical mile
    */
   public static boolean       UNIT_IS_DISTANCE_MILE;

   /**
    * Distance could be km (metric), mile or nautical mile
    */
   public static boolean       UNIT_IS_DISTANCE_NAUTICAL_MILE;

   /**
    * Elevation could be meter (metric) or foot
    */
   public static boolean       UNIT_IS_ELEVATION_FOOT;

   /**
    * Elevation could be meter (metric) or foot
    */
   public static boolean       UNIT_IS_ELEVATION_METER;

   /**
    * Length could be meter (metric) or yard
    */
   public static boolean       UNIT_IS_LENGTH_METER;

   /**
    * Length could be meter (metric) or yard
    */
   public static boolean       UNIT_IS_LENGTH_YARD;

   /**
    * Small length could be mm (metric) or inch
    */
   public static boolean       UNIT_IS_LENGTH_SMALL_MILLIMETER;

   /**
    * Small length could be mm (metric) or inch
    */
   public static boolean       UNIT_IS_LENGTH_SMALL_INCH;

   /**
    * Is <code>true</code> when the measurement system for the atmospheric pressure is millibar
    * (mb), otherwise it is inch of mercury (inHg)
    */
   public static boolean       UNIT_IS_PRESSURE_MILLIBAR;

   /**
    * Is <code>true</code> when the measurement system for the atmospheric pressure is inch of
    * mercury (inHg), otherwise it is millibar (mb)
    */
   public static boolean       UNIT_IS_PRESSURE_MERCURY;

   /**
    * Temperature could be celsius (metric) or fahrenheit
    */
   public static boolean       UNIT_IS_TEMPERATURE_CELSIUS;

   /**
    * Temperature could be celsius (metric) or fahrenheit
    */
   public static boolean       UNIT_IS_TEMPERATURE_FAHRENHEIT;

   /**
    * Weight could be kilogram (metric) or pound
    */
   public static boolean       UNIT_IS_WEIGHT_KILOGRAM;

   /**
    * Weight could be kilogram (metric) or pound
    */
   public static boolean       UNIT_IS_WEIGHT_POUND;

   public static boolean       UNIT_IS_PACE_MIN_PER_KILOMETER;
   public static boolean       UNIT_IS_PACE_MIN_PER_MILE;

   /**
    * Contains the system of measurement value for distances relative to the metric system.
    * <p>
    * The metric system is <code>1</code>, imperial system is {@link #UNIT_MILE} or
    * {@link #UNIT_NAUTICAL_MILE}
    */
   public static float         UNIT_VALUE_DISTANCE            = 1;

   /**
    * contains the system of measurement value for small distances relative to the metric system.
    * <p>
    * The metric system is <code>1</code>, imperial system is {@link #UNIT_YARD}
    */
   public static float         UNIT_VALUE_DISTANCE_SMALL      = 1;

   /**
    * Contains the system of measurement value for very small distances relative to the metric
    * system, the metric system is 1 mm, imperial is 0.03937008 inch.
    */
   public static float         UNIT_VALUE_DISTANCE_MM_OR_INCH = 1;

   /**
    * Contains the system of measurement value for altitudes relative to the metric system, the
    * metric system is <code>1</code>
    */
   public static float         UNIT_VALUE_ELEVATION           = 1;

   /**
    * contains the system of measurement value for the temperature, is set to <code>1</code> for the
    * metric system
    */
   public static float         UNIT_VALUE_TEMPERATURE         = 1;

   /**
    * contains the system of measurement value for the weight, is set to <code>1</code> for the
    * metric system
    */
   public static float         UNIT_VALUE_WEIGHT              = 1;

   /*
    * Contains the unit label in the current measurement system for the distance values
    */
   public static String       UNIT_LABEL_ALTIMETER;
   public static String       UNIT_LABEL_DISTANCE;
   public static String       UNIT_LABEL_DISTANCE_M_OR_YD;
   public static String       UNIT_LABEL_DISTANCE_MM_OR_INCH;

   /**
    * m (meter) or ft (feet)
    */
   public static String       UNIT_LABEL_ELEVATION;
   public static String       UNIT_LABEL_PRESSURE_MBAR_OR_INHG;
   public static String       UNIT_LABEL_TEMPERATURE;
   public static String       UNIT_LABEL_SPEED;
   public static String       UNIT_LABEL_PACE;
   public static String       UNIT_LABEL_WEIGHT;

   public static final String UNIT_LABEL_TIME      = "h";      //$NON-NLS-1$
   public static final String UNIT_LABEL_DIRECTION = "\u00B0"; //$NON-NLS-1$

   /*
    * Labels for the different measurement systems
    */
   public static final String          UNIT_ALTIMETER_M_H         = "m/h";                      //$NON-NLS-1$
   public static final String          UNIT_ALTIMETER_FT_H        = "ft/h";                     //$NON-NLS-1$
   public static final String          UNIT_DISTANCE_KM           = "km";                       //$NON-NLS-1$
   public static final String          UNIT_DISTANCE_MI           = "mi";                       //$NON-NLS-1$
   public static final String          UNIT_DISTANCE_NMI          = "nmi";                      //$NON-NLS-1$
   public static final String          UNIT_DISTANCE_YARD         = "yd";                       //$NON-NLS-1$
   public static final String          UNIT_DISTANCE_INCH         = "inch";                     //$NON-NLS-1$
   public static final String          UNIT_ELEVATION_M           = "m";                        //$NON-NLS-1$
   public static final String          UNIT_ELEVATION_FT          = "ft";                       //$NON-NLS-1$
   public static final String          UNIT_FLUIDS_ML             = "mL";                       //$NON-NLS-1$
   public static final String          UNIT_FLUIDS_L              = "L";                        //$NON-NLS-1$
   public static final String          UNIT_HEIGHT_FT             = "ft";                       //$NON-NLS-1$
   public static final String          UNIT_HEIGHT_IN             = "in";                       //$NON-NLS-1$
   public static final String          UNIT_JOULE                 = "J";                        //$NON-NLS-1$
   public static final String          UNIT_JOULE_KILO            = "kJ";                       //$NON-NLS-1$
   public static final String          UNIT_JOULE_MEGA            = "MJ";                       //$NON-NLS-1$
   public static final String          UNIT_KBYTE                 = "kByte";                    //$NON-NLS-1$
   public static final String          UNIT_MBYTE                 = "MByte";                    //$NON-NLS-1$
   public static final String          UNIT_METER                 = "m";                        //$NON-NLS-1$
   public static final String          UNIT_MM                    = "mm";                       //$NON-NLS-1$
   public static final String          UNIT_MS                    = "ms";                       //$NON-NLS-1$
   public static final String          UNIT_PERCENT               = "%";                        //$NON-NLS-1$
   public static final String          UNIT_POWER                 = "Watt";                     //$NON-NLS-1$
   public static final String          UNIT_POWER_SHORT           = "W";                        //$NON-NLS-1$
   public static final String          UNIT_POWER_TO_WEIGHT_RATIO = "W/Kg";                     //$NON-NLS-1$
   public static final String          UNIT_PACE_MIN_P_KM         = "min/km";                   //$NON-NLS-1$
   public static final String          UNIT_PACE_MIN_P_MILE       = "min/mi";                   //$NON-NLS-1$
   public static final String          UNIT_PRESSURE_MBAR         = "mbar";                     //$NON-NLS-1$
   public static final String          UNIT_PRESSURE_INHG         = "inHg";                     //$NON-NLS-1$
   public static final String          UNIT_SPEED_KM_H            = "km/h";                     //$NON-NLS-1$
   public static final String          UNIT_SPEED_KNOT            = "knot";                     //$NON-NLS-1$
   public static final String          UNIT_SPEED_MPH             = "mph";                      //$NON-NLS-1$
   public static final String          UNIT_TEMPERATURE_C         = "\u00B0C";                  //$NON-NLS-1$
   public static final String          UNIT_TEMPERATURE_F         = "\u00B0F";                  //$NON-NLS-1$
   public static final String          UNIT_VOLT                  = "V";                        //$NON-NLS-1$
   public static final String          UNIT_VOLTAGE               = "Volt";                     //$NON-NLS-1$
   public static final String          UNIT_WEIGHT_KG             = "kg";                       //$NON-NLS-1$
   public static final String          UNIT_WEIGHT_LBS            = "lbs";                      //$NON-NLS-1$
   public static final String          UNIT_WEIGHT_MG             = "mg";                       //$NON-NLS-1$

   public static final PeriodFormatter DEFAULT_DURATION_FORMATTER;
   public static final PeriodFormatter DEFAULT_DURATION_FORMATTER_SHORT;

   private static StringBuilder        _formatterSB               = new StringBuilder();
   private static Formatter            _formatter                 = new Formatter(_formatterSB);

   private static FontMetrics          _dialogFont_Metrics;

// SET_FORMATTING_OFF

   public   static final long beforeCET      = ZonedDateTime.of(1893, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli();
   public   static final long afterCETBegin  = ZonedDateTime.of(1893, 4, 1, 0, 6, 32, 0,ZoneOffset.UTC).toInstant().toEpochMilli();

// SET_FORMATTING_ON

   public static final int     BERLIN_HISTORY_ADJUSTMENT = 6 * 60 + 32;

   public static final int     DAY_IN_SECONDS            = 24 * 60 * 60;

   /**
    * The dialog settings key name for stored dialog x location.
    *
    * @since 3.2
    */
   private static final String DIALOG_ORIGIN_X           = "DIALOG_X_ORIGIN";             //$NON-NLS-1$

   /**
    * The dialog settings key name for stored dialog y location.
    *
    * @since 3.2
    */
   private static final String DIALOG_ORIGIN_Y           = "DIALOG_Y_ORIGIN";             //$NON-NLS-1$

   /**
    * The dialog settings key name for stored dialog width.
    *
    * @since 3.2
    */
   public static final String  DIALOG_WIDTH              = "DIALOG_WIDTH";                //$NON-NLS-1$

   /**
    * The dialog settings key name for stored dialog height.
    *
    * @since 3.2
    */
   public static final String  DIALOG_HEIGHT             = "DIALOG_HEIGHT";               //$NON-NLS-1$

   /**
    * The dialog settings key name for the font used when the dialog height and width was stored.
    *
    * @since 3.2
    */
   private static final String DIALOG_FONT_DATA          = "DIALOG_FONT_NAME";            //$NON-NLS-1$

   public static final Font    AWT_FONT_ARIAL_8          = Font.decode("Arial-plain-8");  //$NON-NLS-1$
   public static final Font    AWT_FONT_ARIAL_10         = Font.decode("Arial-plain-10"); //$NON-NLS-1$
   public static final Font    AWT_FONT_ARIAL_12         = Font.decode("Arial-plain-12"); //$NON-NLS-1$
   public static final Font    AWT_FONT_ARIAL_14         = Font.decode("Arial-plain-14"); //$NON-NLS-1$
   public static final Font    AWT_FONT_ARIAL_16         = Font.decode("Arial-plain-16"); //$NON-NLS-1$
   public static final Font    AWT_FONT_ARIAL_18         = Font.decode("Arial-plain-18"); //$NON-NLS-1$
   public static final Font    AWT_FONT_ARIAL_20         = Font.decode("Arial-plain-20"); //$NON-NLS-1$
   public static final Font    AWT_FONT_ARIAL_24         = Font.decode("Arial-plain-24"); //$NON-NLS-1$
   public static final Font    AWT_FONT_ARIAL_48         = Font.decode("Arial-plain-48"); //$NON-NLS-1$
   public static final Font    AWT_FONT_ARIAL_BOLD_12    = Font.decode("Arial-bold-12");  //$NON-NLS-1$
   public static final Font    AWT_FONT_ARIAL_BOLD_24    = Font.decode("Arial-bold-24");  //$NON-NLS-1$

   /**
    * Is "Segoe UI" with Win10
    */
   public static Font          AWT_DIALOG_FONT;

// SET_FORMATTING_OFF

   private static final Random   RANDOM_GENERATOR                       = new Random();
   private static final String   ALL_SCRAMBLED_CHARS_LOWER              = "abcdefghklmnoprsu";                       //$NON-NLS-1$
   private static final String   ALL_SCRAMBLED_CHARS_UPPER              = "ABCDEFGHKLMNOPRSU";                       //$NON-NLS-1$

   /*
    * image keys for images which are stored in the image registry
    */
   public static final String    IMAGE_ACTION_PHOTO_FILTER              = "IMAGE_ACTION_PHOTO_FILTER";               //$NON-NLS-1$
   public static final String    IMAGE_ACTION_PHOTO_FILTER_NO_PHOTOS    = "IMAGE_ACTION_PHOTO_FILTER_NO_PHOTOS";     //$NON-NLS-1$
   public static final String    IMAGE_ACTION_PHOTO_FILTER_WITH_PHOTOS  = "IMAGE_ACTION_PHOTO_FILTER_WITH_PHOTOS";   //$NON-NLS-1$
   public static final String    IMAGE_ACTION_PHOTO_FILTER_DISABLED     = "IMAGE_ACTION_PHOTO_FILTER_DISABLED";      //$NON-NLS-1$
   public static final String    IMAGE_CONFIGURE_COLUMNS                = "IMAGE_CONFIGURE_COLUMNS";                 //$NON-NLS-1$
   public static final String    IMAGE_EMPTY_16                         = "_empty16";                                //$NON-NLS-1$


   public static Color           SYS_COLOR_BLACK;
   public static Color           SYS_COLOR_BLUE;
   public static Color           SYS_COLOR_CYAN;
   public static Color           SYS_COLOR_GRAY;
   public static Color           SYS_COLOR_GREEN;
   public static Color           SYS_COLOR_MAGENTA;
   public static Color           SYS_COLOR_RED;
   public static Color           SYS_COLOR_WHITE;
   public static Color           SYS_COLOR_YELLOW;

   public static Color           SYS_COLOR_DARK_GRAY;
   public static Color           SYS_COLOR_DARK_GREEN;

   public static Color           SYS_COLOR_LIST_BACKGROUND;

   public static Color           SYS_COLOR_WIDGET_FOREGROUND;
   public static Color           SYS_COLOR_WIDGET_DARK_SHADOW;
   public static Color           SYS_COLOR_WIDGET_BACKGROUND;

// SET_FORMATTING_ON

   public static final ImageRegistry     IMAGE_REGISTRY;

   public static final int               DECORATOR_HORIZONTAL_INDENT = 2;

   /**
    * Contains the value that opacity is 100% opaque
    */
   public static int                     TRANSFORM_OPACITY_MAX;

   private static final GridDataFactory  _gridDataHint_Zero          = GridDataFactory.fillDefaults().hint(0, 0);
   private static final GridDataFactory  _gridDataHint_Default       = GridDataFactory.fillDefaults().hint(SWT.DEFAULT, SWT.DEFAULT);

   private static final IPreferenceStore _prefStore_Common           = CommonActivator.getPrefStore();

   static {

      // overwrite 4k scaling for debugging
      System.setProperty("swt.autoScale", "200");
      System.setProperty("swt.autoScale", "101");

      /**
       * This creates a display which may contain also sleak options, otherwise sleak would not
       * work.
       * <p>
       * Solution found here: "Sleak in RCP: Device is not tracking resource allocation"
       * https://en.it1352.com/article/fb82e2d4ec294636ba29f786e3335066.html
       */
      PlatformUI.createDisplay();

      updateUnits();

      IS_4K_DISPLAY = DPIUtil.getDeviceZoom() >= 140;
      setupUI_FontMetrics();
      setupUI_AWTFonts();

      IMAGE_REGISTRY = CommonActivator.getDefault().getImageRegistry();

// SET_FORMATTING_OFF

      IMAGE_REGISTRY.put(IMAGE_CONFIGURE_COLUMNS,  CommonActivator.getImageDescriptor(CommonImages.CustomizeProfilesColumns));
      IMAGE_REGISTRY.put(IMAGE_EMPTY_16,           CommonActivator.getImageDescriptor(CommonImages.App_EmptyIcon_Placeholder));

      final Display display = Display.getCurrent();

      SYS_COLOR_BLACK               = display.getSystemColor(SWT.COLOR_BLACK);
      SYS_COLOR_BLUE                = display.getSystemColor(SWT.COLOR_BLUE);
      SYS_COLOR_CYAN                = display.getSystemColor(SWT.COLOR_CYAN);
      SYS_COLOR_GRAY                = display.getSystemColor(SWT.COLOR_GRAY);
      SYS_COLOR_GREEN               = display.getSystemColor(SWT.COLOR_GREEN);
      SYS_COLOR_MAGENTA             = display.getSystemColor(SWT.COLOR_MAGENTA);
      SYS_COLOR_RED                 = display.getSystemColor(SWT.COLOR_RED);
      SYS_COLOR_WHITE               = display.getSystemColor(SWT.COLOR_WHITE);
      SYS_COLOR_YELLOW              = display.getSystemColor(SWT.COLOR_YELLOW);

      SYS_COLOR_DARK_GRAY           = display.getSystemColor(SWT.COLOR_DARK_GRAY);
      SYS_COLOR_DARK_GREEN          = display.getSystemColor(SWT.COLOR_DARK_GREEN);

      SYS_COLOR_LIST_BACKGROUND     = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
      SYS_COLOR_WIDGET_BACKGROUND   = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
      SYS_COLOR_WIDGET_DARK_SHADOW  = display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
      SYS_COLOR_WIDGET_FOREGROUND   = display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);

      TRANSFORM_OPACITY_MAX = _prefStore_Common.getInt(ICommonPreferences.TRANSFORM_VALUE_OPACITY_MAX);

      // add prop listener
      _prefStore_Common.addPropertyChangeListener(propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ICommonPreferences.TRANSFORM_VALUE_OPACITY_MAX)) {

            TRANSFORM_OPACITY_MAX = (int) propertyChangeEvent.getNewValue();
         }
      });

// SET_FORMATTING_ON

      final String commaSpace = Messages.Period_Format_CommaSpace;
      final String space2 = Messages.Period_Format_SpaceAndSpace;
      final String[] variants = {

            Messages.Period_Format_Space,
            Messages.Period_Format_Comma,
            Messages.Period_Format_CommaAndAnd,
            Messages.Period_Format_CommaSpaceAnd };

      DEFAULT_DURATION_FORMATTER = new PeriodFormatterBuilder()

            .appendYears()
            .appendSuffix(Messages.Period_Format_Year, Messages.Period_Format_Years)
            .appendSeparator(commaSpace, space2, variants)

            .appendMonths()
            .appendSuffix(Messages.Period_Format_Month, Messages.Period_Format_Months)
            .appendSeparator(commaSpace, space2, variants)

            .appendWeeks()
            .appendSuffix(Messages.Period_Format_Week, Messages.Period_Format_Weeks)
            .appendSeparator(commaSpace, space2, variants)

            .appendDays()
            .appendSuffix(Messages.Period_Format_Day, Messages.Period_Format_Days)
            .appendSeparator(commaSpace, space2, variants)

            .appendHours()
            .appendSuffix(Messages.Period_Format_Hour, Messages.Period_Format_Hours)
            .appendSeparator(commaSpace, space2, variants)

            .appendMinutes()
            .appendSuffix(Messages.Period_Format_Minute, Messages.Period_Format_Minutes)
            .appendSeparator(commaSpace, space2, variants)

            .appendSeconds()
            .appendSuffix(Messages.Period_Format_Second, Messages.Period_Format_Seconds)
            .appendSeparator(commaSpace, space2, variants)

            .appendMillis()
            .appendSuffix(Messages.Period_Format_Millisecond, Messages.Period_Format_Milliseconds)

            .toFormatter();

      DEFAULT_DURATION_FORMATTER_SHORT = new PeriodFormatterBuilder()

            .appendYears()
            .appendSuffix(Messages.Period_Format_Year_Short, Messages.Period_Format_Year_Short)
            .appendSeparator(commaSpace, space2, variants)

            .appendMonths()
            .appendSuffix(Messages.Period_Format_Month_Short, Messages.Period_Format_Month_Short)
            .appendSeparator(commaSpace, space2, variants)

            .appendWeeks()
            .appendSuffix(Messages.Period_Format_Week_Short, Messages.Period_Format_Week_Short)
            .appendSeparator(commaSpace, space2, variants)

            .appendDays()
            .appendSuffix(Messages.Period_Format_Day_Short, Messages.Period_Format_Day_Short)
            .appendSeparator(commaSpace, space2, variants)

            .appendHours()
            .appendSuffix(Messages.Period_Format_Hour_Short, Messages.Period_Format_Hour_Short)
            .appendSeparator(commaSpace, space2, variants)

            .appendMinutes()
            .appendSuffix(Messages.Period_Format_Minute_Short, Messages.Period_Format_Minute_Short)
            .appendSeparator(commaSpace, space2, variants)

            .appendSeconds()
            .appendSuffix(Messages.Period_Format_Second_Short, Messages.Period_Format_Second_Short)
            .appendSeparator(commaSpace, space2, variants)

            .appendMillis()
            .appendSuffix(Messages.Period_Format_Millisecond_Short, Messages.Period_Format_Millisecond_Short)

            .toFormatter();

   }
   /**
    * Number of horizontal dialog units per character, value <code>4</code>.
    */
   private static final int    HORIZONTAL_DIALOG_UNIT_PER_CHAR = 4;

   /**
    * Number of vertical dialog units per character, value <code>8</code>.
    */
//   private static final int   VERTICAL_DIALOG_UNITS_PER_CHAR   = 8;

   private static final String SYS_PROP__SCRAMBLE_DATA         = "scrambleData";                                     //$NON-NLS-1$

   /**
    * When <code>true</code> then data in the UI are scrambled. This is used to create anonymous
    * screenshots.
    * <p>
    * Commandline parameter: <code>-DscrambleData</code>
    */
   public static boolean       IS_SCRAMBLE_DATA                = System.getProperty(SYS_PROP__SCRAMBLE_DATA) != null;

   static {

      if (IS_SCRAMBLE_DATA) {

         Util.logSystemProperty_IsEnabled(UI.class,
               SYS_PROP__SCRAMBLE_DATA,
               "Visible data are scrambled"); //$NON-NLS-1$
      }
   }

   /**
    * Activate provided view when it is not yet active
    *
    * @param viewPart
    * @param viewPartID
    */
   public static void activateView(final IViewPart viewPart, final String viewPartID) {

      final IWorkbenchPart activePart = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow()
            .getActivePage()
            .getActivePart();

      if (activePart != viewPart) {

         try {

            viewPart.getSite().getPage().showView(viewPartID, null, IWorkbenchPage.VIEW_ACTIVATE);

         } catch (final PartInitException e) {

            StatusUtil.log(e);
         }
      }
   }

   /**
    * @param sash
    */
   public static void addSashColorHandler(final Sash sash) {

      final Display display = Display.getCurrent();

      final Color mouseEnterColor = UI.IS_DARK_THEME
            ? ThemeUtil.getDefaultForegroundColor_Shell()
            : display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);

      final Color mouseExitColor = UI.IS_DARK_THEME
            ? ThemeUtil.getDefaultBackgroundColor_Shell()
            : display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);

      final Color mouseDragColor = UI.IS_DARK_THEME
            ? ThemeUtil.getDefaultForegroundColor_Shell()
            : display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);

      sash.addMouseTrackListener(MouseTrackListener.mouseEnterAdapter(mouseEvent -> sash.setBackground(mouseEnterColor)));
      sash.addMouseTrackListener(MouseTrackListener.mouseExitAdapter(mouseEvent -> sash.setBackground(mouseExitColor)));

      // set color when sash is initially displayed
      sash.addControlListener(controlResizedAdapter(controlEvent -> sash.setBackground(mouseExitColor)));

      sash.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {

         // hide background when sash is dragged

         if (selectionEvent.detail == SWT.DRAG) {
            sash.setBackground(null);
         } else {
            sash.setBackground(mouseDragColor);
         }

      }));
   }

   /**
    * @param event
    * @param isDirectionUp
    *           Is <code>true</code> when direction is up, right or forward
    *
    * @return Returns <code>true</code> when the scale value was adjusted, otherwise
    *         <code>false</code>
    */
   public static boolean adjustScaleValueOnKey(final KeyEvent event, final boolean isDirectionUp) {

      boolean isCtrlKey;
      boolean isShiftKey;

      if (IS_OSX) {
         isCtrlKey = (event.stateMask & SWT.MOD1) > 0;
         isShiftKey = (event.stateMask & SWT.MOD3) > 0;
      } else {
         isCtrlKey = (event.stateMask & SWT.MOD1) > 0;
         isShiftKey = (event.stateMask & SWT.MOD2) > 0;
      }

      // skip when not accelerated otherwise it would add at least 1 which increments by 2 as minimum
      if (isCtrlKey == false && isShiftKey == false) {
         return false;
      }

      // accelerate with Ctrl + Shift key
      int accelerator = isCtrlKey ? 10 : 1;
      accelerator *= isShiftKey ? 5 : 1;

      final Scale scale = (Scale) event.widget;
      final int increment = scale.getIncrement();
      final int oldValue = scale.getSelection();
      final int valueDiff = (isDirectionUp
            ? increment
            : -increment)

            * accelerator;

      scale.setSelection(oldValue + valueDiff);

      return true;
   }

   public static void adjustScaleValueOnMouseScroll(final MouseEvent event) {

      boolean isCtrlKey;
      boolean isShiftKey;

      if (IS_OSX) {
         isCtrlKey = (event.stateMask & SWT.MOD1) > 0;
         isShiftKey = (event.stateMask & SWT.MOD3) > 0;
         //         isAltKey = (event.stateMask & SWT.MOD3) > 0;
      } else {
         isCtrlKey = (event.stateMask & SWT.MOD1) > 0;
         isShiftKey = (event.stateMask & SWT.MOD2) > 0;
         //         isAltKey = (event.stateMask & SWT.MOD3) > 0;
      }

      // accelerate with Ctrl + Shift key
      int accelerator = isCtrlKey ? 10 : 1;
      accelerator *= isShiftKey ? 5 : 1;

      final Scale scale = (Scale) event.widget;
      final int increment = scale.getIncrement();
      final int oldValue = scale.getSelection();
      final int valueDiff = ((event.count > 0 ? increment : -increment) * accelerator);

      scale.setSelection(oldValue + valueDiff);
   }

   public static void adjustSpinnerValueOnMouseScroll(final MouseEvent event) {

      adjustSpinnerValueOnMouseScroll(event, 1);
   }

   /**
    * @param event
    * @param defaultAccelerator
    *           Could be 10 to increase e.g. image size by 10 without pressing an accelerator key
    */
   public static void adjustSpinnerValueOnMouseScroll(final MouseEvent event, final int defaultAccelerator) {

      adjustSpinnerValueOnMouseScroll(event, defaultAccelerator, false);
   }

   /**
    * @param event
    * @param defaultAccelerator
    *           Could be 10 to increase e.g. image size by 10 without pressing an accelerator key
    * @param isSmallValueAdjustment
    */
   public static void adjustSpinnerValueOnMouseScroll(final MouseEvent event,
                                                      final int defaultAccelerator,
                                                      final boolean isSmallValueAdjustment) {

      boolean isCtrlKey;
      boolean isShiftKey;

      if (IS_OSX) {
         isCtrlKey = (event.stateMask & SWT.MOD1) > 0;
         isShiftKey = (event.stateMask & SWT.MOD3) > 0;
      } else {
         isCtrlKey = (event.stateMask & SWT.MOD1) > 0;
         isShiftKey = (event.stateMask & SWT.MOD2) > 0;
      }

      final int valueSign = event.count > 0 ? 1 : -1;

      // accelerate with Ctrl + Shift key
      int accelerator = isCtrlKey ? 10 : 1;
      accelerator *= isShiftKey ? 5 : 1;

      accelerator *= defaultAccelerator;

      final Spinner spinner = (Spinner) event.widget;
      int valueAdjustment = valueSign * accelerator;

      final int oldValue = spinner.getSelection();

      if (isSmallValueAdjustment) {

         if (oldValue < 10) {

            valueAdjustment = 1 * valueSign;

         } else if (oldValue < 20) {

            valueAdjustment = 2 * valueSign;

         } else if (oldValue < 100) {

            valueAdjustment = 5 * valueSign;
         }
      }

      spinner.setSelection(oldValue + valueAdjustment);
   }

   /**
    * Computes the average elevation change with given values of total elevation
    * change and total distance.
    *
    * @return
    *         If successful, the average elevation change in the current
    *         measurement system, 0 otherwise.
    */
   public static float computeAverageElevationChange(final float totalElevationChange,
                                                     final float distance) {

      if (Math.signum(distance) == 0) {
         return 0;
      }

      return totalElevationChange / (distance / 1000f);
   }

   /**
    * Computes the BMI (Body Mass Index) for a given user's height and weight.
    *
    * @param weight
    *           The user's weight in kilograms or pounds.
    * @param height
    *           The user's height in meters or feet.
    * @param heightInches
    *           The second part of the user's height in inches if the measurement
    *           system is in inches.
    *
    * @return The BMI value.
    */
   public static float computeBodyMassIndex(double weight, double height, final int heightInches) {

      if (UNIT_IS_LENGTH_SMALL_INCH) {

         height *= 12;
         height += heightInches;
         height = height / UNIT_INCH / 10;
         height = Math.round(height * 10 / 10);
      }
      if (UNIT_IS_WEIGHT_POUND) {

         weight /= UNIT_VALUE_WEIGHT;
         weight = Math.round(weight * 10 / 10);
      }

      height = height / 100;

      final double bmi = height == 0 ? 0 : weight / Math.pow(height, 2);

      return Math.round(bmi * 10.0) / 10.0f;
   }

   /**
    * @param averageElevationChange
    *           In m/km
    *
    * @return Returns the average elevation change in the current measurement system.
    */
   public static float convertAverageElevationChangeFromMetric(final float averageElevationChange) {

      if (UNIT_IS_ELEVATION_METER) {
         return averageElevationChange;
      }

      return averageElevationChange * UNIT_VALUE_DISTANCE / UNIT_VALUE_ELEVATION;
   }

   /**
    * Convert height from metric into inches
    *
    * @param height
    *
    * @return
    */
   public static float convertBodyHeightFromMetric(final float height) {

      if (UNIT_IS_ELEVATION_METER) {
         return height;
      }

      return height * UNIT_METER_TO_INCHES;
   }

   public static float convertBodyHeightToMetric(final float heightMeterOrFeet, final int heightInch) {

      if (UNIT_IS_ELEVATION_METER) {
         return heightMeterOrFeet;
      }

      final float heightFeetToInch = heightMeterOrFeet * 12;
      final float heightInchTotal = heightFeetToInch + heightInch;

      return 100 * heightInchTotal / UNIT_METER_TO_INCHES;
   }

   /**
    * @param bodyWeight
    *
    * @return Returns the weight in the current measurement system.
    */
   public static float convertBodyWeightFromMetric(final float bodyWeight) {

      if (UNIT_IS_WEIGHT_KILOGRAM) {
         return bodyWeight;
      }

      return bodyWeight * UNIT_KILOGRAM_TO_POUND;
   }

   /**
    * @param weight
    *
    * @return Returns the weight from the current measurement system converted into metric
    *         system.
    */
   public static float convertBodyWeightToMetric(final float weight) {

      if (UNIT_IS_WEIGHT_KILOGRAM) {
         return weight;
      }

      return weight / UNIT_KILOGRAM_TO_POUND;
   }

   /**
    * Returns the number of pixels corresponding to the given number of horizontal dialog units.
    * <p>
    * The required <code>FontMetrics</code> parameter may be created in the following way: <code>
    *    GC gc = new GC(control);
    *   gc.setFont(control.getFont());
    *   fontMetrics = gc.getFontMetrics();
    *   gc.dispose();
    * </code>
    * </p>
    *
    * @param fontMetrics
    *           used in performing the conversion
    * @param dlus
    *           the number of horizontal dialog units
    *
    * @return the number of pixels
    *
    * @since 2.0
    */
   private static int convertHorizontalDLUsToPixels(final FontMetrics fontMetrics, final int dlus) {

      // round to the nearest pixel
      return (int) ((fontMetrics.getAverageCharacterWidth() * dlus + HORIZONTAL_DIALOG_UNIT_PER_CHAR / 2.0)
            / HORIZONTAL_DIALOG_UNIT_PER_CHAR);
   }

   /**
    * Returns the number of pixels corresponding to the given number of horizontal dialog units.
    * <p>
    * This method may only be called after <code>initializeDialogUnits</code> has been called.
    * </p>
    * <p>
    * Clients may call this framework method, but should not override it.
    * </p>
    *
    * @param dlus
    *           the number of horizontal dialog units
    *
    * @return the number of pixels
    */
   private static int convertHorizontalDLUsToPixels(final int dlus) {

      if (setupUI_FontMetrics() == false) {

         // create default
         return dlus * 4;
      }

      return convertHorizontalDLUsToPixels(_dialogFont_Metrics, dlus);
   }

   /**
    * @param precipitation
    *           in mm or inch
    *
    * @return Returns the precipitation amount in the current measurement system.
    */
   public static float convertPrecipitation_FromMetric(final float precipitation) {

      if (UNIT_IS_LENGTH_SMALL_MILLIMETER) {
         return precipitation;
      }

      return precipitation * UNIT_METER_TO_INCHES / 1000;
   }

   /**
    * @param precipitation
    *           in mm or inch
    *
    * @return Returns the precipitation amount in the current measurement system.
    */
   public static float convertPrecipitation_ToMetric(final float precipitation) {

      if (UNIT_IS_LENGTH_SMALL_MILLIMETER) {
         return precipitation;
      }

      return precipitation / UNIT_METER_TO_INCHES * 1000;
   }

   /**
    * @param weatherPressure
    *
    * @return Returns the atmospheric pressure value in the current measurement system.
    */
   public static float convertPressure_FromMetric(final float weatherPressure) {

      if (UNIT_IS_PRESSURE_MILLIBAR) {
         return weatherPressure;
      }

      return weatherPressure * 0.02953f;
   }

   /**
    * @param weatherPressure
    *
    * @return Returns the atmospheric pressure value in the current measurement system.
    */
   public static float convertPressure_ToMetric(final float weatherPressure) {

      if (UNIT_IS_PRESSURE_MILLIBAR) {
         return weatherPressure;
      }

      return weatherPressure / 0.02953f;
   }

   /**
    * @param speed
    *
    * @return Returns the speed value in the current measurement system.
    */
   public static float convertSpeed_FromMetric(final float speed) {

      if (UNIT_IS_DISTANCE_MILE) {

         return speed / UI.UNIT_MILE;

      } else if (UNIT_IS_DISTANCE_NAUTICAL_MILE) {

         return speed / UI.UNIT_NAUTICAL_MILE;

      }

      return speed;
   }

   /**
    * Convert a speed value from km/h to m/s
    *
    * @param speed
    *
    * @return Returns the speed value in m/s.
    */
   public static float convertSpeed_KmhToMs(final float speed) {

      return speed * 0.277777778f;
   }

   /**
    * @param speed
    *
    * @return Returns the speed value from the current measurement system into metric
    */
   public static float convertSpeed_ToMetric(final float speed) {

      if (UNIT_IS_DISTANCE_MILE) {

         return speed * UI.UNIT_MILE;

      } else if (UNIT_IS_DISTANCE_NAUTICAL_MILE) {

         return speed * UI.UNIT_NAUTICAL_MILE;

      }

      return speed;
   }

   /**
    * @param temperature
    *
    * @return Returns the temperature in the current measurement system.
    */
   public static float convertTemperatureFromMetric(final float temperature) {

      if (UNIT_IS_TEMPERATURE_CELSIUS) {
         return temperature;
      }

      return temperature * UNIT_FAHRENHEIT_MULTI + UNIT_FAHRENHEIT_ADD;
   }

   /**
    * @param temperature
    *
    * @return Returns the temperature from the current measurement system converted into metric
    *         system.
    */
   public static float convertTemperatureToMetric(final float temperature) {

      if (UNIT_IS_TEMPERATURE_CELSIUS) {
         return temperature;
      }

      return (temperature - UNIT_FAHRENHEIT_ADD) / UNIT_FAHRENHEIT_MULTI;
   }

   /**
    * Converts a hexadecimal Unicode into its surrogate string
    *
    * @param hexUnicode
    *
    * @return
    */
   public static String convertUnicodeCodepointToSurrogate(final String hexUnicode) {

      final int codePoint = Integer.parseInt(hexUnicode, 16);

      final StringBuilder sb = new StringBuilder();

      if (Character.isBmpCodePoint(codePoint)) {

         sb.append((char) codePoint);

      } else if (Character.isValidCodePoint(codePoint)) {

         sb.append(Character.highSurrogate(codePoint));
         sb.append(Character.lowSurrogate(codePoint));

      } else {

         sb.append('?');
      }

      return sb.toString();
   }

   public static void copyTextIntoClipboard(final String text, final String statusMessage) {

      final Display display = Display.getDefault();
      final TextTransfer textTransfer = TextTransfer.getInstance();

      final Clipboard clipBoard = new Clipboard(display);
      {
         clipBoard.setContents(

               new Object[] { text },
               new Transfer[] { textTransfer });
      }
      clipBoard.dispose();

      // show info that data are copied "Data were copied into the clipboard"
      showStatusLineMessage(statusMessage);
   }

   /**
    * Create a cursor resource from an image descriptor. Cursor must be disposed.
    *
    * @param imageName
    *
    * @return
    */
   public static Cursor createCursorFromImage(final ImageDescriptor imageDescriptor) {

      Cursor cursor;

      final Image cursorImage = imageDescriptor.createImage();
      {
         cursor = new Cursor(Display.getDefault(), cursorImage.getImageData(), 0, 0);
      }
      cursorImage.dispose();

      return cursor;
   }

   /**
    * @return Returns a cursor which is hidden. This cursor must be disposed.
    */
   public static Cursor createHiddenCursor() {

      // create a cursor with a transparent image

      final Display display = Display.getDefault();

      final Color white = display.getSystemColor(SWT.COLOR_WHITE);
      final Color black = display.getSystemColor(SWT.COLOR_BLACK);

      final PaletteData palette = new PaletteData(white.getRGB(), black.getRGB());

      final ImageData sourceData = new ImageData(16, 16, 1, palette);
      sourceData.transparentPixel = 0;

      return new Cursor(display, sourceData, 0, 0);
   }

   /**
    * Creates a {@link Label} without text.
    *
    * @param parent
    *
    * @return
    */
   public static Label createLabel(final Composite parent) {

      return new Label(parent, SWT.NONE);
   }

   /**
    * Creates a {@link Label} with text.
    *
    * @param parent
    * @param text
    *
    * @return
    */
   public static Label createLabel(final Composite parent, final String text) {

      final Label label = new Label(parent, SWT.NONE);

      label.setText(text);

      return label;
   }

   /**
    * Creates a {@link Label} with text and style
    *
    * @param parent
    * @param text
    * @param style
    *
    * @return
    */
   public static Label createLabel(final Composite parent, final String text, final int style) {

      final Label label = new Label(parent, style);

      label.setText(text);

      return label;
   }

   public static Label createLabel(final Composite parent, final String text, final String tooltip) {

      final Label label = new Label(parent, SWT.NONE);

      label.setText(text);
      label.setToolTipText(tooltip);

      return label;
   }

   /**
    * @param text
    *
    * @return
    */
   public static String createLinkText(final String text) {

      return "<a>%s</a>".formatted(text); //$NON-NLS-1$
   }

   /**
    * @param href
    * @param text
    *
    * @return
    */
   public static String createLinkText(final String href, final String text) {

      return "<a href=\"%s\">%s</a>".formatted(href, text); //$NON-NLS-1$
   }

   /**
    * Creates a page with a static text by using a {@link FormToolkit}
    *
    * @param formToolkit
    * @param parent
    * @param labelText
    *
    * @return
    */
   public static Composite createPage(final FormToolkit formToolkit, final Composite parent, final String labelText) {

      final Composite container = formToolkit.createComposite(parent);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
      {
         final Label label = formToolkit.createLabel(container, labelText, SWT.WRAP);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
      }

      return container;
   }

   /**
    * Create a spacer for one column
    *
    * @param parent
    *
    * @return
    */
   public static Label createSpacer_Horizontal(final Composite parent) {

      return createSpacer_Horizontal(parent, 1);
   }

   public static Label createSpacer_Horizontal(final Composite parent, final int columns) {

      final Label label = new Label(parent, SWT.NONE);

      GridDataFactory.fillDefaults().span(columns, 1).applyTo(label);

      return label;
   }

   public static void createSpacer_Vertical(final Composite parent, final int height, final int spanHorizontal) {

      final Label label = new Label(parent, SWT.NONE);

      GridDataFactory.fillDefaults()
            .hint(SWT.DEFAULT, height)
            .span(spanHorizontal, 1)
            .applyTo(label);
   }

   /**
    * Creates a {@link Spinner} with minimum, maximum, increment, page increment, number of digits
    *
    * @param parent
    * @param digits
    * @param minimum
    * @param maximum
    * @param increment
    * @param pageIncrement
    *
    * @return
    */
   public static Spinner createSpinner(final Composite parent,
                                       final int digits,
                                       final int minimum,
                                       final int maximum,
                                       final int increment,
                                       final int pageIncrement) {

      final Spinner spinner = new Spinner(parent, SWT.BORDER);

      spinner.setDigits(digits);
      spinner.setMinimum(minimum);
      spinner.setMaximum(maximum);
      spinner.setIncrement(increment);
      spinner.setPageIncrement(pageIncrement);

      return spinner;
   }

   /**
    * Creates one {@link Action} in it's own toolbar.
    *
    * @param parent
    * @param action
    *
    * @return Returns the created {@link ToolBar} for the action
    */
   public static ToolBar createToolbarAction(final Composite parent, final Action action) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(action);

      tbm.update(true);

      return toolbar;
   }

   /**
    * Creates one {@link ContributionItem} in it' own toolbar.
    *
    * @param parent
    * @param contribItem
    */
   public static void createToolbarAction(final Composite parent, final ContributionItem contribItem) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(contribItem);

      tbm.update(true);
   }

   /**
    * @param imageWidth
    * @param imageHeight
    * @param existingImage
    * @param gcPainter
    *
    * @return Returns create image or reused image
    */
   public static Image createTransparentImage(final int imageWidth,
                                              final int imageHeight,
                                              final Image existingImage,
                                              final ImagePainter gcPainter) {

      final Device display = Display.getDefault();
      final RGB rgbTransparent = new RGB(0xfa, 0xfb, 0xfc);

      Image image;

      if (existingImage == null) {

         /*
          * Use a color which is likely not used, the previous color 0xfefefe was used and had bad
          * effects.
          */

         final ImageData imageData = new ImageData(
               imageWidth,
               imageHeight,
               24,
               new PaletteData(0xff, 0xff00, 0xff0000));

         imageData.transparentPixel = imageData.palette.getPixel(rgbTransparent);

         image = new Image(display, imageData);

      } else {

         image = existingImage;
      }

      final GC gc = new GC(image);

      final Color transparentColor = new Color(display, rgbTransparent);
      {
         gc.setBackground(transparentColor);
         gc.fillRectangle(image.getBounds());

         gcPainter.drawImage(gc);
      }
      transparentColor.dispose();
      gc.dispose();

      return image;
   }

   public static Composite createUI_PageNoData(final Composite parent, final String message) {

      final Composite pageNoData = new Composite(parent, SWT.NONE);

      // use a dimmed color, default is white
      pageNoData.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

      GridDataFactory.fillDefaults().grab(true, true).applyTo(pageNoData);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(pageNoData);
      {
         final Label lblNoData = new Label(pageNoData, SWT.WRAP);
         lblNoData.setText(message);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(lblNoData);
      }

      return pageNoData;
   }

   public static Cursor disposeResource(final Cursor resource) {

      if (resource != null) {
         resource.dispose();
      }

      return null;
   }

   /**
    * Disposes an image resource
    *
    * @param image
    *
    * @return
    */
   public static Image disposeResource(final Image resource) {

      if (resource != null) {
         resource.dispose();
      }

      return null;
   }

   public static org.eclipse.swt.graphics.Font disposeResource(final org.eclipse.swt.graphics.Font font) {

      if (font != null) {
         font.dispose();
      }

      return null;
   }

   public static Resource disposeResource(final Resource resource) {

      if (resource != null) {
         resource.dispose();
      }

      return null;
   }

   public static void dumpAllFonts() {

      System.out.println("All available font family names"); //$NON-NLS-1$

      // all fonts available in AWT
      final Font[] allFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();

      for (final Font font : allFonts) {

         String style;

         if (font.isBold()) {
            style = font.isItalic() ? "bolditalic" : "bold"; //$NON-NLS-1$ //$NON-NLS-2$
         } else {
            style = font.isItalic() ? "italic" : "plain"; //$NON-NLS-1$ //$NON-NLS-2$
         }

         System.out.println(EMPTY_STRING

               + "%-35s - %s".formatted( //$NON-NLS-1$

                     font.getFamily(),
//                           font.getName(),
                     style

               ));
      }
   }

   public static void dumpSuperClasses(final Object o) {

      Class<?> subclass = o.getClass();
      Class<?> superclass = subclass.getSuperclass();

      while (superclass != null) {

         final String className = superclass.getCanonicalName();

         System.out.println(className);

         subclass = superclass;
         superclass = subclass.getSuperclass();
      }
   }

   /**
    * Escape the ampersand symbol when it's not a mnemonic but is displayed in a
    * {@link Label#setText(String)}
    * <p>
    * "The mnemonic indicator character'&' can be escaped by doubling it in the string, causing a
    * single '&' to be displayed."
    *
    * @param text
    *
    * @return
    */
   public static String escapeAmpersand(final String text) {

      return text.replace(SYMBOL_AMPERSAND, SYMBOL_AMPERSAND_AMPERSAND);
   }

   public static String format_hh(final long time) {

      _formatterSB.setLength(0);

      return _formatter.format(Messages.Format_TimeDuration_hh,

            time / 3600

      ).toString();
   }

   public static String format_hh_mm(final long time) {

      _formatterSB.setLength(0);

      return _formatter.format(Messages.Format_TimeDuration_hhmm,

            time / 3600,
            time % 3600 / 60

      ).toString();
   }

   /**
    * Hours are ignored when they are 0. An empty string is returned when time = <code>-1</code>
    *
    * @param time
    *           in seconds
    *
    * @return
    */
   public static String format_hh_mm_ss(final long time) {

      if (time == -1) {
         return EMPTY_STRING;
      }

      _formatterSB.setLength(0);

      if (time >= 3600) {

         // display hours

         return _formatter.format(Messages.Format_TimeDuration_hhmmss,

               time / 3600,
               time % 3600 / 60,
               time % 3600 % 60

         ).toString();

      } else {

         // ignore hours

         return _formatter.format(Messages.Format_TimeDuration_hhmm,

               time % 3600 / 60,
               time % 3600 % 60

         ).toString();
      }
   }

   /**
    * force hours to be displayed
    *
    * @param time
    *           in seconds
    *
    * @return
    */
   public static String format_hhh_mm_ss(final long time) {

      _formatterSB.setLength(0);

      return _formatter.format(Messages.Format_TimeDuration_hhmmss,
            time / 3600,
            (time % 3600) / 60,
            (time % 3600) % 60)
            .toString();
   }

   public static String format_mm_ss(final long time) {

      _formatterSB.setLength(0);

      if (time < 0) {
         _formatterSB.append(DASH);
      }

      final long timeAbs = time < 0 ? 0 - time : time;

      return _formatter.format(Messages.Format_TimeDuration_hhmm,

            timeAbs / 60,
            timeAbs % 60

      ).toString();
   }

   /**
    * Format time with {@link #Format_TimeDuration_mmss}
    *
    * @param time
    *
    * @return
    */
   public static String format_mm_ss_WithSign(final long time) {

      _formatterSB.setLength(0);

      if (time < 0) {
         _formatterSB.append(DASH);
      }

      final long timeAbs = time < 0 ? 0 - time : time;

      return _formatter.format(Format_TimeDuration_mmss,

            timeAbs / 60,
            timeAbs % 60

      ).toString();
   }

   public static String format_yyyymmdd_hhmmss(final int year,
                                               final int month,
                                               final int day,
                                               final int hour,
                                               final int minute,
                                               final int second) {

      _formatterSB.setLength(0);

      return _formatter.format(Messages.Format_DateTime_yyyymmdd_hhmmss,

            year,
            month,
            day,
            hour,
            minute,
            second

      ).toString();
   }

   public static String formatDoubleMinMaxElevationMeter(final double value) {

      if (value == -Double.MAX_VALUE) {
         return SYMBOL_INFINITY_MIN;
      } else if (value == Double.MAX_VALUE) {
         return SYMBOL_INFINITY_MAX;
      }

      return Long.toString((long) (value / 1000)) + SPACE + UNIT_DISTANCE_KM;
   }

   /**
    * Hours are ignored when they are 0. An empty string is returned when time = <code>0</code>.
    *
    * @param time
    *           Time in seconds.
    *
    * @return
    */
   public static String formatHhMmSs(long time) {

      if (time == 0) {
         return EMPTY_STRING;
      }

      boolean isNegative = false;

      if (time < 0) {
         isNegative = true;
         time = -time;
      }

      _formatterSB.setLength(0);

      String timeText;
      if (time >= 3600) {

         // display hours

         timeText = _formatter.format(Messages.Format_TimeDuration_hhmmss,

               time / 3600,
               time % 3600 / 60,
               time % 3600 % 60

         ).toString();

      } else {

         // ignore hours

         timeText = _formatter.format(Messages.Format_TimeDuration_hhmm,

               time % 3600 / 60,
               time % 3600 % 60

         ).toString();

      }

      return isNegative ? SYMBOL_DASH + timeText : timeText;
   }

   /**
    * @param mouseEvent
    * @param defaultAccelerator
    *           Could be 10 to increase e.g. image size by 10 without pressing an accelerator key
    *
    * @return
    */
   public static int getAcceleratorFromMouseWheel(final MouseEvent mouseEvent, final int defaultAccelerator) {

      boolean isCtrlKey;
      boolean isShiftKey;

      if (IS_OSX) {
         isCtrlKey = (mouseEvent.stateMask & SWT.MOD1) > 0;
         isShiftKey = (mouseEvent.stateMask & SWT.MOD3) > 0;
      } else {
         isCtrlKey = (mouseEvent.stateMask & SWT.MOD1) > 0;
         isShiftKey = (mouseEvent.stateMask & SWT.MOD2) > 0;
      }

      // accelerate with Ctrl + Shift key
      int accelerator = isCtrlKey ? 10 : 1;
      accelerator *= isShiftKey ? 5 : 1;

      accelerator *= defaultAccelerator;

      return accelerator;
   }

   /**
    * Get best-fit size for an image drawn in an area of maxX, maxY
    * <p>
    *
    * Original:
    * org.eclipse.nebula.widgets.gallery/src/org/eclipse/nebula/widgets/gallery/RendererHelper.java
    *
    * @param imageWidth
    * @param imageHeight
    * @param canvasWidth
    * @param canvasHeight
    *
    * @return
    */
   public static Point getBestFitCanvasSize(final int imageWidth,
                                            final int imageHeight,
                                            final int canvasWidth,
                                            final int canvasHeight) {

      final double widthRatio = (double) imageWidth / (double) canvasWidth;
      final double heightRatio = (double) imageHeight / (double) canvasHeight;

      final double bestRatio = widthRatio > heightRatio ? widthRatio : heightRatio;

      final int ratioWidth = (int) (imageWidth / bestRatio) + 1;
      final int ratioHeight = (int) (imageHeight / bestRatio) + 1;

      /*
       * This will fix a 1 pixel issues because of the ratio rounding
       */
// this do not work, also the previous algorithm :-(
//      if (widthRatio > heightRatio) {
//
//         if (ratioHeight == canvasHeight - 1) {
//            System.out.println("W  %5.3f %5.3f w: %d  h: %d".formatted(widthRatio, heightRatio, ratioWidth, ratioHeight));
//            ratioHeight = canvasHeight;
//         }
//
//      } else {
//
//         if (ratioWidth == canvasWidth - 1) {
//            System.out.println("H  %5.3f %5.3f w: %d  h: %d".formatted(widthRatio, heightRatio, ratioWidth, ratioHeight));
//            ratioWidth = canvasWidth;
//         }
//
//      }

      return new Point(ratioWidth, ratioHeight);
   }

   /**
    * @param degreeDirection
    *           The degree value, 0°...360°
    *
    * @return Returns cardinal direction text
    */
   public static String getCardinalDirectionText(final int degreeDirection) {

      return IWeather.windDirectionText[getCardinalDirectionTextIndex(degreeDirection)];
   }

   /**
    * @param degreeDirection
    *           The degree value, 0°...360°
    *
    * @return Returns cardinal direction index for {@link IWeather#windDirectionText}
    */
   public static int getCardinalDirectionTextIndex(final int degreeDirection) {

      if (degreeDirection == -1) {
         return 0;
      }

      final float degree = (degreeDirection + 11.25f) / 22.5f;

      final int directionIndex = ((int) degree) % 16;

      // We increment the index because the first element is the "empty" direction
      return directionIndex + 1;
   }

   public static FontMetrics getDialogFontMetrics() {

      // ensure that font metrics are setup
      setupUI_FontMetrics();

      return _dialogFont_Metrics;
   }

   public static Rectangle getDisplayBounds(final Control composite, final Point location) {

      Rectangle displayBounds;

      final Monitor[] allMonitors = composite.getDisplay().getMonitors();

      if (allMonitors.length > 1) {

         // By default present in the monitor of the control
         displayBounds = composite.getMonitor().getBounds();

         // Search on which monitor the event occurred
         for (final Monitor monitor : allMonitors) {

            final Rectangle monitorBounds = monitor.getBounds();

            if (monitorBounds.contains(location)) {

               displayBounds = monitorBounds;
               break;
            }
         }

      } else {

         displayBounds = composite.getDisplay().getBounds();
      }

      return displayBounds;
   }

   /**
    * @param allVisibleItems
    * @param allExpandedItems
    *
    * @return Returns {@link TreePath}'s which are expanded and open (not hidden).
    */
   public static TreePath[] getExpandedOpenedItems(final Object[] allVisibleItems, final TreePath[] allExpandedItems) {

      final ArrayList<TreePath> expandedOpened = new ArrayList<>();

      for (final TreePath expandedPath : allExpandedItems) {

         /*
          * The last expanded segment must be in the visible list otherwise it is hidden.
          */
         final Object lastExpandedItem = expandedPath.getLastSegment();

         for (final Object visibleItem : allVisibleItems) {

            if (lastExpandedItem == visibleItem) {

               expandedOpened.add(expandedPath);
               break;
            }
         }
      }

      return expandedOpened.toArray(new TreePath[expandedOpened.size()]);
   }

   /**
    * This is a copy with modifications from {@link org.eclipse.jface.dialogs.Dialog}
    *
    * @param statePrefix
    */
   public static Point getInitialLocation(final IDialogSettings state,
                                          final String statePrefix,
                                          final Shell shell,
                                          final Shell parentShell) {

      Point result = shell.getLocation();

      try {
         final int x = state.getInt(statePrefix + DIALOG_ORIGIN_X);
         final int y = state.getInt(statePrefix + DIALOG_ORIGIN_Y);
         result = new Point(x, y);

         // The coordinates were stored relative to the parent shell.
         // Convert to display coordinates.
         if (parentShell != null) {
            final Point parentLocation = parentShell.getLocation();
            result.x += parentLocation.x;
            result.y += parentLocation.y;
         }
      } catch (final NumberFormatException e) {}

      // No attempt is made to constrain the bounds. The default
      // constraining behavior in Window will be used.
      return result;
   }

   /**
    * @param url
    *
    * @return Returns the url with surrounding < a > tags which can be used for the {@link Link}
    *         control.
    */
   public static String getLinkFromText(final String url) {
      return LINK_TAG_START + url + LINK_TAG_END;
   }

   /**
    * @return Returns the {@link StatusLineManager} of the current active part or <code>null</code>
    *         when not available.
    */
   public static IStatusLineManager getStatusLineManager() {

      final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

      final IWorkbenchPart activePart = activePage.getActivePart();
      if (activePart instanceof final IViewPart viewPart) {

         return viewPart.getViewSite().getActionBars().getStatusLineManager();
      }

      final IWorkbenchPart activeEditor = activePage.getActiveEditor();
      if (activeEditor instanceof final IEditorSite editorSite) {

         return editorSite.getActionBars().getStatusLineManager();
      }

      return null;
   }

   public static GridDataFactory gridLayoutData_AlignBeginningFill() {

      return GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL);
   }

   public static GridDataFactory gridLayoutData_AlignFillCenter() {

      return GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER);
   }

   public static GridDataFactory gridLayoutData_Span_2_1() {

      return GridDataFactory.fillDefaults().span(2, 1);
   }

   /**
    * @param event
    *
    * @return Returns <code>true</code> when <Ctrl> key is pressed.
    */
   public static boolean isCtrlKey(final Event event) {

      return (event.stateMask & SWT.MOD1) > 0;
   }

   public static boolean isCtrlKey(final KeyEvent keyEvent) {

      return (keyEvent.stateMask & SWT.MOD1) > 0;
   }

   public static boolean isCtrlKey(final MouseEvent event) {

      return (event.stateMask & SWT.MOD1) > 0;
   }

   public static boolean isCtrlKey(final SelectionEvent selectionEvent) {

      return (selectionEvent.stateMask & SWT.MOD1) > 0;
   }

   /**
    * @return <code>true</code> when a dark theme is selected in the UI
    */
   public static boolean isDarkTheme() {
      return IS_DARK_THEME;
   }

   public static boolean isLinuxAsyncEvent(final Widget widget) {

      if (IS_LINUX) {

         if (widget.getData(FIX_LINUX_ASYNC_EVENT_1) != null) {
            widget.setData(FIX_LINUX_ASYNC_EVENT_1, null);
            return true;
         }

         if (widget.getData(FIX_LINUX_ASYNC_EVENT_2) != null) {
            widget.setData(FIX_LINUX_ASYNC_EVENT_2, null);
            return true;
         }
      }

      return false;
   }

   public static boolean isShiftKey(final Event event) {

      boolean isShiftKey;

      if (IS_OSX) {
         isShiftKey = (event.stateMask & SWT.MOD3) > 0;
      } else {
         isShiftKey = (event.stateMask & SWT.MOD2) > 0;
      }

      return isShiftKey;
   }

   public static boolean isShiftKey(final KeyEvent keyEvent) {

      boolean isShiftKey;

      if (IS_OSX) {
         isShiftKey = (keyEvent.stateMask & SWT.MOD3) > 0;
      } else {
         isShiftKey = (keyEvent.stateMask & SWT.MOD2) > 0;
      }

      return isShiftKey;
   }

   public static boolean isShiftKey(final MouseEvent mouseEvent) {

      boolean isShiftKey;

      if (IS_OSX) {
         isShiftKey = (mouseEvent.stateMask & SWT.MOD3) > 0;
      } else {
         isShiftKey = (mouseEvent.stateMask & SWT.MOD2) > 0;
      }

      return isShiftKey;
   }

   /**
    * Log RGB values as Java code:
    * <p>
    * <code>
    *  new RGB(0x5B, 0x5B, 0x5B),
    * </code>
    *
    * @param rgb
    *
    * @return
    */
   public static String logRGB(final RGB rgb) {

      if (rgb == null) {
         return "null"; //$NON-NLS-1$
      }

      return "new RGB(" //$NON-NLS-1$
            + "0x" + Integer.toHexString(rgb.red) + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "0x" + Integer.toHexString(rgb.green) + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "0x" + Integer.toHexString(rgb.blue) //$NON-NLS-1$
            + "),"; //$NON-NLS-1$
   }

   public static String nanoTime(final int nanoValue) {

      if (nanoValue > 0) {

         return "0." + Integer.toString(nanoValue + 1000_000_000).substring(1); //$NON-NLS-1$
      }

      return "0.0"; //$NON-NLS-1$
   }

   /**
    * Copied from {@link org.eclipse.ui.internal.handlers.ContextMenuHandler} and adjusted.
    *
    * @param control
    */
   @SuppressWarnings("restriction")
   public static void openContextMenu(final Control control) {

      if (control == null || control.isDisposed()) {
         return;
      }

      final Shell shell = control.getShell();
      final Display display = shell == null ? Display.getCurrent() : shell.getDisplay();

      final Point cursorLocation = display.getCursorLocation();

      final Event event = new Event();
      event.x = cursorLocation.x;
      event.y = cursorLocation.y;
      event.detail = SWT.MENU_MOUSE;

      control.notifyListeners(SWT.MenuDetect, event);

      if (!event.doit) {
         return;
      }

      final Menu menu = control.getMenu();

      if (menu != null && !menu.isDisposed()) {

         if (event.x != cursorLocation.x || event.y != cursorLocation.y) {
            menu.setLocation(event.x, event.y);
         }
         menu.setVisible(true);

      } else {

         final Point size = control.getSize();
         final Point location = control.toDisplay(0, 0);

         final Event mouseEvent = new Event();
         mouseEvent.widget = control;

         if (event.x < location.x
               || location.x + size.x <= event.x
               || event.y < location.y
               || location.y + size.y <= event.y) {

            final Point center = control.toDisplay(Geometry.divide(size, 2));
            mouseEvent.x = center.x;
            mouseEvent.y = center.y;
            mouseEvent.type = SWT.MouseMove;
            display.post(mouseEvent);

         } else {

            mouseEvent.x = event.x;
            mouseEvent.y = event.y;
         }

         mouseEvent.button = 2;
         mouseEvent.type = SWT.MouseDown;
         display.post(mouseEvent);

         mouseEvent.type = SWT.MouseUp;
         display.post(mouseEvent);
      }
   }

   /**
    * Opens the control context menu, the menu is aligned below the control to the right side
    *
    * @param control
    *           Controls which menu is opened
    */
   public static void openControlMenu(final Control control) {

      final Rectangle rect = control.getBounds();
      Point pt = new Point(rect.x, rect.y + rect.height);
      pt = control.getParent().toDisplay(pt);

      final Menu contextMenu = control.getMenu();

      if (contextMenu != null && contextMenu.isDisposed() == false) {
         contextMenu.setLocation(pt.x, pt.y);
         contextMenu.setVisible(true);
      }
   }

   /**
    * Open a notification popup for the number of seconds configured by the user
    *
    * @param title
    * @param imageDescriptor
    * @param text
    */
   public static void openNotificationPopup(final String title,
                                            final ImageDescriptor imageDescriptor,
                                            final String text) {

      final Display display = PlatformUI.getWorkbench().getDisplay();

      if (display.getThread() == Thread.currentThread()) {

         openNotificationPopup_InUIThread(title, imageDescriptor, text);

      } else {

         display.asyncExec(() -> {
            openNotificationPopup_InUIThread(title, imageDescriptor, text);
         });
      }
   }

   /**
    * Open a notification popup for the number of seconds configured by the user
    *
    * @param title
    * @param imageDescriptor
    * @param text
    */
   private static void openNotificationPopup_InUIThread(final String title, final ImageDescriptor imageDescriptor, final String text) {

      final int delay = _prefStore_Common.getInt(ICommonPreferences.APPEARANCE_NOTIFICATION_MESSAGES_DURATION) * 1000;

      final MTNotificationPopup notification = new MTNotificationPopup(
            Display.getCurrent(),
            imageDescriptor,
            title,
            text);
      notification.setDelayClose(delay);
      notification.open();
   }

   /**
    * @param event
    * @param image
    * @param availableWidth
    * @param alignment
    *           SWT.* alignment values
    */
   public static void paintImage(final Event event,
                                 final Image image,
                                 final int availableWidth,
                                 final int alignment) {

      final Rectangle imageRect = image.getBounds();

      final int imageWidth = imageRect.width;
      final int imageWidth2 = imageWidth / 2;

      /*
       * Horizontal alignment
       */
      int xOffset = 0;
      final int horizontalOSOffset = UI.IS_WIN

            // W$ has a horizontal indent which prevents to be exactly centered
            ? 4
            : 0;

      switch (alignment) {

      case SWT.CENTER -> xOffset = ((availableWidth - imageWidth2) / 2) - horizontalOSOffset;
      case SWT.RIGHT  -> xOffset = availableWidth - imageWidth;
      default         -> xOffset = 2; // == left alignment

      }

      /*
       * Vertical alignment: centered
       */
      final int yOffset = Math.max(0, (event.height - imageRect.height) / 2);

      final int devX = event.x + xOffset;
      final int devY = event.y + yOffset;

      final GC gc = event.gc;

//    gc.setBackground(UI.SYS_COLOR_YELLOW);
//    gc.fillRectangle(event.x, devY, availableWidth, imageRect.height);

      gc.drawImage(image, devX, devY);
   }

   public static String replaceHTML_BackSlash(final String filePath) {

      return filePath.replace(
            SYMBOL_BACKSLASH,
            SYMBOL_HTML_BACKSLASH);
   }

   public static String replaceHTML_NewLine(final String text) {

      return text.replace(NEW_LINE1, HTML_NEW_LINE);
   }

   public static String replaceJS_Apostrophe(final String js) {

      return js.replace(JS_APOSTROPHE, JS_APOSTROPHE_REPLACEMENT);
   }

   public static String replaceJS_BackSlash(final String filePath) {

      return filePath.replace(
            SYMBOL_BACKSLASH,
            JS_BACKSLASH_REPLACEMENT);
   }

   public static String replaceJS_QuotaMark(final String js) {

      return js.replace(JS_QUOTA_MARK, JS_QUOTA_MARK_REPLACEMENT);
   }

   public static void resetInitialLocation(final IDialogSettings _state, final String statePrefix) {

      _state.put(statePrefix + DIALOG_ORIGIN_X, (String) null);
      _state.put(statePrefix + DIALOG_ORIGIN_Y, (String) null);
   }

   public static void restoreCombo(final Combo combo, final String[] comboItems) {

      if (comboItems == null || comboItems.length == 0) {
         return;
      }

      for (final String pathItem : comboItems) {
         combo.add(pathItem);
      }

      // restore last used path
      combo.setText(comboItems[0]);
   }

   /**
    * Restore the sash weight from a memento
    *
    * @param sashForm
    * @param state
    * @param weightKey
    * @param sashDefaultWeight
    */
   public static void restoreSashWeight(final SashForm sashForm,
                                        final IDialogSettings state,
                                        final String weightKey,
                                        final int[] sashDefaultWeight) {

      final int[] sashWeights = sashForm.getWeights();
      final int[] newWeights = new int[sashWeights.length];

      for (int weightIndex = 0; weightIndex < sashWeights.length; weightIndex++) {

         try {

            final int mementoWeight = state.getInt(weightKey + Integer.toString(weightIndex));

            newWeights[weightIndex] = mementoWeight;

         } catch (final Exception e) {

            try {
               newWeights[weightIndex] = sashDefaultWeight[weightIndex];

            } catch (final ArrayIndexOutOfBoundsException e2) {
               newWeights[weightIndex] = 100;
            }
         }

      }

      sashForm.setWeights(newWeights);
   }

   /**
    * Restore the sash weight from a memento
    *
    * @param sash
    * @param fMemento
    * @param weightKey
    * @param sashDefaultWeight
    */
   public static void restoreSashWeight(final SashForm sash,
                                        final IMemento fMemento,
                                        final String weightKey,
                                        final int[] sashDefaultWeight) {

      final int[] sashWeights = sash.getWeights();
      final int[] newWeights = new int[sashWeights.length];

      for (int weightIndex = 0; weightIndex < sashWeights.length; weightIndex++) {

         final Integer mementoWeight = fMemento.getInteger(weightKey + Integer.toString(weightIndex));

         if (mementoWeight == null) {
            try {
               newWeights[weightIndex] = sashDefaultWeight[weightIndex];

            } catch (final ArrayIndexOutOfBoundsException e) {
               newWeights[weightIndex] = 100;
            }
         } else {
            newWeights[weightIndex] = mementoWeight;
         }
      }

      sash.setWeights(newWeights);
   }

   /**
    * This is a copy with modifications from {@link org.eclipse.jface.dialogs.Dialog}
    *
    * @param statePrefix
    */
   public static void saveDialogBounds(final IDialogSettings state,
                                       final String statePrefix,
                                       final Shell shell,
                                       final Shell parentShell) {

      if (state != null) {

         final Point shellLocation = shell.getLocation();
         final Point shellSize = shell.getSize();

         if (parentShell != null) {
            final Point parentLocation = parentShell.getLocation();
            shellLocation.x -= parentLocation.x;
            shellLocation.y -= parentLocation.y;
         }

         state.put(statePrefix + DIALOG_ORIGIN_X, shellLocation.x);
         state.put(statePrefix + DIALOG_ORIGIN_Y, shellLocation.y);

         state.put(statePrefix + DIALOG_WIDTH, shellSize.x);
         state.put(statePrefix + DIALOG_HEIGHT, shellSize.y);

         final FontData[] fontDatas = JFaceResources.getDialogFont().getFontData();
         if (fontDatas.length > 0) {
            state.put(statePrefix + DIALOG_FONT_DATA, fontDatas[0].toString());
         }
      }
   }

   /**
    * Store the weights for the sash in a memento
    *
    * @param sashForm
    * @param state
    * @param weightKey
    */
   public static void saveSashWeight(final SashForm sashForm, final IDialogSettings state, final String weightKey) {

      final int[] weights = sashForm.getWeights();

      for (int weightIndex = 0; weightIndex < weights.length; weightIndex++) {
         state.put(weightKey + Integer.toString(weightIndex), weights[weightIndex]);
      }
   }

   /**
    * Store the weights for the sash in a memento
    *
    * @param sash
    * @param memento
    * @param weightKey
    */
   public static void saveSashWeight(final SashForm sash, final IMemento memento, final String weightKey) {

      final int[] weights = sash.getWeights();

      for (int weightIndex = 0; weightIndex < weights.length; weightIndex++) {
         memento.putInteger(weightKey + Integer.toString(weightIndex), weights[weightIndex]);
      }
   }

   public static float scrambleNumbers(final float number) {

      return RANDOM_GENERATOR.nextFloat() * number;
   }

   public static int scrambleNumbers(final int number) {

      return RANDOM_GENERATOR.nextInt() * number;
   }

   public static long scrambleNumbers(final long number) {

      return (long) (RANDOM_GENERATOR.nextFloat() * number);
   }

   public static String scrambleText(final String text) {

      if (StringUtils.isNullOrEmpty(text)) {
         return text;
      }

      final int allLowerCharSize = ALL_SCRAMBLED_CHARS_LOWER.length();
      final int allUpperCharSize = ALL_SCRAMBLED_CHARS_UPPER.length();

      final char[] scrambledText = text.toCharArray();

      for (int charIndex = 0; charIndex < text.length(); charIndex++) {

         final char c = text.charAt(charIndex);

         if (c >= 0x41 && c <= 0x5a) {

            // scramble upper chars

            scrambledText[charIndex] = ALL_SCRAMBLED_CHARS_UPPER.charAt(RANDOM_GENERATOR.nextInt(allUpperCharSize));

         } else if (c != ' ') {

            // scramble other chars except spaces
            scrambledText[charIndex] = ALL_SCRAMBLED_CHARS_LOWER.charAt(RANDOM_GENERATOR.nextInt(allLowerCharSize));
         }
      }

      return new String(scrambledText);
   }

   /**
    * Set the layout data of the button to a GridData with appropriate heights and widths.
    *
    * @param button
    */
   public static void setButtonLayoutData(final Button button) {

      final GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);

      final int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);

      final Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
      final int defaultWidth = minSize.x;

      data.widthHint = Math.max(widthHint, defaultWidth);

      button.setLayoutData(data);
   }

   /**
    * Set the layout data of the button to a GridData with appropriate heights and widths.
    *
    * @param button
    */
   public static void setButtonLayoutWidth(final Button button) {

      // keep existing layout data
      final GridData data = (GridData) button.getLayoutData();

      final int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);

      final Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
      final int defaultWidth = minSize.x;

      data.widthHint = Math.max(widthHint, defaultWidth);

      button.setLayoutData(data);
   }

   /**
    * Initialize cell editing.
    *
    * @param viewer
    */
   public static void setCellEditSupport(final TableViewer viewer) {

      final TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(
            viewer,
            new FocusCellOwnerDrawHighlighter(viewer));

      final ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(viewer) {
         @Override
         protected boolean isEditorActivationEvent(final ColumnViewerEditorActivationEvent event) {

            final int eventType = event.eventType;

            return eventType == ColumnViewerEditorActivationEvent.TRAVERSAL // 5
                  || eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION // 2

                  || ((eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED) // 1
                        && (event.keyCode == SWT.CR))

                  || eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC // 4

            ;
         }
      };

      TableViewerEditor.create(
            viewer,
            focusCellManager,
            actSupport,
            ColumnViewerEditor.TABBING_HORIZONTAL
                  | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
                  | ColumnViewerEditor.TABBING_VERTICAL
                  | ColumnViewerEditor.KEYBOARD_ACTIVATION);
   }

   /**
    * Update colors for all descendants.
    *
    * @param child
    * @param bgColor
    * @param fgColor
    */
   public static void setChildColors(final Control child, final Color fgColor, final Color bgColor) {

      /*
       * ignore these controls because they do not look very good on Linux & OSX
       */
      if (child instanceof Spinner || child instanceof Combo) {
//         return;
      }

      /*
       * Toolbar action render awfully on Win7.
       */
      if (child instanceof ToolBar) {

         /*
          * FOREGROUND CANNOT BE SET
          */

//         final ToolBar tb = (ToolBar) child;
//
//         for (final ToolItem toolItem : tb.getItems()) {
//
//            final Object data = toolItem.getData();
//
//            if (data instanceof ActionContributionItem) {
//
//               final ActionContributionItem action = (ActionContributionItem) data;
//
//               final Widget widget = action.getWidget();
//
//               if (widget instanceof Button) {
//
//                  final Button button = (Button) widget;
//
//                  button.setForeground(fgColor);
//               }
//            }
//         }
      }

      child.setForeground(fgColor);
      child.setBackground(bgColor);

      if (child instanceof final Composite composite) {

         for (final Control element : composite.getChildren()) {

            if (element != null && element.isDisposed() == false) {
               setChildColors(element, fgColor, bgColor);
            }
         }
      }
   }

   /**
    * Set color for all children controls of the parent.
    *
    * @param parent
    * @param foregroundColor
    *           Foreground color
    * @param backgroundColor
    *           Background color
    */
   public static void setColorForAllChildren(final Control parent, final Color foregroundColor, final Color backgroundColor) {

      parent.setForeground(foregroundColor);
      parent.setBackground(backgroundColor);

      if (parent instanceof final Composite composite) {

         final Control[] children = composite.getChildren();

         for (final Control child : children) {

            if (child != null
                  && child.isDisposed() == false //

                  // exclude controls which look ugly
                  && !child.getClass().equals(Combo.class)
                  && !child.getClass().equals(Spinner.class)
            //
            ) {

               setColorForAllChildren(child, foregroundColor, backgroundColor);
            }
         }
      }
   }

   public static void setEnabledForAllChildren(final Control parent, final boolean isEnabled) {

      parent.setEnabled(isEnabled);

      if (parent instanceof final Composite composite) {

         final Control[] children = composite.getChildren();

         for (final Control child : children) {

            if (child != null && child.isDisposed() == false) {

               setEnabledForAllChildren(child, isEnabled);
            }
         }
      }
   }

   /**
    * set width for all controls in one column to the max width value
    */
   public static void setEqualizeColumWidths(final ArrayList<Control> columnControls) {
      setEqualizeColumWidths(columnControls, 0);
   }

   public static void setEqualizeColumWidths(final ArrayList<Control> columnControls, final int additionalSpace) {

      int maxWidth = 0;

      // get max width from all first columns controls
      for (final Control control : columnControls) {

         if (control.isDisposed()) {
            // this should not happen, but it did during testing
            return;
         }

         final int controlWidth = control.getSize().x;

         final int width = controlWidth + additionalSpace;

         maxWidth = width > maxWidth ? width : maxWidth;
      }

      // set width for all first column controls
      for (final Control control : columnControls) {

         // skip spinner controls, they already have the correct width
         if (control instanceof Spinner) {
            continue;
         }

         final Object layoutData = control.getLayoutData();
         if (layoutData != null) {
            ((GridData) layoutData).widthHint = maxWidth;
         }
      }
   }

   public static void setEqualizeColumWidthsWithReset(final ArrayList<Control> columnControls,
                                                      final int additionalSpace) {

      // reset existing widthHint
      for (final Control control : columnControls) {

         final Object layoutData = control.getLayoutData();
         if (layoutData instanceof final GridData gd) {

            gd.widthHint = SWT.DEFAULT;
         }
         control.pack(true);
      }

      int maxWidth = 0;

      // get max width from all first columns controls
      for (final Control control : columnControls) {

         if (control.isDisposed()) {
            // this should not happen, but it did during testing
            return;
         }

         final int controlWidth = control.getSize().x;

         final int width = controlWidth + additionalSpace;

         maxWidth = width > maxWidth ? width : maxWidth;
      }

      // set width for all column controls
      for (final Control control : columnControls) {

         final Object layoutData = control.getLayoutData();
         if (layoutData instanceof final GridData gd) {

            gd.widthHint = maxWidth;
         }
      }
   }

   public static GridData setFieldWidth(final Composite parent, final StringFieldEditor field, final int width) {

      final GridData gd = new GridData();
      gd.widthHint = width;

      field.getTextControl(parent).setLayoutData(gd);

      return gd;
   }

   public static void setIsDarkTheme(final boolean isDarkThemeSelected) {

      IS_DARK_THEME = isDarkThemeSelected;
      IS_BRIGHT_THEME = isDarkThemeSelected == false;
   }

   public static void setIsScrambleData(final boolean isScrambleData) {

      IS_SCRAMBLE_DATA = isScrambleData;
   }

   /**
    * Set the themed image descriptor for a {@link UIElement} with images from the
    * {@link TourbookPlugin} plugin
    *
    * @param uiElement
    * @param imageName
    */
   public static void setThemedIcon(final UIElement uiElement, final String imageName) {

      uiElement.setIcon(CommonActivator.getThemedImageDescriptor(imageName));
   }

   public static void setupThemedImages() {

// SET_FORMATTING_OFF

      // weather images
      IMAGE_REGISTRY.put(IWeather.WEATHER_ID_CLEAR,                  CommonActivator.getThemedImageDescriptor(CommonImages.Weather_Sunny));
      IMAGE_REGISTRY.put(IWeather.WEATHER_ID_PART_CLOUDS,            CommonActivator.getThemedImageDescriptor(CommonImages.Weather_Cloudy));
      IMAGE_REGISTRY.put(IWeather.WEATHER_ID_OVERCAST,               CommonActivator.getThemedImageDescriptor(CommonImages.Weather_Clouds));
      IMAGE_REGISTRY.put(IWeather.WEATHER_ID_LIGHTNING,              CommonActivator.getThemedImageDescriptor(CommonImages.Weather_Lightning));
      IMAGE_REGISTRY.put(IWeather.WEATHER_ID_RAIN,                   CommonActivator.getThemedImageDescriptor(CommonImages.Weather_Rain));
      IMAGE_REGISTRY.put(IWeather.WEATHER_ID_DRIZZLE,                CommonActivator.getThemedImageDescriptor(CommonImages.Weather_Drizzle));
      IMAGE_REGISTRY.put(IWeather.WEATHER_ID_SNOW,                   CommonActivator.getThemedImageDescriptor(CommonImages.Weather_Snow));
      IMAGE_REGISTRY.put(IWeather.WEATHER_ID_SCATTERED_SHOWERS,      CommonActivator.getThemedImageDescriptor(CommonImages.Weather_ScatteredShowers));
      IMAGE_REGISTRY.put(IWeather.WEATHER_ID_SEVERE_WEATHER_ALERT,   CommonActivator.getThemedImageDescriptor(CommonImages.Weather_Severe));

      IMAGE_REGISTRY.put(IMAGE_ACTION_PHOTO_FILTER,                  CommonActivator.getThemedImageDescriptor(CommonImages.PhotoFilter));
      IMAGE_REGISTRY.put(IMAGE_ACTION_PHOTO_FILTER_DISABLED,         CommonActivator.getThemedImageDescriptor(CommonImages.PhotoFilter_Disabled));
      IMAGE_REGISTRY.put(IMAGE_ACTION_PHOTO_FILTER_NO_PHOTOS,        CommonActivator.getThemedImageDescriptor(CommonImages.PhotoFilter_NoPhotos));
      IMAGE_REGISTRY.put(IMAGE_ACTION_PHOTO_FILTER_WITH_PHOTOS,      CommonActivator.getThemedImageDescriptor(CommonImages.PhotoFilter_WithPhotos));

// SET_FORMATTING_ON
   }

   private static void setupUI_AWTFonts() {

      if (IS_WIN) {

         final Font winFont = (Font) Toolkit.getDefaultToolkit().getDesktopProperty("win.messagebox.font"); //$NON-NLS-1$

         if (winFont != null) {
            AWT_DIALOG_FONT = winFont;
         }

      } else if (IS_OSX) {

      } else if (IS_LINUX) {

      }

      if (AWT_DIALOG_FONT == null) {

         AWT_DIALOG_FONT = AWT_FONT_ARIAL_12;
      }
   }

   private static boolean setupUI_FontMetrics() {

      if (_dialogFont_Metrics != null) {
         return true;
      }

      // Compute and keep a font metric

      final Display display = Display.getDefault();
      final Shell shell = new Shell(display);
      final GC gc = new GC(shell);
      {
         gc.setFont(JFaceResources.getDialogFont());

         _dialogFont_Metrics = gc.getFontMetrics();
      }
      gc.dispose();
      shell.dispose();

      return true;
   }

   /**
    * copy from {@link CTabItem}
    *
    * @param gc
    * @param text
    * @param width
    * @param isUseEllipses
    *
    * @return
    */
   public static String shortenText(final GC gc, final String text, final int width, final boolean isUseEllipses) {
      return isUseEllipses ? //
            shortenText(gc, text, width, ELLIPSIS) : shortenText(gc, text, width, EMPTY_STRING);
   }

   public static String shortenText(final GC gc, String text, final int width, final String ellipses) {

      if (gc.textExtent(text, 0).x <= width) {
         return text;
      }

      final int ellipseWidth = gc.textExtent(ellipses, 0).x;
      final int length = text.length();
      final TextLayout layout = new TextLayout(gc.getDevice());
      layout.setText(text);

      int end = layout.getPreviousOffset(length, SWT.MOVEMENT_CLUSTER);
      while (end > 0) {
         text = text.substring(0, end);
         final int l = gc.textExtent(text, 0).x;
         if (l + ellipseWidth <= width) {
            break;
         }
         end = layout.getPreviousOffset(end, SWT.MOVEMENT_CLUSTER);
      }
      layout.dispose();
      return end == 0 ? text.substring(0, 1) : text + ellipses;
   }

   /**
    * copied from {@link Dialog} <br>
    * <br>
    * Shortens the given text <code>textValue</code> so that its width in pixels does not exceed the
    * width of the given control. Overrides characters in the center of the original string with an
    * ellipsis ("...") if necessary. If a <code>null</code> value is given, <code>null</code> is
    * returned.
    *
    * @param textValue
    *           the original string or <code>null</code>
    * @param control
    *           the control the string will be displayed on
    *
    * @return the string to display, or <code>null</code> if null was passed in
    *
    * @since 3.0
    */
   public static String shortenText(final String textValue, final Control control) {
      if (textValue == null) {
         return null;
      }
      final GC gc = new GC(control);
      final int maxWidth = control.getBounds().width - 5;
      final int maxExtent = gc.textExtent(textValue).x;
      if (maxExtent < maxWidth) {
         gc.dispose();
         return textValue;
      }
      final int length = textValue.length();
      final int charsToClip = Math.round(0.95f * length * (1 - ((float) maxWidth / maxExtent)));
      final int pivot = length / 2;
      int start = pivot - (charsToClip / 2);
      int end = pivot + (charsToClip / 2) + 1;
      while (start >= 0 && end < length) {
         final String s1 = textValue.substring(0, start);
         final String s2 = textValue.substring(end, length);
         final String s = s1 + ELLIPSIS + s2;
         final int l = gc.textExtent(s).x;
         if (l < maxWidth) {
            gc.dispose();
            return s;
         }
         start--;
         end++;
      }
      gc.dispose();
      return textValue;
   }

   public static String shortenText(final String text,
                                    final Control control,
                                    final int width,
                                    final boolean isUseEllipses) {

      String shortText;
      final GC gc = new GC(control);
      {
         shortText = shortenText(gc, text, width, isUseEllipses);
      }
      gc.dispose();

      return shortText;
   }

   public static String shortenText(final String text, final int textWidth, final boolean isShowBegin) {

      int beginIndex;
      int endIndex;

      final int textLength = text.length();

      if (isShowBegin) {

         beginIndex = 0;
         endIndex = textLength > textWidth ? textWidth : textLength;

      } else {

         beginIndex = textLength - textWidth;
         beginIndex = beginIndex < 0 ? 0 : beginIndex;

         endIndex = textLength;
      }

      String shortedText = text.substring(beginIndex, endIndex);

      // add ellipsis when text is too long
      if (textLength > textWidth) {

         if (isShowBegin) {
            shortedText = shortedText + ELLIPSIS;
         } else {
            shortedText = ELLIPSIS + shortedText;
         }
      }

      return shortedText;
   }

   /**
    * Set control visible or hidden
    *
    * @param control
    * @param isVisible
    */
   public static void showHideControl(final Control control,
                                      final boolean isVisible) {

      showHideControl(control, isVisible, SWT.DEFAULT, SWT.DEFAULT);
   }

   /**
    * Set control visible or hidden
    *
    * @param control
    * @param isVisible
    * @param defaultWidth
    */
   public static void showHideControl(final Control control,
                                      final boolean isVisible,
                                      final int defaultWidth) {

      showHideControl(control, isVisible, defaultWidth, SWT.DEFAULT);
   }

   /**
    * Set control visible or hidden
    *
    * @param control
    * @param isVisible
    * @param defaultWidth
    * @param defaultHeight
    */
   public static void showHideControl(final Control control,
                                      final boolean isVisible,
                                      final int defaultWidth,
                                      final int defaultHeight) {

      if (isVisible) {

         if (control.getLayoutData() instanceof final GridData gridData) {

            gridData.widthHint = defaultWidth;
            gridData.heightHint = defaultHeight;

         } else {

            _gridDataHint_Default.applyTo(control);
         }

         // allow tab access
         control.setVisible(true);

      } else {

         if (control.getLayoutData() instanceof final GridData gridData) {

            gridData.widthHint = 0;
            gridData.heightHint = 0;

         } else {

            _gridDataHint_Zero.applyTo(control);
         }

         // deny tab access
         control.setVisible(false);
      }
   }

   public static void showSQLException(final SQLException ex) {

      Display.getDefault().asyncExec(() -> {

         SQLException e = ex;

         while (e != null) {

            final String sqlExceptionText = Util.getSQLExceptionText(e);

            // log also the stacktrace
            StatusUtil.logError(sqlExceptionText + Util.getStackTrace(e));

            MessageDialog.openError(
                  Display.getDefault().getActiveShell(),
                  "SQL Error", //$NON-NLS-1$
                  sqlExceptionText);

            e = e.getNextException();
         }
      });
   }

   /**
    * Show a status line message for 3 seconds
    *
    * @param statusMessage
    */
   public static void showStatusLineMessage(final String statusMessage) {

      final IStatusLineManager statusLineMgr = UI.getStatusLineManager();

      if (statusLineMgr != null) {

         statusLineMgr.setMessage(statusMessage);

         final int delay = _prefStore_Common.getInt(ICommonPreferences.APPEARANCE_NOTIFICATION_MESSAGES_DURATION) * 1000;
         // cleanup message
         Display.getDefault().timerExec(delay, () -> statusLineMgr.setMessage(null));
      }
   }

   /**
    * Show worked values in progress monitor with the format
    *
    * <pre>
    * {0} / {1} - {2} % - {3} Δ
    * </pre>
    *
    * @param monitor
    * @param numWorked
    * @param numAll
    * @param numLastWorked
    */
   public static void showWorkedInProgressMonitor(final IProgressMonitor monitor,
                                                  final int numWorked,
                                                  final int numAll,
                                                  final int numLastWorked) {

      final long numDiff = numWorked - numLastWorked;

      final String percentValue = numAll == 0

            ? UI.EMPTY_STRING
            : String.format(NUMBER_FORMAT_1F, (float) numWorked / numAll * 100.0);

      // "{0} / {1} - {2} % - {3} Δ"
      monitor.subTask(NLS.bind(SUB_TASK_PROGRESS,
            new Object[] {
                  numWorked,
                  numAll,
                  percentValue,
                  numDiff,
            }));
   }

   /**
    * Converts {@link java.awt.Point} into {@link org.eclipse.swt.graphics.Point}
    *
    * @param awtPoint
    *
    * @return
    */
   public static Point SWT_Point(final java.awt.Point awtPoint) {
      return new Point(awtPoint.x, awtPoint.y);
   }

   public static String timeStamp() {
      return (new Timestamp()).toString(Format.Log);
   }

   public static String timeStampNano() {
      return (new Timestamp()).logWithNano();
   }

   /**
    * Transform from 0...255 to {@link #TRANSFORM_OPACITY_MAX}
    *
    * @param opacity
    *
    * @return
    */
   public static int transformOpacity_WhenRestored(final int opacity) {

      int transformedValue = Math.round(opacity / 255.0f * TRANSFORM_OPACITY_MAX);

      // ensure valid opacity
      if (transformedValue > 255) {
         transformedValue = 255;
      }

      return transformedValue;
   }

   /**
    * Transform value from {@link #TRANSFORM_OPACITY_MAX} to 0...255
    *
    * @param opacity
    *
    * @return
    */
   public static int transformOpacity_WhenSaved(final int opacity) {

      int transformedValue = Math.round(255.0f / TRANSFORM_OPACITY_MAX * opacity);

      // ensure valid opacity
      if (transformedValue > 255) {
         transformedValue = 255;
      }

      return transformedValue;
   }

   public static void updateScrolledContent(final Composite composite) {

      Composite child = composite;
      Composite parent = composite.getParent();

      while (parent != null) {

         // go up until the first scrolled container

         if (parent instanceof final ScrolledComposite scrolledContainer) {

            /*
             * update layout: both methods must be called because the size can be modified and a
             * layout with resized controls MUST be done !!!!
             */
            scrolledContainer.setMinSize(child.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            scrolledContainer.layout(true, true);

            break;
         }

         child = parent;
         parent = parent.getParent();
      }
   }

   /**
    * Update units from the pref store into the application variables
    */
   public static void updateUnits() {

      final MeasurementSystem activeSystem = MeasurementSystem_Manager.getActiveMeasurementSystem();

      UNIT_HASH_CODE = activeSystem.getSystemDataHash();

// SET_FORMATTING_OFF

      /*
       * Atmospheric pressure
       */
      UNIT_IS_PRESSURE_MERCURY            = false;
      UNIT_IS_PRESSURE_MILLIBAR           = false;

      if (activeSystem.getPressure_Atmosphere() == Unit_Pressure_Atmosphere.INCH_OF_MERCURY) {

         // set imperial measure system

         UNIT_IS_PRESSURE_MERCURY         = true;
         UNIT_LABEL_PRESSURE_MBAR_OR_INHG = UNIT_PRESSURE_INHG;

      } else {

         // default is the metric measure system

         UNIT_IS_PRESSURE_MILLIBAR        = true;
         UNIT_LABEL_PRESSURE_MBAR_OR_INHG = UNIT_PRESSURE_MBAR;
      }

      /*
       * Distance
       */
      UNIT_IS_DISTANCE_KILOMETER          = false;
      UNIT_IS_DISTANCE_MILE               = false;
      UNIT_IS_DISTANCE_NAUTICAL_MILE      = false;

      final Unit_Distance distance = activeSystem.getDistance();
      if (distance == Unit_Distance.MILE) {

         // set imperial measure system

         UNIT_IS_DISTANCE_MILE            = true;

         UNIT_LABEL_DISTANCE              = UNIT_DISTANCE_MI;
         UNIT_LABEL_SPEED                 = UNIT_SPEED_MPH;

         UNIT_VALUE_DISTANCE              = UNIT_MILE;

      } else if (distance == Unit_Distance.NAUTIC_MILE) {

         UNIT_IS_DISTANCE_NAUTICAL_MILE   = true;

         UNIT_LABEL_DISTANCE              = UNIT_DISTANCE_NMI;
         UNIT_LABEL_SPEED                 = UNIT_SPEED_KNOT;

         UNIT_VALUE_DISTANCE              = UNIT_NAUTICAL_MILE;

      } else {

         // default is the metric measure system

         UNIT_IS_DISTANCE_KILOMETER       = true;

         UNIT_LABEL_DISTANCE              = UNIT_DISTANCE_KM;
         UNIT_LABEL_SPEED                 = UNIT_SPEED_KM_H;

         UNIT_VALUE_DISTANCE              = 1;
      }

      /*
       * Pace
       */
      UNIT_IS_PACE_MIN_PER_KILOMETER      = false;
      UNIT_IS_PACE_MIN_PER_MILE           = false;

      if (activeSystem.getPace() == Unit_Pace.MINUTES_PER_MILE) {

         UNIT_IS_PACE_MIN_PER_MILE        = true;
         UNIT_LABEL_PACE                  = UNIT_PACE_MIN_P_MILE;

      } else {

         // default is the metric measure system

         UNIT_IS_PACE_MIN_PER_KILOMETER   = true;
         UNIT_LABEL_PACE                  = UNIT_PACE_MIN_P_KM;
      }

      /*
       * Length
       */
      UNIT_IS_LENGTH_METER                = false;
      UNIT_IS_LENGTH_YARD                 = false;

      if (activeSystem.getLength() == Unit_Length.YARD) {

         UNIT_IS_LENGTH_YARD              = true;

         UNIT_LABEL_DISTANCE_M_OR_YD      = UNIT_DISTANCE_YARD;
         UNIT_VALUE_DISTANCE_SMALL        = UNIT_YARD;

      } else {

         // default is the metric measure system

         UNIT_IS_LENGTH_METER             = true;

         UNIT_LABEL_DISTANCE_M_OR_YD      = UNIT_METER;
         UNIT_VALUE_DISTANCE_SMALL        = 1;
      }

      /*
       * Small length
       */
      UNIT_IS_LENGTH_SMALL_MILLIMETER     = false;
      UNIT_IS_LENGTH_SMALL_INCH           = false;

      if (activeSystem.getLengthSmall() == Unit_Length_Small.INCH) {

         UNIT_IS_LENGTH_SMALL_INCH        = true;

         UNIT_LABEL_DISTANCE_MM_OR_INCH   = UNIT_DISTANCE_INCH;
         UNIT_VALUE_DISTANCE_MM_OR_INCH   = UNIT_INCH;

      } else {

         // default is the metric measure system

         UNIT_IS_LENGTH_SMALL_MILLIMETER  = true;

         UNIT_LABEL_DISTANCE_MM_OR_INCH   = UNIT_MM;
         UNIT_VALUE_DISTANCE_MM_OR_INCH   = 1;
      }

      /*
       * Elevation
       */
      UNIT_IS_ELEVATION_FOOT              = false;
      UNIT_IS_ELEVATION_METER             = false;

      if (activeSystem.getElevation() == Unit_Elevation.FOOT) {

         // set imperial measure system

         UNIT_IS_ELEVATION_FOOT           = true;

         UNIT_LABEL_ELEVATION             = UNIT_ELEVATION_FT;
         UNIT_LABEL_ALTIMETER             = UNIT_ALTIMETER_FT_H;

         UNIT_VALUE_ELEVATION             = UNIT_FOOT;

      } else {

         // default is the metric measure system

         UNIT_IS_ELEVATION_METER          = true;

         UNIT_LABEL_ELEVATION             = UNIT_ELEVATION_M;
         UNIT_LABEL_ALTIMETER             = UNIT_ALTIMETER_M_H;

         UNIT_VALUE_ELEVATION             = 1;
      }

      /*
       * Temperature
       */
      UNIT_IS_TEMPERATURE_CELSIUS         = false;
      UNIT_IS_TEMPERATURE_FAHRENHEIT      = false;

      if (activeSystem.getTemperature() == Unit_Temperature.FAHRENHEIT) {

         // set imperial measure system

         UNIT_IS_TEMPERATURE_FAHRENHEIT   = true;

         UNIT_LABEL_TEMPERATURE           = UNIT_TEMPERATURE_F;
         UNIT_VALUE_TEMPERATURE           = UNIT_FAHRENHEIT_ADD;


      } else {

         // default is the metric measure system

         UNIT_IS_TEMPERATURE_CELSIUS      = true;

         UNIT_LABEL_TEMPERATURE           = UNIT_TEMPERATURE_C;
         UNIT_VALUE_TEMPERATURE           = 1;
      }

      /*
       * Weight
       */
      UNIT_IS_WEIGHT_KILOGRAM            = false;
      UNIT_IS_WEIGHT_POUND                = false;

      if (activeSystem.getWeight() == Unit_Weight.POUND) {

         // set imperial measure system

         UNIT_IS_WEIGHT_POUND             = true;

         UNIT_LABEL_WEIGHT                = UNIT_WEIGHT_LBS;
         UNIT_VALUE_WEIGHT                = UNIT_POUND;

      } else {

         // default is the metric measure system

         UNIT_IS_WEIGHT_KILOGRAM         = true;

         UNIT_LABEL_WEIGHT                = UNIT_WEIGHT_KG;
         UNIT_VALUE_WEIGHT                = 1;
      }

// SET_FORMATTING_ON

   }

   public static VerifyListener verifyFilenameInput() {

      return verifyEvent -> {

         // check invalid chars
         for (final char invalidChar : INVALID_FILENAME_CHARS) {
            if (invalidChar == verifyEvent.character) {
               verifyEvent.doit = false;
               return;
            }
         }
      };
   }

   public static VerifyListener verifyFilePathInput() {

      return verifyEvent -> {

         // check invalid chars
         for (final char invalidChar : INVALID_FILEPATH_CHARS) {
            if (invalidChar == verifyEvent.character) {
               verifyEvent.doit = false;
               return;
            }
         }
      };
   }

   public static void verifyIntegerInput(final Event e, final boolean canBeNegative) {

      // check backspace and del key
      if (e.character == SWT.BS || e.character == SWT.DEL) {
         return;
      }

      // check '-' key
      if (canBeNegative && e.character == '-') {
         return;
      }

      try {
         Integer.parseInt(e.text);
      } catch (final NumberFormatException ex) {
         e.doit = false;
      }
   }

   public static boolean verifyIntegerValue(final String valueString) {

      if (valueString.trim().length() == 0) {
         return false;
      }

      try {
         Integer.parseInt(valueString);
         return true;
      } catch (final NumberFormatException ex) {
         return false;
      }
   }

   public static VerifyListener verifyListenerInteger(final boolean canBeNegative) {

      return verifyEvent -> {

         // check backspace and del key
         if (verifyEvent.character == SWT.BS || verifyEvent.character == SWT.DEL) {
            return;
         }

         // check '-' key
         if (canBeNegative && verifyEvent.character == '-') {
            return;
         }

         try {
            Integer.parseInt(verifyEvent.text);
         } catch (final NumberFormatException ex) {
            verifyEvent.doit = false;
         }
      };
   }

   public static VerifyListener verifyListenerTypeLong() {

      return verifyEvent -> {
         if (verifyEvent.text.equals(EMPTY_STRING)) {
            return;
         }
         try {
            Long.parseLong(verifyEvent.text);
         } catch (final NumberFormatException e1) {
            verifyEvent.doit = false;
         }
      };
   }
}

//this conversion is not working for all png images, found SWT2Dutil.java
//
//   /**
//    * Converts a Swing BufferedImage into a lightweight ImageData object for SWT
//    *
//    * @param bufferedImage
//    *            the image to be converted
//    * @param originalImagePathName
//    * @return An ImageData that represents the same image as bufferedImage
//    */
//   public static ImageData convertAWTimageIntoSWTimage(final BufferedImage bufferedImage, final String imagePathName) {
//
//      try {
//
//         if (bufferedImage.getColorModel() instanceof DirectColorModel) {
//            final DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
//            final PaletteData palette = new PaletteData(
//                  colorModel.getRedMask(),
//                  colorModel.getGreenMask(),
//                  colorModel.getBlueMask());
//            final ImageData data = new ImageData(
//                  bufferedImage.getWidth(),
//                  bufferedImage.getHeight(),
//                  colorModel.getPixelSize(),
//                  palette);
//            final WritableRaster raster = bufferedImage.getRaster();
//            final int[] pixelArray = new int[3];
//            for (int y = 0; y < data.height; y++) {
//               for (int x = 0; x < data.width; x++) {
//                  raster.getPixel(x, y, pixelArray);
//                  final int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
//                  data.setPixel(x, y, pixel);
//               }
//            }
//            return data;
//
//         } else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
//
//            final IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
//            final int size = colorModel.getMapSize();
//            final byte[] reds = new byte[size];
//            final byte[] greens = new byte[size];
//            final byte[] blues = new byte[size];
//            colorModel.getReds(reds);
//            colorModel.getGreens(greens);
//            colorModel.getBlues(blues);
//            final RGB[] rgbs = new RGB[size];
//            for (int i = 0; i < rgbs.length; i++) {
//               rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
//            }
//            final PaletteData palette = new PaletteData(rgbs);
//            final ImageData data = new ImageData(
//                  bufferedImage.getWidth(),
//                  bufferedImage.getHeight(),
//                  colorModel.getPixelSize(),
//                  palette);
//            data.transparentPixel = colorModel.getTransparentPixel();
//            final WritableRaster raster = bufferedImage.getRaster();
//            final int[] pixelArray = new int[1];
//            for (int y = 0; y < data.height; y++) {
//               for (int x = 0; x < data.width; x++) {
//                  raster.getPixel(x, y, pixelArray);
//                  data.setPixel(x, y, pixelArray[0]);
//               }
//            }
//            return data;
//
//         } else if (bufferedImage.getColorModel() instanceof ComponentColorModel) {
//
//            final ComponentColorModel colorModel = (ComponentColorModel) bufferedImage.getColorModel();
//
//            //ASSUMES: 3 BYTE BGR IMAGE TYPE
//
//            final PaletteData palette = new PaletteData(0x0000FF, 0x00FF00, 0xFF0000);
//            final ImageData data = new ImageData(
//                  bufferedImage.getWidth(),
//                  bufferedImage.getHeight(),
//                  colorModel.getPixelSize(),
//                  palette);
//
//            //This is valid because we are using a 3-byte Data model with no transparent pixels
//            data.transparentPixel = -1;
//
//            final WritableRaster raster = bufferedImage.getRaster();
//
////            final int[] pixelArray = new int[3];
//            final int[] pixelArray = colorModel.getComponentSize();
//
//            for (int y = 0; y < data.height; y++) {
//               for (int x = 0; x < data.width; x++) {
//                  raster.getPixel(x, y, pixelArray);
//                  final int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
//                  data.setPixel(x, y, pixel);
//               }
//            }
//            return data;
//         }
//
//      } catch (final Exception e) {
//
//         System.out.println(NLS.bind(//
//               UI.timeStamp() + "Cannot convert AWT image into SWT image: {0}",
//               imagePathName));
//      }
//
//      return null;
//   }
