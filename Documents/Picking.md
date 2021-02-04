# Creation of new waypoints and routes

This task could be used to create new waypoints or
new routes.  
It could be used on a clean map when starting from the task menu
of the application or it could be used on a map already displaying
some GPX file from the display task.  
After creation of some number of waypoints or one route, the new
items could be stored in a new GPX file or appended to an existing
file.

The creation is performed with a picking session through the Msb2Map
application. Please consult the documentation for this application.  
One has to drag a reticle marker to the appropriate location where
it appears as a butterfly marker. A name and optionally an altitude
are requested for each waypoint.  
The successive points of a route are
ordered but no name is needed. But at the return from Msb2Map a name is
needed for the route itself.

![PickRoute](Gallery/PickRoute.jpg)

A file to store the new items has to be selected at the return
from the picking session.  
A new file could be created in the current directory.  
Or an existing file could be selected: the content of
this file could then be completely overwrite or the new data could
be added near the end of the file.  
The currently displayed file could also be selected.  
An alternative file could be selected if there is an error: the file
or the directory could be write protected.

The removable storage is often not writable: see the "SAF" documentation
file.

The newly written file become the current file.

