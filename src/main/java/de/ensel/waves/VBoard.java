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
import static java.lang.Math.abs;
import static java.lang.Math.max;

public class VBoard implements VBoardInterface {
    public static final int NO_PIECE_ID = -1;  //todo: why not using EMPTY from ChessBasics piece types?
    public static int usageCounter = 0;
    ChessBoard baseBoard;    // a dependency to a subtype is unusual, but its my basis...
    // VBoardInterface preBoard;

    // superseding data
    private int[] piecePos; // accessed by pceId
    private Move[] moves;
    private int countMovesSinceBaseBoard = 0;
    private int[] countPieces = new int[2];
    private int captureEvalSoFar = 0;
    private final List<Move>[] checkingMoves = new ArrayList[2];

    // local caching
    private List<Move>[]   firstMovesOverSq;
    private List<Move>[][] firstMovesToSq;      // not just caching, but needed to be calculated early to see if move sets check
    private List<Move>[]   firstMovesFromPce;
    boolean all1stMovesFromPceComplete;

    // for debugging only
    private ChessPiece capturedPiece;


    //// constructor + factory

    protected VBoard() {
        this.baseBoard = (ChessBoard)this;
        resetVBaseBoard();
    }

    /** Not a copy constructor(!), but to generate a follow-up board, thus it needs to be followed by addMove().
     * Used for internally creating a new VBoard referring backwards to preBoard.
     * @param preBoard
     */
    private VBoard(VBoard preBoard) {
        this.countPieces[CIWHITE] = preBoard.countPieces[CIWHITE];
        this.countPieces[CIBLACK] = preBoard.countPieces[CIBLACK];
        if (preBoard instanceof ChessBoard) {
            this.baseBoard = (ChessBoard)preBoard;
            resetVBaseBoard();
            baseBoard.findAndSetCheckingMoves();
            return;
        }
        // else
        resetMoveCache();
        // later, we might use copyMoveCacheAndCheckerFrom(preBoard);
        // but this needs a post-correction of the moves cache in addMove and remembering all changes for the eval-functions

        this.captureEvalSoFar = preBoard.captureEvalSoFar;
        this.baseBoard = preBoard.baseBoard;
        this.piecePos = Arrays.copyOf(preBoard.piecePos, preBoard.piecePos.length);
        this.capturedPiece = null;
        this.countMovesSinceBaseBoard = preBoard.countMovesSinceBaseBoard;
        this.moves = Arrays.copyOf(preBoard.moves, preBoard.moves.length);
        //checkingMoves[CIWHITE] = new ArrayList<>(preBoard.getCheckingMoves(CIWHITE));
        //checkingMoves[CIBLACK] = new ArrayList<>(preBoard.getCheckingMoves(CIBLACK));
//        checker[CIWHITE] = new ArrayList<>();
//        checker[CIBLACK] = new ArrayList<>();
        // old: no copying of checkers here, as addMove has to follow, it would no longer be valid
        // checker[CIWHITE] = preBoard.checker[CIWHITE] == null ? new ArrayList<>() : new ArrayList<>(preBoard.checker[CIWHITE]);
        // checker[CIBLACK] = preBoard.checker[CIBLACK] == null ? new ArrayList<>() : new ArrayList<>(preBoard.checker[CIBLACK]);

        // the following were be not thread safe - it relies on clean backtracking, no "forking" path of valid VBoard existing in parallel.
        // So for now, we always make less efficient, but safe copies
        // this.moves = preBoard.moves;
    }

