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


package ims.cs.qsample.perceptron;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Store a set of weights associated to strings
 * Created by scheibcn on 11/5/15.
 */
public class Weights implements Serializable {
    private static final long serialVersionUID = 2945274488514737545L;

    // map for holding weights
    Map<String, Double> weightMap;

    // map for storing the weight history for averaging
    // for a clean description of the algorithm,
    // see for example Chapter 3 in Hal Daume's "A Course in Machine Learning"
    Map<String, Double> weightCacheMap;

    public boolean doAveraging = true;
    int averagingCoefficient = 0;

    public Weights() {
        // allocate some large maps
        weightMap = new HashMap<String, Double>(100000);
        weightCacheMap = new HashMap<String, Double>(100000);
    }

    /**
     * Resets all weights to 0
     */
    public void resetWeights() {
        averagingCoefficient = 0;
        weightMap.clear();
        weightCacheMap.clear();
    }

    /**
     * Get the most recent weight of a feature. Returns 0 if the feature is unknown.
     * @param feature
     * @return
     */
    public double get(String feature) {
        if (weightMap.containsKey(feature)) {
            return weightMap.get(feature);
        } else {
            return 0;
        }
    }

    /**
     * Get the averaged weight of a feature. Returns 0 if the feature is unknown.
     * @param feature
     * @return
     */
    public double getAvg(String feature) {
        if (weightMap.containsKey(feature)) {
            Double cache = weightCacheMap.get(feature);
            if (cache == null) cache = 0.0;
            return weightMap.get(feature) - (cache/averagingCoefficient);
        } else {
            return 0;
        }
    }

    /**
     * Update the weight of a feature by value
     * @param feature
     * @param value
     */
    public void update(String feature, double value) {
        // update the weight of the feature
        if (!weightMap.containsKey(feature)) {
            weightMap.put(feature, value);
        } else {
            weightMap.put(feature, weightMap.get(feature) + value);
        }

        // also add to averaging map if averaging is on
        if (doAveraging) {
            if (!weightCacheMap.containsKey(feature)) {
                weightCacheMap.put(feature, value * averagingCoefficient);
            } else {
                weightCacheMap.put(feature, weightCacheMap.get(feature) + value * averagingCoefficient);
            }

            averagingCoefficient++;
        }
    }
}
