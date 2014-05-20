

<center><img src="Vesta.jpg"></center>

## Marker Selection

### Overview --- trim markers

**<font color=blue>Trim markers</font>** (color coded blue) are used to tell Jocular which observation points can be excluded from the solution process. None, one, or both of these markers can be specified. Points to the left of the left trim marker are excluded while points to the right of the right trim are excluded when the command *Apply Trims* is executed (this is in the *Operations* 
menu).

### Overview --- D and R markers 

These markers serve a dual purpose: to delineate data points for use in estimating noise levels; or to speed calculations when long observations are being processed.

These markers can be used to speed up the solution calculation by restricting the
range of candidate *solutions*.  For observations with fewer than 1000 points,
it may take more time to place the markers than to simply let the *solver*
find the biggest thing present using an exhaustive search.

**<font color=red>D limit markers</font>** (color coded red) bracket points that Jocular is to consider as candidate solution D transition points or to delineate a set of data points to be included in a noise estimation. Either none or 
both markers must be placed.  They can be placed outside the region of
observation points if that is useful.

**<font color=green>R limit markers</font>** (color coded green) bracket points that Jocular is to consider as candidate solution R transition points or to delineate a set of data points to be included in a noise estimation. Either none or both markers must be placed.  They can be placed outside the region of
observation points if that is useful.

#### Note ---

    When placing your cursor to select a marker position, put it
    in a completely clear area.  If you put it on a data point, 
    that data point captures the mouse event (so that it can display
    the coordinate values for you) and so will not respond to a 
    mouse click intended to place a marker.  The same is true for
    grid lines and lines between points.  If a 'click' fails to
    result in a new marker, move the cursor to a nearby clear area
    and 'click' again.
    
#### Markers 'snap' ---

    The position of markers is always adjusted (snaps) to fall
    exactly between readings.  That is, marker position values
    are always of the form: xxx.5 
    
#### Standard workflow ---

It is expected that you will place the desired markers from left to right. To help with this process, as each marker is placed, the next one to the right will be automatically selected. To terminate the process, click the **None** radio button (if necessary).

The usual procedure is to click (select) the radio button that corresponds to the first marker to be placed, then left-clicking in the plot area at the desired position for the currently selected *marker* and continuing in this manner from left to right.

#### Adjusting a marker a marker position ---

Click the radio button that corresponds to the marker that is to be adjusted, then left-click at the desired new position.

#### Deleting a marker ---

Click the radio button that corresponds to the marker that is to be removed completely, then click the **Erase Marker** button, then click the **None** radio button. 

#### Temporarily hiding marker ---

To reduce visual clutter, the markers may be hidden by clicking the **Hide/Unhide** button. The markers remain effective whether or not they are hidden from sight.

#### Erase all markers ---

Click the **Erase All** button is a time saver if more than one previously placed marker is to be deleted.
