package de.ensel.waves;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ensel.chessbasics.ChessBasics.*;
import static de.ensel.waves.VBoardInterface.GameState.*;

public class VBoard implements VBoardInterface {
    public static int usageCounter = 0;
    private final List<Move> moves = new ArrayList<>();
    private final List<ChessPiece> capturedPieces = new ArrayList<>();
    VBoardInterface preBoard;
    ChessBoard baseBoard;

    private VBoard(VBoardInterface preBoard) {
        this.preBoard = preBoard;
        if (preBoard instanceof ChessBoard) {
            this.baseBoard = (ChessBoard)preBoard;
            return;
        }
        VBoard preVB = (VBoard)preBoard;
        this.baseBoard = preVB.baseBoard;
        this.moves.addAll(preVB.moves);
        this.capturedPieces.addAll(preVB.capturedPieces);
    }

    // factory, based on this + one Mmove
    public static VBoard createNext(VBoardInterface origin, Move plusOneMove) {
        VBoard newVB = new VBoard(origin);
        newVB.addMove(plusOneMove);
        return newVB;
    }


    ////

    public VBoard addMove(Move move) {
        usageCounter++;
        // if this new move captures a piece, let's remember that
        ChessPiece movingPiece = getPieceAt(move.from());
        int toPos = move.to();
        doAddMoveInternals(move, movingPiece, toPos);
        return this;
    }

    public VBoard addMove(ChessPiece mover, int toPos) {
        Move move = mover.getDirectMoveAfter(toPos, this);
        doAddMoveInternals(move, mover, toPos);
        return this;
    }

    private void doAddMoveInternals(Move move, ChessPiece movingPiece, int toPos) {
        moves.add(move);   // needs to be called first, so depth will be correct from here on
        if (preBoard.hasPieceOfColorAt(opponentColor(move.piece().color()), toPos)) {
            ChessPiece capturedPiece = preBoard.getPieceAt(toPos);
            capturedPiece.setPosAfter(NOWHERE, this);
            capturedPieces.add(capturedPiece);
        }
        movingPiece.setPosAfter(toPos, this);
    }


//    public List<Move> getMoves() {
//        return this.moves;
//    }

    @Override
    public Stream<ChessPiece> getPieces() {
        return baseBoard.getPieces()
                .filter( p -> !capturedPieces.contains(p) );
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

    @Override
    public VBoardInterface preBoard() {
        return preBoard;
    }

    @Override
    public boolean hasPreBoard(VBoardInterface searchBoard) {
        int d = depth();
        if (searchBoard == null || searchBoard.depth() >= d)
            return false;
        VBoardInterface pre = this;
        do {
            pre = pre.preBoard();
            d--;
        } while (d > searchBoard.depth());  // go down to the right depth
        if (pre == searchBoard)            // found
            return true;
        return false;
    }

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
        if (!capturedPieces.isEmpty()) {
            result.append(" (");
            result.append(capturedPieces.stream()
                    .map(p -> "x"+fenCharFromPceType(p.pieceType()) )
                    .collect(Collectors.joining(" ")));
            result.append(")");
        }
        return result.toString();
    }

    @Override
    public boolean isCaptured(ChessPiece pce) {
        return capturedPieces.contains(pce);
    }

//    public int getNrOfPieces(int color) {
//        return board.getNrOfPieces(color)
//                - (int)(capturedPieces.stream()
//                            .filter(p -> p.color() == color)
//                            .count());
//    }

    @Override
    public int getPiecePos(ChessPiece pce) {
        assert(!isCaptured(pce));
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

