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

    public HashMap<Integer, ErrorBarItem> getErrorBars(ArrayList<HistStatItem> statsArray, boolean centered) {
        HashMap<Integer, ErrorBarItem> ans = new HashMap<>();

        // Extract numTrials by adding up the counts in statsArray
        int numTrials = 0;
        for (HistStatItem item : statsArray) {
            numTrials += item.count;
        }

        ans.put(68, getErrorBarItem(statsArray, numTrials, 68, centered));
        ans.put(90, getErrorBarItem(statsArray, numTrials, 90, centered));
        ans.put(95, getErrorBarItem(statsArray, numTrials, 95, centered));
        ans.put(99, getErrorBarItem(statsArray, numTrials, 99, centered));

        return ans;
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

        errItem.actualCI = (double) cumCountActual / numTrials * 100.0;
        errItem.targetCI = confidenceLevel;
        errItem.barCenter = barCenter;
        errItem.leftIndex = indexOfBarLeftEdge;
        errItem.rightIndex = indexOfBarRightEdge;
        errItem.peakIndex = indexOfBarPeak;
        errItem.width = indexOfBarRightEdge - indexOfBarLeftEdge;
        errItem.barPlus = indexOfBarRightEdge - barCenter;
        errItem.barMinus = barCenter - indexOfBarLeftEdge;

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
