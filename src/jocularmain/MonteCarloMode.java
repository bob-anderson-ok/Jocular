package jocularmain;


// Here we define the mode that controls how we determine the level
// at the trials transition point.

public enum MonteCarloMode {
    MID_POINT,     // transition level = (B+A)/2
    RANDOM,        // transition level is linearly random between A and B (>A and <=B)
    LEFT_EDGE,     // transition level = A   
    RIGHT_EDGE     // transition level = B 
}
