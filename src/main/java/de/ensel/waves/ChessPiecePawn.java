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

import static de.ensel.chessbasics.ChessBasics.onSameFile;
import static de.ensel.chessbasics.ChessBasics.opponentColor;

public class ChessPiecePawn extends ChessPiece {
    public ChessPiecePawn(ChessBoard myChessBoard, int pceType, int pceID, int pcePos) {
        super(myChessBoard, pceType, pceID, pcePos);
    }

    @Override
    public Move getDirectMoveAfter(int toPos, VBoardInterface fb) {
        int fromPos = posAfter(fb);
        if (exceptionNotPossibleForPawnAfter(fromPos, toPos, fb))
            return null;
        return getMove(fromPos, toPos);
    }

    @Override
    public boolean isADirectMoveAfter(Move move, VBoardInterface fb) {
        if (exceptionNotPossibleForPawnAfter(move.from(), move.to(), fb))
            return false;
        return super.isADirectMoveAfter(move, fb);
    }


    private boolean exceptionNotPossibleForPawnAfter(int fromPos, int toPos, VBoardInterface fb) {
        if (onSameFile(fromPos, toPos)) {
            // pawn can only go there if square is empty
            if (!fb.isSquareEmpty(toPos))
                return true;
        }
        else {
            // pawn can only go there if it is a capture
            if (!fb.hasPieceOfColorAt(opponentColor(color()), toPos))
                return true;
        }
        return false;
    }

}
