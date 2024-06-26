package de.ensel.waves;

import static de.ensel.chessbasics.ChessBasics.onSameFile;
import static de.ensel.chessbasics.ChessBasics.opponentColor;

public class ChessPiecePawn extends ChessPiece {
    public ChessPiecePawn(ChessBoard myChessBoard, int pceType, int pceID, int pcePos) {
        super(myChessBoard, pceType, pceID, pcePos);
    }

    @Override
    public Move getDirectMoveAfter(int toPos, VBoardInterface fb) {
        int fromPos = posAfter(fb);
        if (exceptionNotPossibleForPawnAfter(fromPos, toPos, fb))
            return null;
        return getMove(fromPos, toPos);
    }

    @Override
    public boolean isADirectMoveAfter(Move move, VBoardInterface fb) {
        if (exceptionNotPossibleForPawnAfter(move.from(), move.to(), fb))
            return false;
        return super.isADirectMoveAfter(move, fb);
    }


    private boolean exceptionNotPossibleForPawnAfter(int fromPos, int toPos, VBoardInterface fb) {
        if (onSameFile(fromPos, toPos)) {
            // pawn can only go there if square is empty
            if (!fb.isSquareEmpty(toPos))
                return true;
        }
        else {
            // pawn can only go there if it is a capture
            if (!fb.hasPieceOfColorAt(opponentColor(color()), toPos))
                return true;
        }
        return false;
    }

}
