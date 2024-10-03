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

import java.util.List;
import java.util.stream.Stream;

import static de.ensel.chessbasics.ChessBasics.chessBasicRes;

/**
 * VBoard virtually represents a ChessBoard. Like a Chessboard, it provides methods to 
 * query the state of the board, but relies on an underlying real ChessBoard, but makes variants 
 * of it, esp. for future states, after some moves.
 */
public interface VBoardInterface {
    /// const
    int         MAX_PIECES = 48; // 16 w + 18 b + max 16 Qs promoted from the 16 pawns...
    enum        GameState {
        NOTSTARTED, ONGOING, DRAW, WHITE_WON, BLACK_WON
    }

    //// game & board
    GameState   gameState       ();
    boolean     isCheck         ();
    List<Move>  getCheckingMoves(int color);
    void        addCheckingMove (Move m);

    int         getTurnCol      ();
    boolean     hasLegalMoves   (int color);
    int         getNrOfRepetitions();
    int         captureEvalSoFar();

    //// squares & pieces
    boolean     isSquareEmpty   (int pos);
    //int[]       countBlockerBetween(int fromExcl, int toExcl);
    int[]       countBlockerForMove(Move move);
    int         getSlidingDelay (int[] nrOfBlockers, Move slidingMove, int turnCol);
    boolean     hasPieceOfColorAt(int color, int pos);
    Stream<ChessPiece> getPieces();
    Stream<ChessPiece> getPieces(int color);
    int         getNrOfPieces   (int color);  // nr of pieces on the board, incl. kings - can get 0 for testboards without king
    ChessPiece  getPieceAt      (int pos);
    int         getPiecePos     (ChessPiece pce);


    ////
    int         depth           ();
    int         futureLevel     ();


    //// default implementations

    default String getGameStateDescription(GameState s) {
        String res = switch (s) {
            case WHITE_WON -> chessBasicRes.getString("state.whiteWins");
            case BLACK_WON -> chessBasicRes.getString("state.blackWins");
            case DRAW -> chessBasicRes.getString("state.remis");
            case NOTSTARTED -> chessBasicRes.getString("state.notStarted");
            case ONGOING -> chessBasicRes.getString("state.ongoing");
        };
//        if (getNrOfRepetitions() > 0)
//            res += " (" + getNrOfRepetitions() + " " + chessBasicRes.getString("repetitions") + ")";
        return res;
    }

    //// not needed at the moment:
    // VBoardInterface preBoard();
    // boolean hasPreBoard(VBoardInterface preBoard);
    // int getNrOfPieces(int color);
    // boolean isCaptured(ChessPiece piece);
}


//b4d6->[300@2]!{
//    [ b4d6->[]!{[b4d6->]},
//      b7a8->[]!{[b4d6->[]!{[b4d6->]}, b7a8->]},
//      d6e7->[300@2]!{[b4d6->[]!{[b4d6->]}, b7a8->[]!{[b4d6->[]!{[b4d6->]}, b7a8->]}, d6e7->]/(w/o [schwarzer Springer on e7])},
//      a8a4->
//    ]}

