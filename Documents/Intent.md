# Invoked by another application

The Vtrk application could be invoked for some request by
another application like a file explorer or a mail application.  
It is registered to process requests to open a GPX file and
requests to display a specific location on the map.

## File

The data scheme in the intent is of the type "file".  
The argument should be the full path to a GPX file. This file
is set in the Display task as it would have been selected.

## Geo

The data scheme in the intent is of the type "geo".  
Several forms are accepted:

+ geo:50.894667,4.342549
+ geo:0,0?q=50.894667,4.342549
+ geo:0,0?q=50.894667,4.342549(Atomium)

A street address is not accepted as there is no access to Internet.  
No altitude is available.

This type of notation could be found on Web pages, sent by mail or
embedded in a QR code.

The Display task is started with no file but with a single location
in the "Focus on" field. A marker for this location is added when
the map is displayed.

