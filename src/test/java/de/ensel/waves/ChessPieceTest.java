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
import org.junit.jupiter.params.ParameterizedTest;

import java.util.Comparator;

import static de.ensel.chessbasics.ChessBasics.*;
import static org.junit.jupiter.api.Assertions.*;

class ChessPieceTest {

    @org.junit.jupiter.api.Test
    void newPiece_Test() {
        ChessBoard board = new ChessBoard(FENPOS_EMPTY);
        board.spawnPieceAt(BISHOP, 9);
        assertEquals(BISHOP, board.getPieceAt(9).pieceType());
    }

    @org.junit.jupiter.api.Test
    void canMove_Test() {
        ChessBoard board = new ChessBoard(FENPOS_EMPTY);
        board.spawnPieceAt(BISHOP, 9);
        assertTrue(board.getPieceAt(9).canMove());
    }

    @org.junit.jupiter.api.Test
    void pos_Test() {
        ChessBoard board = new ChessBoard(FENPOS_EMPTY);
        int pos = 9;
        board.spawnPieceAt(BISHOP, pos);
        assertEquals(pos, board.getPieceAt(pos).pos());
    }

    @Test
    void isALegalMoveForMe_true_Test() {
        int pcePos = coordinateString2Pos("b7");
        int pce2Pos = coordinateString2Pos("e4");
        int pce3Pos = coordinateString2Pos("c6");
        // 8 ░░░   ░░░   ░░░   ░░░
        // 7    ░X░   ░░░   ░░░   ░░░   p1: rotated between B, b, Q and q
        // 6 ░░░   ░b+   ░░░   ░░░      p3: b or B in opposite color of p1
        // 5    ░░░   ░░+   ░░░   ░░░
        // 4 ░░░   ░░░   ░X+   ░░░      p2: same as p1 - to have own piece standing in the way
        //... A  B  C  D  E  F  G  H

        for (int pceType : new int[]{BISHOP, BISHOP_BLACK, QUEEN, QUEEN_BLACK}) {
            ChessBoard board = new ChessBoard(FENPOS_EMPTY);
            int opponentPceType = (isPieceTypeWhite(pceType) ? BISHOP_BLACK : BISHOP);
            board.spawnPieceAt(pceType, pcePos);
            ChessPiece p = board.getPieceAt(pcePos);
            assertTrue(p.isALegalMoveForMe(new SimpleMove("b7c6")));
            assertTrue(p.isALegalMoveForMe(new SimpleMove("b7e4")));

            board.spawnPieceAt(pceType, pce2Pos);
            assertTrue(p.isALegalMoveForMe(new SimpleMove("b7c6")));
            assertTrue(p.isALegalMoveForMe(new SimpleMove("b7d5")));

            board.spawnPieceAt(opponentPceType, pce3Pos);
            assertTrue(p.isALegalMoveForMe(new SimpleMove("b7c6")));
        }
    }

