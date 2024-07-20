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
import java.util.stream.Stream;

import static de.ensel.chessbasics.ChessBasics.*;
import static de.ensel.waves.Move.addMoveToSortedListOfCol;
import static de.ensel.waves.VBoardInterface.GameState.*;

public class VBoard implements VBoardInterface {
    public static final int NO_PIECE_ID = -1;  //todo: why not using EMPTY from ChessBasics piece types?
    public static int usageCounter = 0;
    ChessBoard baseBoard;    // a dependency to a subtype is unusual, but its my basis...
    // VBoardInterface preBoard;

    // superseding data
    private final int[] piecePos; // accessed by pceId
    private Move[] moves;
    private int nrOfMoves = 0;
    private final int[] countPieces = new int[2];

    // local caching
    private final List<Move>[] firstMovesOverSq = new List[NR_SQUARES];

    // for debugging only
    private ChessPiece capturedPiece;
    private int captureEvalSoFar = 0;


    //// constructor + factory

    protected VBoard() {
        this.baseBoard = (ChessBoard) this;
        this.piecePos = new int[MAX_PIECES];
        Arrays.fill(this.piecePos, POS_UNSET);
        // leave it null for now.  Arrays.setAll(this.firstMovesOverSq, i -> new ArrayList<Move>());
        moves = new Move[ChessEngineParams.MAX_SEARCH_DEPTH+3];  // + lookahead of primitive eval method
    }

    private VBoard(VBoard preBoard) {
        this.countPieces[CIWHITE] = preBoard.countPieces[CIWHITE];
        this.countPieces[CIBLACK] = preBoard.countPieces[CIBLACK];
        //this.firstMovesOverSq = Arrays.copyOf(preBoard.firstMovesOverSq, NR_SQUARES);
        //no copy for now: Arrays.setAll(this.firstMovesOverSq, i -> preBoard.firstMovesOverSq[i]);   // we take over all first piece moves of each square from the prBoard, and then exchange the changed ones.
        // this.preBoard = preBoard;
        if (preBoard instanceof ChessBoard) {
            this.baseBoard = (ChessBoard)preBoard;
            this.piecePos = new int[MAX_PIECES];
            Arrays.fill(this.piecePos, POS_UNSET);
            moves = new Move[ChessEngineParams.MAX_SEARCH_DEPTH+5];  // + lookahead of primitive eval method incl. recursive local clashes
            Arrays.fill(this.piecePos, POS_UNSET);
            return;
        }
        // else VBoard
        this.baseBoard = preBoard.baseBoard;
        this.nrOfMoves = preBoard.nrOfMoves;
        this.moves = preBoard.moves;               // this is not thread safe - it relies on clean backtracking, no "forking" path of valid VBoard existing in parallel. If needed, use createSafeCopy instead
        this.piecePos = Arrays.copyOf(preBoard.piecePos, preBoard.piecePos.length);
        this.capturedPiece = null;
    }

    public boolean isSafeCopy = false;
    public static VBoard createSafeCopy(VBoard other) {
        VBoard newVB = new VBoard(other);
        newVB.moves = Arrays.copyOf(other.moves, other.moves.length);
        newVB.isSafeCopy = true;
        return newVB;
    }

    // factory, based on a VBoardInterface (VBoard or ChesBoard) + one move
    public VBoard createNext(Move plusOneMove) {
        VBoard newVB = new VBoard(this);
        newVB.addMove(this, plusOneMove);
        return newVB;
    }

    public VBoard createNext(String plusOneMoveString) {
        VBoard newVB = new VBoard(this);
        newVB.addMove(this, new Move( newVB, plusOneMoveString));
        return newVB;
    }

