Copied from org.eclipse.equinox.p2.ui.sdk R3_8_2 and customized.

http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_uipolicy.htm

"Use the org.eclipse.equinox.p2.ui.sdk bundle as a model when determining which contributions to make."


---------------------------------------------------------------------------------------------------------------------

VERY IMPORTANT to setup/run update

<configurations>

	<plugin id="<my product>" 											autoStart="false" 	startLevel="5" />

	<plugin id="org.eclipse.core.runtime" 							autoStart="true"		startLevel="4" />
	<plugin id="org.eclipse.equinox.common" 						autoStart="true"		startLevel="2" />
	<plugin id="org.eclipse.equinox.ds" 							autoStart="true"		startLevel="2" />
	<plugin id="org.eclipse.equinox.simpleconfigurator"		autoStart="true" 		startLevel="1" />

</configurations>

---------------------------------------------------------------------------------------------------------------------