    @Test
    void isALegalMoveForMe_false_Test() {
        int pcePos = coordinateString2Pos("b7");
        int pce2Pos = coordinateString2Pos("e4");
        int pce3Pos = coordinateString2Pos("c6");
        // 8 ░░░   ░░░  -░░░   ░░░
        // 7    ░X░   -░░   ░░░   ░░░   p1: rotated between B, b, Q and q
        // 6 ░░░-  ░b░   ░░░   ░░░      p3: b or B in opposite color of p1
        // 5    ░░░   ░░-   ░░░   ░░░
        // 4 ░░░   ░░░   ░X-   ░░░      p2: same as p1 - to have own piece standing in the way
        // 3    ░░░   ░░░   ░░-   ░░░
        // 2 ░░-   ░░░   ░░░   ░░░
        // 1    ░░░   ░░░   ░░░   ░░░
        //    A  B  C  D  E  F  G  H
        for (int pceType : new int[]{BISHOP, BISHOP_BLACK, QUEEN, QUEEN_BLACK}) {
            ChessBoard board = new ChessBoard(FENPOS_EMPTY);
            int opponentPceType = (isPieceTypeWhite(pceType) ? BISHOP_BLACK : BISHOP);
            board.spawnPieceAt(pceType, pcePos);
            ChessPiece p = board.getPieceAt(pcePos);
            System.out.println("Testing: " + p);
            assertFalse(p.isALegalMoveForMe(new SimpleMove("b6c6")));
            assertFalse(p.isALegalMoveForMe(new SimpleMove("d7e6")));
            assertFalse(p.isALegalMoveForMe(new SimpleMove("b7a2")));
            assertFalse(p.isALegalMoveForMe(new SimpleMove("b7d8")));

            board.spawnPieceAt(pceType, pce2Pos);
            assertFalse(p.isALegalMoveForMe(new SimpleMove("b7e4")));
            assertFalse(p.isALegalMoveForMe(new SimpleMove("b7f3")));

            board.spawnPieceAt(opponentPceType, pce3Pos);
            assertFalse(p.isALegalMoveForMe(new SimpleMove("b7d5")));
            assertFalse(p.isALegalMoveForMe(new SimpleMove("b7f3")));
        }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {BISHOP, BISHOP_BLACK, QUEEN, QUEEN_BLACK})
    void getDirectMoveToEvaluation_Diag_Test(int pceType) {
        int p1Pos = coordinateString2Pos("b7");
        int p2Pos = coordinateString2Pos("e4");
        // 8 ░░░   ░░░   ░░░   ░░░
        // 7    ░X░   ░░░   ░░░   ░░░   p1: rotated between B, b, Q and q
        // 6 ░░░   ░░░   ░░░   ░░░
        // 5    ░░░   ░░░   ░░░   ░░░
        // 4 ░░░   ░░░   ░b░   ░░░      p2: b or B in opposite color of p1
        //... A  B  C  D  E  F  G  H

        ChessBoard board = new ChessBoard(FENPOS_EMPTY);
        board.spawnPieceAt(pceType, p1Pos);
        ChessPiece p1 = board.getPieceAt(p1Pos);
        int opponentPceType = (isPieceTypeWhite(pceType) ? BISHOP_BLACK : BISHOP);
        board.spawnPieceAt(opponentPceType, p2Pos);
        ChessPiece p2 = board.getPieceAt(p2Pos);

        assertEval4MoveTo(p1, p2Pos, -p2.getValue());
        assertEval4MoveTo(p2, p1Pos, -p1.getValue());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {ROOK, ROOK_BLACK, QUEEN, QUEEN_BLACK})
    void getDirectMoveToEvaluation_HV_Test(int pceType) {
        int p1Pos = coordinateString2Pos("b7");
        int p2Pos = coordinateString2Pos("b4");
        int p3Pos = coordinateString2Pos("e7");
        // 8 ░░░   ░░░   ░░░   ░░░
        // 7    ░X░   ░░░ N ░░░   ░░░   p1: rotated between R, r, Q and q
        // 6 ░░░   ░░░   ░░░   ░░░      p3: N of same color as p1
        // 5    ░░░   ░░░   ░░░   ░░░
        // 4 ░░░ b ░░░   ░░░   ░░░      p2: b or B in opposite color of p1
        //... A  B  C  D  E  F  G  H

        ChessBoard board = new ChessBoard(FENPOS_EMPTY);
        board.spawnPieceAt(pceType, p1Pos);
        int opponentBishopPceType = (isPieceTypeWhite(pceType) ? BISHOP_BLACK : BISHOP);
        board.spawnPieceAt(opponentBishopPceType, p2Pos);
        int sameColorKnightPceType = (isPieceTypeWhite(pceType) ? KNIGHT : KNIGHT_BLACK);
        board.spawnPieceAt(sameColorKnightPceType, p3Pos);
        ChessPiece p = board.getPieceAt(p1Pos);
        ChessPiece p2 = board.getPieceAt(p2Pos);
        ChessPiece p3 = board.getPieceAt(p3Pos);

        assertEval4MoveTo(p, p2Pos, -p2.getValue());
        assertCannotMoveTo(p2, p1Pos);
        assertEval4MoveTo(p2, p3Pos, -p3.getValue());
        assertCannotMoveTo(p, p3Pos);
    }


