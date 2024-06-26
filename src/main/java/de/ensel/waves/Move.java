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

import java.util.List;

import static de.ensel.chessbasics.ChessBasics.*;

/** Complex representation of a Chess moves of a certain piece from a square position to another one position.
 *  Store and handles conditions, obstacles and consequences: i.e. which other moves are required to enable this move,
 *  which ones can block or delay this move and the other way round, which ones are enabled or blocked/delayed.
 */
public class Move implements Comparable<Move> {
    private ChessPiece myPiece;
    private Square fromSq;

    private Square toSq;
    private int promotesTo;
    private boolean isCheckGiving = false;

    private Square[] intermedSqs;
    private Evaluation eval = new Evaluation();


    //// Constructors

    public Move(ChessPiece myPiece, Square fromSq, Square toSq, Square[] intermedSqs) {
        this.myPiece = myPiece;
        this.fromSq = fromSq;
        this.toSq = toSq;
        promotesTo = EMPTY;
        this.intermedSqs = intermedSqs;
    }

    public Move(ChessPiece myPiece, Square fromSq, Square toSq, int promotesToPceTypee, Square[] intermedSqs) {
        this.myPiece = myPiece;
        this.fromSq = fromSq;
        this.toSq = toSq;
        promotesTo = promotesToPceTypee;
        this.intermedSqs = intermedSqs;
    }

    public Move(Move origin) {
        this.myPiece = origin.myPiece;
        this.fromSq = origin.fromSq;
        this.toSq = origin.toSq;
        this.promotesTo = origin.promotesTo;
        this.isCheckGiving = origin.isCheckGiving;
        this.eval = origin.eval;
        this.intermedSqs = origin.intermedSqs;
    }


    //// handling evaluations

    /**
     * adds or substracts to/from an eval on a certain future level (passthrough to Evaluation)
     * beware: is unchecked
     * @param evalValue
     * @param futureLevel the future level from 0..max
     */
    void addEval(int evalValue, int futureLevel) {
        eval.addEval(evalValue,futureLevel);
    }


    Move addEval(Evaluation addEval) {
        eval.addEval(addEval);
        return this;
    }

    void addEvalAt(int eval, int futureLevel) {
        this.eval.addEval(eval,futureLevel);
    }

    void subtractEvalAt(int eval, int futureLevel) {
        this.eval.addEval(-eval,futureLevel);
    }

    /**
     * calcs and stores the max of this eval and the given other eval individually on all levels
     * @param meval the other evaluation
     */
    public void incEvaltoMaxFor(Evaluation meval, boolean color) {
        eval.maxEvalPerFutureLevelFor(meval, color);
    }

    boolean isBetterForColorThan(final int color, final Move other) {
        boolean probablyBetter = eval.isBetterForColorThan( color, other.getEval());
        return probablyBetter;
    }

    /**
     * See if evMove is among the best, i.e. best or max maxTopEntries-st best, in the list sortedTopMoves.
     * If yes it is put there, else into restMoves. If sortedTopMoves grows too large, the too many lower ones are also moved to restMoves.
     * @param evMove move to be sorted in
     * @param sortedTopMoves top moves so far. sortedTopMoves needs to be sorted from the beginning (or empty).
     * @param color to determine which evaluations are better (higher or lower)
     * @param maxTopEntries the max nr of entries that should be in sortedTopMoves
     * @param restMoves is not sorted.
     * @return true if evMove is a new top move, false otherwise
     */
    static boolean addMoveToSortedListOfCol(Move evMove,
                                            List<Move> sortedTopMoves,
                                            int color, int maxTopEntries,
                                            List<Move> restMoves) {
        // todo: do at least a binary search, not one by one through the top list
        int i;
        for (i = sortedTopMoves.size() - 1; i >= 0; i--) {
            if (!evMove.isBetterForColorThan(color, sortedTopMoves.get(i))) {
                // not better, but it was better than the previous, so add below
                if (i < maxTopEntries)
                    sortedTopMoves.add(i + 1, evMove);
                // move lower rest if top list became too big
                while (sortedTopMoves.size() > maxTopEntries) {
                    restMoves.add(
                            sortedTopMoves.remove(maxTopEntries) );
                }
                return false;
            }
        }
        //it was best!!
        sortedTopMoves.add(0, evMove);
        // move lower rest if top list became too big
        while (sortedTopMoves.size() > maxTopEntries) {
            restMoves.add(
                    sortedTopMoves.remove(maxTopEntries) );
        }
        return true;
    }


