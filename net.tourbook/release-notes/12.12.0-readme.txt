Version: 12.12.0                                    12. December 2012 



New Features 

* photo fullscreen viewer can show all available photos in a photo 
gallery, just move mouse to the top of the screen 

* zoom position marker in the tour chart shows, how far a zoomed in 
chart is panned to the right or left side 

* spanish translation, provided by Pedro Merino Laso 



New Experimental Features 

* a photo without GPS can be displayed in the map, how it works is 
described here: link photos with tours 

* show tour photos in 
- Tour Chart and History Tour 
- Tour Map 
- Tour Photos view
- Photo Tooltip

* overview: all photo features 



!!! Version 12.12 can crash on Linux !!!

During my testing, the application crashed using Ubuntu 12.04. This log 
entry is displayed in the Eclipse console: 

mytourbook: /build/buildd/cairo-1.10.2/src/cairo-surface.c:637: 
cairo_surface_destroy: Assertion `((*&(&surface->ref_count)->ref_count) 
> 0)' failed.
Aborted (core dumped) 

Currently I can not say exactly when the crash occured, I've not much 
Linux experience and havn't found a dump file. 

If the app is crashing on Linux when using new photo features, report it 
in this bug tracker. 



Improvements

* set tour marker visibility (can be used when doing a screenshot to 
hide awkward markers) 

* support air planes when break time is calculated (increased minimum 
speed: 10 -> 100 km/h) feature request from Stefan Rado 


 
Behaviour changed 

* horizontal autoscrolling in the zoomed in tour chart can only be done 
now, when the mouse vertical position is below the horizontal x-axis at 
the bottom graph (when multiple graphs are displayed), previously 
autoscrolling worked within the whole y-axis area.



Fixed Bugs 	

 * just detected (24.9.2012), that power & speed series from a device 
are not transferred correctly during the upgrade from version 11.8 to 
12.1, the problem is that values are too small. 

This bug is now fixed. When version 12.1 or later is not yet used, no 
correction is necessary. However when version 12.1 or later is already 
used, tour data must be reimported to fix this problem with your data. A 
reimport can be done in the Tour Import view with this action in the 
context menu: Tour Reimport/All Time Slice Values. 

This bug mainly occures for tours which are recorded from an ergometer 
because it is recording power & speed. 



New Splash 	

 
 
Development 	

* restructured plugins:
  - removed net.tourbook.util
  - added net.tourbook.common, net.tourbook.photo

 

System 	

* Eclipse 3.8.1
