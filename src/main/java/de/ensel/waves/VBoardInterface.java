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

import java.util.stream.Stream;

/**
 * VBoard virtually represents a ChessBoard. Like a Chessboard, it provides methods to 
 * query the state of the board, but relies on an underlying real ChessBoard, but makes variants 
 * of it, esp. for future states, after some moves.
 */
public interface VBoardInterface {
    // game / board
    boolean isGameOver();
    boolean isCheck(int color);

    // squares & pieces
    boolean hasPieceOfColorAt(int color, int pos);
    boolean isSquareEmpty(int pos);
    Stream<ChessPiece> getPieces();
    ChessPiece getPieceAt(int pos);
    int getPiecePos(ChessPiece pce);

    // other
    int futureLevel();

    boolean isCaptured(ChessPiece piece);
}


//b4d6->[300@2]!{
//    [ b4d6->[]!{[b4d6->]},
//      b7a8->[]!{[b4d6->[]!{[b4d6->]}, b7a8->]},
//      d6e7->[300@2]!{[b4d6->[]!{[b4d6->]}, b7a8->[]!{[b4d6->[]!{[b4d6->]}, b7a8->]}, d6e7->]/(w/o [schwarzer Springer on e7])},
//      a8a4->
//    ]}

