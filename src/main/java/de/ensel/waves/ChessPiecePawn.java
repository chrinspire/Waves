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

import java.util.stream.Stream;

import static de.ensel.chessbasics.ChessBasics.*;

public class ChessPiecePawn extends ChessPiece {
    public ChessPiecePawn(ChessBoard myChessBoard, int pceType, int pceID, int pcePos) {
        super(myChessBoard, pceType, pceID, pcePos);
    }

    @Override
    public Move getDirectMoveAfter(int toPos, VBoard fb) {
        int fromPos = fb.getPiecePos(this);
        if (!movePossibleForPawnAfter(fromPos, toPos, fb))
            return null;
        return getMove(fromPos, toPos);
    }

    @Override
    public boolean isALegalMoveAfter(Move move, VBoard fb) {
        return movePossibleForPawnAfter(move.from(), move.to(), fb);
    }

    @Override
    public boolean isCoveringTargetAfter(Move move, VBoard fb) {
        return !onSameFile(move.to(), move.from());
    }

//    public boolean isADirectMoveAfter(Move move, VBoard fb) {
//        if (exceptionNotPossibleForPawnAfter(move.from(), move.to(), fb))
//            return false;
//        return super.isADirectMoveAfter(move, fb);
//    }

    private boolean movePossibleForPawnAfter(int fromPos, int toPos, VBoard fb) {
        return pawnmoveOnPiecePossible(fromPos, toPos, fb.getPieceAt(toPos));
    }

    /** checks pawn mobility to a square - considering it will be capturing or moving straight.
     *
     * @param fromPos
     * @param toPos
     * @param pceAtTo ChessPiece at toPos or null for empty.
     * @return if pawn can go from fromPos to toPos assuming piece pceAtTo - which can be null for no piece - is at the toPos.
     */
    public boolean pawnmoveOnPiecePossible(int fromPos, int toPos, ChessPiece pceAtTo) {
        //TODO? could in a even more general form (w pawn color but w/o ChessPiece, but type) be moved to ChessBasics
        if (onSameFile(fromPos, toPos))
            return pceAtTo == null;      // pawn can only go there if square is empty
        else
            return pceAtTo != null && pceAtTo.color() == opponentColor(color());  // pawn can only go there if it is a capture
    }

    @Override
    boolean coveringMoveToIsLegalAfter(Move move, VBoard fb) {
        return movePossibleForPawnAfter(move.from(), move.to(), fb);
    }

    @Override
    Stream<Move> allMovesFrom(int fromPos) {
        if (!canPawnOfColorReachPos(color(), pos(), fromPos))
            return Stream.empty();
        return super.allMovesFrom(fromPos);
    }

}
