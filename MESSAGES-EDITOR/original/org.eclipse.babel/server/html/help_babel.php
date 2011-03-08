<?php
/*******************************************************************************
 * Copyright (c) 2008 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eclipse Foundation - Initial API and implementation
*******************************************************************************/

# Note: the style attribute for the ol tag below was causing a truncation in IE.
# I removed the attribute. The layout is still acceptable. Future modifications to this file should be tested in IE also.

include("global.php");

$pageTitle 		= "Babel Project - Eclipse translation";
$pageKeywords 	= "translation,language,nlpack,pack,eclipse,babel,english,french,german,chinese,japanese,spanish,arabic,hebrew,hungarian,polish,italian,russian,dutch,finnish,greek,norwegian,sweedish,turkish";

global $addon;
$addon->callHook("head");

?>

<h1 id="page-message">Welcome to the Babel Project</h1>
<div id="index-page" style='width: 510px; padding-right: 190px;'>

	<img src="<?php echo imageRoot() ?>/large_icons/categories/preferences-desktop-peripherals.png"><h2>Become a Committer on the Babel Project</h2>
	<br style='clear: both;'>
	<p>The main goal of the Babel project is to involve you the community in making Eclipse projects available in any language.  
	   To make this effort a success we need quality translators to help with the translation.
	   And we need help from developers to bring new features to Babel.
	</p>

	<h3 style='margin-left: 40px;'>Everybody can:</h3>
	<ol>
		<li><a href="translate.php">Help translate Eclipse projects into your language</li>
		<li><a href="https://bugs.eclipse.org/bugs/enter_bug.cgi?assigned_to=babel.core-inbox%40eclipse.org&blocked=&bug_file_loc=http%3A%2F%2F&bug_severity=normal&bug_status=NEW&comment=&component=Website&contenttypeentry=&contenttypemethod=autodetect&contenttypeselection=text%2Fplain&data=&dependson=&description=&flag_type-1=X&flag_type-2=X&flag_type-4=X&flag_type-6=X&form_name=enter_bug&keywords=&maketemplate=Remember%20values%20as%20bookmarkable%20template&op_sys=All&priority=P3&product=Babel&qa_contact=&rep_platform=All&short_desc=&version=unspecified">Open a bug you found in Babel</a></li>
		<li><a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=&classification=Technology&product=Babel&long_desc_type=allwordssubstr&long_desc=&bug_file_loc_type=allwordssubstr&bug_file_loc=&status_whiteboard_type=allwordssubstr&status_whiteboard=&keywords_type=anywords&keywords=helpwanted+bugday&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&emailtype1=substring&email1=&emailtype2=substring&email2=&bugidtype=include&bug_id=&votes=&chfieldfrom=&chfieldto=Now&chfieldvalue=&cmdtype=doit&order=Reuse+same+sort+as+last+time&known_name=babel&query_based_on=babel&field0-0-0=product&type0-0-0=substring&value0-0-0=babel&field0-0-1=component&type0-0-1=substring&value0-0-1=babel&field0-0-2=short_desc&type0-0-2=substring&value0-0-2=babel&field0-0-3=status_whiteboard&type0-0-3=substring&value0-0-3=babel&field0-0-4=longdesc&type0-0-4=substring&value0-0-4=babel">Squash a bug already open</a></li>
		<li><a href="https://dev.eclipse.org/mailman/listinfo/babel-translators">Join the mailing list</a></li>
	</ol>	  

	<h3 style='margin-left: 40px;'>Projects Members can:</h3>
	<ol>
		<li><a href="importing.php">Get your Eclipse project imported to Babel</a></li>
		<li>Help make specific language packs for Eclipse projects (pending)</li>
		<li>Review translations for quality/consistency (pending)</li>
		<li>Promote your projects translation progress (pending)</li>
	</ol>
	  
</div>

<?php
	global $addon;
    $addon->callHook("footer");
?>