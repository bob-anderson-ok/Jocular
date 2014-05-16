package joculartest;

import jocularmain.JocularMain;
import jocularmain.SqSolver;
import jocularmain.XYChartMarker;
import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SqSolverTest {
    
    public SqSolverTest() {
    }

    private JocularMain jocularMain = mock(JocularMain.class);
    private XYChartMarker dLeftMarker = mock(XYChartMarker.class);
    private XYChartMarker dRightMarker = mock(XYChartMarker.class);
    private XYChartMarker rLeftMarker = mock(XYChartMarker.class);
    private XYChartMarker rRightMarker = mock(XYChartMarker.class);

    @Before
    public void setup() {
        // Simulate a left trim applied at 4.5
        when (jocularMain.getOutOfRangeOfObsOnTheLeft()).thenReturn(5);
        when (jocularMain.getOutOfRangeOfObsOnTheRight()).thenReturn(150);
        
        when (dLeftMarker.isInUse()).thenReturn(Boolean.TRUE);
        when (dRightMarker.isInUse()).thenReturn(Boolean.TRUE);
        when (rLeftMarker.isInUse()).thenReturn(Boolean.TRUE);
        when (rRightMarker.isInUse()).thenReturn(Boolean.TRUE);
        
        when (dLeftMarker.getXValue()).thenReturn(7.5);
        when (dRightMarker.getXValue()).thenReturn(15.5);
        when (rLeftMarker.getXValue()).thenReturn(70.5);
        when (rRightMarker.getXValue()).thenReturn(81.5);
    }
    
    @Test
    public void computeCandidates_getsDandRlimitsRight_whenAllInBounds() {
        
        SqSolver.computeCandidates(jocularMain, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);
        
        assertThat(SqSolver.dLeft).isEqualTo(8);
        assertThat(SqSolver.dRight).isEqualTo(15);
        assertThat(SqSolver.rLeft).isEqualTo(71);
        assertThat(SqSolver.rRight).isEqualTo(81);
    }
    
    @Test
    public void computeCandidates_getsDandRlimitsRight_whenDleftNotInUse() {
        
        when ( dLeftMarker.isInUse()).thenReturn(false);
        SqSolver.computeCandidates(jocularMain, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);
        
        assertThat(SqSolver.dLeft).isEqualTo(5);
        assertThat(SqSolver.dRight).isEqualTo(15);
        assertThat(SqSolver.rLeft).isEqualTo(71);
        assertThat(SqSolver.rRight).isEqualTo(81);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void computeCandidates_throwsException_whenDandRrangesOverlap() {
        when (rLeftMarker.getXValue()).thenReturn(10.5);
        SqSolver.computeCandidates(jocularMain, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void computeCandidates_throwsException_whenDlimitsFlipped() {
        when (dLeftMarker.getXValue()).thenReturn(15.5);
        when (dRightMarker.getXValue()).thenReturn(7.5);
        SqSolver.computeCandidates(jocularMain, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void computeCandidates_throwsException_whenRlimitsFlipped() {
        when (rLeftMarker.getXValue()).thenReturn(81.5);
        when (rRightMarker.getXValue()).thenReturn(70.5);
        SqSolver.computeCandidates(jocularMain, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);
    }
}