    @Override
    public int getSlidingDelay(int[] nrOfBlockers, Move slidingMove, int turnCol) {
        final int slidingMoverCol = slidingMove.piece().color();
        return (getPiecePos(slidingMove.piece()) == slidingMove.from() ? 0 : 2)
                + (nrOfBlockers[opponentColor(slidingMoverCol)] << 1)       // opponents need to move away (fastest possible: every 2nd ply one opponent)
                + (max(0, nrOfBlockers[slidingMoverCol] - nrOfBlockers[opponentColor(slidingMoverCol)]) << 1)  // same color can move away in between opponents, unless it has more own pieces in the way
                - 2                                                         // the two lines above count 2 to many, because of the piece moving away
                + (turnCol == slidingMoverCol ? 2 : 1);                     // opponent could slide directly after, but I myself have to wait until after opponents ply in any case
    }

    public void copyMoveCacheAndCheckerFrom(VBoard other) {
        // the cache content is actually fully valid here and even safe and not in conflict when the other.cache-arrays get exchanged
        firstMovesOverSq  = Arrays.copyOf(other.firstMovesOverSq, other.firstMovesOverSq.length);
        // copy other.firstMovesToSq
        for (int color = CIWHITE; color <= CIBLACK; color++)
            firstMovesToSq[color] = Arrays.copyOf(other.firstMovesToSq[color], other.firstMovesToSq[color].length);
        firstMovesFromPce = Arrays.copyOf(other.firstMovesFromPce, other.firstMovesFromPce.length);
        all1stMovesFromPceComplete = other.all1stMovesFromPceComplete;
        checkingMoves[CIWHITE] = new ArrayList<>(other.checkingMoves[CIWHITE]);
        checkingMoves[CIBLACK] = new ArrayList<>(other.checkingMoves[CIWHITE]);
    }

    // factory, based on a VBoardInterface (VBoard or ChesBoard) + one move
    public VBoard createNext(Move plusOneMove) {
        VBoard newVB = new VBoard(this);
        if (!newVB.addMove(this, plusOneMove))
            return null;
        return newVB;
    }

    public VBoard createNext(String plusOneMoveString) {
        VBoard newVB = new VBoard(this);
        if (!newVB.addMove(this, new Move( newVB, plusOneMoveString)))
            return null;
        return newVB;
    }

    protected void resetVBaseBoard() {
        resetMoveCache();
        this.captureEvalSoFar = 0;
        moves = new Move[ChessEngineParams.MAX_SEARCH_DEPTH+3];  // + lookahead of primitive eval method
        countMovesSinceBaseBoard = 0;
        this.piecePos = new int[MAX_PIECES];
        Arrays.fill(this.piecePos, POS_UNSET);
        moves = new Move[ChessEngineParams.MAX_SEARCH_DEPTH+5];  // + lookahead of primitive eval method incl. recursive local clashes
        Arrays.fill(this.piecePos, POS_UNSET);
        checkingMoves[CIWHITE] = null;  // = not calculated
        checkingMoves[CIBLACK] = null;
    }

    private void resetMoveCache() {
        firstMovesOverSq  = new List[NR_SQUARES];
        firstMovesToSq    = new List[2][NR_SQUARES];
        firstMovesFromPce = new List[MAX_PIECES];
        all1stMovesFromPceComplete = false;
    }

