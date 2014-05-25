

<center><img src="Vesta.jpg"></center>

## Marker Selection

### Overview --- trim markers

**<font color=blue>Trim markers</font>** (color coded blue) are used to tell Jocular which observation points can be excluded from the solution (and noise estimation) process. None, one, or both of these markers can be specified. Points to the left of the left trim marker are excluded while points to the right of the right trim are excluded when the  **Apply** button is 
clicked.

When the **Apply** button is clicked, the trim markers are automatically cleared so that a subsequent click on the button will easily 'undo' the trim action.

### Overview --- D and R markers 

These markers serve a combined purpose: to delineate data points for use in estimating noise levels and to speed calculations when observations with many data points are being processed.

**<font color=red>D limit markers</font>** (color coded red) bracket points that Jocular is to consider as candidate solution D transition points. They also serve to delineate the baseline points that
are to the left of the **D** transition zone and the event points that are between the **D** and **R** transition zones. Either none or 
both markers must be placed.  They can be placed outside the region of
observation points if that is useful.

**<font color=green>R limit markers</font>** (color coded green) bracket points that Jocular is to consider as candidate solution R transition points. They also serve to delineate the baseline points that are to the right of the **R** transition zone and the event points that are between the **D** and **R** transition zones.  Either none or both markers must be placed.  They can be placed outside the region of
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
   
#### Marker pairs auto-advance ---

Because trim, D and R markers are nearly always used in pairs, there is an *auto-advance* feature that moves the marker selection from the *left* marker, then to the *right* marker, then to *none* on subsequent left clicks in the plot area.
 
#### Adjusting a marker a marker position ---

Click the radio button that corresponds to the marker that is to be adjusted, then left-click at the desired new position.

#### Deleting a marker ---

Click the radio button that corresponds to the marker that is to be removed completely, then click the **Erase Marker** button.

#### Temporarily hiding marker ---

To reduce visual clutter, the markers may be hidden by clicking the **Hide/Unhide** button. The markers remain effective whether or not they are hidden from sight.

#### Erase all markers ---

Click the **Erase All** button is a time saver if more than one previously placed marker is to be deleted.
