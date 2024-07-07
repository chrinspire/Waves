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
 *
 *     Base methods like doAndTestPuzzle() and many test cases have been borrowed
 *     from the test classes of of TideEval v0.48.
 */


package de.ensel.waves;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static de.ensel.chessbasics.ChessBasics.*;
import static de.ensel.chessbasics.ChessBasics.QUEEN_BLACK;
import static de.ensel.waves.ChessBoard.DEBUGMSG_MOVEEVAL;
import static de.ensel.waves.ChessEngineParams.*;
import static org.junit.jupiter.api.Assertions.*;

public class ChessBoardTest {
    //public static ChessEngineParams testEngParams = new ChessEngineParams(LEVEL_TEST_MID);
    static final int DEFAULT_TEST_ENGINE_LEVEL = LEVEL_TEST_MID;

    // temporary/debug tests: choose the one best move
    @Disabled
    @ParameterizedTest
    @CsvSource({
        //  "3qkbnr/p1pp1ppp/b3p3/2P5/1r1P4/4P3/P2B1PPP/RN2K1NR b KQk - 1 9, a1a1" // bug: ignore Q taking and checking on f7?
        "8/5k2/2R3r1/3K4/8/8/6R1/8 b - - 0 1, g6g2"
    })
    void DEBUG_ChessBoardGetBestMove_isBestMove_Test(String fen, String expectedBestMove) {
        doAndTestPuzzle(fen,expectedBestMove, "SimpleTest", true);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {ROOK, ROOK_BLACK, QUEEN, QUEEN_BLACK})
    void getBestMoveForCol_Test(int pceType) {
        // arrange
        int p1Pos = coordinateString2Pos("b7");
        int p2Pos = coordinateString2Pos("b4");
        int p3Pos = coordinateString2Pos("e7");
        // 8 ░░░   ░░░   ░░░   ░░░
        // 7    ░X░   ░░░ N ░░░   ░░░   p1: rotated between R, r, Q and q
        // 6 ░░░   ░░░   ░░░   ░░░      p3: N of same color as p1
        // 5    ░░░   ░░░   ░░░   ░░░
        // 4 ░░░ b ░░░   ░░░   ░░░      p2: b or B in opposite color of p1
        //... A  B  C  D  E  F  G  H

        // e7c6 b4d6 b7d7 d6c5 - before d7d4
        // 8 ░░░   ░░░   ░░░   ░░░
        // 7    ░.░   ░R░ . ░░░   ░░░
        // 6 ░░░   ░N░ . ░░░   ░░░
        // 5    ░░░ b ░░░   ░░░   ░░░
        // 4 ░░░ . ░░░ * ░░░   ░░░
        //... A  B  C  D  E  F  G  H
        //  w:
        //[_, N, _, X, _, _, _, _,
        // X, R, R, _, X, R, R, R,
        // _, _, _, R, _, _, _, _,
        // N, _, _, _, N, _, _, _,
        // _, N, _, N, _, _, _, _,  ...]
        // b:
        //[_, _, _, _, _, B, _, _,
        // B, _, _, _, B, _, _, _,
        // _, B, _, B, _, _, _, _,
        // _, _, _, _, _, _, _, _,
        // _, B, _, B, _, _, _, _,
        // B, _, _, _, B, _, _, _,
        // _, _, _, _, _, B, _, _,
        // _, _, _, _, _, _, B, _]
        //
        //[_, B, _, _, _, B, _, _,
        // _, _, B, _, B, _, _, _,
        // _, _, _, _, _, _, _, _,
        // _, _, B, _, B, _, _, _,
        // _, B, _, _, _, B, _, _,
        // B, _, _, _, _, _, B, _,
        // _, _, _, _, _, _, _, B,
        // _, _, _, _, _, _, _, _]
        //
        //[_, _, _, _, _, B, _, _,
        // _, _, _, _, B, _, _, _,
        // _, _, _, B, _, _, _, _,
        // B, _, B, _, _, _, _, _,
        // _, _, _, _, _, _, _, _,
        // B, _, B, _, _, _, _, _,
        // _, _, _, B, _, _, _, _,
        // _, _, _, _, B, _, _, _]


        ChessBoard board = new ChessBoard(FENPOS_EMPTY);
        board.setEngParams(new ChessEngineParams(DEFAULT_TEST_ENGINE_LEVEL));
        board.spawnPieceAt(pceType, p1Pos);
        int opponentBishopPceType = (isPieceTypeWhite(pceType) ? BISHOP_BLACK : BISHOP);
        board.spawnPieceAt(opponentBishopPceType, p2Pos);
        int sameColorKnightPceType = (isPieceTypeWhite(pceType) ? KNIGHT : KNIGHT_BLACK);
        board.spawnPieceAt(sameColorKnightPceType, p3Pos);
        ChessPiece p = board.getPieceAt(p1Pos);
        ChessPiece p2 = board.getPieceAt(p2Pos);
        ChessPiece p3 = board.getPieceAt(p3Pos);
        ChessEngineParams engParams = new ChessEngineParams(LEVEL_TEST_QUICK);
        board.completePreparation();

        // act
        Stream<Move> bestMoveForCol = board.getBestMovesForColAfter(CIWHITE, engParams, board, Integer.MIN_VALUE, Integer.MAX_VALUE);

        // assert
        StringBuilder result = new StringBuilder();
        bestMoveForCol
                //.sorted(Comparator.comparingInt(Move::hashId))
                .forEach(m -> {
                    result.append(" ");
                    if (DEBUGMSG_MOVEEVAL)
                        result.append("\n");
                    result.append(m.toString());
                });
        System.out.printf("Result: " + result + ".\n");
        assertNotNull(bestMoveForCol);
    }

