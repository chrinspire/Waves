package de.ensel.waves;

import static de.ensel.chessbasics.ChessBasics.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello and welcome!");

        ChessBoard board = new ChessBoard("Simple-2-rook-testboard","2r5/8/8/8/8/8/8/2R5 w ---- - 0 1");
        System.out.printf("board: %s\n", board.toString());

    }
}