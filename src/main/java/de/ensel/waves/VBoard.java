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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ensel.chessbasics.ChessBasics.*;
import static de.ensel.waves.VBoardInterface.GameState.*;

public class VBoard implements VBoardInterface {
    public static int usageCounter = 0;
    private final List<Move> moves = new ArrayList<>();
    ChessBoard baseBoard;
    // VBoardInterface preBoard;

    // superseding data
    final private int[] piecePos; // = new int[MAX_PIECES];

    // for debugging only
    private ChessPiece capturedPiece;
    private int captureEvalSoFar = 0;


    //// constructor + factory

    private VBoard(VBoardInterface preBoard) {
        // this.preBoard = preBoard;
        if (preBoard instanceof ChessBoard) {
            this.baseBoard = (ChessBoard)preBoard;
            piecePos = new int[MAX_PIECES];
            Arrays.fill(piecePos, POS_UNSET);
            return;
        }
        VBoard preVB = (VBoard)preBoard;
        this.baseBoard = preVB.baseBoard;
        this.moves.addAll(preVB.moves);
        this.piecePos = Arrays.copyOf(preVB.piecePos, preVB.piecePos.length);
        this.capturedPiece = null;
    }

    // factory, based on a VBoardInterface (VBoard or ChesBoard) + one move
    public static VBoard createNext(VBoardInterface preBoard, Move plusOneMove) {
        VBoard newVB = new VBoard(preBoard);
        newVB.addMove(preBoard, plusOneMove);
        return newVB;
    }

    private VBoard addMove(VBoardInterface preBoard, Move move) {
        usageCounter++;
        // if this new move captures a piece, let's remember that
        ChessPiece movingPiece = move.piece();
        int toPos = move.to();
        moves.add(move);   // needs to be called first, so depth will be correct from here on
        if (preBoard.hasPieceOfColorAt(opponentColor(move.piece().color()), toPos)) {
            capturedPiece = preBoard.getPieceAt(toPos);
            piecePos[capturedPiece.id()] = NOWHERE;
            captureEvalSoFar -= capturedPiece.getValue();
        }
        piecePos[movingPiece.id()] = toPos;
        return this;
    }


    ////

//    public List<Move> getMoves() {
//        return this.moves;
//    }

    @Override
    public Stream<ChessPiece> getPieces() {
        return baseBoard.getPieces()
                .filter( p -> getPiecePos(p) != NOWHERE );
    }

    @Override
    public ChessPiece getPieceAt(int pos) {
        int foundAt = lastMoveNrToPos(pos);
        // found or not: check if it (the found one or the original piece) did not move away since then...
        for (int i = foundAt+1; i < moves.size(); i++) {
            if (moves.get(i).from() == pos)
                return null;
        }
        if (foundAt >= 0)
            return moves.get(foundAt).piece();
        // orig piece is still there
        return baseBoard.getPieceAt(pos);
    }

    @Override
    public boolean hasPieceOfColorAt(int color, int pos) {
        ChessPiece p = getPieceAt(pos);
        if (p == null)
            return false;
        return p.color() == color;
    }

    @Override
    public int getNrOfRepetitions() {
        //TODO
        return 0;
    }

    @Override
    public int captureEvalSoFar() {
        return captureEvalSoFar;
    }

    @Override
    public GameState gameState() {
        int turn = getTurnCol();
        if (hasLegalMoves(turn))
            return ONGOING;
        if (isCheck(turn))
            return isWhite(turn) ? BLACK_WON : WHITE_WON;
        return DRAW;
    }

    @Override
    public boolean isCheck(int color) {
        // TODO
        return false;
    }

    @Override
    public int getTurnCol() {
        return (moves.size() % 2 == 0) ? baseBoard.getTurnCol() : opponentColor(baseBoard.getTurnCol());
    }

    @Override
    public boolean hasLegalMoves(int color) {
        for (Iterator<ChessPiece> it = getPieces().filter(p -> p.color() == color).iterator(); it.hasNext(); ) {
            if ( it.next().legalMovesAfter(this).findAny().orElse(null) != null )
                return true;
        }
        return false;
    }

//    @Override
//    public VBoardInterface preBoard() {
//        return preBoard;
//    }

//    @Override
//    public boolean hasPreBoard(VBoardInterface searchBoard) {
//        int d = depth();
//        if (searchBoard == null || searchBoard.depth() >= d)
//            return false;
//        VBoardInterface pre = this;
//        do {
//            pre = pre.preBoard();
//            d--;
//        } while (d > searchBoard.depth());  // go down to the right depth
//        if (pre == searchBoard)            // found
//            return true;
//        return false;
//    }

    @Override
    public int depth() {
        return moves.size();
    }

    @Override
    public int futureLevel() {
        // TODO/idea: do not count forcing moves
        return moves.size();
    }

    @Override
    public boolean isSquareEmpty(final int pos){
        // check moves backwards, if this pos has last been moved to or may be away from

        for (int i = moves.size() - 1; i >= 0; i--) {
            if (moves.get(i).from() == pos)
                return true;
            if (moves.get(i).to() == pos)
                return false;
        }
        // nothing changed here
        return baseBoard.isSquareEmpty(pos);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(moves.stream()
                .map(Move::toString)
                .collect(Collectors.joining(" ")));
        if (capturedPiece != null)
            result.append("(x"+fenCharFromPceType(capturedPiece.pieceType())+")" );
        return result.toString();
    }

//    @Override
//    public boolean isCaptured(ChessPiece pce) {
//        return capturedPieces.contains(pce);
//    }

//    public int getNrOfPieces(int color) {
//        return board.getNrOfPieces(color)
//                - (int)(capturedPieces.stream()
//                            .filter(p -> p.color() == color)
//                            .count());
//    }

    @Override
    public int getPiecePos(ChessPiece pce) {
        if (piecePos[pce.id()] == POS_UNSET) { // not yet set
            piecePos[pce.id()] = calcPiecePos(pce);
        }
        return piecePos[pce.id()];
    }

    private int calcPiecePos(ChessPiece pce) {
        // check moves backwards, if this piece has already moved
        for (int i = moves.size() - 1; i >= 0; i--) {
            if (moves.get(i).piece() == pce)
                return moves.get(i).to();
        }
        // it did not move
        return pce.pos();
    }


    //// internal helpers

    /**
     // look at moves to see which piece came here last.
     * @param pos position where to look
     * @return index in moves[] or -1 if not found
     */
    private int lastMoveNrToPos(int pos) {
        int foundAt = -1;
        // look at moves in backwards order
        for (int i = moves.size() - 1; i >= 0; i--) {
            if (moves.get(i).to() == pos) {
                foundAt = i;
                break;
            }
        }
        return foundAt;
    }
}