    // choose the one best move
    @Disabled
    @ParameterizedTest
    @CsvSource({
            //simple ones
            "r1bqkbnr/pppppppp/B7/8/3PP3/2P5/PP3PPP/RNBQK1NR b KQk - 0 4, b7a6",
            "8/8/2r2Q2/2k5/4K3/8/5b2/8 w - - 0 1, f6f2",
            "8/8/2r2Q2/8/2k1K3/8/5b2/8 w - - 0 1, f6c6",
            "8/2r5/2k5/8/4KQ2/8/8/2b5 w - - 0 1, f4c1",
            "8/2r5/8/bk1N4/4K3/8/8/8 w - - 0 1, d5c7",
            "3r4/8/8/3Q2K1/8/8/n1k5/3r4 w - - 0 1, d5a2"
            , "r2qkb1r/pp2pppp/2p2n2/3P4/Q3PPn1/2N5/PP3P1P/R1B1KB1R w KQkq - 0 11, d5c6|h2h3|f2f3"  // //interresting for future move selection - best move remains best, even if second best h2h3 is chosen
            , "8/8/2r2Q2/2k5/4K3/8/5b2/8 w - - 0 1, f6f2" // king coverage: take free b
            , "8/8/2r2Q2/8/2k1K3/8/5b2/8 w - - 0 1, f6c6" // almost same, but no more king coverage: take free t
            , "8/2r5/8/bk1N4/4K3/8/8/8 w - - 0 1, d5c7"  // simple last take
            //
            , "rnbqk2r/pp2Bpp1/2pb3p/3p4/3P4/2N2N2/PPP1BPPP/R2QK2R b KQkq - 0 8, d8e7|d6e7" // better dont take with king
            //Forks:
            , "8/8/8/k3b1K1/8/4N3/3P4/8 w - - 0 1, e3c4"
            , "8/8/8/k3b1K1/3p4/4N3/3P4/8 w - - 0 1, e3c4"
            , "8/3p4/1q3N2/8/1k6/8/3P4/2K5 w - - 0 1, f6d5" // make fork instead of greedily taking a pawn
            // do not allow opponent to fork
            , "r1bq1rk1/pp1nbpp1/4pn1p/3p2B1/P2N4/2NBP3/1PP2PPP/R2Q1RK1 w - - 0 10, g5h4|g5f6"  // NOT g5f4 as it enables a p fork on e5. but g5h4|g5f6 - from https://lichess.org/EizzUkMY#18
            , "3q1b1r/rbp2ppp/1pk2n2/p2p4/P7/2N2BP1/1PQPPP1P/R1B2KNR b - - 0 17, c6d7" // NOT b7a6, not sufficient to block the abzugschach with rook fork
            //stop/escape check:
            , "rnb1kbnr/pppp1ppp/8/4p3/7q/2N1P3/PPPPP1PP/R1BQKBNR  w KQkq - 2 3, g2g3"
            /*TODO?*/   , "8/3pk3/R7/1R2Pp1p/2PPnKr1/8/8/8 w - - 4 43, f4f3|f4e3",  // f5  looks most attractive at the current first glance, but should be f4e3|f4f3 - and NOT f4f5 -> #1
            "r6k/pb4r1/1p1Qpn2/4Np2/3P4/4P1P1/P4P1q/3R1RK1 w - - 0 24, g1h2",
            "rnl1k2r/pppp1ppp/4p3/8/3Pn2q/5Pl1/PPP1P2P/RNLQKLNR  w KQkq - 0 7, h2g3",
            "r1lq1l1r/p1ppkppp/p4n2/1P3PP1/3N4/P4N2/2P1Q2P/R1L1K2R  b KQ - 4 17, e7d6|f6e4",
            "6k1/1b3pp1/p3p2p/Bp6/1Ppr2K1/P3R1PP/5n2/5B1R w - - 1 37, g4h5",  // https://lichess.org/bMwlzoVV
            "r1lq2r1/1p6/p3pl2/2p1N3/3PQ2P/2PLk3/PP4P1/5RK1  b - - 4 23, e3d2"
            , "3r3k/1bqpnBp1/p1n4R/1p6/4P3/8/PP1Q1PPP/2R3K1 b - - 0 22, g7h6" // not null! pg7xh6 not listed as valid move!
            , "3qk2r/2p1bpp1/1r6/pb1QPp1p/P2P4/2P2N1P/1P3PP1/R1B1K2R w KQk - 0 17 moves c3c4 e7b4, c1d2" // NOT 0-0, because it is check
            /*FUTURE?*/ , "r1b1k2r/ppp2pp1/2n1p3/b6p/2BPq3/P1N1nN2/1PPQ1PPP/R3K2R w KQkq - 0 12, f2e3|d2e3"  // just take n, from https://lichess.org/eTPndxVD/white#22 - but may be too complex
            // pawn endgames:
            , "8/P7/8/8/8/8/p7/8 b - - 0 1, a2a1q"
            , "8/P7/8/8/8/8/p7/8 w - - 0 1, a7a8q"
            // (ex)blunders from tideeval test games against local SF
            , "1r1q1rk1/2p2pbp/p1ppbnp1/4p3/1NP1P3/P1N1BP2/1P1Q2PP/R3KB1R b KQ - 3 14, d8d7|d8e8|e6d7" // cover forking square - NOT c6c5
            //// (ex)blunders from tideeval online games
            , "1rbqk2r/p1ppbp1p/2n1pnp1/4P3/1p1P1P2/2P1BN1P/PPQNB1P1/R4RK1 b - - 0 13, f6d5|f6h5"  // instead of blundering the knight with g6g5
            , "1rb2rk1/p1pp1pp1/1pn5/3p2p1/2B1Nb2/2P5/PP1N1PPP/R1B1K2R w KQ - 0 19, c4d5"  // bug was moving away with N and getting l beaten...
            , "rnbqkbnr/pp2ppp1/3p3p/2p3B1/8/2NP4/PPP1PPPP/R2QKBNR w KQkq - 0 4, g5d2|g5f4|g5c1|g5h4"  // B is attacked - move it away!
            // X ray
            , "r1b2rk1/pp4pp/8/2Q2p2/8/P4N2/1PP1qPPP/R3R1K1 b - - 1 16, e2a6" // NOT e2e8, thinking, the R would not cover through q by X-RAY
            , "r4rk1/1p3pp1/p1ppbnq1/4b3/2Q1P1P1/P1N1BB2/R1P5/4K1R1 w - - 2 29, c4d3|c4b4"

            // do not leave behind
            , "rnqk3r/pp2ppbp/5np1/1Rp5/P6P/3Q2P1/1P2PP2/1NB1K1NR b K - 0 12, b8d7"  // not q e8d7 which looks like magical right triangle keeps protecting the b7 pawn, but isn't because of moving into check blocking            // fake checkmate wrongly acoiden :-)
            , "r1bqk2r/pppnbp2/4p1P1/3pPn2/3P1P1P/2N2Q2/PPP1NB2/3RKB1R b Kkq - 0 17, f7g6|f5h4"
            //Warum nicht einfach die Figur nehmen?
            , "5rk1/p2qppb1/3p2pp/8/4P1b1/1PN1BPP1/P1Q4K/3R4 b - - 0 24, g4f3" // lxP statt Zug auf Feld wo eingesperrt wird,  https://lichess.org/7Vi88ar2/black#79
            , "r4rk1/pbqnbppp/1p2pn2/2Pp4/8/1P1BPN1P/PBPNQPP1/R4RK1 b - - 0 11, d7c5|b6c5|c7c5|e7c5"  //  - sieht auch noch nach komischen Zug aus, der etwas decken will aber per Abzug einen Angriff frei gibt.   https://lichess.org/dhVlMZEC/black
            , "r2qkb1r/ppp2ppp/2n1bn2/4p3/Q7/2N2NP1/PP2pPBP/R1B2RK1 w kq - 0 9, c3e2|f1e1"  // NOT f3d2, but just take pawn or save rook and take pawn later
            , "r1r3k1/pp3p1p/2b3p1/q2p4/N2BnP1P/1B5R/PPP1Q1P1/2K5 b - - 0 20, c6a4" // win N, NOT a5d2 losing n
            // qa5c3 acceptable for now as q is in danger behind N , "r1b1kbnr/3n1ppp/p3p3/qppp4/3P4/1BN1PN2/PPPB1PPP/R2QK2R b KQkq - 1 8, c5c4" // would have trapped B - https://lichess.org/Cos4w11H/black#15
            /*Todo*/           , "r1b1kbnr/3n1ppp/p3p3/q1pp4/Np1P4/1B2PN2/PPPB1PPP/R2QK2R b KQkq - 1 9, c5c4" // still same: c5c4 would have trapped B
            , "rn2qk1r/1pp4p/3p1p2/p2b1N2/1b1P4/6P1/PPPBPPB1/R2QK3 w Q - 0 16, g2d5"  // do not take the other b first, although it could give check
            , "8/pp6/8/4N3/6P1/2R5/2k1K3/8 b - - 0 61, c2c3"  // blunder was c2b1??
            // best move not so clear: "1r1qk1r1/p1p1bpp1/1p5p/4p3/1PQ4P/P3N1N1/1B1p1PP1/3K3R w - - 2 29, b2e5"   // https://lichess.org/ZGLMBHLF/white
            , "r1b1k2r/ppppnppp/2N2q2/2b5/4P3/2P1B3/PP3PPP/RN1QKB1R b KQkq - 0 7, c5f3|f6c6"  // from gamesC#1 2-fold-clash with only one solution
    })
    void ChessBoardGetBestMove_isBestMoveTest(String fen, String expectedBestMove) {
        doAndTestPuzzle(fen,expectedBestMove, "Simple  Test", false);
    }

