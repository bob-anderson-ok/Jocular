

<center><img src="Vesta.jpg"></center>

## RandGenSeed

You will normally leave this field empty, in which case every 'click' on the **Create Sample Data** button will produce a unique set of observation values.

If you want/need the same set of gaussian noise values to be generated at each click, set an integer value in this field.

    The usual process is that at each observation point,
    a "normal gaussian" variate is generated. A "normal gaussian"
    variate has a mean of 0.0, and a sigma of 1.0.
    
    What is kept constant by the specification of a RandGenSeed
    value is the exact sequence of values for these variates. 
    They will be the same at each 'click' if the seed is the same.
    
    The variates are then scaled by the sigma value in effect at that
    observation point, followed by the addition of the offset 
    from 0.0 that is proper at that point: this completes the
    calculation of the simulated observation point.
    
     

