

<center><img src="Vesta.jpg"></center>

## Getting Started


 1.  Read an observation file (using appropriate entry in the **File** menu) or create a sample data set (use **Operations | Generate Sample Data**).
 
 2. Place ** D markers** around the expected **D** transition zone (if present).  (See note below)
 
 3. Place **R markers** around the expected **R** transition zone (if present).  (See note below)
 
 4. Click the `Estimate Noise Values` button.
 
 5. Set any desired search limitations (min/max event size; min/max magDrop)
 
 6. Click the `Find Solution(s)` button.
 
#### Note

In the special case where a potential signal is buried in noise to the extent that there are no obvious transition zones, do not place any **D** or **R** markers.  In this case, **Jocular** will assume that baseline noise and event noise are equal and estimate the noise by differentiating the observation (which suppresses step changes), and determining the noise of the differentiated signal, adjusting for the additional noise introduced by the differentiation process by dividing by the square root of 2 and using this value for both the baseline and the event noise.
  
 
  
    
     