    /** add move to VBoard and thus make the next followup board out of it.
     * Thus, only private. From outside, the factory method createNext() should be used.
     * @param preBoard
     * @param move
     * @return true if created, false if move is not legal, e.g. because king would move into check.
     */
    private boolean addMove(VBoard preBoard, Move move) {
        // if this new move captures a piece, let's remember that
        ChessPiece movingPiece = move.piece();
        int toPos = move.to();
        int color = move.piece().color();
        // to know if it is a legal move, it is necessary to check opponents piece coverage for kings
        if (isKing(move.piece().pieceType())) {
            if (preBoard.posIsCoveredBy(move.to(), opponentColor(color)))
                return false;  // move not possible, king would move into check
            //TODO!!: case where king walk "behind himself" - which seems not covered on preBoard, but is covered on newVB=this
        }
        // if it is already check, then this move needs to un-check
        if (!seeminglyLegalMoveIsReallyLegalOnBoard(preBoard, move)) {      // no blocking of check
            return false; // move not possible, as it is check and move does not take the check away
        }
        usageCounter++;
        if (countMovesSinceBaseBoard == moves.length) {
            // emergency, the moves array was too small
            Move[] oldMoves = moves;
            moves = new Move[countMovesSinceBaseBoard +5];
            System.arraycopy(oldMoves, 0, moves, 0, countMovesSinceBaseBoard);
        }
        moves[countMovesSinceBaseBoard++] = move;   // needs to be called first, so depth will be correct from here on
        if (preBoard.hasPieceOfColorAt(opponentColor(color), toPos)) {
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
//        if (move.isChecking())
//            addCheck(move.piece().getDirectMoveAfter(kingPos(opponentColor(color)), this));
        //calcSingleMovesSlidingOver(move.toSq());
        int checkingMoveColor = opponentColor(getTurnCol());
        int kingPos = kingPos(getTurnCol());
        // todo: do not recalc completely, but only look at changes the new move brings
        initCheckingMoves();
        if (kingPos >= 0)
            baseBoard.getSquare(kingPos)
                .getSingleMovesToHere(checkingMoveColor, this)
                .forEach(this::addCheckingMove);

        move.setConsequences( MoveConsequences.calcMoveConsequences(preBoard, move, this) );
        return true;
    }

    /**
     * watch out, it is not checking for king-pinned pieces moving out of the way, as it is assumed, that these are never
     * fed into this function...
     * @param preBoard
     * @param move
     * @return true if a seemingly Legal Move (which my not be king pinned) Is Really Legal On Board now
     */
    static boolean seeminglyLegalMoveIsReallyLegalOnBoard(VBoard preBoard, Move move) {
        int color = move.piece().color();
        if (isKing(move.piece().pieceType())) {
            if (preBoard.posIsCoveredBy(move.to(), opponentColor(color)))
                return false;   // king must not move into check - if there is currently a check or not...
            // treat case where king itself is the only "block" for coverage at toPos of check-giving piece
            for (Move checkingMove : preBoard.getCheckingMoves(opponentColor(color))) {
                if (checkingMove.piece().getMove(checkingMove.from(), move.to()) != null)
                    return false;  // the checker was directly attacking the king and has a move towards the king's target, so it must be covering that toPos after the king has moved.
            }
            return true;
        }
        if (!preBoard.isCheck())
            return true;    // not checking king pins here, thus if there is no check, there are no problems...
        if (preBoard.getCheckingMoves(opponentColor(color)).size() > 1)
            return false;   // two checkers means no one can block
        return preBoard.getCheckingMoves(opponentColor(color)).size() == 1                      // exactly one checker, this is better
                 && (move.to() == preBoard.getCheckingMoves(opponentColor(color)).get(0).from()  // capturing only checker helps
                     || move.blocksCheckAfter(preBoard));                                        // so does blocking the way
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
        for (int i = foundAt+1; i < countMovesSinceBaseBoard; i++) {
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
        if (isCheck())
            return isWhite(turn) ? BLACK_WON : WHITE_WON;
        if (getNrOfPieces(turn) == 0)        // only for test boards - capturing the last (i.e. on a board without king) is a win.
            return isWhite(turn) ? BLACK_WON : WHITE_WON;
        return DRAW;
    }

    @Override
    public void addCheckingMove(Move m) {
        checkingMoves[m.piece().color()].add(m);
    }

    protected void initCheckingMoves() {
        checkingMoves[CIWHITE] = new ArrayList<>();
        checkingMoves[CIBLACK] = new ArrayList<>();
    }

    public @Override
    boolean isCheck() {
        final int lastMoverColor = moves[countMovesSinceBaseBoard-1].piece().color();
        return hasCheckingMoves(lastMoverColor);
    }

    protected boolean hasCheckingMoves(int color) {
        return !checkingMoves[color].isEmpty();
    }

    @Override
    public List<Move> getCheckingMoves(int color) {
        return checkingMoves[color];
    }

    @Override
    public int getTurnCol() {
        return opponentColor(moves[countMovesSinceBaseBoard -1].piece().color());
    }

    @Override
    public boolean hasLegalMoves(int color) {
        Move oneMove = getLegalMovesStream(color)
                .filter(move -> {
                    return seeminglyLegalMoveIsReallyLegalOnBoard(this, move);
                })
                .findAny()
                .orElse(null) ;
        return oneMove != null;
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
        return countMovesSinceBaseBoard;
    }

    @Override
    public int futureLevel() {
        // TODO/idea: do not count forcing moves
        return countMovesSinceBaseBoard;
    }


    //// more complex infos
    /**
     * partly checks if the game has ended and sets the eval in move accordingly.
     * @param move just the target to put the eval in.
     * @param debugOutputprefix prefix for debug output, esp used to indent the output on different search depth
     * @return true if the game has surely ended, false if unclear ... sorry
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
     * checks if piece is blocked by king-pin and also a move to toPos is not mossible, because it is still
     * blocked by king-pin.(includes calculation of getPinnerOfPceToPos(), so do not call in a loop per piece, use
     * 2-param-version instead)
     * @param p the piece to move
     * @param toPos the target position
     * @return true if p cannot move to toPos, because it is already king-pinned would move out of the way.
     */
    public boolean moveIsBlockedByKingPin(ChessPiece p, int toPos) {
        ChessPiece pinner = getPinnerOfPceToPos(p, kingPos(p.color()));
        return pinner != null && moveIsBlockedByKingPin(p, toPos, pinner);
    }

    /**
     * checks if move is blocked by king-pin. Beware assumes, that getPinnerOfPceToPos()!=null has already been
     * checked before. If not, then use 2-param-version instead
     * @param p the piece to move
     * @param toPos the target position
     * @return true if p cannot move to toPos, because it is already king-pinned would move out of the way.
     */
    public boolean moveIsBlockedByKingPin(ChessPiece p, int toPos, ChessPiece pinner) {
        if (pinner == null)
            return false;
        // assume, this has been tested before, so this must not be repeated before every move of the piece:
        //      if (getPinnerOfPceToPos(p, sameColorKingPos) == null)
        //          return false;   // p is not king-pinned
        if (colorlessPieceType(p.pieceType()) == KNIGHT)
            return true;  // a king-pinned knight can never move away in a way that it still avoids the check
        int sameColorKingPos = kingPos(p.color());
        // or it is pinned, but does not move out of the way.
        int king2PceDir = calcDirFromTo(sameColorKingPos, toPos);
        int king2TargetDir = calcDirFromTo(sameColorKingPos, getPiecePos(p));
        return king2PceDir != king2TargetDir;
        // TODO?:  could also be solved by more intelligent condition stored in the distance to the king
    }

    public ChessPiece getPinnerOfPceToPos(ChessPiece pinnedPce, int targetPos) {
        if (isKing(pinnedPce.pieceType()))
            return null;
        int sameColorKingPos = kingPos(pinnedPce.color());
        if (sameColorKingPos < 0)
            return null;  // king does not exist... should not happen, but is part of some test-positions
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

        for (int i = countMovesSinceBaseBoard - 1; i >= 0; i--) {
            if (moves[i].from() == pos)
                return true;
            if (moves[i].to() == pos)
                return false;
        }
        // nothing changed here
        return baseBoard.isSquareEmpty(pos);
    }

    /** counts pieces in the way from fromPos to toPos (both excl, so it can be used for a sliding move)
     * Only to be used for sliding moves that would be valid on an empty beard.
     * @return number of pieces in the way per color [white, black]
     */
    private int[] countBlockerBetweenExcept(int fromExcl, int toExcl, ChessPiece exceptPce) {
        int dir = calcDirFromTo(fromExcl, toExcl);
        assert (dir!=NONE);
        int[] count = new int[]{0,0};
        for (int p=fromExcl+dir; p != toExcl; p+=dir) {
            ChessPiece blocker = getPieceAt(p);
            if (blocker != null && blocker != exceptPce)
                count[blocker.color()]++;
        }
        return count;
    }

    /** counts pieces in the way from fromPos to toPos (both excl, so it can be used for a sliding move)
     * Only to be used for sliding moves that would be valid on an empty beard.
     * @return number of pieces in the way per color [white, black]
     */
    @Override
    public int[] countBlockerForMove(Move move) {
        int[] count = countBlockerBetweenExcept(move.from(), move.to(), move.piece());
        ChessPiece blocker = getPieceAt(move.to());
        if (blocker != null && blocker != move.piece()
                && blocker.color() == move.piece().color())  // a same colored piece is in the way at the end position
            count[move.piece().color()]++;
        return count;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < countMovesSinceBaseBoard; i++) {
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
        for (int i = countMovesSinceBaseBoard - 1; i >= 0; i--) {
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
        for (int i = countMovesSinceBaseBoard - 1; i >= 0; i--) {
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
     * @param alreadyEvaluated moves to exclude here, because they were already evaluated (to avoid double evaluation)
     * @param notToPos do not add moves to this position, because these were also already counted
     */
    void getBestPositiveCaptureMovesUnlessInOrTo(int color, List<Move> bestOppMoves, int maxBestMoves,
                                                 List<Move> alreadyEvaluated, int notToPos) {
        getLegalMovesStream(color)
                .filter(move -> (hasPieceOfColorAt(opponentColor(color), move.to())
                                 // todo! currently not possible, no followups generated, so no check detected...:
                                 //  || move.getPostVBoard().isCheck()
                                )
                                && move.to() != notToPos
                                && !alreadyEvaluated.contains(move))
                .forEach(move -> {
/*!*/       Evaluation oppEval = move.piece().getMoveEvalInclFollowUpAfter(move, this);
            //System.err.println("Opp move " + move + " ("+oppEval+")");
            if (oppEval == null)
                return;
            if (oppEval.isGoodForColor(color)) {
                //System.err.println(" is good for " + colorName(color));
                addMoveToSortedListOfCol((new Move(move)).setEval(oppEval), bestOppMoves, color, maxBestMoves, null);
            }
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

    // from:
    public void calcSingleMovesStreamOfColorTo(int color, final Square toSq) {
        firstMovesToSq[color][toSq.pos()] = Stream.concat(toSq.depMovesEnd[CIWHITE].stream(),
                                                          toSq.depMovesEnd[CIBLACK].stream())
                .filter(move -> move.from() == getPiecePos(move.piece()))   // automatically filters out moves of captured pieces, as their pos is POS_UNSET
                .toList();
    }

    public List<Move> getSingleMovesOfColorTo(final int color, final int toPos) {
        if (!all1stMovesFromPceComplete) {
            // finish all from-moves for all pieces, this will also generate all to moves
            // (we do not just generate the ones to here, as we have to run over all pieces anyway and running over
            // all to-moves also does not seem more efficient.)
            getPieces().forEach(this::calcPiecesFirstLegalMoves);
            all1stMovesFromPceComplete = true;
        }
        return firstMovesToSq[color][toPos];
    }

    public Stream<Move> getSingleMovesStreamOfColorTo(final int color, final int toPos) {
        if (all1stMovesFromPceComplete && firstMovesToSq[color][toPos] == null)
            return Stream.empty();
        List<Move> result = getSingleMovesOfColorTo(color,toPos);
        return result == null ? Stream.empty() : result.stream();
    }

    public boolean posIsCoveredBy(final int pos, final int color) {
        return getSingleMovesStreamOfColorTo(color, pos).findAny().isPresent();
    }

    public ChessPiece cheapestAttackerOfColor(final int pos, final int color) {
        return getSingleMovesStreamOfColorTo(color, pos)
                .map(Move::piece)
                .min(Comparator.comparingInt(p -> abs(p.getValue())))
                .orElse(null);
    }

    public Stream<Move> getSingleMovesStreamTo(final int pos) {
        return  Stream.concat( getSingleMovesStreamOfColorTo(CIWHITE, pos),
                               getSingleMovesStreamOfColorTo(CIBLACK, pos) );
    }

    // from:
    private List<Move> getSingleCoveringsFromPce(final ChessPiece mover) {
        if (firstMovesFromPce[mover.id()] == null)
            calcPiecesFirstLegalMoves(mover);
        return firstMovesFromPce[mover.id()];
    }

    public Stream<Move> getSingleCoveringStreamFromPce(final ChessPiece mover) {
        return getSingleCoveringsFromPce(mover).stream();
    }

    public Stream<Move> getSingleMovesStreamFromPce(final ChessPiece mover) {
        return getSingleCoveringsFromPce(mover).stream()
                .filter(move -> mover.coveringMoveToIsLegalAfter(move, this));
    }


    /**
     * All currently (on board fb) legal moves and coverings (which are no legal moves),
     * Note: costly, use with caution or consider caching results, if you need it several times.
     * Note also: Results are stored in the cache as side effect: The List of legal moves and additional coverings
     * go to the pieces from-cache. This also partly fills (i.e. only for this piece) the from-cache.
     * It does not construct follow-up-VBoards.
     * @param mover
     * @return
     */
    void calcPiecesFirstLegalMoves(ChessPiece mover) {
        //Option: would this be nicer in the ChessPieces class? but it also heavily relies on the context of this VBoard
        int fromPos = getPiecePos(mover);
        // no? Todo:check:  assert(!moves[posAfter(fb)].isEmpty());
        firstMovesFromPce[mover.id()] = new ArrayList<>();
        //Todo: cache and reset after a real move
        //loop over all moves and check if the moves are legal or covering an own piece
        if (isSlidingPieceType(mover.pieceType())) {
            // for non-sliding pieces, the generic move/toPos list is not performant enough, as it does not stop after hitting a piece
            ChessPiece pinner = null;
            boolean pinnerIsCalculated = false;
            int[] dirs = pieceDirections(mover.pieceType());
            for (int d : dirs) {
                int pos = fromPos;
                // loop along the direction d until we hit something
                while (plusDirIsStillLegal(pos, d)) {
                    pos += d;
                    // needs check if it is really a legal move - however isALegalMoveAfter() checks way too much things we already know, only the kin-pin-check is missing:
                    if (!pinnerIsCalculated) {
                        pinner = getPinnerOfPceToPos(mover, kingPos(mover.color()));
                        pinnerIsCalculated = true;
                    }
                    if (moveIsBlockedByKingPin(mover, pos, pinner))
                        break;  // if one step would already uncover a check, all further steps in that direction are also not needed to be checked
                    Move m = mover.getMove(fromPos, pos);
                    addFromAndToCache(mover, m);
                    if (!isSquareEmpty(pos))
                        break;
                }
            }
        }
        else {
            // for non-sliding pieces, we can easily use our generic move/toPos list
            for (Move m : mover.moves[fromPos]) {
                if ( m.isCoveringAfter(this)
                        // exception for pawns: we also add the straight move as "covering", so legal is always a subset of covering...
                        || (m.piece() instanceof ChessPiecePawn p && p.isALegalMoveAfter(m, this)) ) {
                    addFromAndToCache(mover, m);
                }
            }
        }
    }

    private void addFromAndToCache(ChessPiece p, Move m) {
        firstMovesFromPce[p.id()].add(m);
        if (firstMovesToSq[p.color()][m.to()] == null)
            firstMovesToSq[p.color()][m.to()] = new ArrayList<>();
        firstMovesToSq[p.color()][m.to()].add(m);
    }

}

