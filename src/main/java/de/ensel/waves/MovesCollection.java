/*
 *     TideEval - Wired New Chess Algorithm
 *     Copyright (C) 2023 Christian Ensel
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.ensel.waves;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class MovesCollection extends AbstractCollection<SimpleMove> {
    private HashMap<Integer, SimpleMove> moves;

    private final int color;  // color is needed to know how to aggregate move evaluations (board perspective:

    public MovesCollection(int color) {
        this.moves = new HashMap<>(8);;
        this.color = color;
    }
    // larger number are better for white, smaller is better for black)

    Collection<SimpleMove> getAllMoves() {
        if (moves == null || moves.isEmpty())
            return null;
        return moves.values();
    }


    @Override
    /**
     * add move
     * @param em
     * @return
     */
    public boolean add(SimpleMove m) {
        SimpleMove old = moves.put(m.hashCode(),m);
        return m != old;
    }

    @Override
    public Iterator<SimpleMove> iterator() {
        return moves.values().iterator();
    }

    @Override
    public int size() {
        return moves.size();
    }

    //// specialized getters

    protected int color() {
        return color;
    }

}