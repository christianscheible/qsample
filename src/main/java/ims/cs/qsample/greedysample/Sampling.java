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

package ims.cs.qsample.greedysample;

import java.util.List;
import java.util.Random;

/**
 * Sample an element from a list of elements with a score.
 * Created by scheibcn on 11/5/15.
 */
public class Sampling {


    Random random;
    public boolean doExp = true;

    public Sampling(Random random) {
        this.random = random;
    }


    /**
     * Sample an element proportionally to sigmoid-transformed scores
     * @param items
     */
    public int sampleOne(List<HasScore> items, double temperature, double bias) {
        double[] values = new double[items.size()];
        double sum = 0;

        // first compute scores and normalize
        for (int i = 0; i < values.length; i++) {
            double score = items.get(i).getScore();
            values[i] = (score + bias) / temperature;

            if (doExp) {
                values[i] = 1/(1+Math.exp(-values[i]));
            }
            sum += values[i];
        }

        // then sample proportionally
        double sumNorm = 0;
        double r = random.nextDouble();
        int resultPosition = 0;

        for (int i = 0; i < values.length; i++) {
            values[i] /= sum;
            sumNorm += values[i];
            if (sumNorm > r) {
                resultPosition = i;
                break;
            }
        }

        return resultPosition;
    }

}
