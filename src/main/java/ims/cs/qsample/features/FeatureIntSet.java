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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A feature set storing features as integers.
 * Created by scheibcn on 6/1/16.
 */
public class FeatureIntSet implements FeatureSet {

    // internal mapping from feature strings to integers
    static FeatureIndexMap featureIndexMap = new FeatureIndexMap(); // a static map across all feature sets
    Set<Integer> featureIndices =  new HashSet<>();

    @Override
    public int size() {
        return featureIndices.size();
    }

    @Override
    public boolean isEmpty() {
        return featureIndices.isEmpty();
    }



    @Override
    public boolean add(String s) {
        int index = featureIndexMap.getIndex(s);
        featureIndices.add(index);
        return true;
    }

    @Override
    public Iterator<String> iterator() { return new StringIterator(); }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        if (c instanceof FeatureIntSet) {
            // just call addAll on the index sets
            featureIndices.addAll(((FeatureIntSet) c).featureIndices);
        } else if (c instanceof Collection) {
            for (String s : c) this.add(s);
        } else {
            throw new Error("Incompatible types");
        }

        return true;
    }


    @Override
    public boolean contains(Object o) {
        int targetIndex = featureIndexMap.getIndex((String) o);
        return featureIndices.contains(targetIndex);
    }

    @Override
    public void clear() { featureIndices.clear(); }


    /**
     * Iterator that automatically maps the stored indices to strings
     */
    class StringIterator implements Iterator<String> {

        Iterator<Integer> featureIndexIter;

        StringIterator () { featureIndexIter = featureIndices.iterator(); }

        @Override
        public boolean hasNext() {
            return featureIndexIter.hasNext();
        }

        @Override
        public String next() {
            int index = featureIndexIter.next();
            return featureIndexMap.getFeature(index);
        }

        @Override
        public void remove() {
            featureIndexIter.remove();
        }
    }



    // NOTE: for compatibility, FeatureSets are collections
    // BELOW: interfaces inherited from collection that we do not need to implement

    @Override
    public Object[] toArray() { throw new Error("Not implemented"); }

    @Override
    public <T> T[] toArray(T[] a) { throw new Error("Not implemented"); }

    @Override
    public boolean remove(Object o) { throw new Error("Not implemented"); }

    @Override
    public boolean containsAll(Collection<?> c) { throw new Error("Not implemented"); }

    @Override
    public boolean removeAll(Collection<?> c) { throw new Error("Not implemented"); }

    @Override
    public boolean retainAll(Collection<?> c) { throw new Error("Not implemented"); }

}
