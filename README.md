# MyTourbook

All documentations for this project are available on the website <http://mytourbook.sourceforge.net>

**Some contributor links**

[Translation](http://mytourbook.sourceforge.net/mytourbook/index.php/development/translation) 

[Development](http://mytourbook.sourceforge.net/mytourbook/index.php/development)

[Release Notes](https://github.com/wolfgang-ch/mytourbook/tree/master/info/release-notes "Release Notes") - These changelog files are created from the release notes texts on the [webpage](http://mytourbook.sourceforge.net)  

**Build the product** is automated with maven/tycho, the build process for a new version needs still some manual adjustments which are documented [in this file](https://github.com/wolfgang-ch/mytourbook/blob/master/info/_HELP-create-build.txt "build") 


_This document will not repeat already written text in the documentation._



## Commandline arguments

### Logging

**Log map 2.5 http traffic** 

-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG


**Log calendar profiles when modified**

-DlogCalendarProfile

**Log FIT data**

-DlogFitData

### Other

**Scramble content/values**

It is scrambeling text and values. This is used to create anynymous screenshots

-DscrambleData

**Each tour has a unique id so that a tour can be imported multiple times**

-DcreateRandomTourId
