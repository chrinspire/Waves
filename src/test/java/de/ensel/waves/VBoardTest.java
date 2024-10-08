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

import java.util.Arrays;
import java.util.List;

import static de.ensel.chessbasics.ChessBasics.*;
import static de.ensel.waves.ChessBoard.DEBUGMSG_MOVEEVAL;
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
        board.completePreparation();
        ChessPiece p1 = board.getPieceAt(p1Pos);
        ChessPiece p2 = board.getPieceAt(p2Pos);
        ChessPiece p3 = board.getPieceAt(p3Pos);

        // act
        Move p1move = p1.getMove(p1Pos,p3Pos);
        Move p2move = p2.getMove(p2Pos,p3Pos);
        VBoard vBoard = board.createNext(p2move );

        // assert
        assertEquals(true, vBoard.hasPieceOfColorAt(p2.color(), p3Pos));

        // repeat :-)
        VBoard vBoard2 = vBoard.createNext(p1move );
        assertEquals(true, vBoard2.hasPieceOfColorAt(p1.color(), p3Pos));
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
        board.completePreparation();
        ChessPiece p1 = board.getPieceAt(p1Pos);
        ChessPiece p2 = board.getPieceAt(p2Pos);
        ChessPiece p3 = board.getPieceAt(p3Pos);

        // act
        VBoard vBoard = board.createNext(p1.getMove(p1Pos, pos4) );

        // assert
        assertEquals(true, vBoard.hasPieceOfColorAt(p3.color(), p3Pos));
        assertEquals(false, vBoard.hasPieceOfColorAt( opponentColor(p3.color()), p3Pos));

        // repeat :-)
        VBoard vBoard2 = vBoard.createNext(p2.getMove(p2Pos, p3Pos) );
        assertEquals(true, vBoard2.hasPieceOfColorAt(p2.color(), p3Pos));
        assertEquals(false, vBoard2.hasPieceOfColorAt( opponentColor(p2.color()), p3Pos));

        // repeat :-)
        VBoard vBoard3 = vBoard2.createNext(p1.getMove(p1Pos, p3Pos) );
        assertEquals(true, vBoard3.hasPieceOfColorAt(p1.color(), p3Pos));
        assertEquals(false, vBoard3.hasPieceOfColorAt( opponentColor(p1.color()), p3Pos));
    }

    @Test
    void moveIsNotBlockedByKingPin_Test() {
        DEBUGMSG_MOVEEVAL = true;
        ChessBoard board = new ChessBoard("5k2/8/4Rrq1/8/5R2/P7/1P6/KN6 b - - 4 3");
        ChessPiece pinnedRook = board.getPieceAt(coordinateString2Pos("f6"));
        String res1 = Arrays.toString(board.getSingleMovesStreamFromPce(pinnedRook).toArray());
        assertEquals( "[f6f5, f6f4, f6f7]", res1 );

        ChessBoard board2 = new ChessBoard("5k2/8/3r2q1/8/1R6/P7/1P2r1R1/KN6 w - - 0 1");
        VBoard vBoard2 = board2
                .createNext("g2e2")
                .createNext("d6e6")
                .createNext("b4f4")
                .createNext("e6f6")
                .createNext("e2e6");
        ChessPiece pinnedRook2 = vBoard2.getPieceAt(coordinateString2Pos("f6"));
        String res2 = Arrays.toString(vBoard2.getSingleMovesStreamFromPce(pinnedRook2).toArray());
        assertEquals( "[f6f5, f6f4, f6f7]", res2 );

    }

    @Test
    void isCheck_atBaseBoard_Test() {
        ChessBoard board = new ChessBoard("5rnr/p1p2kpp/p4pb1/2NPN3/P2P4/1P2R3/5PPP/R5K1 b - - 5 25"); // check and only last possible move
        assertTrue(board.isCheck());
    }

    @Test
    void getCheckingMoves_atBaseBoard_Test() {
        ChessBoard board = new ChessBoard("5rnr/p1p2kpp/p4pb1/2NPN3/P2P4/1P2R3/5PPP/R5K1 b - - 5 25"); // check and only last possible move
        assertEquals("[e5f7]", Arrays.toString(board.getCheckingMoves(CIWHITE).toArray()));
        assertEquals("[]", Arrays.toString(board.getCheckingMoves(CIBLACK).toArray()));
    }

    @Test
    void isCheck_atVBoard_Test() {
        ChessBoard board = new ChessBoard("5rnr/p1p2kpp/p4pb1/2NPp3/P2P4/1P2RN2/5PPP/R5K1 w - - 5 25");
        VBoard vBoard = board.createNext("f3e5");// check and only last possible move
        assertTrue(vBoard.isCheck());
    }

    @Test
    void getCheckingMoves_atVBoard_Test() {
        ChessBoard board = new ChessBoard("5rnr/p1p2kpp/p4pb1/2NPp3/P2P4/1P2RN2/5PPP/R5K1 w - - 5 25");
        VBoard vBoard = board.createNext("f3e5");// check and only last possible move
        assertEquals("[e5f7]", Arrays.toString(vBoard.getCheckingMoves(CIWHITE).toArray()));
        assertEquals("[]", Arrays.toString(vBoard.getCheckingMoves(CIBLACK).toArray()));
    }

    @Test
    void hasLegalMoves_Test() {
        ChessBoard board = new ChessBoard("r2k3r/p1pp1ppp/8/8/3P4/N1P2b2/PP2qP1P/R3K3 w Q - 0 18");
        assertFalse(board.hasLegalMoves(CIWHITE));
    }

    @Test
    void hasLegalMoves_VBoard_Test() {
        ChessBoard board = new ChessBoard("r2k3r/p1pp1ppp/q7/8/3P4/N1P2b2/PP2NP1P/R3K3 b Q - 0 17");
        VBoard vBoard = board.createNext("a6e2");// check and only last possible move
        assertFalse(vBoard.hasLegalMoves(CIWHITE));
    }

}