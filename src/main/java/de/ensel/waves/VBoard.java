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
    private final List<ChessPiece>[] capturedPieces = new List[2];
    ChessBoard board;


    private VBoard(ChessBoard baseBoard) {
        capturedPieces[0] = new ArrayList<>();
        capturedPieces[1] = new ArrayList<>();
        this.board = baseBoard;
    }

    private VBoard(VBoard origin) {
        capturedPieces[0] = new ArrayList<>();
        capturedPieces[1] = new ArrayList<>();
        this.board = origin.board;
        this.moves.addAll(origin.moves);
        for (int color = 0; color < 1; color++)
            this.capturedPieces[color].addAll(origin.capturedPieces[color]);
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
        usageCounter++;
        // if this new move captures a piece, let's remember that
        int opponentColor = opponentColor(move.piece().color());
        if (hasPieceOfColorAt(opponentColor, move.to())) {
            capturedPieces[opponentColor].add(getPieceAt(move.to()));
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
                .filter( p -> !(capturedPieces[CIWHITE].contains(p) || capturedPieces[CIBLACK].contains(p)) );
    }

    @Override
    public Stream<ChessPiece> getPieces(int color) {
        return board.getPieces(color)
                .filter( p -> !capturedPieces[color].contains(p) );
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
    public int getNrOfRepetitions() {
        //TODO
        return 0;
    }

    @Override
    public GameState gameState() {
        int turn = getTurnCol();
        if (hasLegalMoves(turn))
            return ONGOING;
        if (getNrOfPieces(turn) == 0   // last piece is gone -> only happens in test positions :-)
            || isCheck(turn))
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
        return (moves.size() % 2 == 0) ? board.getTurnCol() : opponentColor(board.getTurnCol());
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
        if ( !capturedPieces[CIWHITE].isEmpty() || !capturedPieces[CIBLACK].isEmpty()) {
            result.append(" (");
            result.append( Stream.concat(capturedPieces[CIWHITE].stream(), capturedPieces[CIBLACK].stream())
                    .map(p -> "x"+fenCharFromPceType(p.pieceType()) )
                    .collect(Collectors.joining(" ")));
            result.append(")");
        }
        return result.toString();
    }

    @Override
    public boolean isCaptured(ChessPiece pce) {
        return capturedPieces[pce.color()].contains(pce);
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

    @Override
    public int getNrOfPieces(int color) {
        return board.getNrOfPieces(color) - capturedPieces[color].size();
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

