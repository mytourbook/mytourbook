
#include AddBackslash(SourcePath) + "mytourbook-include-app.iss"
#include AddBackslash(SourcePath) + "mytourbook-include-languages.iss"

[Setup]
AppName=MyTourbook (32bit)
AppVerName=MyTourbook {#AppVersion} (32bit)
AppCopyright=MyTourbook Contributors 2005, {#AppYear}
AppID={{37852811-BC7D-411C-8122-E69CCA892582}

OutputDir={#ProductRoot}{#AppVersion}
OutputBaseFilename=mytourbook_{#AppVersion}{#AppSubVersion}.win32.win32.x86.setup

DefaultGroupName=MyTourbook
DefaultDirName={pf}\MyTourbook

VersionInfoVersion={#AppVersion}
VersionInfoCompany=MyTourbook

WizardImageBackColor=clWhite
WizardSmallImageFile={#AppRoot}\mytourbook\net.tourbook\icons\application\tourbook55-32-win-installer.bmp

LanguageDetectionMethod=locale
UninstallDisplayIcon={#AppRoot}\mytourbook\net.tourbook\icons\application\tourbook16.ico









[Files]
Source: {#ProductRoot}{#AppVersion}\mytourbook_{#AppVersion}{#AppSubVersion}.win32.win32.x86\mytourbook\*; DestDir: {app}; Flags: recursesubdirs; Tasks: ; Languages:

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
