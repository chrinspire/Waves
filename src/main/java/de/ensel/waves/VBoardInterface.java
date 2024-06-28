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

import static de.ensel.chessbasics.ChessBasics.chessBasicRes;

/**
 * VBoard virtually represents a ChessBoard. Like a Chessboard, it provides methods to 
 * query the state of the board, but relies on an underlying real ChessBoard, but makes variants 
 * of it, esp. for future states, after some moves.
 */
public interface VBoardInterface {
    // game / board
    enum GameState {
        NOTSTARTED, ONGOING, DRAW, WHITE_WON, BLACK_WON
    }
    GameState gameState();
    boolean isCheck(int color);
    int getTurnCol();
    boolean hasLegalMoves(int color);

        // squares & pieces
    boolean hasPieceOfColorAt(int color, int pos);

    int getNrOfRepetitions();

    boolean isSquareEmpty(int pos);
    Stream<ChessPiece> getPieces();

    StackedList<ChessPiece> capturedPieces();

    ChessPiece getPieceAt(int pos);
    int getPiecePos(ChessPiece pce);

    // other
    int futureLevel();

    boolean isCaptured(ChessPiece piece);

    default String getGameStateDescription() {
        GameState s = gameState();
        String res = switch (s) {
            case WHITE_WON -> chessBasicRes.getString("state.whiteWins");
            case BLACK_WON -> chessBasicRes.getString("state.blackWins");
            case DRAW -> chessBasicRes.getString("state.remis");
            case NOTSTARTED -> chessBasicRes.getString("state.notStarted");
            case ONGOING -> chessBasicRes.getString("state.ongoing");
        };
        if (getNrOfRepetitions() > 0)
            res += " (" + getNrOfRepetitions() + " " + chessBasicRes.getString("repetitions") + ")";
        return res;
    }

    //// not needed:
    // int getNrOfPieces(int color);
}


//b4d6->[300@2]!{
//    [ b4d6->[]!{[b4d6->]},
//      b7a8->[]!{[b4d6->[]!{[b4d6->]}, b7a8->]},
//      d6e7->[300@2]!{[b4d6->[]!{[b4d6->]}, b7a8->[]!{[b4d6->[]!{[b4d6->]}, b7a8->]}, d6e7->]/(w/o [schwarzer Springer on e7])},
//      a8a4->
//    ]}

