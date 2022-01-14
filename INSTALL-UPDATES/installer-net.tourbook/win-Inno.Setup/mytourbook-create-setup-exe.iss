    
#include AddBackslash(SourcePath) + "mytourbook-include-app.iss"
#include AddBackslash(SourcePath) + "mytourbook-include-languages.iss"

[Setup]
AppName              = MyTourbook (64bit)
AppVerName           = MyTourbook {#AppVersion} (64bit)
AppVersion           = {#AppVersion}
AppPublisher         = MyTourbook Contributors
AppCopyright         = MyTourbook Contributors 2005, {#AppYear}
AppID                = {{A20227AD-322E-4B3D-B67A-9C1FECF7B4A6}

OutputDir            = {#ProductRoot}{#AppVersion}
OutputBaseFilename   = mytourbook-{#AppVersion}{#AppSubVersion}-win-setup

DefaultGroupName     = MyTourbook
DefaultDirName       = {pf}\MyTourbook

VersionInfoVersion   = {#AppVersion}
VersionInfoCompany   = MyTourbook

;WizardImageBackColor   = clWhite
WizardSmallImageFile    = {#AppRoot}\mytourbook\bundles\net.tourbook\icons\application\win\tourbook55-32-installer.bmp

LanguageDetectionMethod = locale
UninstallDisplayIcon    = {app}\mytourbook.exe

; "ArchitecturesAllowed=x64" specifies that Setup cannot run on
; anything but x64.
ArchitecturesAllowed=x64
; "ArchitecturesInstallIn64BitMode=x64" requests that the install be
; done in "64-bit mode" on x64, meaning it should use the native
; 64-bit Program Files directory and the 64-bit view of the registry.
ArchitecturesInstallIn64BitMode=x64

[Files]
Source: {#ProductRoot}{#AppVersion}\mytourbook-{#AppVersion}{#AppSubVersion}-win\mytourbook\*; DestDir: {app}; Flags: recursesubdirs; Tasks: ; Languages:
