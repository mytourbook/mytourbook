package org.eclipse.babel.core.message.resource.ser;

public interface IPropertiesSerializerConfig {

    /** New Line Type: Default. */
    public static final int NEW_LINE_DEFAULT = 0;
    /** New Line Type: UNIX. */
    public static final int NEW_LINE_UNIX = 1;
    /** New Line Type: Windows. */
    public static final int NEW_LINE_WIN = 2;
    /** New Line Type: Mac. */
    public static final int NEW_LINE_MAC = 3;

    /**
     * Default true.
     * @return Returns the unicodeEscapeEnabled.
     */
    boolean isUnicodeEscapeEnabled();

    /**
     * Default to "NEW_LINE_DEFAULT".
     * @return Returns the newLineStyle.
     */
    int getNewLineStyle();

    /**
     * Default is 1.
     * @return Returns the groupSepBlankLineCount.
     */
    int getGroupSepBlankLineCount();

    /**
     * Defaults to true.
     * @return Returns the showSupportEnabled.
     */
    boolean isShowSupportEnabled();

    /**
     * Defaults to true.
     * @return Returns the groupKeysEnabled.
     */
    boolean isGroupKeysEnabled();

    /**
     * Defaults to true.
     * @return Returns the unicodeEscapeUppercase.
     */
    boolean isUnicodeEscapeUppercase();

    /**
     * Defaults to 80.
     * @return Returns the wrapLineLength.
     */
    int getWrapLineLength();

    /**
     * @return Returns the wrapLinesEnabled.
     */
    boolean isWrapLinesEnabled();

    /**
     * @return Returns the wrapAlignEqualsEnabled.
     */
    boolean isWrapAlignEqualsEnabled();

    /**
     * Defaults to 8.
     * @return Returns the wrapIndentLength.
     */
    int getWrapIndentLength();

    /**
     * Defaults to true.
     * @return Returns the spacesAroundEqualsEnabled.
     */
    boolean isSpacesAroundEqualsEnabled();

    /**
     * @return Returns the newLineNice.
     */
    boolean isNewLineNice();

    /**
     * @return Returns the groupLevelDepth.
     */
    int getGroupLevelDepth();

    /**
     * @return Returns the groupLevelSeparator.
     */
    String getGroupLevelSeparator();

    /**
     * @return Returns the alignEqualsEnabled.
     */
    boolean isAlignEqualsEnabled();

    /**
     * Defaults to true.
     * @return Returns the groupAlignEqualsEnabled.
     */
    boolean isGroupAlignEqualsEnabled();
    
    /**
     * Defaults to true.
     * @return <code>true</code> if keys are to be sorted
     */
    boolean isKeySortingEnabled();

}