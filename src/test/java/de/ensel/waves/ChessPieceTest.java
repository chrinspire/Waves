package de.ensel.waves;

import org.junit.jupiter.api.Test;

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

    @Test
    void getMoveEvaluation_Test() {
        int pcePos = coordinateString2Pos("b7");
        int pce2Pos = coordinateString2Pos("e4");
        int pce3Pos = coordinateString2Pos("c6");

        for (int pceType : new int[]{BISHOP, BISHOP_BLACK, QUEEN, QUEEN_BLACK}) {
            ChessBoard board = new ChessBoard(FENPOS_EMPTY);
            board.spawnPieceAt(pceType, pcePos);
            ChessPiece p = board.getPieceAt(pcePos);
            int opponentPceType = (isPieceTypeWhite(pceType) ? BISHOP_BLACK : BISHOP);
            board.spawnPieceAt(opponentPceType, pce2Pos);
            ChessPiece p2 = board.getPieceAt(pce2Pos);
            System.out.println("Testing: " + p + " taking " + p2 + ":");
            Evaluation result = p.getMoveEvaluation(pce2Pos);
            System.out.println("    Result = " + result + ".");
            assertEquals( result.getEvalAt(0) , -p2.getValue());
        }
    }

}