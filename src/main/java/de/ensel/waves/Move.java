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
public class Move {
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

//    public Move(Move origin) {
//        this.fromSq = origin.fromSq;
//        this.toSq = origin.toSq;
//        this.promotesTo = origin.promotesTo;
//        this.isCheckGiving = origin.isCheckGiving;
//        this.eval = origin.eval;
//    }


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


    void addEval(Evaluation addEval) {
        eval.addEval(addEval);
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

    boolean isBetterForColorThan(boolean color, Move other) {
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
                                            boolean color, int maxTopEntries,
                                            List<Move> restMoves) {
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


    //// Overrides

    @Override
    public String toString() {
        return "" +
                fromSq.toString()
            // for debugging only    + (isBasicallyALegalMove() ? "" : "'")
                + toSq.toString()
                + ( promotesTo!=EMPTY  ? Character.toLowerCase(fenCharFromPceType(promotesTo)) : "")
                + "->" + eval.toString();
    }

    /**
     * std.equals(), hint: does not compare isLegal flag
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

    //// setter

    public void setPromotesTo(int pceType) {
        promotesTo = pceType;
    }

    public void setisChecking() {
        isCheckGiving = true;
    }

    public void setEval(Evaluation eval) {
        this.eval = eval;
    }

    public boolean isALegalMoveNow() {
        return !isBlockedByKingPin() && isASingleMoveNow();
    }

    private boolean isBlockedByKingPin() {
        //TODO
        return false;
    }

    public boolean isASingleMoveNow() {
        assert( myPiece.id() == fromSq.myPieceID() );  // mov must be starting at my real piece
        if (toSq.hasPieceOfColor(myPiece.color()))
            return false; // target already occupied
        // loop over all intermediate Sqs, if they are free
        for (Square iSq : intermedSqs) {
            if (!iSq.isEmpty())
                return false;
        }
        return true;
    }

    public Evaluation getMoveEvaluation() {
        if (!piece().board().hasPieceOfColorAt(opponentColorIndex(piece().color()), to()) || !isASingleMoveNow())
            return new Evaluation();
        return new Evaluation(-piece().board().getPieceAt(to()).getValue(), 0);
    }

    public Evaluation getMoveEvaluation(List<Move> moveContext) {
        // TODO: consider context of already done moves! really check who is now left there.
        // This is just an incomplete first implementation working for 1-move contexts!:
        // if (any of) the moves has taken my piece or moved away as a target, then 0
        for (Move m : moveContext) {
            if (m.from() == toSq.pos() && m.to() == fromSq.pos())
                return new Evaluation();
        }
        // if my target has moved away, then diminish eval by the taken piece, but also needs to check if I now get beaten there (or may be not, as this is a later step...)
        return getMoveEvaluation();
    }
}

