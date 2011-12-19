MyTourbook 12.1 BETA 1 release notes (19 December 2011) 



!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 
                              
			      VERY IMPORTANT INFO 

Major structural changes has been done for the tour database in this version. It 
is STRONGLY recommended to do a backup of the data before the new version is 
started. An additional warning is displayed when the application is started.

When the database update is performed and the splash screen has been covered 
with another window, it can happen (on Win7) that the splash screen is not 
updated until the database update has finished which is showing an info dialog 
box. 

The database update is performing about 10 tours per second, my test values: 
1727 tours / 149 seconds = 11.6 tours/second 

!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 


I'm aware that this version is not yet fully and thoroughly tested because the 
database restructuring required that almost every part of the application had to 
be adjusted. This is the reason why this is still a BETA version. 

The website is not yet updated, therefor the release notes has more details. 



Major Improvements (see details below): 

- new tour chart features

- the behaviour in the tour chart is different than before but is more 
convenient when moving the mouse in a zoomed chart or when dragging vertical 
slider (you will propably not miss the old behaviour when you have recognized 
the new) 

- new value point tooltip in the tour chart 

- all data series are saved now in floating point format 

- altitude values are displayed exactly as saved in the import file and not 
truncated at the decimal point. The altitude values for already saved tours can 
be reimported but altitude adjustments can not be preserved when they have been 
done 



Other Improvements: 

- in the adjust altitude dialog when using the method "approach altitude with 
SRTM" a new checkbox is available to select the whole tour 

- reimporting can also be done in the tourbook view (available in the context 
menu) 



Tour Chart Improvements: 

- transparency and antialiasing for the line graphs can be set in the 
preferences 

- major and minor grid lines can be set in the preferences 

- improved performance for drawing the tour chart 



New Tour Chart Behaviour: 

- when zooming the chart in or out with the mouse wheel, the horizontal position 
in the chart will be preserved where the mouse is located (in the previous 
versions the last mouse double click position was preserved, this is still the 
behaviour in the map) 

- when the chart is zoomed in and the mouse is moved within the left or right 
x-axis area, the graph is scrolled to left or right with 5 different speeds 
depending on the mouse horizontal position (mouse cursor shows the speed), this 
behaviour applies also when a vertical slider is dragged. A side effect of this 
new behaviour is, that you can do an animation of the tour in the chart and the 
map when a vertical slider is dragged, the Home or End key can restart the 
animation 

- when the left mouse button is clicked and the mouse is not hovering other 
chart elements, the mouse has 3 different vertical positions in the chart which 
are displayed with different cursors: 

  - upper part: the left most slider will be positions to the mouse 
  
  - lower part: the right most slider will be positions to the mouse 

  - middle part: when the mouse is clicked and button is kept down, the chart can 
    be dragged (this is the behaviour in previous versions)
    
- mouse wheel zooming can be done in the left or right x-axis area or when the 
mouse is hovering the value point tool tip 

- when the mouse is clicked at a vertical slider, this slider is pinned to the 
horizontal position of the mouse until a second mouse click is done 

- the horizontal scrolling with the scrollbar for a zoomed chart has been 
removed, this was available in the statistic charts 



Hint for using Tour Chart and Map: 

- when a dragged vertical slider in the tour chart is not moving the slider in 
the map, ensure that the tour chart has the focus 



Value Point Tooltip 

- it's default position is in the top right corner of the tour chart 

- all features for this tooltip can be set with the button in the top/right 
corner of the tooltip 

- when the tooltip has been set to be hidden, the tour chart options menu has 
the menu item "Show Value Point Tooltip" 

- the tooltip can be dragged with the mouse, the selected pin position keeps the 
tooltip when the chart is move or resized 



System:

- Eclipse 3.7.1 

- Derby 10.8.2.1 

- decreased number of plugins, moved most of the device plugins into net.tourbook.device 



Fixed bugs: 

- Tour editor not dirty when changing tour type 
https://sourceforge.net/tracker/?func=detail&aid=3459317&group_id=179799&atid=890601 

- a tour with 53952 time slices cannot be saved 
https://sourceforge.net/tracker/?func=detail&aid=3421203&group_id=179799&atid=890601 

