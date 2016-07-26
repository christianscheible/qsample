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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Automatically counting string to int mapping for feature sets.
 * Created by scheibcn on 6/1/16.
 */
public class FeatureIndexMap {
    Map<String, Integer> f2i;
    List<String> i2f;

    int maxIndex = -1;

    FeatureIndexMap () {
        f2i = new HashMap<>();
        i2f = new ArrayList<>();
    }

    /**
     * Translate string to index. If the string is unknown, it is assigned a new index.
     * @param feature
     * @return
     */
    public int getIndex(String feature) {
        if (f2i.containsKey(feature)) {
            return f2i.get(feature);
        } else {
            maxIndex++;
            f2i.put(feature, maxIndex);
            i2f.add(feature);
            return maxIndex;
        }
    }

    /**
     * Translate index to string.
     * @param index
     * @return
     */
    public String getFeature(int index) {
        if (index <= maxIndex) {
            return i2f.get(index);
        } else {
            throw new Error("Lookup error");
        }
    }
}
