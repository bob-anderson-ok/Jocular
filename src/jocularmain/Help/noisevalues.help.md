

<center><img src="Vesta.jpg"></center>

## Baseline and Event Noise

The calculation of a 'fit' to an observation requires an accurate estimate of the observation noise occurring prior to the occultation (`baseline noise`, often referred to as `sigmaB`) and the observation noise occurring during the occultation (`event noise`, often referred to as `sigmaA`).

## 
### Suggested procedures for common 'use cases' ...

* __Case: D and or R zones 'obvious'.__ Here you need only place 'markers' around the D and/or R transition zones and press the button labeled `Estimate Noise Values`. The program treats all points to the left of the leftmost D 'marker' and those to the right of the rightmost R 'marker' as belonging to the baseline and uses these points to estimate the baseline noise value (which is calculated as the standard deviation of those points). Similarly, the points between the rightmost D 'marker' and the leftmost R 'marker' are treated as event points and used to estimate the event noise.
### 
* __Case: The signal is buried in noise.__ In this case, the transition zones may be too nebulous to permit reasonable placement of 'markers' and there is also no obvious region of differing noise values, thus `event noise` may be reasonably treated as equal to `baseline noise`. The suggested procedure is:
    1. Do not place any 'markers' in the observation data.
    2. Click `Estimate Noise Values`. In the absence of 'markers', the program calculates the standard deviation of the _first difference_ of the entire observation (to minimize the effect of a buried event). The resulting value is then divided by the square root of 2 (to compensate for the additional noise introduced by the differencing procedure) and used as the`baseline noise` and `event noise` values.
    3. (Optional) Set appropriate search 'limiters' (Min/Max Dur MinMax MagDrop). 
    4. Finally, click the `Find Solution(s)` button to do a exhaustive search.

### 
* __Case: Event is short but clearly has lower noise.__ The problem in this case is that there may be too few clearly evident event points and a manual estimation of event noise based on baseline noise will have to be relied on. One effective technique is: 
    1. Place 'markers' to isolate properly the baseline points, even if that causes the number of isolated event points to become very small, or even zero (if you set overlapping D and R 'markers' for instance). 
    2. Click `Estimate Noise Values` to get a valid `baseline noise` value while ignoring any messages that may pop-up regarding too few event points. 
    3. Click the `Enable manual noise entry` checkbox and enter your 'eyeball estimate' of the event noise in the text field. 
    4. Erase all the transition zone 'markers' (click the `Erase All` button).
    5. (Optional) Set appropriate search 'limiters' (Min/Max Dur MinMax MagDrop). 
    6. Finally, click the `Find Solution(s)` button to do a exhaustive search.




 
    
     

