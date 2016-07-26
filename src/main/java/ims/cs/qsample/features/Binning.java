/*
 * This file is part of QSample.
 * QSample is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QSample is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QSample.  If not, see <http://www.gnu.org/licenses/>.
 */


package ims.cs.qsample.features;

import java.util.ArrayList;
import java.util.List;

/**
 * Binning for distances
 * Created by scheibcn on 3/4/16.
 */
public class Binning {
    /**
     * Bins that stack up from 0 to 100
     * @param distance
     * @param prefix
     * @return
     */
    public static List<String> distanceBinsStackUp (int distance, String prefix) {
        List<String> features = new ArrayList<>();
        
        if (distance > 0) features.add(prefix + ">=1");
        if (distance > 1) features.add(prefix + ">=2");
        if (distance > 2) features.add(prefix + ">=3");
        if (distance > 3) features.add(prefix + ">=4");
        if (distance > 4) features.add(prefix + ">=5");
        if (distance > 5) features.add(prefix + ">=6");
        if (distance > 6) features.add(prefix + ">=7");
        if (distance > 7) features.add(prefix + ">=8");
        if (distance > 10) features.add(prefix + ">=11");
        if (distance > 15) features.add(prefix + ">=16");
        if (distance > 20) features.add(prefix + ">=21");
        if (distance > 25) features.add(prefix + ">=26");
        if (distance > 30) features.add(prefix + ">=31");
        if (distance > 40) features.add(prefix + ">=41");
        if (distance > 50) features.add(prefix + ">=51");
        if (distance > 60) features.add(prefix + ">=61");
        if (distance > 70) features.add(prefix + ">=71");
        if (distance > 80) features.add(prefix + ">=81");
        if (distance > 90) features.add(prefix + ">=91");
        if (distance > 100) features.add(prefix + ">=101");

        return features;
    }

    /**
     * Bins that stack down from 0 to 100
     * @param distance
     * @param prefix
     * @return
     */
    public static List<String> distanceBinsStackDown (int distance, String prefix) {
        List<String> features = new ArrayList<>();

        if (distance < 2) features.add(prefix + "<=1");
        if (distance < 3) features.add(prefix + "<=2");
        if (distance < 4) features.add(prefix + "<=3");
        if (distance < 5) features.add(prefix + "<=4");
        if (distance < 6) features.add(prefix + "<=5");
        if (distance < 7) features.add(prefix + "<=6");
        if (distance < 8) features.add(prefix + "<=7");
        if (distance < 9) features.add(prefix + "<=8");
        if (distance < 12) features.add(prefix + "<=11");
        if (distance < 17) features.add(prefix + "<=16");
        if (distance < 22) features.add(prefix + "<=21");
        if (distance < 27) features.add(prefix + "<=26");
        if (distance < 32) features.add(prefix + "<=31");
        if (distance < 42) features.add(prefix + "<=41");
        if (distance < 52) features.add(prefix + "<=51");
        if (distance < 62) features.add(prefix + "<=61");
        if (distance < 72) features.add(prefix + "<=71");
        if (distance < 82) features.add(prefix + "<=81");
        if (distance < 92) features.add(prefix + "<=91");
        if (distance < 102) features.add(prefix + "<=101");

        return features;
    }

    /**
     * Interval bins from 0 to 100
     * @param distance
     * @param prefix
     * @return
     */
    public static List<String> distanceBins1to100(int distance, String prefix) {
        List<String> features = new ArrayList<>();

        if (distance > 0 && distance < 5) features.add(prefix + "_in_[0,5)");
        if (distance >= 5 && distance < 10) features.add(prefix + "_in_[5,10)");
        if (distance >= 10 && distance < 20) features.add(prefix + "_in_[10,20)");
        if (distance >= 20 && distance < 40) features.add(prefix + "_in_[20,40)");
        if (distance >= 40 && distance < 60) features.add(prefix + "_in_[40,60)");
        if (distance >= 60 && distance < 80) features.add(prefix + "_in_[60,80)");
        if (distance >= 80 && distance <= 100) features.add(prefix + "_in_[60,100]");

        return features;
    }


    /**
     * Bins from 0 to 100, intervals and stacking up & down
     * @param distance
     * @param prefix
     * @return
     */
    public static List<String> distanceBinsAll (int distance, String prefix) {
        List<String> features = new ArrayList<>();
        features.addAll(distanceBins1to100(distance, prefix));
        features.addAll(distanceBinsStackDown(distance,prefix));
        features.addAll(distanceBinsStackUp(distance,prefix));
        return features;
    }
}