    // choose the one best move in very simple scenarios
    @ParameterizedTest
    @CsvSource({
        //simple ones
        "8/8/2R5/8/8/8/2r5/8 w - - 0 1, c6c2"           // R takes
        ,"8/8/2R5/8/8/8/2r5/8 b - - 0 1, c2c6"          // r takes
        ,"8/8/6B1/8/8/8/2r5/8 w - - 0 1, g6c2"          // B takes
        ,"8/8/2B3B1/8/8/8/2r5/8 b - - 0 1, c2c6"        // r flees and takes
        ,"8/5k2/2R3r1/3K4/8/8/6R1/8 b - - 0 1, g6g2"    // r needs to take the right (uncovered) R
        ,"5k2/8/3r2q1/8/1R6/P7/1P2r1R1/K7 w - - 0 1, g2e2"      // take q and lose R? better take r (+#)
    })
    void ChessBoardGetBestMove_Basics_Test(String fen, String expectedBestMove) {
        doAndTestPuzzle(fen,expectedBestMove, "Simple  Test", false);
    }

    // choose the one best move in simple scenarios, but with many pieces
    @ParameterizedTest
    @CsvSource({
        "r1bqkbnr/1ppppppp/p1n5/3N4/1P1PPB2/P7/2P2PPP/R2QKBNR b KQk - 1 7, d7d6"      // need to block an attacked piece (where is a fork at the same time)
        ,"r1b1kbnr/pppp1ppp/4p3/2PP4/1P1n3q/P4N2/4PPPP/RNBQKB1R b KQkq - 2 7, d4f3"
        ,"1rbqkbnr/p1pppppp/p7/8/3P4/4P3/PPPB1PPP/RN1QK1NR b KQk - 1 4, b8b2"         // take free pawn
    })
    void ChessBoardGetBestMove_moreComplex_Test(String fen, String expectedBestMove) {
        doAndTestPuzzle(fen,expectedBestMove, "Simple  Test", false);
    }


