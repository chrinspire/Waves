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

import org.junit.jupiter.api.Test;

import static de.ensel.chessbasics.ChessBasics.*;
import static org.junit.jupiter.api.Assertions.*;

class VBoardTest {

    @Test
    void addMoveNhasPieceOfColorAt_Test() {
        // arrange
        int p1Pos = coordinateString2Pos("b7");
        int p2Pos = coordinateString2Pos("b4");
        int p3Pos = coordinateString2Pos("e7");
        // 8 ░░░   ░░░   ░░░   ░░░
        // 7    ░q░   ░░░ n ░░░   ░░░   p1: q
        // 6 ░░░   ░░░   ░░░   ░░░      p3: n
        // 5    ░░░   ░░░   ░░░   ░░░
        // 4 ░░░ B ░░░   ░░░   ░░░      p2: B in opposite color of p1
        //... A  B  C  D  E  F  G  H

        ChessBoard board = new ChessBoard(FENPOS_EMPTY);
        board.spawnPieceAt(QUEEN_BLACK, p1Pos);
        int opponentBishopPceType = (isPieceTypeWhite(QUEEN_BLACK) ? BISHOP_BLACK : BISHOP);
        board.spawnPieceAt(opponentBishopPceType, p2Pos);
        int sameColorKnightPceType = (isPieceTypeWhite(QUEEN_BLACK) ? KNIGHT : KNIGHT_BLACK);
        board.spawnPieceAt(sameColorKnightPceType, p3Pos);
        ChessPiece p1 = board.getPieceAt(p1Pos);
        ChessPiece p2 = board.getPieceAt(p2Pos);
        ChessPiece p3 = board.getPieceAt(p3Pos);

        // act
        Move p1move = p1.getMove(p1Pos,p3Pos);
        Move p2move = p2.getMove(p2Pos,p3Pos);
        VBoard vBoard = VBoard.createNext(board, p2move );

        // assert
        assertEquals(true, vBoard.hasPieceOfColorAt(p2.color(), p3Pos));

        // repeat :-)
        vBoard.addMove( p1move );
        assertEquals(true, vBoard.hasPieceOfColorAt(p1.color(), p3Pos));
    }

    @Test
    void addMoveNhasPieceOfColorAt_2_Test() {
        // arrange
        int p1Pos = coordinateString2Pos("b7");
        int p2Pos = coordinateString2Pos("b4");
        int p3Pos = coordinateString2Pos("e7");
        int pos4 = coordinateString2Pos("e4");
        // 8 ░░░   ░░░   ░░░   ░░░
        // 7    ░q░   ░░░ n ░░░   ░░░   p1: q
        // 6 ░░░   ░░░   ░░░   ░░░      p3: n
        // 5    ░░░   ░░░   ░░░   ░░░
        // 4 ░░░ B ░░░   ░4░   ░░░      p2: B in opposite color of p1
        //... A  B  C  D  E  F  G  H

        ChessBoard board = new ChessBoard(FENPOS_EMPTY);
        board.spawnPieceAt(QUEEN_BLACK, p1Pos);
        int opponentBishopPceType = (isPieceTypeWhite(QUEEN_BLACK) ? BISHOP_BLACK : BISHOP);
        board.spawnPieceAt(opponentBishopPceType, p2Pos);
        int sameColorKnightPceType = (isPieceTypeWhite(QUEEN_BLACK) ? KNIGHT : KNIGHT_BLACK);
        board.spawnPieceAt(sameColorKnightPceType, p3Pos);
        ChessPiece p1 = board.getPieceAt(p1Pos);
        ChessPiece p2 = board.getPieceAt(p2Pos);
        ChessPiece p3 = board.getPieceAt(p3Pos);

        // act
        VBoard vBoard = VBoard.createNext(board, p1.getMove(p1Pos, pos4) );

        // assert
        assertEquals(true, vBoard.hasPieceOfColorAt(p3.color(), p3Pos));
        assertEquals(false, vBoard.hasPieceOfColorAt( opponentColor(p3.color()), p3Pos));

        // repeat :-)
        vBoard.addMove( p2.getMove(p2Pos, p3Pos) );
        assertEquals(true, vBoard.hasPieceOfColorAt(p2.color(), p3Pos));
        assertEquals(false, vBoard.hasPieceOfColorAt( opponentColor(p2.color()), p3Pos));

        // repeat :-)
        vBoard.addMove( p1.getMove(p1Pos, p3Pos) );
        assertEquals(true, vBoard.hasPieceOfColorAt(p1.color(), p3Pos));
        assertEquals(false, vBoard.hasPieceOfColorAt( opponentColor(p1.color()), p3Pos));
    }
}