    @Test
    void getDirectMoveToEvaluation_Pawn_Test() {
        int p1Pos = coordinateString2Pos("b6");
        int p2Pos = coordinateString2Pos("e4");
        int p3Pos = coordinateString2Pos("a7");
        int p4Pos = coordinateString2Pos("b7");
        int p5Pos = coordinateString2Pos("f5");
        // 8 ░░░   ░░░   ░░░   ░░░
        // 7  N ░n░   ░░░   ░░░   ░░░   p3+p4: N and n standing in the way
        // 6 ░░░ P ░░░   ░░░   ░░░      p1: P to be tested
        // 5    ░░░   ░░░   ░n░   ░░░   p5: to be taken
        // 4 ░░░   ░░░   ░P░   ░░░      p2: P to be tested
        //... A  B  C  D  E  F  G  H

        ChessBoard board = new ChessBoard(FENPOS_EMPTY);
        board.spawnPieceAt(PAWN, p1Pos);
        board.spawnPieceAt(PAWN, p2Pos);
        board.spawnPieceAt(KNIGHT, p3Pos);
        board.spawnPieceAt(KNIGHT_BLACK, p4Pos);
        board.spawnPieceAt(KNIGHT_BLACK, p5Pos);
        ChessPiece p1 = board.getPieceAt(p1Pos);
        ChessPiece p2 = board.getPieceAt(p2Pos);
        ChessPiece p3 = board.getPieceAt(p3Pos);
        ChessPiece p4 = board.getPieceAt(p4Pos);
        ChessPiece p5 = board.getPieceAt(p5Pos);

        assertCannotMoveTo(p1, p3Pos);
        assertCannotMoveTo(p1, p4Pos);
        assertCannotMoveTo(p1, p1Pos+UPRIGHT);

        assertCannotMoveTo(p2, p2Pos+UPLEFT);
        assertEval4MoveTo(p2, p2Pos+UP, 0);
        assertEval4MoveTo(p2, p2Pos+UPRIGHT, -p5.getValue());
    }

    private static void assertCannotMoveTo(ChessPiece p, int pos) {
        System.out.print("Test: Impossible for " + p + " to go to " + squareName(pos) + ":");
        Evaluation result = p.getDirectMoveToEvaluation(pos);
        System.out.println("    Result = " + result + ".");
        assertEquals( null, result );
    }

    private static void assertEval4MoveTo(ChessPiece p, int pos, int targetEvalAt0 ) {
        System.out.print("Test: eval for " + p + " to " + squareName(pos) + ":");
        Evaluation result = p.getDirectMoveToEvaluation(pos);
        System.out.println("    Result=" + result + ".");
        assertEquals( targetEvalAt0, result == null ? null : result.getEvalAt(0) );
    }

    private static void assertCannotMoveToAfter(ChessPiece p, int pos, VBoard fb) {
        System.out.print("Test: Impossible for " + p + " to go to " + squareName(pos) + ":");
        Evaluation result = p.getMoveToEvalAfter(pos, fb);
        System.out.println("    Result = " + result + ".");
        assertEquals( null, result );
    }

