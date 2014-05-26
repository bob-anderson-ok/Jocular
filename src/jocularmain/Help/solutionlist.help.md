

<center><img src="Vesta.jpg"></center>

## Solution List

### Transition point:  defined ---

When an occultation <font color="red">D</font> or <font color="green">R</font> transition occurs after reading `i-1` but before or at reading `i`, then reading `i` is a called a **transition point**.  All other points of the observation were recorded when either the asteroid plus star light level was present for the entire frame time, or just the asteroid light level was present for the entire frame time; such values are independent of when a transition occurred.

### Transition points at the extremes

A transition point at 0 means that <font color="red">D</font> occurred just before or at the beginning of the observation recording and so the first reading is affected by the time of <font color="red">D</font>.

A transition point at -1 (or smaller) means that <font color="red">D</font> occurred at least one frame time before the recording began.  In this case, the reading at point 0 is unaffected by <font color="red">D</font>.

If there are `n` readings in the observation, a transition point at `n-1` (the last reading in the recording --- we're zero based for array indexing) means that <font color="green">R</font> occurred just before or at the last reading, so it is only the last reading that is affected by exactly when <font color="green">R</font> occurred. A transition point at `n` (or greater) means that <font color="green">R</font> occurred after the recording was stopped.

## 

Jocular 'solves' for the best pair of transition points by doing an exhaustive search over the complete range of D and R transition points that it is given. It tests every pair, but skips the full calculation for invalid pairs.  Some reasons why a pair would be invalid:

* R is before D
* There is no baseline point
* There is no event point
* B is less than A
* When a **Min Event Dur** is specified and the event duration is less
* When a **Max Event Dur** is specified and the event duration is greater
* When a mag drop is less than the value present in the **Min Mag Drop** field
* When a mag drop is greater than the value present in the **Max Mag Drop** field. 

The first line in the solution list gives the statistics of the 'search'.  If you provide no limitations for the search, then the number of candidate pairs will be `(n+2)^2`. Jocular is fast enough that it can handle observations with around 500 points without the need to take any steps to restrict the range of candidate pairs. 

The principal means to restrict the search is to place <font color="red">D</font> and/or <font color="green">R</font> markers on the observation plot around the zones (assuming that the transitions are obvious enough) to be searched for transition candidates. 

The other means to restrict search (which does not require the transitions to be unambiguously visible) is to set minimum and/or maximum values for event duration and place reasonable limits for expected magDrop.

## 

The second line of the solution list shows what the **AICc** (Akaike Information Criteria with finite sample size correction) value would be for a straight line fit to the data: that is, it is the **AICc** value expected if there were no event present. (Note: with **AICc** values, smaller is better). When searching for events buried in noise, this number can be used to decide whether an event is present. Experience has shown that an unconstrained search for an event in noise should require a large (>500) relative probability of an event (SqWave) versus a straight line (no event) before considering that a statistically significant event is present in the data.

If the event size is reasonably constrained, or suitable magDrop limits have been entered, a smaller number may be used with caution. 

## 

Subsequent entries in the solution list have a fixed format and are sorted in ascending value of **AICc**, so the 'best' solution is the first one in the list. 

**If any of these entries are clicked on, that 'solution' will be plotted on the observation data display.  The first (best solution) is automatically displayed at the end of the Find Solution procedure -- no need to click on that one.**

The initial part gives the transition pair that produced the results

    [  51,  101]
    [  -1,  201]  // this pair has no D --- R only
    
**relLike** gives the comparison (relative likelihood) of this 'solution' versus the 'best' solution
    
**AICc** stands for Akaike Information Criteria (with correction or finite sample size).

**logL** stands for log likelihood

**D** gives the D transition time (in readings) and will show **NaN** if there is no D transition.

**R** gives the R transition time (in readings) and will show **NaN** if there is no R transition.

**B** is the baseline intensity value

**A** is the event intensity value.

**magDrop** is the event magDrop

**k** is the number of adjustable parameters in the solution. The possible adjustable parameters are:

* B
* A
* D (if present)
* R (if present)
* D subframe timing level (when allowed by AICc computation at that point)
* R subframe timing level (when allowed by AICc computation at that point)

So `k` will range from 3 to 6 in practice.

### Length of Solution List

The **Solution List** only shows those 'solutions' that have a relative likelihood of at least 0.01.  





 
    
     

