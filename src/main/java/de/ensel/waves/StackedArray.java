package de.ensel.waves;

import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StackedArray<T> implements Iterable<T> {
    private Object[] data;   // due to unknown size at compile time (or some other fancy explanation) an array T[] is not possible
    private int size;
    //StackedArray<T> pre;

    public StackedArray(T data) {
        initFirstElement(data);
    }

    private void initFirstElement(T elem) {
        // first element
        if (elem != null && !(elem instanceof T))
            throw new RuntimeException("Type error: inserting data object with incorrect type.");
        // note: fixes size is ok here, we never store more than 1 element per move(depth) (assumed that pawn promoting
        // exchanges the pawn for a queen)
        // otherwise a dynamically growing array would be needed - either a Java.Array or e.g. in bigger junks do reallocate a new array and (hmm) copying
        this.data = new Object[ChessEngineParams.MAX_SEARCHDEPTH+1];
        // for tests below: this.data = new Object[2_000_010];   // fixes size is ok, we never store more than 1 element per move(depth) (assumed that pawn promoting exchanges the pawn for a queen)
        if (elem != null) {
            this.size = 1;
            this.data[0] = elem;
        }
        else {
            this.size = 0;
        }
        //this.pre = null;
    }

    public StackedArray(StackedArray<T> pre, T elem) {
        if (pre == null) {
            initFirstElement(elem);
            return;
        }
        //this.pre = pre;   // skip empty predecessors
        this.data = pre.data;  // we are good and all share the same array, so no copying necessary!
        int pos = pre.size;
        if (elem == null) {
            // nothing new ...
            size = pos;
        }
        else {
            data[pos] = elem;  // ok, we never check the boundary - in our case we know the move depth limit
            this.size = pos+1;
        }
    }

    public int size() {
        return size;
    }

    public T elem() {
        return (T)data[size-1];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean isFirst = true;
        for(T e : this) {
            if (isFirst)
                isFirst = false;
            else
                sb.append(", ");
            sb.append(e);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * be aware, this is comparing pointers, not using equals
     * @param elem
     * @return
     */
    public boolean contains(T elem) {
        for (Object e : data) {
            if (e == elem)
                return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return size == 0;
    }


    //// Iterator

    @Override
    public Iterator<T> iterator() {
        return new StackedArrayIterator();
    }

    private class StackedArrayIterator implements Iterator<T> {
        private Object[] iData;
        private int iPos;

        public StackedArrayIterator() {
            iData = StackedArray.this.data;
            iPos = StackedArray.this.size-1;
        }

        @Override
        public boolean hasNext() {
            return iPos >= 0;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T result = (T)iData[iPos];
            iPos--;
            return result;
        }
    }

    //// Stream
    public Stream<T> stream() {
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }


    //// "Demo"
    public static void main(String[] args) {
        class Elem {
            int value;
            Elem(int v) { value = v; }
            public int v() { return value; }
            Elem setValue(int v) { value = v; return this; }
            public String toString() { return "<" + value + ">"; }
        }

        Elem firstE = new Elem(1);
        StackedArray<Elem> al1 = new StackedArray<>(firstE);
        Elem midE = new Elem(2);
        StackedArray<Elem> al2 = new StackedArray<>(al1, midE);
        Elem lastE = new Elem(3);
        StackedArray<Elem> al3 = new StackedArray<>(al2, lastE);

        System.out.println("1) " + al1 + " " + (al1.contains(firstE) ? "ok" : "NIO") + " size="+al1.size() );
        System.out.println("2) " + al2 + " " + (al2.contains(midE) ? "ok" : "NIO") + " size="+al2.size() );
        System.out.println("3) " + al3 + " " + (al3.contains(lastE) ? "ok" : "NIO") + " size="+al3.size() );

        firstE.setValue(111);
        midE.setValue(222);

        System.out.println("1) " + al1 + " " + (al1.contains(firstE) ? "ok" : "NIO") + " size="+al1.size() );
        System.out.println("2) " + al2 + " " + (al2.contains(midE) ? "ok" : "NIO") + " size="+al2.size() );
        System.out.println("3) " + al3 + " " + (al3.contains(lastE) ? "ok" : "NIO") + " size="+al3.size() );

        StackedArray<Elem> al0 = new StackedArray<>(null);
        al1 = new StackedArray<>(al0, firstE);
        al2 = new StackedArray<>(al1, midE);
        al3 = new StackedArray<>(al2, lastE);
        System.out.println("1) " + al1 + " " + (al1.contains(firstE) ? "ok" : "NIO") + " size="+al1.size() );
        System.out.println("2) " + al2 + " " + (al2.contains(midE) ? "ok" : "NIO") + " size="+al2.size() );
        System.out.println("3) " + al3 + " " + (al3.contains(lastE) ? "ok" : "NIO") + " size="+al3.size() );
        System.out.println("3/1) " + (al3.contains(firstE) ? "ok" : "NIO") + " size="+al3.size() );
        System.out.println("3/2) " + (al3.contains(new Elem(2)) ? "NIO" : "ok") + " size="+al3.size() );

        // test a large list
        /* // NEEDS enlarged inner data array --> change in initFirstElement() above, to do this test here
        long startTime = System.currentTimeMillis();
        Elem content = new Elem(3);
        Elem otherc = new Elem(5);
        StackedArray<Elem> listend = new StackedArray<>(content);
        int i;
        for (i = 1; i < 1_000_000; i++) {
            listend = new StackedArray<>(listend, content);
        }
        listend = new StackedArray<>(listend, otherc);
        for (i = 1; i < 1_000_000; i++) {
            listend = new StackedArray<>(listend, content);
        }
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("done 1a: " + listend.size() + ", " + duration + " ms");
        startTime = System.currentTimeMillis();
        boolean found = listend.contains(otherc);
        duration = System.currentTimeMillis() - startTime;
        System.out.println("done 1b: " + found + ", " + duration + " ms");
        startTime = System.currentTimeMillis();
        long sum = 0;
        for (Elem e : listend) {
            sum += (long)e.v() ;
        }
        duration = System.currentTimeMillis() - startTime;
        System.out.println("Done 1c: " + sum + ", " + duration + " ms");

        // compare with Java.List
        startTime = System.currentTimeMillis();
        List<Elem> list = new ArrayList<>();
        for (i = 0; i < 1_000_000; i++) {
            list.add(content);
        }
        list.add(otherc);
        for (i = 1; i < 1_000_000; i++) {
            list.add(content);
        }
        duration = System.currentTimeMillis() - startTime;
        System.out.println("done 2a: " + list.size() + ", " + duration + " ms");
        startTime = System.currentTimeMillis();
        found = listend.contains(otherc);
        duration = System.currentTimeMillis() - startTime;
        System.out.println("done 2b: " + found + ", " + duration + " ms");
        startTime = System.currentTimeMillis();
        sum = 0;
        for (Elem e : list) {
            sum += (long)e.v() ;
        }
        duration = System.currentTimeMillis() - startTime;
        System.out.println("Done 2c: " + sum + ", " + duration + " ms");

        // compare with Java.Stack
        startTime = System.currentTimeMillis();
        Stack<Elem> stack = new Stack<>();
        for (i = 0; i < 1_000_000; i++) {
            stack.push(content);
        }
        stack.push(otherc);
        for (i = 1; i < 1_000_000; i++) {
            stack.push(content);
        }
        duration = System.currentTimeMillis() - startTime;
        System.out.println("done 3a: " + stack.size() + ", " + duration + " ms");
        startTime = System.currentTimeMillis();
        found = listend.contains(otherc);
        duration = System.currentTimeMillis() - startTime;
        System.out.println("done 3b: " + found + ", " + duration + " ms");
        startTime = System.currentTimeMillis();
        sum = 0;
        for (Elem e : stack) {
            sum += (long)e.v() ;
        }
        duration = System.currentTimeMillis() - startTime;
        System.out.println("Done 3c: " + sum + ", " + duration + " ms total");
        */
    }
}

