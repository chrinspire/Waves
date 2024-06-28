package de.ensel.waves;

import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StackedList<T> implements Iterable<T> {
    private T data;
    private int size = 0;
    StackedList<T> pre;

    public StackedList(T data) {
        // first element
        this.data = data;
        if (data != null)
            this.size = 1;
        this.pre = null;
    }

    public StackedList(StackedList<T> pre, T data) {
        this.data = data;
        this.pre = pre;   // skip empty predecessors
        if (data != null)
            this.size = (pre == null ? 0 : pre.size()+1);
    }

    public int size() {
        return size;
    }

    public T data() {
        return data;
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

    public boolean contains(T e) {
        for (T t : this) {
            if (t.equals(e))
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
        return new StackedListIterator();
    }

    private class StackedListIterator implements Iterator<T> {
        private StackedList<T> current;

        public StackedListIterator() {
            current = StackedList.this;
            while (current != null && current.data == null)
                current = current.pre;
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T result = current.data;
            do {
                current = current.pre;
            } while (current != null && current.data == null);
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
        StackedList<Elem> al1 = new StackedList<>(firstE);
        Elem midE = new Elem(2);
        StackedList<Elem> al2 = new StackedList<>(al1, midE);
        Elem lastE = new Elem(3);
        StackedList<Elem> al3 = new StackedList<>(al2, lastE);

        System.out.println("1) " + al1 + " " + (al1.contains(firstE) ? "ok" : "NIO") + " size="+al1.size() );
        System.out.println("2) " + al2 + " " + (al2.contains(midE) ? "ok" : "NIO") + " size="+al2.size() );
        System.out.println("3) " + al3 + " " + (al3.contains(lastE) ? "ok" : "NIO") + " size="+al3.size() );

        firstE.setValue(111);
        al2.data.setValue(222);

        System.out.println("1) " + al1 + " " + (al1.contains(firstE) ? "ok" : "NIO") + " size="+al1.size() );
        System.out.println("2) " + al2 + " " + (al2.contains(midE) ? "ok" : "NIO") + " size="+al2.size() );
        System.out.println("3) " + al3 + " " + (al3.contains(lastE) ? "ok" : "NIO") + " size="+al3.size() );

        StackedList<Elem> al0 = new StackedList<>(null);
        al1 = new StackedList<>(al0, firstE);
        al2 = new StackedList<>(al1, midE);
        al3 = new StackedList<>(al2, lastE);
        System.out.println("1) " + al1 + " " + (al1.contains(firstE) ? "ok" : "NIO") + " size="+al1.size() );
        System.out.println("2) " + al2 + " " + (al2.contains(midE) ? "ok" : "NIO") + " size="+al2.size() );
        System.out.println("3) " + al3 + " " + (al3.contains(lastE) ? "ok" : "NIO") + " size="+al3.size() );
        System.out.println("3/1) " + al3 + " " + (al3.contains(firstE) ? "ok" : "NIO") + " size="+al3.size() );
        System.out.println("3/2) " + al3 + " " + (al3.contains(new Elem(2)) ? "NIO" : "ok") + " size="+al3.size() );

        // test a large list
        long startTime = System.currentTimeMillis();
        Elem content = new Elem(3);
        Elem otherc = new Elem(5);
        StackedList<Elem> listend = new StackedList<>(content);
        int i;
        for (i = 1; i < 1_000_000; i++) {
            listend = new StackedList<>(listend, content);
        }
        listend = new StackedList<>(listend, otherc);
        for (i = 1; i < 1_000_000; i++) {
            listend = new StackedList<>(listend, content);
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

    }
}