    private static void assertEval4MoveToAfter(ChessPiece p, int pos, int targetEvalAt0, VBoard fb ) {
        System.out.print("Test: eval for " + p + " to " + squareName(pos) + ":");
        Evaluation result = p.getMoveToEvalAfter(pos, fb);
        System.out.println("    Result=" + result + " " + result.getReason() + ".");
        assertEquals( targetEvalAt0, result == null ? null : result.getEvalAt(0) );
    }


    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {ROOK, ROOK_BLACK, QUEEN, QUEEN_BLACK})
    void getMoveToEvaluationAfter_HV_Test(int pceType) {
        int p1Pos = coordinateString2Pos("b7");
        int p1ToPos = coordinateString2Pos("c7");
        int p2Pos = coordinateString2Pos("b4");
        int p3Pos = coordinateString2Pos("e7");
        // 8 ░░░   ░░░   ░░░   ░░░
        // 7    ░X->X ░░░ N ░░░   ░░░   p1: rotated between R, r, Q and q
        // 6 ░░░   ░░░   ░░░   ░░░      p3: N of same color as p1
        // 5    ░░░   ░░░   ░░░   ░░░
        // 4 ░░░ b ░░░   ░░░   ░░░      p2: b or B in opposite color of p1
        //... A  B  C  D  E  F  G  H

        ChessBoard board = new ChessBoard(FENPOS_EMPTY);
        board.spawnPieceAt(pceType, p1Pos);
        ChessPiece p1 = board.getPieceAt(p1Pos);
        int opponentBishopPceType = (isPieceTypeWhite(pceType) ? BISHOP_BLACK : BISHOP);
        board.spawnPieceAt(opponentBishopPceType, p2Pos);
        ChessPiece p2 = board.getPieceAt(p2Pos);
        int sameColorKnightPceType = (isPieceTypeWhite(pceType) ? KNIGHT : KNIGHT_BLACK);
        board.spawnPieceAt(sameColorKnightPceType, p3Pos);
        ChessPiece p3 = board.getPieceAt(p3Pos);

        // so far same setup as above, but evaluation different as now it is looked at the board after the move b7c7
        VBoard fb = VBoard.createNext(board, p1.getDirectMoveAfter(p1ToPos, board));
        p1Pos = p1ToPos;

        assertCannotMoveToAfter(p1, p2Pos, fb);
        assertCannotMoveToAfter(p2, p1Pos, fb);
        assertEval4MoveToAfter(p2, p3Pos, 0, fb);  // now "0" after evaluation learned about simple recaptures. was: -p3.getValue(), fb);
        assertCannotMoveToAfter(p1, p3Pos, fb);

        // additional test with two moves
        int p2ToPos = coordinateString2Pos("a5");
        // 8 ░░░   ░░░   ░░░   ░░░
        // 7    ░X->X ░░░ B ░░░   ░░░   p1: rotated between R, r, Q and q
        // 6 ░░░ / ░░░   ░░░   ░░░      p3: same as p1
        // 5  b.░░░   ░░░   ░░░   ░░░
        // 4 ░░░^b ░░░   ░░░   ░░░      p2: b or B in opposite color of p1
        //... A  B  C  D  E  F  G  H
        VBoard fb2 = VBoard.createNext(board, p2.getDirectMoveAfter(p2ToPos, fb));
        VBoard fb3 = VBoard.createNext(fb2, p1.getDirectMoveAfter(p1ToPos, fb2));
        assertEval4MoveToAfter(p2, p1Pos, -p1.getValue(), fb3);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {BISHOP, BISHOP_BLACK, QUEEN, QUEEN_BLACK})
    void legalMovesAfter_Test(int pceType) {
        int p1Pos = coordinateString2Pos("b7");
        int p1ToPos = coordinateString2Pos("c7");
        int p2Pos = coordinateString2Pos("b4");
        int p3Pos = coordinateString2Pos("e7");
        ChessBoard board = new ChessBoard(FENPOS_EMPTY);
        int opponentRookPceType = (isPieceTypeWhite(pceType) ? ROOK_BLACK :ROOK);
        int opponentKnightPceType = (isPieceTypeWhite(pceType) ? KNIGHT_BLACK : KNIGHT);
        board.spawnPieceAt(opponentRookPceType, p1Pos);
        ChessPiece p1 = board.getPieceAt(p1Pos);
        board.spawnPieceAt(pceType, p2Pos);
        ChessPiece p2 = board.getPieceAt(p2Pos);
        board.spawnPieceAt(opponentKnightPceType, p3Pos);
        ChessPiece p3 = board.getPieceAt(p3Pos);

        // test with two moves
        int p2ToPos = coordinateString2Pos("a5");
        // 8 ░░░   ░░░   ░░░   ░░░
        // 7    ░R->R ░░░ N ░░░   ░░░   p1: r or R in opposite color of p3
        // 6 ░░░ / ░░░   ░░░   ░░░      p3: N/n same color as p1
        // 5  x.░░░   ░░░   ░░░   ░░░
        // 4 ░░░^x ░░░   ░░░   ░░░      p2: rotated between B, b, Q and q
        //... A  B  C  D  E  F  G  H
        VBoard fb1 = VBoard.createNext(board, p2.getDirectMoveAfter(p2ToPos, board));
        VBoard fb2 = VBoard.createNext(fb1, p1.getDirectMoveAfter(p1ToPos, fb1));
        StringBuilder result = new StringBuilder();
        p2.legalMovesStreamAfter(fb2)
                .sorted(Comparator.comparingInt(Move::hashId))
                .forEach(m -> {
            result.append(" ");
            result.append(m.toString().split("-")[0]);
        });
        System.out.println("Result: " + result + ".");
        switch(pceType) {
            case BISHOP:
            case BISHOP_BLACK:
                assertEquals("a5c7 a5b6 a5b4 a5c3 a5d2 a5e1", result.toString().trim() );
                break;
            case QUEEN:
            case QUEEN_BLACK:
                assertEquals("a5a8 a5a7 a5c7 a5a6 a5b6 a5b5 a5c5 a5d5 a5e5 a5f5 a5g5 a5h5 a5a4 a5b4 a5a3 a5c3 a5a2 a5d2 a5a1 a5e1", result.toString().trim());
                break;
        }

        if (!isQueen(pceType))
            return;

        // fpr Queen test with four moves
        int p1ToPos2 = coordinateString2Pos("c3");
        int p2ToPos2 = coordinateString2Pos("e5");
        // 8 ░░░   ░░░   ░░░   ░░░
        // 7    ░r->r ░░░ n ░░░   ░░░   p1: r or R in opposite color of p3
        // 6 ░░░ / ░|░   ░░░   ░░░      p3: N/n same color as p1
        // 5  Q.-░-----░->Q ░░░   ░░░
        // 4 ░░░^Q ░|░   ░░░   ░░░      p2: rotated between Q and q
        // 3    ░░░ r ░░░   ░░░   ░░░
        // 2 ░░░   ░░░   ░░░   ░░░
        //... A  B  C  D  E  F  G  H
        VBoard fb3 = VBoard.createNext(fb2, p2.getDirectMoveAfter(p2ToPos2, fb2));
        VBoard fb4 = VBoard.createNext(fb3, p1.getDirectMoveAfter(p1ToPos2, fb3));
        StringBuilder result2 = new StringBuilder();
        p2.legalMovesStreamAfter(fb4)
                .sorted(Comparator.comparingInt(Move::hashId))
                .forEach(m -> {
                    result2.append(" ");
                    result2.append(m.toString().split("-")[0]);
                });
        System.out.println("Result: " + result + ".");
        assertEquals("e5b8 e5h8 e5c7 e5e7 e5g7 e5d6 e5e6 e5f6 e5a5 e5b5 e5c5 e5d5 e5f5 e5g5 e5h5 e5d4 e5e4 e5f4 e5c3 e5e3 e5g3 e5e2 e5h2 e5e1", result2.toString().trim());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {ROOK, ROOK_BLACK, QUEEN, QUEEN_BLACK})
    void posAfter_Test(int pceType) {
        int p1Pos = coordinateString2Pos("b7");
        int p2Pos = coordinateString2Pos("b4");
        int p3Pos = coordinateString2Pos("e7");
        // 8 ░░░   ░░░   ░░░   ░░░
        // 7    ░X->X ░░░ N ░░░   ░░░   p1: rotated between R, r, Q and q
        // 6 ░░░   ░░░   ░░░   ░░░      p3: N of same color as p1
        // 5    ░░░   ░░░   ░░░   ░░░
        // 4 ░░░ b ░░░   ░░░   ░░░      p2: b or B in opposite color of p1
        //... A  B  C  D  E  F  G  H

        ChessBoard board = new ChessBoard(FENPOS_EMPTY);
        board.spawnPieceAt(pceType, p1Pos);
        ChessPiece p1 = board.getPieceAt(p1Pos);
        int opponentBishopPceType = (isPieceTypeWhite(pceType) ? BISHOP_BLACK : BISHOP);
        board.spawnPieceAt(opponentBishopPceType, p2Pos);
        ChessPiece p2 = board.getPieceAt(p2Pos);
        int sameColorKnightPceType = (isPieceTypeWhite(pceType) ? KNIGHT : KNIGHT_BLACK);
        board.spawnPieceAt(sameColorKnightPceType, p3Pos);
        ChessPiece p3 = board.getPieceAt(p3Pos);

        // 1st move
        int p1ToPos = coordinateString2Pos("c7");
        VBoard fb1 = VBoard.createNext(board, p1.getDirectMoveAfter(p1ToPos, board));
        // 2nd move
        int p2ToPos = coordinateString2Pos("a5");
        VBoard fb2 = VBoard.createNext(fb1, p2.getDirectMoveAfter(p2ToPos, fb1));
        // 3rd move
        int p1ToPos2 = coordinateString2Pos("c6");
        VBoard fb3 = VBoard.createNext(fb2, p1.getDirectMoveAfter(p1ToPos2, fb2));
        // 8 ░░░   ░░░   ░░░   ░░░
        // 7    ░X-1. ░░░ B ░░░   ░░░   p1: rotated between R, r, Q and q
        // 6 ░░░   3X░   ░░░   ░░░      p3: same as p1
        // 5 2b.░░░   ░░░   ░░░   ░░░
        // 4 ░░░^b ░░░   ░░░   ░░░      p2: b or B in opposite color of p1
        //... A  B  C  D  E  F  G  H

        // repeat 3 times  (as psAfter in previous boards let's pce assume there was backtracking, but for unchanged boards it should still remember
        for (int i = 0; i < 3; i++) {
            System.out.println("Iteration: " + i);
            assertEquals(p1Pos, ((VBoardInterface) board).getPiecePos(p1));
            assertEquals(p2Pos, ((VBoardInterface) board).getPiecePos(p2));
            assertEquals(p3Pos, ((VBoardInterface) board).getPiecePos(p3));

            assertEquals(p1ToPos, ((VBoardInterface) fb1).getPiecePos(p1));
            assertEquals(p2Pos, ((VBoardInterface) fb1).getPiecePos(p2));
            assertEquals(p3Pos, ((VBoardInterface) fb1).getPiecePos(p3));

            assertEquals(p1ToPos, ((VBoardInterface) fb2).getPiecePos(p1));
            assertEquals(p2ToPos, ((VBoardInterface) fb2).getPiecePos(p2));
            assertEquals(p3Pos, ((VBoardInterface) fb2).getPiecePos(p3));

            assertEquals(p1ToPos2, ((VBoardInterface) fb3).getPiecePos(p1));
            assertEquals(squareName(p2ToPos), squareName(((VBoardInterface) fb3).getPiecePos(p2)));
            assertEquals(p3Pos, ((VBoardInterface) fb3).getPiecePos(p3));
        }

        // but again after asking for the past, what happens if we add another move in the future?
        assertEquals(p1ToPos, ((VBoardInterface) fb1).getPiecePos(p1));
        assertEquals(p2Pos, ((VBoardInterface) fb1).getPiecePos(p2));
        assertEquals(p3Pos, ((VBoardInterface) fb1).getPiecePos(p3));
        // 4th move
        int p2ToPos2 = coordinateString2Pos("b6");
        VBoard fb4 = VBoard.createNext(fb3, p2.getDirectMoveAfter(p2ToPos2, fb3));
        // 5th move
        int p1ToPos3 = coordinateString2Pos("b6");  // xb
        VBoard fb5 = VBoard.createNext(fb4, p1.getDirectMoveAfter(p1ToPos3, fb4));

        assertEquals(p1ToPos3, ((VBoardInterface) fb5).getPiecePos(p1));
        assertEquals(NOWHERE, ((VBoardInterface) fb5).getPiecePos(p2));
        assertEquals(p3Pos, ((VBoardInterface) fb3).getPiecePos(p3));
        // the past again
        assertEquals(p1Pos, ((VBoardInterface) board).getPiecePos(p1));
        assertEquals(p2ToPos, ((VBoardInterface) fb2).getPiecePos(p2));
        // and the actual again
        assertEquals(p1ToPos3, ((VBoardInterface) fb5).getPiecePos(p1));
        assertEquals(NOWHERE, ((VBoardInterface) fb5).getPiecePos(p2));
        assertEquals(p3Pos, ((VBoardInterface) fb3).getPiecePos(p3));

    }

}