    public static void doAndTestPuzzle(String fen, String expectedMoves, String themes, boolean debugmoves) {
        //ChessBoard.DEBUGMSG_MOVEEVAL = debugmoves;
        ChessBoard.DEBUGMSG_MOVESELECTION = debugmoves;
        ChessBoard board = new ChessBoard(themes, fen, new ChessEngineParams(DEFAULT_TEST_ENGINE_LEVEL));
        String[] splitt = expectedMoves.trim().split(" ", 2);
        if (splitt.length==2 && splitt[1]!=null && splitt[1].length()>0) {
            // if expected moves is a series of moves, then the very first is still before the puzzle and must be moved first...
            board.doMove(splitt[0]);
            expectedMoves = splitt[1];
        }
        else
            expectedMoves = splitt[0];
        // get calculated best move
        System.out.print("Searching Best move for Board: " + board.getBoardName() + ": " + board.getBoardFEN() + " .  ");
        Move bestMove = board.getBestMove();
        ChessBoard.DEBUGMSG_MOVEEVAL = false;
        ChessBoard.DEBUGMSG_MOVESELECTION = false;

        if (bestMove==null) {
            System.out.println("--> Failed on board " + board.getBoardName() + ": " + board.getBoardFEN() + ": No move?");
            assertEquals(Arrays.toString(expectedMoves.split("\\|")) , "" );
        }

        // check if correct
        boolean found = false;
        for (String expectedString : expectedMoves.split("\\|")) {
            if (expectedString.length()>4)
                expectedString = (new SimpleMove(expectedString.substring(0, 5).trim())).toString();
            //System.out.println("opt="+expectedString+".");
            if (expectedString.equalsIgnoreCase(bestMove.toString())) {
                found = true;
                break;
            }
        }
        if (!found) {
            System.out.println("--> Failed on board " + board.getBoardName() + ": " + board.getBoardFEN() + ": "
                    + bestMove.toString() + " (expected: " + expectedMoves + ")");
            assertEquals(Arrays.toString(expectedMoves.split("\\|")) , bestMove.toString() );
        }
        System.out.println("--> Passed with move " + bestMove + " ("+ board.countCalculatedBoards+" boards evaluated).");
    }



}
