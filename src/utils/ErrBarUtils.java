package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class ErrBarUtils {

    private ErrBarUtils() {
    }

    private static class ErrBarUtilsHolder {
        private static final ErrBarUtils INSTANCE = new ErrBarUtils();
    }

    public static ErrBarUtils getInstance() {
        return ErrBarUtilsHolder.INSTANCE;
    }

    public ArrayList<HistStatItem> buildHistStatArray(int[] hist) {
        ArrayList<HistStatItem> arrayList = new ArrayList<>();

        // Initialize arrayList.
        for (int i = 0; i < hist.length; i++) {
            HistStatItem item = new HistStatItem();
            item.count = hist[i];
            item.position = i;
            item.cumCount = 0;
            arrayList.add(item);
        }

        // Process arrayList to set cumCounts.
        calculateCumCounts(arrayList);

        // And sort it descending order of count
        sortStatsArrayDescendingOnCounts(arrayList);

        return arrayList;
    }

    public HashMap<String, ErrorBarItem> getErrorBars(ArrayList<HistStatItem> statsArray, boolean centered) {
        HashMap<String, ErrorBarItem> ans = new HashMap<>();

        // Extract numTrials by adding up the counts in statsArray
        int numTrials = 0;
        for (HistStatItem item : statsArray) {
            numTrials += item.count;
        }

        ErrorBarItem ci68 = getErrorBarItem(statsArray, numTrials, 68, centered);
        ErrorBarItem ci90 = getErrorBarItem(statsArray, numTrials, 90, centered);
        ErrorBarItem ci95 = getErrorBarItem(statsArray, numTrials, 95, centered);
        ErrorBarItem ci99 = getErrorBarItem(statsArray, numTrials, 99, centered);
        
        ans.put("D68", ci68);
        ans.put("D90", ci90);
        ans.put("D95", ci95);
        ans.put("D99", ci99);
        
        ans.put("R68", swapPlusAndMinusBarValues(ci68));
        ans.put("R90", swapPlusAndMinusBarValues(ci90));
        ans.put("R95", swapPlusAndMinusBarValues(ci95));
        ans.put("R99", swapPlusAndMinusBarValues(ci99));

        return ans;
    }
    
    private ErrorBarItem swapPlusAndMinusBarValues(ErrorBarItem errBarItem) {
        ErrorBarItem copy = new ErrorBarItem();
        copy.actualCI = errBarItem.actualCI;
        copy.barCenter = errBarItem.barCenter;
        copy.barMinus = errBarItem.barPlus;    // swapping
        copy.barPlus = errBarItem.barMinus;    // swapping
        copy.leftIndex = errBarItem.leftIndex;
        copy.peakIndex = errBarItem.peakIndex;
        copy.rightIndex = errBarItem.rightIndex;
        copy.targetCI = errBarItem.targetCI;
        copy.width = errBarItem.width;

        return copy;
    }

    private void calculateCumCounts(ArrayList<HistStatItem> statsArray) {
        int cumCount = 0;
        for (HistStatItem item : statsArray) {
            cumCount += item.count;
            item.cumCount = cumCount;
        }
    }

    private void sortStatsArrayDescendingOnCounts(ArrayList<HistStatItem> statsArray) {
        DescendingCountComparator descendingCountComparator = new DescendingCountComparator();
        Collections.sort(statsArray, descendingCountComparator);
    }

    class DescendingCountComparator implements Comparator<HistStatItem> {

        @Override
        public int compare(HistStatItem one, HistStatItem two) {
            return Integer.compare(two.count, one.count);
        }
    }

    private ErrorBarItem getErrorBarItem(ArrayList<HistStatItem> statsArray, int numTrials, int confidenceLevel, boolean centered) {
        ErrorBarItem errItem = new ErrorBarItem();

        int cumCountsRequired = (int) (numTrials * confidenceLevel * 0.01);
        ArrayList<HistStatItem> contributors = contributorsRequiredForGivenConfidenceLevel(statsArray, cumCountsRequired);
        int cumCountActual = contributors.get(contributors.size() - 1).cumCount;
        sortContributorsAscendingOnPosition(contributors);

        int indexOfBarPeak = statsArray.get(0).position;
        int indexOfBarLeftEdge = contributors.get(0).position;
        int indexOfBarRightEdge = contributors.get(contributors.size() - 1).position;

        double barCenter = indexOfBarPeak;

        if (centered) {
            barCenter = (statsArray.size() / 2) - 0.5;
        }

        errItem.subFrameTimingEstimate = false;
        errItem.actualCI = (double) cumCountActual / numTrials * 100.0;
        errItem.targetCI = confidenceLevel;
        errItem.barCenter = barCenter;
        errItem.leftIndex = indexOfBarLeftEdge;
        errItem.rightIndex = indexOfBarRightEdge;
        errItem.peakIndex = indexOfBarPeak;
        errItem.width = indexOfBarRightEdge - indexOfBarLeftEdge;
        errItem.barPlus = Math.abs(indexOfBarRightEdge - barCenter);
        errItem.barMinus = Math.abs(barCenter - indexOfBarLeftEdge);

        return errItem;
    }

    private ArrayList<HistStatItem> contributorsRequiredForGivenConfidenceLevel(ArrayList<HistStatItem> statsArray, int cumCountNeeded) {
        ArrayList<HistStatItem> shortList = new ArrayList<>();
        for (HistStatItem item : statsArray) {
            shortList.add(item);
            if (item.cumCount >= cumCountNeeded) {
                break;
            }
        }
        return shortList;
    }

    private void sortContributorsAscendingOnPosition(ArrayList<HistStatItem> contributors) {
        AscendingPositionComparator ascendingPositionComparator = new AscendingPositionComparator();
        Collections.sort(contributors, ascendingPositionComparator);
    }

    class AscendingPositionComparator implements Comparator<HistStatItem> {

        @Override
        public int compare(HistStatItem one, HistStatItem two) {
            return Integer.compare(one.position, two.position);
        }
    }
}