    private void addMove(VBoard preBoard, Move move) {
        usageCounter++;
        // if this new move captures a piece, let's remember that
        ChessPiece movingPiece = move.piece();
        int toPos = move.to();
        if (nrOfMoves == moves.length) {
            // emergency, the moves array was too small
            Move[] oldMoves = moves;
            moves = new Move[nrOfMoves+5];
            for (int i = 0; i < nrOfMoves; i++)
                moves[i] = oldMoves[i];
        }
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
        //calcSingleMovesSlidingOver(move.toSq());
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

    final public int kingPos(final int color) {
        if (baseBoard.kingId[color] == NO_PIECE_ID)
            return NOWHERE;
        if (piecePos[baseBoard.kingId[color]] == POS_UNSET)
            return baseBoard.getPiece(baseBoard.kingId[color]).pos();
        return piecePos[baseBoard.kingId[color]];
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
        return getLegalMovesStream(color).findAny().orElse(null) != null;
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


    //// more complex infos
    /**
     * Checks if the game has ended and sets the eval in move accordingly.
     * @param move just the target to put the eval in.
     * @param debugOutputprefix
     * @return true if the game has ended, false otherwise
     */
    protected boolean checkAndSetGameEndEval(Move move, String debugOutputprefix) {
        if (!hasLegalMoves(opponentColor(move.piece().color()))) {  // be sure it was not null due to not wanting to calc deeper any more
            GameState state = gameState();
            if (state == DRAW) {
                //TODO: use -piece-value-sum or other board evaluation here, so we do not like draws with more pieces on the board
                Evaluation drawEval = new Evaluation(0, 0);
                move.addEval(drawEval);
            }
            else {
                move.addEval(new Evaluation(ChessBoard.checkmateEvalIn(getTurnCol(), depth()), 0));
            }
            move.getEval().setReason((this instanceof ChessBoard ? "" : this)
                    + " " + move + "!" + getGameStateDescription(gameState()) + "!");
            if (ChessBoard.DEBUGMSG_MOVESELECTION2 /* && upToNowBoard.futureLevel() == 0 */)
                ChessBoard.debugPrint(ChessBoard.DEBUGMSG_MOVESELECTION, debugOutputprefix
                        + "EOG:" /*+ move + " to " + move.getEval()
                        + " reason: " */ + move.getEval().getReason());
            return state != DRAW;
        }
        return false;
    }

    /**
     * p is not king-pinned or it is pinned but does not move out of the way.
     */
    public boolean moveIsNotBlockedByKingPin(ChessPiece p, int topos){
        if (isKing(p.pieceType()))
            return true;
        int sameColorKingPos = kingPos(p.color());
        if (sameColorKingPos < 0)
            return true;  // king does not exist... should not happen, but is part of some test-positions
        if (getPinnerOfPceToPos(p, sameColorKingPos) == null)
            return true;   // p is not king-pinned
        if (colorlessPieceType(p.pieceType()) == KNIGHT)
            return false;  // a king-pinned knight can never move away in a way that it still avoids the check
        // or it is pinned, but does not move out of the way.
        int king2PceDir = calcDirFromTo(sameColorKingPos, topos);
        int king2TargetDir = calcDirFromTo(sameColorKingPos, getPiecePos(p));
        return king2PceDir == king2TargetDir;
        // TODO?:  could also be solved by more intelligent condition stored in the distance to the king
    }

    public ChessPiece getPinnerOfPceToPos(ChessPiece pinnedPce, int targetPos) {
        if (isKing(pinnedPce.pieceType()))
            return null;
        int pPos = getPiecePos(pinnedPce);
        ChessPiece pinner = null;
        // manually run along the board (going over generic moves2here would be to slow I think
        int d = calcDirFromTo(targetPos, pPos);
        if (d == NONE)
            return null;  // pin not possible in strange directions...

        int pos = targetPos;
        // loop along the direction d until we hit something: the pinnedPce - and then the searched for pinner
        while (plusDirIsStillLegal(pos, d)) {
            pos += d;
            if (!isSquareEmpty(pos)) {
                if (getPieceAt(pos) != pinnedPce)
                    return null;    // there seems to be some other piece in between, so pinnedPce is not the real pinnedPce (at least there are more, which does not count here)
                break;
            }
        }
        if (!plusDirIsStillLegal(pos, d))
            return null;            // pinnedPce was not found or there is no more room for a pinner behind it
        while (plusDirIsStillLegal(pos, d)) {
            pos += d;
            if (!isSquareEmpty(pos)) {
                pinner = getPieceAt(pos);
                if (pinner.color() == pinnedPce.color() || !isSlidingPieceType(pinner.pieceType())
                        || !isCorrectSlidingPieceDirFromTo(pinner.pieceType(), pos, targetPos))
                    return null;    // pinner is not a piece that could pin here...
                break;   // found!
            }
        }
        return pinner;
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
        for (int i = 0; i < nrOfMoves; i++) {
            if (i>0)
                result.append(' ');
            result.append(moves[i].fromSq());
            result.append(moves[i].toSq());
        }
        if (capturedPiece != null)
            result.append("x"+fenCharFromPceType(capturedPiece.pieceType()) );
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

    public Stream<Move> getLegalMovesStream(int color) {
        return getPieces(color).flatMap(this::getSingleMovesStreamFromPce);
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

    /**
     * Selects the best capture moves for the given color.
     * @param color        select for which player
     * @param bestOppMoves where to add the moves
     * @param maxBestMoves max nr of sorted best capturing moves (rest is discarded)
     */
    void getBestPositiveCaptureMoves(int color, List<Move> bestOppMoves, int maxBestMoves) {
        List<Move> restOppMoves = new ArrayList<>();
        getLegalMovesStream(color)
                .filter(move -> hasPieceOfColorAt(opponentColor(color), move.to()) || move.isChecking())
                .forEach(move -> {
/*!*/       Evaluation oppEval = move.piece().getMoveEvalInclFollowUpAfter(move, this);
            if (oppEval.isGoodForColor(color))
                addMoveToSortedListOfCol( (new Move(move)).setEval(oppEval), bestOppMoves, color, maxBestMoves, restOppMoves);
        });
        if (ChessBoard.DEBUGMSG_MOVESELECTION2 && depth()< ChessBoard.DEBUGMSG_MOVESELECTION2_MAXDEPTH) // && upToNowBoard.futureLevel() == 0)
            ChessBoard.debugPrint(ChessBoard.DEBUGMSG_MOVESELECTION, "OppCounterCapture: "
                    + (bestOppMoves.isEmpty() ? "none" : bestOppMoves.get(0) + " " + bestOppMoves.get(0).getEval()
                    // + "(" + bestOppMoves.get(0).getEval().getReason() + ").")
                    //+ "Alternatives: " + Arrays.toString(bestOppMoves.toArray()) + ".");
                       ));
    }

    public void calcSingleMovesSlidingOver(final Square overSq) {
        firstMovesOverSq[overSq.pos()] = Stream.concat(overSq.depMovesOver[CIWHITE].stream(), overSq.depMovesOver[CIBLACK].stream())
                .filter(move -> move.from() == getPiecePos(move.piece()))   // automatically filters out moves of captured pieces, as their pos is POS_UNSET
                .toList();
    }

//    public Stream<Move> getSingleMovesStreamSlidingOver(final int color, final Square sq) {
//        return firstMovesOverSq[sq.pos()][color].stream();
//    }

    public Stream<Move> getSingleMovesStreamSlidingOver(final Square sq) {
        // old, when per color Streams are necessary
        //        return  Stream.concat( getSingleMovesStreamSlidingOver(CIWHITE, sq),
        //                               getSingleMovesStreamSlidingOver(CIBLACK, sq) );

        if (firstMovesOverSq[sq.pos()] == null)
            calcSingleMovesSlidingOver(sq);
        return firstMovesOverSq[sq.pos()].stream();

        // OR: w/o caching - had for now actually the same performance, becaus it was only called one per VBoard/Sq :-)
//        return Stream.concat(sq.depMovesOver[CIWHITE].stream(), sq.depMovesOver[CIBLACK].stream())
//                  .filter(move -> move.from() == getPiecePos(move.piece()));
    }
}

