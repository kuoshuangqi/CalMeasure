/**
 * This file is part of the Java Machine Learning Library
 * 
 * The Java Machine Learning Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * The Java Machine Learning Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Java Machine Learning Library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * Copyright (c) 2006-2012, Thomas Abeel
 * 
 * Project: http://java-ml.sourceforge.net/
 * 
 */
package itu.edu.embeddedlab.swiftforestjava;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;

class Fold implements Dataset {
    private int[] indices;

    private Dataset parent;

    public Fold(Dataset parent, int[] indices) {
        // System.out.println("construction: "+parent.classes());
        this.indices = indices;
        this.parent = parent;
    }

     
    public boolean add(Instance i) {
        throw new UnsupportedOperationException("Cannot do this on a fold of a dataset");
    }

     
    public SortedSet<Object> classes() {
        // System.out.println("Call");
        return parent.classes();
    }

     
    public Dataset[] folds(int numFolds, Random rg) {
        // TODO this method can be implemented on a fold.
        throw new UnsupportedOperationException("Method is not yet implemented");
    }

     
    public Instance instance(int index) {
        // System.out.println(parent);
        // System.out.println(parent.size());
        // System.out.println(index);
        return parent.instance(indices[index]);
    }

     
    public Set<Instance> kNearest(int k, Instance inst,DistanceMeasure dm) {
        // TODO this method can be implemented on a fold.
        throw new UnsupportedOperationException("Method is not yet implemented");
    }

     
    public void add(int index, Instance element) {
        throw new UnsupportedOperationException("Cannot do this on a fold of a dataset");
    }

     
    public boolean addAll(Collection<? extends Instance> c) {
        throw new UnsupportedOperationException("Cannot do this on a fold of a dataset");
    }

     
    public boolean addAll(int index, Collection<? extends Instance> c) {
        throw new UnsupportedOperationException("Cannot do this on a fold of a dataset");
    }

     
    public void clear() {
        throw new UnsupportedOperationException("Cannot do this on a fold of a dataset");

    }

     
    public boolean contains(Object o) {
        // TODO this method can be implemented on a fold.
        throw new UnsupportedOperationException("Method is not yet implemented");
    }

     
    public boolean containsAll(Collection<?> c) {
        // TODO this method can be implemented on a fold.
        throw new UnsupportedOperationException("Method is not yet implemented");
    }

     
    public Instance get(int index) {
        return instance(index);
    }

     
    public int indexOf(Object o) {
        // TODO this method can be implemented on a fold.
        throw new UnsupportedOperationException("Method is not yet implemented");
    }

     
    public boolean isEmpty() {
        return false;
    }

    class FoldIterator implements ListIterator<Instance> {

        private int currentIndex = 0;

        public FoldIterator(int index) {
            this.currentIndex = index;
        }

        public FoldIterator() {
            this(0);
        }

         
        public boolean hasNext() {
            return currentIndex < indices.length;
        }

         
        public Instance next() {
            currentIndex++;
            return instance(currentIndex - 1);
        }

         
        public void remove() {
            throw new UnsupportedOperationException("You cannot do this on a fold.");

        }

         
        public void add(Instance arg0) {
            throw new UnsupportedOperationException("You cannot do this on a fold.");

        }

         
        public boolean hasPrevious() {
            return currentIndex > 0;
        }

         
        public int nextIndex() {
            return currentIndex;
        }

         
        public Instance previous() {
            currentIndex--;
            return instance(currentIndex);
        }

         
        public int previousIndex() {
            return currentIndex;
        }

         
        public void set(Instance arg0) {
            throw new UnsupportedOperationException("You cannot do this on a fold.");

        }

    }

     
    public Iterator<Instance> iterator() {
        return new FoldIterator();
    }

     
    public int lastIndexOf(Object o) {
        // TODO this method can be implemented on a fold.
        throw new UnsupportedOperationException("Method is not yet implemented");
    }

     
    public ListIterator<Instance> listIterator() {
        return new FoldIterator();
    }

     
    public ListIterator<Instance> listIterator(int index) {
        return new FoldIterator(index);
    }

     
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("You cannot do this on a fold.");
    }

     
    public Instance remove(int index) {
        throw new UnsupportedOperationException("You cannot do this on a fold.");
    }

     
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("You cannot do this on a fold.");
    }

     
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("You cannot do this on a fold.");
    }

     
    public Instance set(int index, Instance element) {
        throw new UnsupportedOperationException("You cannot do this on a fold.");
    }

     
    public int size() {
        return indices.length;
    }

     
    public List<Instance> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("You cannot do this on a fold.");
    }

     
    public Object[] toArray() {
        Object[] out = new Object[indices.length];
        for (int i = 0; i < size(); i++) {
            out[i] = instance(i);
        }
        return out;

    }

    @SuppressWarnings("unchecked")
     
    public <T> T[] toArray(T[] a) {
        Vector<T> tmp = new Vector<T>();
        for (Instance i : this) {
            tmp.add((T) i);
        }
        return tmp.toArray(a);
    }

     
    public int noAttributes() {
        return parent.noAttributes();
    }

     
    public int classIndex(Object clazz) {
        return parent.classIndex(clazz);
    }

     
    public Object classValue(int index) {
        return parent.classValue(index);
    }

     
    public Dataset copy() {
        Dataset out=new DefaultDataset();
        for(Instance i:this)
            out.add(i.copy());
        return out;
    }
}