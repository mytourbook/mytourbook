    
#include AddBackslash(SourcePath) + "mytourbook-include-app.iss"
#include AddBackslash(SourcePath) + "mytourbook-include-languages.iss"

[Setup]
AppName=MyTourbook (64bit)
AppVerName=MyTourbook {#AppVersion} (64bit)
AppCopyright=MyTourbook Contributors 2005, {#AppYear}
AppID={{A20227AD-322E-4B3D-B67A-9C1FECF7B4A6}

OutputDir={#ProductRoot}{#AppVersion}
OutputBaseFilename=mytourbook-{#AppVersion}{#AppSubVersion}-win-64-setup

DefaultGroupName=MyTourbook
DefaultDirName={pf}\MyTourbook

VersionInfoVersion={#AppVersion}
VersionInfoCompany=MyTourbook

WizardImageBackColor=clWhite
WizardSmallImageFile={#AppRoot}\mytourbook\bundles\net.tourbook\icons\application\win\tourbook55-32-installer.bmp

LanguageDetectionMethod=locale
UninstallDisplayIcon={#AppRoot}\mytourbook\bundles\net.tourbook\icons\application\win\tourbook16.ico

; "ArchitecturesAllowed=x64" specifies that Setup cannot run on
; anything but x64.
ArchitecturesAllowed=x64
; "ArchitecturesInstallIn64BitMode=x64" requests that the install be
; done in "64-bit mode" on x64, meaning it should use the native
; 64-bit Program Files directory and the 64-bit view of the registry.
ArchitecturesInstallIn64BitMode=x64

[Files]
Source: {#ProductRoot}{#AppVersion}\mytourbook-{#AppVersion}{#AppSubVersion}-win32.win32.x86_64\mytourbook\*; DestDir: {app}; Flags: recursesubdirs; Tasks: ; Languages:

[InstallDelete]
Name: {app}\*; Type: files
Name: {app}\configuration; Type: filesandordirs
Name: {app}\features; Type: filesandordirs
Name: {app}\plugins; Type: filesandordirs
Name: {app}\workspace; Type: filesandordirs
Name: {app}\p2; Type: filesandordirs
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
