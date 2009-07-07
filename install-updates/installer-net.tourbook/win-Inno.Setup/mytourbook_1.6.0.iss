[Setup]
AppName=MyTourbook
AppVerName=MyTourbook 1.6.0
AppCopyright=MyTourbook Contributors 2005, 2008
AppID={{37852811-BC7D-411C-8122-E69CCA892582}

LicenseFile=M:\mytourbook TRUNK\net.tourbook\gpl.txt

OutputDir=m:\MyTourbook Product\1.6.0
OutputBaseFilename=mytourbook_1.6.0.win32.win32.x86.setup

DefaultGroupName=MyTourbook
DefaultDirName={pf}\MyTourbook

VersionInfoVersion=1.6.0
VersionInfoCompany=MyTourbook

WizardImageBackColor=clWhite
WizardSmallImageFile=m:\mytourbook TRUNK\net.tourbook\icons\application\tourbook48-32-white.bmp

LanguageDetectionMethod=locale

[Files]
Source: m:\MyTourbook Product\1.6.0\mytourbook_1.6.0.win32.win32.x86\mytourbook\*; DestDir: {app}; Flags: recursesubdirs; Tasks: ; Languages:

[InstallDelete]
Name: {app}\*; Type: files
Name: {app}\configuration; Type: filesandordirs
Name: {app}\features; Type: filesandordirs
Name: {app}\plugins; Type: filesandordirs
Name: {app}\workspace; Type: filesandordirs
Name: {app}; Type: dirifempty

[Icons]
Name: {group}\MyTourbook; Filename: {app}\mytourbook.exe; IconFilename: {app}\mytourbook.exe
Name: {group}\Uninstall MyTourbook; Filename: {app}\{uninstallexe}; IconFilename: {uninstallexe}
Name: {commondesktop}\MyTourbook; Filename: {app}\mytourbook.exe; IconFilename: {app}\mytourbook.exe; WorkingDir: {app}; Tasks: desktopicon\common
Name: {userdesktop}\MyTourbook; Filename: {app}\mytourbook.exe; IconFilename: {app}\mytourbook.exe; WorkingDir: {app}; Tasks: desktopicon\user
Name: {userappdata}\Microsoft\Internet Explorer\Quick Launch\MyTourbook; Filename: {app}\MyTourbook.exe; WorkingDir: {app}; Tasks: " quicklaunchicon"

[Tasks]
Name: desktopicon; Description: {cm:CreateDesktopIcon}; GroupDescription: {cm:GroupCreateIcons}
Name: desktopicon\common; Description: {cm:AllUsers}; GroupDescription: {cm:GroupCreateIcons}; Flags: exclusive
Name: desktopicon\user; Description: {cm:CurrentUser}; GroupDescription: {cm:GroupCreateIcons}; Flags: exclusive unchecked
Name: quicklaunchicon; Description: {cm:CreateQuickLaunchIcon}


[Run]
Filename: {app}\mytourbook.exe; WorkingDir: {app}; Description: {cm:StartMyTourbook}; Flags: postinstall nowait

[Languages]
Name: de; MessagesFile: compiler:Languages\German.isl; LicenseFile: M:\mytourbook TRUNK\net.tourbook\gpl-de.rtf
Name: en; MessagesFile: compiler:Default.isl

[CustomMessages]
en.GroupCreateIcons=Additional icons:
en.CreateDesktopIcon=Create a &desktop icon
en.AllUsers=For &all users
en.CurrentUser=For the &current user only

de.GroupCreateIcons=Icons erstellen:
de.CreateDesktopIcon=Symbol auf dem &Desktop erstellen
de.AllUsers=Für &alle Benutzer
de.CurrentUser=Nur für den aktuellen &Benutzer

en.CreateQuickLaunchIcon=Create a &quick launch icon
de.CreateQuickLaunchIcon=&Schnellstart Symbol erstellen

en.StartMyTourbook=Run &MyTourbook
de.StartMyTourbook=&MyTourbook starten
