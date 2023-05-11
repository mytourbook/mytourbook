
#define AppYear         "2023"
#define AppVersion      "23.5.0"

#define AppSubVersion   ""
;#define AppSubVersion   "_BETA_1"


;#define AppRoot        "c:\dat\MT\"
#define AppRoot         "..\..\..\.."

;a relative path causes an error because the file pathname is > 255
;#define ProductRoot    "..\..\..\..\..\mytourbook-PRODUCT\"
#define ProductRoot     "c:\dat\mytourbook-PRODUCT\"


[InstallDelete]
Name: {app}\*;              Type: files
Name: {app}\configuration;  Type: filesandordirs
Name: {app}\features;       Type: filesandordirs
Name: {app}\plugins;        Type: filesandordirs
Name: {app}\workspace;      Type: filesandordirs
Name: {app}\p2;             Type: filesandordirs
Name: {app}\jre;            Type: filesandordirs
Name: {app};                Type: dirifempty

[Icons]
Name: {group}\MyTourbook;             Filename: {app}\mytourbook.exe; IconFilename: {app}\mytourbook.exe
Name: {group}\Uninstall MyTourbook;   Filename: {app}\{uninstallexe}; IconFilename: {uninstallexe}
Name: {commondesktop}\MyTourbook;     Filename: {app}\mytourbook.exe; IconFilename: {app}\mytourbook.exe;   WorkingDir: {app}; Tasks: desktopicon\common
Name: {userdesktop}\MyTourbook;       Filename: {app}\mytourbook.exe; IconFilename: {app}\mytourbook.exe;   WorkingDir: {app}; Tasks: desktopicon\user
Name: {userappdata}\Microsoft\Internet Explorer\Quick Launch\MyTourbook;  Filename: {app}\MyTourbook.exe;   WorkingDir: {app}; Tasks: " quicklaunchicon"

[Tasks]
Name: desktopicon;          Description: {cm:CreateDesktopIcon};  GroupDescription: {cm:GroupCreateIcons}
Name: desktopicon\common;   Description: {cm:AllUsers};           GroupDescription: {cm:GroupCreateIcons}; Flags: exclusive
Name: desktopicon\user;     Description: {cm:CurrentUser};        GroupDescription: {cm:GroupCreateIcons}; Flags: exclusive unchecked
Name: quicklaunchicon;      Description: {cm:CreateQuickLaunchIcon}

[Run]
Filename: {app}\mytourbook.exe; WorkingDir: {app}; Description: {cm:StartMyTourbook}; Flags: postinstall nowait
