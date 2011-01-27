<div id="maincontent">
<div id="midcolumn">
<h1><?= $pageTitle ?></h1>

<div id="index-page">

	<a href="https://bugs.eclipse.org/bugs/createaccount.cgi"><img src="<?php echo imageRoot() ?>/large_icons/categories/preferences-desktop-peripherals.png">	<h2>A Bugzilla Account is all you need</h2></a>
    <br style='clear: both;'>
	<p>If you don't already have an Eclipse Bugzilla account then <a href="https://bugs.eclipse.org/bugs/createaccount.cgi">create one today</a>.  
	It takes Babel a few minutes to receive your new Bugzilla account information. 
	If logging in doesn't work after a few minutes, please contact <a href="mailto:webmaster@eclipse.org">webmaster@eclipse.org</a>.</p>

    <br style='clear: both;'>
	<p>If you already have an Eclipse Bugzilla account, then log in and started helping Eclipse speak your language.</p>

<form style="margin-left: 35px;" name="frmLogin" method="post">
<div>

	<?php 
		if($GLOBALS['g_ERRSTRS'][0]){ 
			?>
			  <img style='margin-left: 70px;' src='<?php echo imageRoot() ?>/small_icons/actions/process-stop.png'>
		      <div style='color: red; font-weight: bold; '><?=$GLOBALS['g_ERRSTRS'][0]?></div>
		      <br style='clear: both;'>
		    <?
	    }else{
			?>
	    		<img style='margin-left: 70px;' src="<?php echo imageRoot() ?>/small_icons/emblems/emblem-important.png">	<h2 style='font-size: 14px; margin-top: 0px; background-color: yellow;'>Use your Bugzilla login information</h2>
		    	<br style='clear: both;'>
		   <?
	    }
	 ?>
	
	<div style='width: 70px; float: left;'>Email:</div>
	<input type="text" name="username" value="<?= $USERNAME ?>" size="42" maxlength="255" /> 
	
	<?php if($GLOBALS['g_ERRSTRS'][1]){ print "<div>".$GLOBALS['g_ERRSTRS'][1]."</div>"; } ?>
	
</div>

<div>
	<div style='width: 70px; float: left;'>Password:</div>
	<input type="password" name="password" value="<?= $PASSWORD ?>" size="42" maxlength="255" /> 
	<?= $GLOBALS['g_ERRSTRS'][2] ?>
</div>

<div style='margin-left: 65px;'>
	<input type="checkbox" name="remember" value="1" <?= $REMEMBER ?> />remember me
	<div style='float: right; margin-right: 100px;'><a href="https://bugs.eclipse.org/bugs/index.cgi?GoAheadAndLogIn=1#forgot">Forgot my password</a> </div>
	
</div>

<div style='margin-left: 65px;'>
<input type="submit" name="submit" value="Login" style="font-size:14px;" />
</div>

</form>
</div>

<script language="javascript">
	document.forms['frmLogin'].username.focus();
</script>