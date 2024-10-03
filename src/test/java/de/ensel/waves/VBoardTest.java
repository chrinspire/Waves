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

import java.util.*;

import static de.ensel.chessbasics.ChessBasics.*;
import static de.ensel.waves.ChessBoard.DEBUGMSG_MOVEEVAL;
import static de.ensel.waves.VBoard.seeminglyLegalMoveIsReallyLegalOnBoard;
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

    @Test
    void calcMoveConsequences_Test() {
        ChessBoard board = new ChessBoard("3k4/1p6/8/8/1r2pK2/2R5/3N4/8 b - - 7 76");
        Move firstMove = board.getPieceAt(coordinateString2Pos("e4"))
                .getDirectMove(coordinateString2Pos("e3"));
        VBoard vBoard = board.createNext(firstMove);
        System.out.println( "" + firstMove.conseqs );

        String enabled = Arrays.toString( firstMove.conseqs.getEnabledMoves()
                .map(Move::toString).sorted().toArray() );
        //System.out.println( "Enabled moves: " + enabled );
        String blocked = Arrays.toString( firstMove.conseqs.getBlockedMoves()
                .map(Move::toString).sorted().toArray() );
        //System.out.println( "Blocked moves: " + blocked );
        String changed = Arrays.toString( firstMove.conseqs.getChangedEvalMoves()
                .map(Move::toString).sorted().toArray() );
        //System.out.println( "Changed move consequences: " + changed );
        
        assertEquals("[b4e4, b4f4, e3d2, e3e2]", enabled);
        assertEquals("[c3f3, c3g3, c3h3, e4d3, e4e3, e4f3]", blocked);
        assertEquals("[c3e3, d2e4, f4e3, f4e4]", changed);
    }
    
    @Test
    void calcMoveConsequences_multiple_Test() {
        ChessBoard board = new ChessBoard("3k4/1p6/8/8/1r2pK2/2R5/3N4/8 b - - 7 76");
        Set<Move> enabledMoves = new HashSet<>();
        for (int color = CIWHITE; color <= CIBLACK; color++) {
            board.getLegalMovesStream(color)
                    .filter(move -> seeminglyLegalMoveIsReallyLegalOnBoard(board, move))
                    .forEach( move -> {
                MoveConsequences.calcMoveConsequences(board, move);
                System.out.println( "" + move +": " + move.conseqs );
                enabledMoves.addAll(move.conseqs.getEnabledMoves(CIWHITE));
                enabledMoves.addAll(move.conseqs.getEnabledMoves(CIBLACK));
            });
        }
        System.out.println( "All enabled moves: " + enabledMoves );
        assertEquals(203, enabledMoves.size());

        Set<Move> enabledMoves2 = new HashSet<>();
        for (Move move : enabledMoves) {
            // move can now be non-legal (actually all, because they needed to be enabled by another move first...
            MoveConsequences.calcMoveConsequences(board, move);
            System.out.println( "    > " + move +": " + move.conseqs );
            if (move.conseqs != null) {
                enabledMoves2.addAll(move.conseqs.getEnabledMoves(CIWHITE));
                enabledMoves2.addAll(move.conseqs.getEnabledMoves(CIBLACK));
            }
        }
        System.out.println( "additionally enabled moves: " + enabledMoves2 );
        assertEquals(620, enabledMoves2.size());

        enabledMoves2.removeAll(enabledMoves);
        System.out.println( "thereof new: " +  enabledMoves2.size() );
        assertEquals(547, enabledMoves2.size());
    }

    @Test
    void SPIKE_calcMoveConsequences() {
        VBoard vBoard = new ChessBoard("8/8/8/8/8/Rp1r4/8/R7 b - - 7 76");
        // 3 ░R░ p ░░░ r ░░
        // 2    ░░░   ░░░
        // 1 ░R░   ░░░   ░░
        //... A  B  C  D  E
        Set<Move> enabledMoves = new HashSet<>();
        int turnCol = vBoard.getTurnCol();
        int oppCol = opponentColor(turnCol);
        ChessPiece mover = vBoard.getPieceAt(coordinateString2Pos("b3"));
        for (int color = CIWHITE; color <= CIBLACK; color++) {
                vBoard.getLegalMovesStream(color)
                    .filter(move -> seeminglyLegalMoveIsReallyLegalOnBoard(vBoard, move))
                    .forEach( move -> {
                MoveConsequences.calcMoveFullConseq(vBoard, move);
                System.out.println( "" + move +": " + move.conseqs );
                        enabledMoves.addAll(move.conseqs.getEnabledMoves(CIWHITE));
                        enabledMoves.addAll(move.conseqs.getEnabledMoves(CIBLACK));
            });
        }
        System.out.println( "All enabled moves: " + enabledMoves );
        assertEquals(203, enabledMoves.size());

        Set<Move> enabledMoves2 = new HashSet<>();
        for (Move move : enabledMoves) {
            // move can now be non-legal (actually all, because they needed to be enabled by another move first...
            MoveConsequences.calcMoveFullConseq(vBoard, move);
            System.out.println( "    > " + move +": " + move.conseqs );
            if (move.conseqs != null) {
                enabledMoves2.addAll(move.conseqs.getEnabledMoves(CIWHITE));
                enabledMoves2.addAll(move.conseqs.getEnabledMoves(CIBLACK));
            }
        }
        System.out.println( "additionally enabled moves: " + enabledMoves2 );
        assertEquals(620, enabledMoves2.size());

        enabledMoves2.removeAll(enabledMoves);
        System.out.println( "thereof new: " +  enabledMoves2.size() );
        assertEquals(547, enabledMoves2.size());
    }

    @Test
    void SPIKE_calcMoveMinTempi() {
        VBoard vBoard = new ChessBoard("8/8/8/8/8/Rp1r4/8/R7 b - - 7 76");
        // 3 ░R░ p ░░░ r ░░
        // 2    ░░░   ░░░
        // 1 ░R░   ░░░   ░░
        //... A  B  C  D  E
        final Set<Move>[] interestingMoves  = (Set<Move>[])new Set<?>[]{new HashSet<>(), new HashSet<>()};   // moves that are made legal or covering
        final Set<Move>[] potentialMoves  = (Set<Move>[])new Set<?>[]{new HashSet<>(), new HashSet<>()};   // moves that are made legal or covering
        final Set<Move>[] blockedMoves  = (Set<Move>[])new Set<?>[]{new HashSet<>(), new HashSet<>()};   // moves that are made legal or covering
        int turnCol = vBoard.getTurnCol();
        int oppCol = opponentColor(turnCol);
        ChessPiece mover = vBoard.getPieceAt(coordinateString2Pos("b3"));
        int minTempi = 0;

        // prefill sets with what is possible on board now (pretend opponent moves are "legal")
        retrievePotentialAndInterestingLegalMoves(vBoard,
                turnCol, 0, potentialMoves[turnCol], interestingMoves[turnCol]);
        retrievePotentialAndInterestingLegalMoves(vBoard,
                oppCol, 1, potentialMoves[oppCol], interestingMoves[oppCol]);
        //assertEquals(620, enabledMoves2.size());

        retrievePotentialAndInterestingFollowUpMoves(vBoard,
                turnCol, 2, potentialMoves, interestingMoves, blockedMoves);
        //Bug: pb3b2: MoveConsequences{ white: ... preparedMoves=[..., Rb1b4, Rb1b8,
        retrievePotentialAndInterestingFollowUpMoves(vBoard,
                oppCol, 3, potentialMoves, interestingMoves, blockedMoves);
        //assertEquals(547, enabledMoves2.size());
    }

    /**
     * used at current board to collect the first moves (which must be legal ones).
     * Both sets are for turnCol only and should be empty when called.
     * @param vBoard - the current board
     * @param turnCol - whos turn it is
     * @param potentialMoves - filled with all legal moves for [turnCol] except interesting moves
     * @param interestingMoves  - filled with legal moves for [turnCol] that look interesting (like capture or check)
     */
    private static void retrievePotentialAndInterestingLegalMoves(VBoard vBoard, int turnCol, int minTempi, Set<Move> potentialMoves, Set<Move> interestingMoves) {
        int oppCol = opponentColor(turnCol);
        boolean[] firstRound = { false };
        vBoard.getLegalMovesStream(turnCol)
                .filter(move -> seeminglyLegalMoveIsReallyLegalOnBoard(vBoard, move)) // at level 0+1 (first move for turnCOl and opponent) we only take really legal moves
                .forEach( move -> {
                    move.setMinTempi(minTempi);
                    //MoveConsequences.calcMoveFullConseq(vBoard, move);
                    System.out.println( "A. " + move +": " + move.conseqs );
                    if (/*minTempi == 0 ||*/ MoveConsequences.isInteresting(vBoard, move) )
                        interestingMoves.add(move);
                    else
                        potentialMoves.add(move);
                });
        System.out.println( "Interesting first moves: " + interestingMoves
                            + " All other potential moves: " + potentialMoves );
    }

    /**
     * used at current board for the first moves (which must be legal ones).
     * All three sets are double (so per color) and should be empty when called.
     * @param vBoard - the current board
     * @param turnCol - whos turn it is
     * @param potentialMoves - filled with all legal moves for [turnCol] except interesting moves
     * @param interestingMoves  - filled with legal moves for [turnCol] that look interesting (like capture or check)
     * @param blockedMoves - filled for [opponent color] with opponent's moves which are blocked by all my moves (should unually/mainly be empty)
     */
    private static void retrievePotentialAndInterestingFollowUpMoves(VBoard vBoard, int turnCol, int minTempi, Set<Move>[] potentialMoves, Set<Move>[] interestingMoves, Set<Move>[] blockedMoves) {
        int oppCol = opponentColor(turnCol);
        boolean[] firstRound = {false};
        final Set<Move>[] moreInterestingMoves = (Set<Move>[])new Set<?>[]{new HashSet<Move>(), new HashSet<Move>()};

        for (Move move : interestingMoves[turnCol]) {
            // note: move is probably non-legal (actually all but tempo==0, because they needed to be enabled by another move first...
            MoveConsequences.calcMoveFullConseq(vBoard, move);
            System.out.println( "    > " + move +": " + move.conseqs );
            if (move.conseqs != null) {
                moreInterestingMoves[CIWHITE].addAll(move.conseqs.getInterestingMoves(CIWHITE));
                moreInterestingMoves[CIBLACK].addAll(move.conseqs.getInterestingMoves(CIBLACK));
            }
        }
        System.out.println("All enabled moves: turnCol: " + potentialMoves[turnCol]
                + "; oppCol: " + potentialMoves[oppCol]);
        System.out.println("In any way blocked moves: turnCol: " + blockedMoves[turnCol]
                + "; oppCol: " + blockedMoves[oppCol]);
    }
}