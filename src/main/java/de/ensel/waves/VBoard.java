package de.ensel.waves;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ensel.chessbasics.ChessBasics.*;

public class VBoard implements VBoardInterface {
    private final List<Move> moves = new ArrayList<>();
    private final List<ChessPiece> capturedPieces = new ArrayList<>();
    ChessBoard board;


    public VBoard(ChessBoard baseBoard) {
        this.board = baseBoard;
    }

    public VBoard(VBoard origin) {
        this.board = origin.board;
        this.moves.addAll(origin.moves);
        this.capturedPieces.addAll(origin.capturedPieces);
    }

    // factory, based on this + one Mmove
    public static VBoard createNext(VBoardInterface origin, Move plusOneMove) {
        VBoard newVB;
        if (origin instanceof VBoard)
            newVB = new VBoard((VBoard)origin);
        else
            newVB = new VBoard((ChessBoard)origin);
        newVB.addMove(plusOneMove);
        return newVB;
    }


    ////

    public VBoard addMove(Move move) {
        // if this new move captures a piece, let's remember that
        if (hasPieceOfColorAt(opponentColor(move.piece().color()),move.to())) {
            capturedPieces.add(getPieceAt(move.to()));
        }
        moves.add(move);
        return this;
    }

    public VBoard addMove(ChessPiece mover, int toPos) {
        addMove(mover.getDirectMoveAfter(toPos, this));
        return this;
    }


//    public List<Move> getMoves() {
//        return this.moves;
//    }

    @Override
    public Stream<ChessPiece> getPieces() {
        return board.getPieces()
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
        return board.getPieceAt(pos);
    }

    @Override
    public boolean hasPieceOfColorAt(int color, int pos) {
        ChessPiece p = getPieceAt(pos);
        if (p == null)
            return false;
        return p.color() == color;
    }

    @Override
    public boolean isCheck(int color) {
        // TODO
        return false;
    }

    @Override
    public boolean isGameOver() {
        // TODO
        return false;
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
        return board.isSquareEmpty(pos);
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
