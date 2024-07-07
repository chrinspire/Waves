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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ensel.chessbasics.ChessBasics.*;
import static de.ensel.waves.VBoardInterface.GameState.*;

public class VBoard implements VBoardInterface {
    public static int usageCounter = 0;
    ChessBoard baseBoard;
    // VBoardInterface preBoard;

    // superseding data
    private final int[] piecePos; // = new int[MAX_PIECES];
    private final Move[] moves;
    private int nrOfMoves = 0;
    private final int[] countPieces = new int[2];

    // for debugging only
    private ChessPiece capturedPiece;
    private int captureEvalSoFar = 0;


    //// constructor + factory

    protected VBoard() {
        this.piecePos = new int[MAX_PIECES];
        Arrays.fill(this.piecePos, POS_UNSET);
        moves = new Move[ChessEngineParams.MAX_SEARCH_DEPTH+3];  // + lookahead of primitive eval method
    }

    private VBoard(VBoard preBoard) {
        this.countPieces[CIWHITE] = preBoard.countPieces[CIWHITE];
        this.countPieces[CIBLACK] = preBoard.countPieces[CIBLACK];
        // this.preBoard = preBoard;
        if (preBoard instanceof ChessBoard) {
            this.baseBoard = (ChessBoard)preBoard;
            this.piecePos = new int[MAX_PIECES];
            Arrays.fill(this.piecePos, POS_UNSET);
            moves = new Move[ChessEngineParams.MAX_SEARCH_DEPTH+3];  // + lookahead of primitive eval method
            Arrays.fill(this.piecePos, POS_UNSET);
            return;
        }
        // else VBoard
        VBoard preVB = preBoard;
        this.baseBoard = preVB.baseBoard;
        this.nrOfMoves = preVB.nrOfMoves;
        this.moves = preVB.moves;
        this.piecePos = Arrays.copyOf(preVB.piecePos, preVB.piecePos.length);
        this.capturedPiece = null;
    }

    // factory, based on a VBoardInterface (VBoard or ChesBoard) + one move
    public static VBoard createNext(VBoard preBoard, Move plusOneMove) {
        VBoard newVB = new VBoard(preBoard);
        newVB.addMove(preBoard, plusOneMove);
        return newVB;
    }

    private void addMove(VBoard preBoard, Move move) {
        usageCounter++;
        // if this new move captures a piece, let's remember that
        ChessPiece movingPiece = move.piece();
        int toPos = move.to();
        moves[nrOfMoves++] = move;   // needs to be called first, so depth will be correct from here on
        if (preBoard.hasPieceOfColorAt(opponentColor(move.piece().color()), toPos)) {
            // it's a capture
            capturedPiece = preBoard.getPieceAt(toPos);
            piecePos[capturedPiece.id()] = NOWHERE;
            captureEvalSoFar -= capturedPiece.getValue();
            decNrOfPieces(capturedPiece.color());
        }
        else {
            capturedPiece = null;
        }
        piecePos[movingPiece.id()] = toPos;
    }



    //// getter

    public int getNrOfPieces(int color) {
        return countPieces[color];
    }

//    public List<Move> getMoves() {
//        return this.moves;
//    }

    @Override
    public Stream<ChessPiece> getPieces() {
        return baseBoard.getPieces()
                .filter( p -> getPiecePos(p) != NOWHERE );
    }

    @Override
    public Stream<ChessPiece> getPieces(int color) {
        return baseBoard.getPieces(color)
                .filter( p -> getPiecePos(p) != NOWHERE );
    }

    @Override
    public ChessPiece getPieceAt(int pos) {
        int foundAt = lastMoveNrToPos(pos);
        // found or not: check if it (the found one or the original piece) did not move away since then...
        for (int i = foundAt+1; i < nrOfMoves; i++) {
            if (moves[i].from() == pos)
                return null;
        }
        if (foundAt >= 0)
            return moves[foundAt].piece();
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
        if (getNrOfPieces(turn) == 0)        // only for test boards - capturing the last (i.e. on a board without king) is a win.
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
        return opponentColor(moves[nrOfMoves-1].piece().color());
    }

