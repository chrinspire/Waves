package de.ensel.waves;

import org.junit.jupiter.api.Test;

import static de.ensel.chessbasics.ChessBasics.BISHOP;
import static de.ensel.chessbasics.ChessBasics.FENPOS_EMPTY;
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
    void isALegalMoveForMe() {
        ChessBoard board = new ChessBoard(FENPOS_EMPTY);
        int pos = 9;
        board.spawnPieceAt(BISHOP, pos);
        ChessPiece p = board.getPieceAt(pos);
        assertTrue( p.isALegalMoveForMe(new SimpleMove("b7c6")));
        assertTrue( p.isALegalMoveForMe(new SimpleMove("b7e4")));
        assertFalse( p.isALegalMoveForMe(new SimpleMove("b7c7")));
        assertFalse( p.isALegalMoveForMe(new SimpleMove("b6c6")));
        assertFalse( p.isALegalMoveForMe(new SimpleMove("d7ec6")));
   }
}