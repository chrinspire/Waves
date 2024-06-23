/*
 *     Waves - Another Wired New Chess Algorithm
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

import de.ensel.chessbasics.ChessBasics;
import static de.ensel.chessbasics.ChessBasics.*;

/**
 * SimpleMove represents a Chess moves of a certain piece from a square position to another one position.
 *  Optionally the from or to position can be set to the placeholder ANY from ChessBasics.
 */
public class SimpleMove {
    private int from;
    private int to;
    private int promotesTo;

    //// Constructors

    public SimpleMove(int from, int to) {
        this.from = from;
        this.to = to;
        promotesTo = EMPTY;
    }

    public SimpleMove(int from, int to, int promotesToPceType) {
        this.from = from;
        this.to = to;
        promotesTo = promotesToPceType;
    }

    /**
     * Allows only simple notation like FEN-moves a1b2 or with dash a1-b2.
     * Appended Piece type letters points out which piece to promote a pawn to.
     * @param move move string in simple notation
     */
    public SimpleMove(String move) {
        if ( move.length()>=4
                && isFileChar( move.charAt(0)) && isRankChar(move.charAt(1) )
                && isFileChar( move.charAt(2)) && isRankChar(move.charAt(3) )
        ) {
            // move-string starts with a lower case letter + a digit and is at least 4 chars long
            // --> standard fen-like move-string, like "a1b2"
            from = coordinateString2Pos(move, 0);
            to = coordinateString2Pos(move, 2);
            if ( move.length() > 4 )
                promotesTo = getPceTypeFromPromoteChar(move.charAt(4));
            else
                promotesTo = EMPTY;
            //System.out.format(" %c,%c %c,%c = %d,%d-%d,%d = %d-%d\n", input.charAt(0), input.charAt(1), input.charAt(2), input.charAt(3), (input.charAt(0)-'A'), input.charAt(1)-'1', (input.charAt(2)-'A'), input.charAt(3)-'1', from, to);
        }
        else  if ( move.length()>=5
                && isFileChar( move.charAt(0)) && isRankChar(move.charAt(1) )
                && move.charAt(2)=='-'
                && isFileChar( move.charAt(3)) && isRankChar(move.charAt(4) )
        ) {
            // move-string starts with a lower case letter + a digit + a '-' and is at least 5 chars long
            // --> simple move-string, like "a1-b2"
            from = coordinateString2Pos(move, 0);
            to = coordinateString2Pos(move, 3);
            if ( move.length() > 5 )
                promotesTo = getPceTypeFromPromoteChar(move.charAt(5));
            else
                promotesTo = EMPTY;
            //System.out.format(" %c,%c %c,%c = %d,%d-%d,%d = %d-%d\n", input.charAt(0), input.charAt(1), input.charAt(2), input.charAt(3), (input.charAt(0)-'A'), input.charAt(1)-'1', (input.charAt(2)-'A'), input.charAt(3)-'1', from, to);
        }
        else {
            this.from = -64;
            this.to = -64;
            promotesTo = EMPTY;
        }
    }

    public SimpleMove(SimpleMove origin) {
        this.from = origin.from;
        this.to = origin.to;
        this.promotesTo = origin.promotesTo;
    }


    //// Overrides

    @Override
    public String toString() {
        return ChessBasics.squareName( from)
            // for debugging only    + (isBasicallyALegalMove() ? "" : "'")
                + ChessBasics.squareName(to)
                + ( promotesTo!=EMPTY  ? Character.toLowerCase(fenCharFromPceType(promotesTo)) : "");
    }

    /**
     * std.equals(), hint: does not compare isLegal flag
     * @param o other move to compare with
     * @return true if members from, to and promotesTo are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleMove move)) return false;
        return from == move.from && to == move.to && promotesTo == move.promotesTo;
    }

    @Override
    public int hashCode() {
        return hashId();
    }


    /**
     * move sequence factory
     */
    public static SimpleMove[] getMoves(String movesString) {
        String[] moveStrings = movesString.trim().split(" ");
        if (moveStrings.length==0 || moveStrings.length==1 && moveStrings[0].isEmpty())
            return null;
        int start = moveStrings[0].equalsIgnoreCase("moves") ? 1 : 0;
        SimpleMove[] moves = new SimpleMove[moveStrings.length-start];
        for (int m = start; m< moveStrings.length; m++) {
            //System.out.println("<" +  moveStrings[m] + ">");
            if ( moveStrings[m]==null || moveStrings[m].isEmpty()) {
                start++;  // skip an empty one
                continue;
            } else if ( moveStrings[m].length()<4 || moveStrings[m].length()>5 ) {
                //System.err.println("**** Fehler beim Parsen der moves am Ende des FEN-Strings " + fenString);
                return null;
            }
            moves[m - start] = new SimpleMove(moveStrings[m]);
        }
        return moves;
    }


    //// simple info

    public boolean isMove() {
        return from >= 0 && from < NR_SQUARES
                && to >= 0 && to < NR_SQUARES;
    }

    public int promotesTo() {
        return promotesTo==EMPTY? QUEEN : promotesTo;
    }

    public Integer hashId() {
        return (from << 8) + to;
    }


    //// getter

    public int from() {
        return from;
    }

    public int to() {
        return to;
    }


    //// setter

    public void setFrom(int from) {
        this.from = from;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public void setPromotesTo(int pceType) {
        promotesTo = pceType;
    }

}