    @Override
    public boolean hasLegalMoves(int color) {
        for (Iterator<ChessPiece> it = getPieces(color).iterator(); it.hasNext(); ) {
            if ( it.next().legalMovesStreamAfter(this).findAny().orElse(null) != null )
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
        return nrOfMoves;
    }

    @Override
    public int futureLevel() {
        // TODO/idea: do not count forcing moves
        return nrOfMoves;
    }

    /**
     * Checks if the game has ended and sets the eval accordingly.
     * @param move
     * @param nextBoard
     * @param debugOutputprefix
     * @return true if the game has ended, false otherwise
     */
    protected boolean checkAndSetGameEndEval(Move move, VBoard nextBoard, String debugOutputprefix) {
        if (!nextBoard.hasLegalMoves(opponentColor(move.piece().color()))) {  // be sure it was not null due to not wanting to calc deeper any more
            GameState state = nextBoard.gameState();
            if (state == DRAW) {
                //TODO: use -piece-value-sum or other board evaluation here, so we do not like draws with more pieces on the board
                Evaluation drawEval = new Evaluation(0, 0);
                move.addEval(drawEval);
            }
            else {
                move.addEval(new Evaluation(ChessBoard.checkmateEvalIn(nextBoard.getTurnCol(), depth()), 0));
            }
            move.getEval().setReason((this instanceof ChessBoard ? "" : this)
                    + " " + move + "!" + nextBoard.getGameStateDescription(nextBoard.gameState()) + "!");
            if (ChessBoard.DEBUGMSG_MOVESELECTION /* && upToNowBoard.futureLevel() == 0 */)
                ChessBoard.debugPrintln(ChessBoard.DEBUGMSG_MOVESELECTION, debugOutputprefix
                        + "Reevaluated " /*+ move + " to " + move.getEval()
                        + " reason: " */ + move.getEval().getReason());
            return state != DRAW;
        }
        return false;
    }

    @Override
    public boolean isSquareEmpty(final int pos){
        // check moves backwards, if this pos has last been moved to or may be away from

        for (int i = nrOfMoves - 1; i >= 0; i--) {
            if (moves[i].from() == pos)
                return true;
            if (moves[i].to() == pos)
                return false;
        }
        // nothing changed here
        return baseBoard.isSquareEmpty(pos);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(Arrays.stream(moves)
                        .filter(Objects::nonNull)
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
    public int getPiecePos(final ChessPiece pce) {
        if (piecePos[pce.id()] == POS_UNSET) { // not yet set
            piecePos[pce.id()] = calcPiecePos(pce);
        }
        return piecePos[pce.id()];
    }

    private int calcPiecePos(ChessPiece pce) {
        // check moves backwards, if this piece has already moved
        for (int i = nrOfMoves - 1; i >= 0; i--) {
            if (moves[i].piece() == pce)
                return moves[i].to();
        }
        // it did not move
        return pce.pos();
    }


    //// setter

    protected void initNrOfPieces() {
        countPieces[CIWHITE] = 0;
        countPieces[CIBLACK] = 0;
    }

    protected void incNrOfPieces(int color) {
        countPieces[color]++;
    }

    protected void decNrOfPieces(int color) {
        countPieces[color]--;
    }


    //// internal helpers

    /**
     // look at moves to see which piece came here last.
     * @param pos position where to look
     * @return index in moves[] or -1 if not found
     */
    private int lastMoveNrToPos(final int pos) {
        int foundAt = -1;
        // look at moves in backwards order
        for (int i = nrOfMoves - 1; i >= 0; i--) {
            if (moves[i].to() == pos) {
                foundAt = i;
                break;
            }
        }
        return foundAt;
    }
}

