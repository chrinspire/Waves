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
 *     This file borrows from small parts of class Square of TideEval v0.48.
 */

package de.ensel.waves;

import java.util.*;
import java.util.stream.Stream;

import static de.ensel.chessbasics.ChessBasics.*;
import static de.ensel.waves.ChessBoard.NO_PIECE_ID;
import static java.lang.Math.abs;

public class Square {
    final ChessBoard board;
    private final int myPos; // mainly for debugging and output
    private int myPieceID;  // the ID of the ChessPiece sitting directly on this square - if any, otherwise NO_PIECE_ID

    private int clashEvalResult = 0;
    boolean[] blocksCheckFor = new boolean[2];  // tells if a piece here can block a check here (for king with colorindex) by taking a checker of moving in the way

    List<Move>[] depMovesOver  = new List[2];
    List<Move>[] depMovesStart = new List[2];
    List<Move>[] depMovesEnd   = new List[2];
    //// Constructors

    Square(ChessBoard myChessBoard, int myPos) {
        this.board = myChessBoard;
        this.myPos = myPos;
        this.myPieceID = NO_PIECE_ID;
        for (int c = 0; c < 2; c++) {
            depMovesOver[c] = new ArrayList<>();
            depMovesStart[c] = new ArrayList<>();
            depMovesEnd[c] = new ArrayList<>();
        }
    }


    //// simple infp

    String getCoverageInfoByColorForLevel(final int color, final int level) {
        StringBuilder s = new StringBuilder(20);
        s.append(level).append(": -");
        return s.toString();
    }

    public int countDirectAttacksWithout2ndRowWithColor(int col) {
        return (int)(getSingleMovesToHere(col, board).count());
    }

    public List<ChessPiece> directAttackersWithout2ndRowWithColor(int col) {
        return null; //TODO
    }

    public boolean isEmpty() {
        return myPieceID() == NO_PIECE_ID;
    }

    public boolean isEmptyAfter(VBoardInterface bc) {
        return bc.isSquareEmpty(pos());
    }

    int myPieceType() {
        if (isEmpty())
            return EMPTY;
        return myPiece().pieceType();
    }

    public ChessPiece myPiece() {
        if (myPieceID()==NO_PIECE_ID)
            return null;
        return board.getPiece(myPieceID());
    }

    public boolean walkable4king(final int kingColor) {
        int acol = opponentColor(kingColor);
        return true; //TODO
//        return (!extraCoverageOfKingPinnedPiece(acol))
//                && countDirectAttacksWithout2ndRowWithColor(acol) == 0  // no really direct attacks
//                && ( countDirectAttacksWithColor(acol) == 0
//                || (countDirectAttacksWithColor(acol) == 1    // no x-ray through king
//                && coverageOfColorPerHops.get(1).get(colorIndex(acol)).get(0).getRawMinDistanceFromPiece()
//                .doesNotHaveThisSingleFromToAnywhereCondition(board.getKingPos(kingColor)) ) );
    }


    //// Overrides
    @Override
    public String toString() {
        return squareName(myPos);
    }

    public String toStringWithPce() {
        return squareName(myPos) + (isEmpty() ? "" : "(" + fenCharFromPceType(board.getPieceTypeAt(myPos)) + ")");
    }


    //// infos

    public boolean hasPieceOfColor(int color) {
        return board.hasPieceOfColorAt(color, pos());
    }

    public boolean hasPieceOfColorAfter(int color, VBoard bc) {
        return bc.hasPieceOfColorAt(color, pos());
    }

    ChessPiece cheapestDefenderHereForPieceAfter(ChessPiece pce, VBoard fb) {
        return getMovesToHere(pce.color())
                .filter(move ->
                        move.piece() != pce
                        && move.isCoveringAfter(fb))
                .map(Move::piece)
                .min(Comparator.comparingInt(p -> abs(p.getValue())))
                .orElse(null);
    }

//    ChessPiece cheapestAttackersOfColorToHereAfter(int color, VBoard fb) {
//        return getMovesToHere(color)
//                .filter(move -> move.isALegalMoveAfter(fb))
//                .map(Move::piece)
//                .min(Comparator.comparingInt(p -> abs(p.getValue())))
//                .orElse(null);
//    }

    //// getter

    /** getter for myPos, i.e. the position of this square
     * @return position of this square
     */
    int pos() {
        return myPos;
    }

    /**
     *
     * @return the ID of the ChessPiece sitting directly on this square - if any, otherwise NO_PIECE_ID
     */
    int myPieceID() {
        return myPieceID;
    }

    public boolean blocksCheckFor(int color) {
        return blocksCheckFor[color];
    }

    public int clashEval() {
        return clashEvalResult;
    }


    //// setter

    public void resetBlocksChecks() {
        blocksCheckFor[0] = false;
        blocksCheckFor[1] = false;
    }

    public void setBlocksCheckFor(int color) {
        blocksCheckFor[color] = true;
    }

    public boolean canMoveHere(int pceID) {
        return true; //TODO
    }

    void prepareNewPiece(int newPceID) {
        ; // TODO
    }

    void spawnPiece(int pid) {
        //the Piece had not existed so far, so prefill the move-net
        myPieceID = pid;
        // TODO
    }

    void movePieceHereFrom(int pid, int frompos) {
        //a new or existing Piece must correct its move-net
        if (myPieceID != NO_PIECE_ID) {
            // this piece is beaten...
            board.removePiece(myPieceID);
        }
        myPieceID = pid;
    }

    public void removePiece(int pceID) {
        if (myPieceID == pceID)
            myPieceID = NO_PIECE_ID;
    }

    void emptySquare() {
        /*VirtualPieceOnSquare vPce = getvPiece(myPieceID);
        if (vPce!=null)
            vPce.resetDistances();*/
        myPieceID = NO_PIECE_ID;
    }

    public int getDistanceToPieceId(int squareFromPceId) {
        return 0;
    }

    public void setupAddDependentMoveSlidingOver(Move m) {
        depMovesOver[m.piece().color()].add(m);
    }

    public void setupAddDependentMoveStart(Move m) {
        depMovesStart[m.piece().color()].add(m);
    }

    public void setupAddDependentMoveEnd(Move m) {
        depMovesEnd[m.piece().color()].add(m);
    }

    /**
     *
     * @return Stream of all Moves of all pieces that end here.
     */
    public Stream<Move> getMovesToHere(int color) {
        return depMovesEnd[color].stream();
    }

    /**
     * like getMovesToHere(), but filtered to those move coming directly from a piece origin.
     * @return Stream of all Moves of all pieces that can come here directly.
     */
    public Stream<Move> getSingleMovesToHere(final int color, final VBoard fb) {
        return depMovesEnd[color].stream()
                .filter(move -> move.isALegalMoveAfter(fb));   // not needed, is part of isALegal...: move -> move.from() == fb.getPiecePos(move.piece())
    }

    //    /**
//     * ! result is no copy, keep unmodified
//     * @return List of Moves of all pieces that could slide over this square.
//     */
//    public List<Move> getMovesSlidingOver() {
//        return depMovesOver;
//    }
//
//    /**
//     * ! result is no copy, keep unmodified
//     * @return List of Moves of all pieces that could start from here.
//     */
//    public List<Move> getMovesFromHere() {
//        return depMovesStart;
//    }
}
