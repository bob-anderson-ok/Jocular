

<center><img src="Vesta.jpg"></center>

## About

**Jocular** extracts occultation timing parameters from *square wave* events.

A *square wave* event is one in which the underlying light curve transitions from its brightest value to its minimum value in a time that is less (usually substantially less) than the time interval between video frames.  This will happen when the star being occulted has no perceptible disk.

**Jocular** assumes that the video camera integrates incoming light at each pixel during a complete frame time --- dead time while the results of the previous light integration are being read out is zero or negligible.

### Special Feature --- Rigorous Sub-Frame timing determination

**Jocular** uses AIC (Akaike Information Criteria) to determine precisely when it is statistically valid to interpret a D or R transition value as belonging to an intermediate point rather than 'belonging' to the 'baseline' or the 'event'.

Introducing an intermediate point adds one more adjustable parameter to the 'model' that is being fitted.  This will of course improve the 'fit'. The AIC calculation takes into account this 'normal and expected' fit improvement and allows the precise determination of when the 'fit' has improved more than that expected by simply adding another adjustable parameter --- it tells us when the more complex 'model' with an intermediate point should be selected instead of the simpler model. When this is the case, then the intermediate value can be used in a sub-frame timing calculation, and fractional D and/or R times become possible. (The AIC decision is made independently at each transition).

The requirement for sub-frame timing to be applicable in an observation has a particularly simple form when baseline noise and event noise can be considered equal --- in this case

    Given:  signal = B - A  (= baseline.intensity - event.intensity)
    and:    noise  = sigma  (noise in the observation)
    
    then sub-frame timing becomes justified when:  
    
           signal/noise > 2 sqrt(2)  (or about 2.8)

The above equation results from simple algebra manipulations of the AIC equation.  Unfortunately, when event noise and baseline noise are not equal, the equation to determine sub-frame timing applicability does not have such a tidy form.  But the numerical calculation needed to justify sub-frame timing is still easy to perform.

### Author

Bob Anderson

bob.anderson.ok@gmail.com

 
    
     

