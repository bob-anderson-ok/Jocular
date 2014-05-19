package joculartest;

import java.util.List;
import jocularmain.JocularMain;
import jocularmain.SolutionStats;
import jocularmain.SqSolver;
import jocularmain.XYChartMarker;
import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import utils.Observation;
import utils.SampleDataGenerator;
import utils.SqSolution;

public class SqSolverTest {
    
    public SqSolverTest() {
    }

    private JocularMain jocularMain = mock(JocularMain.class);
    private XYChartMarker dLeftMarker = mock(XYChartMarker.class);
    private XYChartMarker dRightMarker = mock(XYChartMarker.class);
    private XYChartMarker rLeftMarker = mock(XYChartMarker.class);
    private XYChartMarker rRightMarker = mock(XYChartMarker.class);
    private SolutionStats solStat = new SolutionStats();

    @Before
    public void setup() {
        
        // Create a non-trivial 'observation' using a SampleDataGenerator ...
        SampleDataGenerator dataGen = new SampleDataGenerator("testSetTwo");

        dataGen.setDevent(10.5)
                .setRevent(76.5)
                .setSigmaA(1.0)
                .setSigmaB(1.0)
                .setAintensity(2)
                .setBintensity(12)
                .setNumDataPoints(150)
                .setParams();
        
        Observation trialObs = dataGen.build();
        
        when(jocularMain.getCurrentObservation()).thenReturn(trialObs);
        
        // Simulate a left trim applied at 4.5
        when (jocularMain.getOutOfRangeOfObsOnTheLeft()).thenReturn(5);
        // Simulate a right trim applied at 150.5
        when (jocularMain.getOutOfRangeOfObsOnTheRight()).thenReturn(150);
        
        when (dLeftMarker.isInUse()).thenReturn(true);
        when (dRightMarker.isInUse()).thenReturn(true);
        when (rLeftMarker.isInUse()).thenReturn(true);
        when (rRightMarker.isInUse()).thenReturn(true);
        
        when (dLeftMarker.getXValue()).thenReturn(7.5);
        when (dRightMarker.getXValue()).thenReturn(15.5);
        when (rLeftMarker.getXValue()).thenReturn(70.5);
        when (rRightMarker.getXValue()).thenReturn(81.5);
    }
    
    @Test
    public void computeCandidates_getsDandRlimitsRight_whenAllInBounds() {
        
        List<SqSolution> answers = SqSolver.computeCandidates(
            jocularMain, solStat, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);
        
        assertThat(SqSolver.dLeft).isEqualTo(8);
        assertThat(SqSolver.dRight).isEqualTo(15);
        assertThat(SqSolver.rLeft).isEqualTo(71);
        assertThat(SqSolver.rRight).isEqualTo(81);
        
        assertThat(SqSolver.dTranCandidates.length).isEqualTo(8);
        assertThat(SqSolver.rTranCandidates.length).isEqualTo(11);
        
        assertThat(SqSolver.dTranCandidates[0]).isEqualTo(8);
        assertThat(SqSolver.dTranCandidates[7]).isEqualTo(15);
        
        assertThat(SqSolver.rTranCandidates[0]).isEqualTo(71);
        assertThat(SqSolver.rTranCandidates[10]).isEqualTo(81);
        
        for(SqSolution answer: answers ) {
            System.out.println(answer);
        }
                    
    }
    
    @Test
    public void computeCandidates_getsDandRlimitsRight_whenDmarkersNotInUse() {
        
        when (dLeftMarker.isInUse()).thenReturn(false);
        when (dRightMarker.isInUse()).thenReturn(false);
        
        SqSolver.computeCandidates(
            jocularMain, solStat, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);
        
        assertThat(SqSolver.dLeft).isEqualTo(5);
        assertThat(SqSolver.dRight).isEqualTo(150);
        assertThat(SqSolver.rLeft).isEqualTo(71);
        assertThat(SqSolver.rRight).isEqualTo(81);
    }
    
    @Test
    public void computeCandidates_getsDandRlimitsRight_whenRmarkersNotInUse() {
        
        when (rLeftMarker.isInUse()).thenReturn(false);
        when (rRightMarker.isInUse()).thenReturn(false);
        
        SqSolver.computeCandidates(
            jocularMain, solStat, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);
        
        assertThat(SqSolver.dLeft).isEqualTo(8);
        assertThat(SqSolver.dRight).isEqualTo(15);
        assertThat(SqSolver.rLeft).isEqualTo(5);
        assertThat(SqSolver.rRight).isEqualTo(150);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void computeCandidates_throwsException_whenMarkersNotPaired() {
        
        when (rRightMarker.isInUse()).thenReturn(false);
        when (dLeftMarker.isInUse()).thenReturn(false);
        
        SqSolver.computeCandidates(
            jocularMain, solStat, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);
        
    }
    
    @Test
    public void computeCandidates_getsDandRlimitsRight_whenNoMarkersInUse() {
        
        when (dLeftMarker.isInUse()).thenReturn(false);
        when (dRightMarker.isInUse()).thenReturn(false);
        when (rLeftMarker.isInUse()).thenReturn(false);
        when (rRightMarker.isInUse()).thenReturn(false);
        
        SqSolver.computeCandidates(
            jocularMain, solStat, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);
        
        assertThat(SqSolver.dLeft).isEqualTo(5);
        assertThat(SqSolver.dRight).isEqualTo(150);
        assertThat(SqSolver.rLeft).isEqualTo(5);
        assertThat(SqSolver.rRight).isEqualTo(150);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void computeCandidates_throwsException_whenRisBeforeD() {
        when (rLeftMarker.getXValue()).thenReturn(1.5);
        when (rRightMarker.getXValue()).thenReturn(1.5);
        SqSolver.computeCandidates(
            jocularMain, solStat, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void computeCandidates_throwsException_whenDlimitsFlipped() {
        when (dLeftMarker.getXValue()).thenReturn(15.5);
        when (dRightMarker.getXValue()).thenReturn(7.5);
        SqSolver.computeCandidates(
            jocularMain, solStat, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void computeCandidates_throwsException_whenRlimitsFlipped() {
        when (rLeftMarker.getXValue()).thenReturn(81.5);
        when (rRightMarker.getXValue()).thenReturn(70.5);
        SqSolver.computeCandidates(
            jocularMain, solStat, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);
    }
}