    /**
    static boolean updateMoveToSortedListOfCol(Move evMove,
                                            List<Move> sortedTopMoves,
                                            int color, int maxTopEntries,
                                            List<Move> restMoves) {
        // todo: do at least a binary search, not one by one through the top list
        // todo: get rid of code duplication right before at return false&true...
        int i;
        boolean oldIsRemoved = false;
        for (i = sortedTopMoves.size() - 1; i >= 0; i--) {
            if (evMove.equals(sortedTopMoves.get(i))) {
                // found original move, remove it
                sortedTopMoves.remove(i);
                oldIsRemoved = true;
            }
            if (!evMove.isBetterForColorThan(color, sortedTopMoves.get(i))) {
                // not better, but it was better than the previous, so add below
                if (i < maxTopEntries)
                    sortedTopMoves.add(i + 1, evMove);
                // look through rest of top-list
                for (int j = i-1; j >= 0; j--) {
                    if (evMove.equals(sortedTopMoves.get(j))) {
                        // found original move, remove it
                        sortedTopMoves.remove(j);
                        oldIsRemoved = true;
                        break;
                    }
                }
                if (!oldIsRemoved) {
                    // it must be in the rest list
                    for (int j = 0; j < restMoves.size(); j++) {
                        if (evMove.equals(restMoves.get(j))) {
                            restMoves.remove(j);
                            break;
                        }
                    }
                }
                // move lower rest if top list became too big
                while (sortedTopMoves.size() > maxTopEntries) {
                    restMoves.add(
                            sortedTopMoves.remove(maxTopEntries) );
                }
                return false;
            }
        }
        //it was best!!
        sortedTopMoves.add(0, evMove);
        if (!oldIsRemoved) {
            // it must be in the rest list
            for (int j = 0; j < restMoves.size(); j++) {
                if (evMove.equals(restMoves.get(j))) {
                    restMoves.remove(j);
                    break;
                }
            }
        }
        // move lower rest if top list became too big
        while (sortedTopMoves.size() > maxTopEntries) {
            restMoves.add(
                    sortedTopMoves.remove(maxTopEntries) );
        }
        return true;
    }
**/

    //// Overrides

    @Override
    public String toString() {
        return "" +
                fromSq.toString()
            // for debugging only    + (isBasicallyALegalMove() ? "" : "'")
                + toSq.toString()
                + ( promotesTo!=EMPTY  ? Character.toLowerCase(fenCharFromPceType(promotesTo)) : "");
               // + "->" + eval.toString();
    }

    /**
     * std.equals(), hint: does not compare evaluation!
     * @param o other move to compare with
     * @return true if members from, to and promotesTo are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move)) return false;
        Move move = (Move) o;
        return fromSq.pos() == move.fromSq.pos() && toSq.pos() == move.toSq.pos() && promotesTo == move.promotesTo;
    }

    @Override
    public int hashCode() {
        return hashId();
    }


    //// simple info

    public int getDir() {
        return calcDirFromTo(from(), to());
    }

    public int promotesTo() {
        return promotesTo==EMPTY? QUEEN : promotesTo;
    }

    public Integer hashId() {
        return (fromSq.pos() << 8) + toSq.pos();
    }

    public int getSingleValueEval() {
        return eval.getEvalAt(0);
    }

    public boolean isMove() {
        return fromSq != null && toSq != null
                && fromSq.pos() >= 0 && fromSq.pos() < NR_SQUARES
                && toSq.pos() >= 0 && toSq.pos() < NR_SQUARES;
    }


    //// getter

    public int from() {
        return fromSq.pos();
    }

    public int to() {
        return toSq.pos();
    }

    public boolean isChecking() {
        return isCheckGiving;
    }

    public Evaluation getEval() {
        return eval;
    }

    public ChessPiece piece() {
        return myPiece;
    }

    public Square fromSq() {
        return fromSq;
    }

    public Square toSq() {
        return toSq;
    }

    public Square[] intermedSqs() {
        return intermedSqs;
    }


    //// setter

    public void setPromotesTo(int pceType) {
        promotesTo = pceType;
    }

    public void setisChecking() {
        isCheckGiving = true;
    }

    public Move setEval(Evaluation eval) {
        this.eval = eval;
        return this;
    }

    public boolean isALegalMoveNow() {
        return isALegalMoveAfter(piece().board().NOCHANGE);
    }

    public boolean isALegalMoveAfter(VBoardInterface bc) {
        return piece().posAfter(bc) == from()  // piece is still here
                && !isBlockedByKingPin(bc)
                && myPiece.isADirectMoveAfter(this, bc);
    }

    private boolean isBlockedByKingPin(VBoardInterface bc) {
        //TODO
        return false;
    }

    public Evaluation getSimpleMoveEval() {
        if (!piece().board().hasPieceOfColorAt(opponentColor(piece().color()), to()) || !myPiece.isADirectMoveAfter(this, piece().board().NOCHANGE))
            return new Evaluation();
        return new Evaluation(-piece().board().getPieceAt(to()).getValue(), 0);
    }

    public Evaluation getSimpleMoveEvalAfter(VBoardInterface bc) {
        assert (isALegalMoveAfter(bc));
        // consider context of already done moves.
       ChessPiece capturedPiece = bc.getPieceAt(to());
       if (capturedPiece == null)
           return new Evaluation();
       return new Evaluation(-capturedPiece.getValue(), 0);  // bc.futureLevel());
    }

    @Override
    public int compareTo(Move other) {
        //return Integer.compare(this.eval.getScore(), other.eval.getScore());
        if ( isBetterForColorThan(piece().color(), other) )
            return 1;
        if ( other.isBetterForColorThan(piece().color(), this) )
            return -1;
        return 0;
    }

}

