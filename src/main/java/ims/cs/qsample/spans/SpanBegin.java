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


package ims.cs.qsample.spans;

import ims.cs.qsample.greedysample.HasScore;

/**
 * Representation of a span begin. It is useful to have this as a separate class since this makes sampling easier.
 * Created by scheibcn on 11/5/15.
 */
public class SpanBegin implements HasScore {
    // each begin has a position and a score
    public int position;
    public Double score = null;

    public SpanBegin(int position, double score) {
        this.position = position;
        this.score = score;
    }

    public SpanBegin(int position) {
        this.position = position;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "SpanBegin(pos=" + position + ",score=" + score + ")";
    }
}
