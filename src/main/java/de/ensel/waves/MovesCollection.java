/*
 *     Waves - Another Wired New Chess Engine
 *     Copyright (C) 2024 Christian Ensel
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

/**
 * only use for Move-Lists with unique to-positions
 */
public class MovesCollection extends AbstractCollection<Move> {
    private HashMap<Integer, Move> moves;

    public MovesCollection() {
        this.moves = new HashMap<>(8);;
    }

    Collection<Move> getAllMoves() {
        if (moves == null || moves.isEmpty())
            return null;
        return moves.values();
    }

    public Move getMoveTo(int to) {
        return moves.get(to);
    }

    @Override
    /**
     * add move
     * @param em
     * @return
     */
    public boolean add(Move m) {
        Move old = moves.put(m.to(), m);
        return m != old;
    }

    @Override
    public Iterator<Move> iterator() {
        return moves.values().iterator();
    }

    @Override
    public int size() {
        return moves.size();
    }

    @Override
    public boolean isEmpty() {
        return moves.isEmpty();
    }

}