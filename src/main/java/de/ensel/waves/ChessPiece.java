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
 *     This class borrowed some methods from ChessPiece of TideEval v0.48.
 */

package de.ensel.waves;

import de.ensel.chessbasics.ChessBasics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import static de.ensel.chessbasics.ChessBasics.*;
import static de.ensel.waves.ChessBoard.*;
import static java.lang.Math.abs;
import static java.lang.Math.max;

public class ChessPiece {

    private final ChessBoard board;
    private final int myPceType;
    private final int myColor;
    private final int myPceID;
    private int myPos;

    private MovesCollection[] moves;

    //// constructors + factory

    ChessPiece(ChessBoard myChessBoard, int pceType, int pceID, int pcePos) {
        this.board = myChessBoard;
        myPceType = pceType;
        myColor = colorIndexOfPieceType(pceType);
        myPceID = pceID;
        myPos = pcePos;
        resetPieceBasics();
    }

    // factory
    public static ChessPiece newPiece(ChessBoard chessBoard, int pceType, int newPceID, int pos) {
        ChessPiece newPce = isPawn(pceType)
                ? new ChessPiecePawn(chessBoard, pceType, newPceID, pos)
                : new ChessPiece(chessBoard, pceType, newPceID, pos);
        int[][] moveMatrix =  switch (colorlessPieceType(pceType)) {
            case BISHOP -> newPce.moveMatrix4Bishop;
            case ROOK -> newPce.moveMatrix4Rook;
            case KNIGHT -> newPce.moveMatrix4Knight;
            case QUEEN -> newPce.moveMatrix4Queen;
            case KING -> newPce.moveMatrix4King;
            case PAWN -> isPieceTypeWhite(pceType)
                    ? newPce.moveMatrix4PawnWhite
                    : newPce.moveMatrix4PawnBlack;
            default -> throw new IllegalStateException("Unexpected value: " + pceType);
        };
        newPce.setupMovesWithDependencies(moveMatrix);
        return newPce;
    }

    private void setupMovesWithDependencies(int[][] moveMatrix) {
        moves = new MovesCollection[64];
        for (int fromPos = 0; fromPos < NR_SQUARES; fromPos++) {
            moves[fromPos] = new MovesCollection();
            for (int n = 0; n < moveMatrix[fromPos].length; n++) {
                // prepare connection to intermediate squares (sliding over)
                int toPos = moveMatrix[fromPos][n];
                int[] intermedPos = calcPositionsFromTo(fromPos, toPos);
                Square[] intermediateSquares = new Square[max(0,intermedPos.length-1)];
                for (int i = 0; i < intermediateSquares.length; i++) {
                    intermediateSquares[i] = board.getSquare(intermedPos[i+1]);
                }
                // create new move
                Move newMove = new Move(
                        this,
                        board.getSquare(fromPos),
                        board.getSquare(toPos),
                        intermediateSquares);
                moves[fromPos].add( newMove );
                // setup connection to start, end and intermediate squares
                board.getSquare(fromPos).setupAddDependentMoveStart(newMove);
                board.getSquare(toPos).setupAddDependentMoveEnd(newMove);
                for (int i = 0; i < intermediateSquares.length; i++) {
                    intermediateSquares[i].setupAddDependentMoveSlidingOver(newMove);
                }
            }
        }
    }


    private void resetPieceBasics() {
        clearMovesAndAllChances();
    }

    public int getValue() {
        // Todo calc real/better value of piece

        // adjusted basic piece value
        if (isLightPieceType(pieceType())
                && board.getLightPieceCounterForPieceType(pieceType()) == 1
        ) {
            // exception for single (left over) light pieces
            return singleLightPieceBaseValue(pieceType());
        }
        else if (isPawn(pieceType()) ) {
            //TODO something
        }

        int val = pieceBaseValue(pieceType());

        return val;
    }

    public boolean isALegalMoveForMe(SimpleMove m) {
        if (m.from() != pos() )
            return false;

        Move move = getMove(pos(), m.to());
        if (move == null)
            return false;

        // TODO, check like here:
        /* isBasicallyALegalMoveForMeTo(m.to())   // TODO: do we need to check this again, or may .isBasicallyLegal be used here?
                && board.moveIsNotBlockedByKingPin(this, m.to())
                && (!board.isCheck(color())
                    || board.nrOfChecks(color()) == 1 && board.posIsBlockingCheck(color(), m.to())
                    || isKing(myPceType)); */
        return move.isALegalMoveNow();
    }

    public Move getMove(int from, int to) {
        if (moves == null || moves[from] == null || moves[from].isEmpty())
            return null;
        return moves[from].getMoveTo(to);
    }

    public Move getDirectMove(int toPos) {
        return getMove(pos(), toPos);
    }

    public Move getDirectMoveAfter(int toPos, VBoardInterface fb) {
        int fromPos = posAfter(fb);
        return getMove(fromPos, toPos);
    }


    public Evaluation getMoveToEvalAfter(int toPos, VBoardInterface fb) {
        int fromPos = posAfter(fb);
        Move evaluatedMove = getEvaluatedMoveToAfter(getMove(fromPos, toPos), fb);
        if (evaluatedMove == null)
            return null;
        return evaluatedMove.getEval();
    }

    /** generates a new evaluated move
     * @param move2Bevaluated the basic move or any other already evaluated move
     * @param fb future board as VBoardInterface
     * @return a new evaluated Move
     */
    public Move getEvaluatedMoveToAfter(Move move2Bevaluated, VBoardInterface fb) {
        if (move2Bevaluated == null || !move2Bevaluated.isALegalMoveAfter(fb))
            return null;
        VBoard fbAfter = VBoard.createNext(fb, move2Bevaluated);
        if (DEBUGMSG_MOVEEVAL)
            System.out.println("Evaluating move " + move2Bevaluated + " after <"+fb+">:");

        // basic evaluation
        final Evaluation eval = move2Bevaluated.getSimpleMoveEvalAfter(fb);
        if (SHOW_REASONS && !eval.isAboutZero())
            eval.addReason("direct_effect:" + eval);

        // a) include moves that now can take this piece at the toPos
        Square toSq = board.getSquare(move2Bevaluated.to());
        // like for b) a loop is not necessary here at the moment. Only take the cheapest attacker, as piece can only be taken once...
        //        final boolean[] captureable = {false};
        //        toSq.getSingleMovesToHere()
        //                .filter(oppMove -> oppMove.isALegalMoveAfter(fbAfter))
        //                .forEach(oppMove -> {

        Evaluation oppFUpEval = getFollowUpEvalAtSqAfter(toSq, fbAfter);
        if (oppFUpEval != null && oppFUpEval.isGoodForColor(opponentColor(color()))) {
            if (DEBUGMSG_MOVEEVAL)
                System.out.println("Capturing at toPos seems possible, changing eval by " + oppFUpEval + ".");
            eval.addEval(oppFUpEval);
            if (SHOW_REASONS)
                eval.addReason(/*"<"+fbAfter+">*/ "likely_being_captured_"+toSq+":"+oppFUpEval);
        } else if (oppFUpEval != null) {
            if (DEBUGMSG_MOVEEVAL)
                System.out.println("Capturing at "+toSq+" seems not reasonable for opponent.");
//            if (SHOW_REASONS)
//                eval.addReason("<"+fbAfter+">");
        }
//        else if (SHOW_REASONS)
//            eval.addReason("<"+fbAfter+">");

        // b) counter effects of moves (resp. just of one move) that would have taken this piece here
            // Note: If enabling all captures is necessary (for further simulation), then taking the cheapest is not sufficient
            //       It then needs something like back to the prev. code :-)
            //        fromSq.getSingleMovesToHere()
            //                .filter(move -> move.isALegalMoveAfter(fbAfter))
            //                .forEach(move -> {
            //                    if ( move.isLegalAfter(fb) )
            //                        eval.subtractEval(move.getSimpleMoveEvalAfter(fb));
            //                    eval.addEval(move.getSimpleMoveEvalAfter(fbAfter));
            //                });
        Square fromSq = board.getSquare(move2Bevaluated.from());
        ChessPiece cheapestFromPosAttacker = fromSq.cheapestAttackersOfColorToHereAfter(opponentColor(color()), fb);
        if (cheapestFromPosAttacker != null) {
            Move disabledCapture = cheapestFromPosAttacker.getDirectMoveAfter(move2Bevaluated.from(), fb);
            if (disabledCapture != null) {
                Evaluation disabledCaptureEval = disabledCapture.getSimpleMoveEvalAfter(fb);
                if (!disabledCaptureEval.isAboutZero()) {
                    disabledCaptureEval.timeWarp(+1);  // capturing if do not move is an opponents move coming (potentially) after my moves
                    eval.subtractEval(disabledCaptureEval);  // undo old evaluation
                    //eval.addEval(disabledCapture.getSimpleMoveEvalAfter(fbAfter));     // instead add new one - should be 0 - and is also not always legal (for pawns)
                    if (DEBUGMSG_MOVEEVAL)
                        System.out.println("Fleeing from capture at fromPos " + disabledCapture + " changes eval to " + eval + ".");
                    if (SHOW_REASONS)
                        eval.addReason("avoiding_capture_by_" + disabledCapture + ":" + disabledCaptureEval);
                }
            }
        }

        // c) enable moves that now can slide over this square here
        //Todo: should not add all moves, but only all best moves per piece
        fromSq.getSingleMovesSlidingOverHere()
                .filter(move -> move.isALegalMoveAfter(fbAfter))
                .forEach(move -> {
                    Evaluation enabledEval = getMoveEvalInclFollowUpAfter(move,fbAfter);
                    if (enabledEval.isGoodForColor(move.piece().color())) {
                        if (SHOW_REASONS)
                            eval.addReason("enabling_" + move + getMoveEvalInclFollowUpAfter(move, fbAfter));
                        if (DEBUGMSG_MOVEEVAL)
                            System.out.println("Enabling " + move + " sliding over " + move2Bevaluated.fromSq()
                                    + " -> adding " + enabledEval);
                        if (move.piece().color() == move2Bevaluated.piece().color())
                            enabledEval.timeWarp(+2);  // enable my own piece for the next round only
                        eval.addEval(enabledEval);
                    }
                });

        // d) delay sliding moves that are now blocked at the toPos
        //Todo: also process blocked 1-hop moves incl. formerly capturing pawns and straight moving pawns,
        // as well as straight moving opponent pawns + in any case: do not use blocks if there was a blocking piece already
        toSq.getSingleMovesSlidingOverHere()
                .filter(move -> move.isALegalMoveAfter(fbAfter))
                .forEach(move -> {
                    Evaluation e = move.getSimpleMoveEvalAfter(fbAfter);
                    eval.subtractEval(e);   // not possible any more
                    e.timeWarp(+1);
                    eval.addEval(e);        // but possible in the future
                    if (SHOW_REASONS)
                        eval.addReason("blocking_"+move+e);
                    if (DEBUGMSG_MOVEEVAL)
                        System.out.println("blocking " + move + " leads to " + eval + ".");
                });

        if (oppFUpEval == null || oppFUpEval.isGoodForColor(color())) {
            // e) see what this piece can capture from here
            Move bestDirectFollowUpMove = getBestEvaluatedDirectFollowUpMoveAfter(fbAfter);
            Evaluation nextBestEval = null;
            if (bestDirectFollowUpMove != null) {
                nextBestEval = bestDirectFollowUpMove.getEval();
                if (oppFUpEval != null && oppFUpEval.isBetterForColorThan(opponentColor(color()), nextBestEval)) {
                    // beating my piece is better for the opponent then the followup, so let's not calculate my followup, but his (taking my piece)
                    nextBestEval = oppFUpEval;
                }
                nextBestEval.timeWarp(+2);  // attention: may have side-effect on oppFUpEval (which is "luckily" not used any more)
                eval.addEval(nextBestEval);        // but possible in the future
            }
            if (bestDirectFollowUpMove != null && !bestDirectFollowUpMove.getEval().isAboutZero() ) {
                if (SHOW_REASONS)
                    eval.addReason("threatening_" + bestDirectFollowUpMove + ":" + nextBestEval);
                if (DEBUGMSG_MOVEEVAL)
                    System.out.println("+threatening " + bestDirectFollowUpMove + nextBestEval);
            }

            // f) see what this piece can cover from here - should be calculated by the bigger recursion, however,
            // seemingly boring moves (if covering counts as 0) would then never make it into the recursion
            Move fUDefenceMove = getBestDefenceEvalAfter(fb, fbAfter);
            if (fUDefenceMove != null) {
                Evaluation fUDefenceEval = fUDefenceMove.getEval();
                if (fUDefenceEval != null) {
                    fUDefenceEval.timeWarp(+2);
                    eval.addEval(fUDefenceEval);        // but possible in the future
                    if (SHOW_REASONS)
                        eval.addReason("defending_" + fUDefenceMove + ":" + fUDefenceEval);
                    if (DEBUGMSG_MOVEEVAL)
                        System.out.println("+defending " + fUDefenceMove + ":" + fUDefenceEval);
                }
            }
        }

        Move res = new Move(move2Bevaluated).setEval(eval);
        return res;
    }

    private Evaluation getMoveEvalInclFollowUpAfter(Move move, VBoard fb) {
        return getMoveEvalInclFollowUpAfter(move, fb, VBoard.createNext(fb, move));
    }

    /** returns the potential benefit of capturing with this move + checks if recapturing is reasonably possible
     *  (after some guessing, not precise!) for the opponent. If so, this is also calculated in.
     * @param move
     * @param fbNext
     * @return
     */
    private Evaluation getMoveEvalInclFollowUpAfter(Move move, VBoardInterface fb, VBoardInterface fbNext) {
        Evaluation atToSqEval = move.getSimpleMoveEvalAfter(fb);
        Evaluation oppFUpEval = getFollowUpEvalAtSqAfter(move.toSq(), fbNext);
        if (oppFUpEval != null && oppFUpEval.isGoodForColor(opponentColor(move.piece().color()))) {
            atToSqEval.addEval(oppFUpEval);
        }
        return atToSqEval;
    }

    /** returns the (approximated) benefit to capture the piece on this square.
     *  if this is not beneficial, it still returns the evaluation (which is negative for the opponent, so
     * he would probably not capture...)
     * @param toSq
     * @param fbNext
     * @return benefit or price to pay to capture, null if it is not possible to capture, or there is no piece to capture.
     */
    private Evaluation getFollowUpEvalAtSqAfter(Square toSq, VBoardInterface fbNext) {
        ChessPiece cheapestToPosAttacker = toSq.cheapestAttackersOfColorToHereAfter(opponentColor(color()), fbNext);
        if (cheapestToPosAttacker == null)
            return null;
        Move enabledCaptureMove = cheapestToPosAttacker.getDirectMoveAfter(toSq.pos(), fbNext);
        if (enabledCaptureMove == null)
            return null;
        ChessPiece attackedPce = fbNext.getPieceAt(toSq.pos());
        if (attackedPce == null)
            return null;
        if (DEBUGMSG_MOVEEVAL)
            System.out.print("enabling@to: " + enabledCaptureMove + " ");
        Evaluation captureEval = enabledCaptureMove.getSimpleMoveEvalAfter(fbNext);
        // as we do not go deeper here, we can actually not know the correct evaluation.
        // Can the opponent capture safely? or will he also lose his piece - or something in between...
        ChessPiece cheapestToPosDefender = toSq.cheapestDefenderHereForPieceAfter(attackedPce, fbNext);
        if (cheapestToPosDefender != null) {
            // it is defended at least by one, so opponent will probably be lost, too
            int oppValue = enabledCaptureMove.piece().getValue();
            captureEval.addEval(-(oppValue - (oppValue >> 2)), 0);  // take 3/4 of opponent
            ChessPiece cheapestOppDefender = toSq.cheapestDefenderHereForPieceAfter(enabledCaptureMove.piece(), fbNext);  //restriction: does not generate next fbNext (fbNext+enabledCaptureMove) - but if we go too far, we could almost also start a deep simulation here ... :-)
            if (cheapestOppDefender != null)
                captureEval.addEval(-cheapestToPosDefender.getValue() >> 1, 1);
        }
        if (captureEval.isGoodForColor(enabledCaptureMove.piece().color())) {
            return captureEval;
        }
        if (DEBUGMSG_MOVEEVAL)
            System.out.println("Capturing back at "+ toSq +" does not seem possible (as defended by " + cheapestToPosDefender + ").");
        return captureEval;  // return anyway - the "price to kill"
    }

    private Move getBestEvaluatedDirectFollowUpMoveAfter(VBoard fb) {
        Move bestFollowUpMove = null;
        for (Iterator<Move> it = legalMovesAfter(fb).iterator(); it.hasNext(); ) {
            Move move = it.next();
            Evaluation eval = getMoveEvalInclFollowUpAfter(move, fb);
            if (eval != null) {
                if (bestFollowUpMove == null || eval.isBetterForColorThan(color(), move.getEval())) {
                    bestFollowUpMove = new Move(move).setEval(eval);
                }
            }
        };
        return bestFollowUpMove;
    }

    private Move getBestDefenceEvalAfter(VBoardInterface  fb, VBoardInterface fbAfter) {
        Evaluation bestDefenceEval = null;
        Move bestDefenceMove = null;
        for (Iterator<Move> it = coveringMovesAfter(fbAfter).iterator(); it.hasNext(); ) {
            Move move = it.next();
            if (move.to() == posAfter(fb))
                continue;; // I was not covering myself before the move and I will not do so after the move...
            Evaluation evalBefore = getFollowUpEvalAtSqAfter(move.toSq(), fb);
            if (evalBefore == null)
                continue;  // no capturing possible, even without me...
            Evaluation evalAfter = getFollowUpEvalAtSqAfter(move.toSq(), fbAfter);
            if (evalAfter == null)
                evalAfter = new Evaluation();
            Evaluation evalDelta = evalAfter.subtractEval(evalBefore);
            if (bestDefenceEval == null || evalDelta.isBetterForColorThan(color(), bestDefenceEval)) {
                bestDefenceEval = evalDelta;
                bestDefenceMove = move;
            }
        }
        if (bestDefenceMove == null)
            return null;
        return new Move(bestDefenceMove).setEval(bestDefenceEval);
    }

    public Evaluation getDirectMoveToEvaluation(int to) {
        Move move = getMove(pos(), to);
        if (move == null || !move.isALegalMoveNow())
            return null;
        return move.getSimpleMoveEval();
    }

    public boolean canMove() {
        if (moves == null || moves[pos()] == null || moves[pos()].isEmpty() ) {
            return false;
        }
        // loop over all moves and check if at least one is legal now
        for (Move m : moves[pos()]) {
            if (m.isALegalMoveNow()) {
                return true;
            }
        }
        return false;
    }

    public Stream<Move> legalMovesAfter(VBoardInterface fb) {
        assert(moves != null);
        assert(moves[posAfter(fb)] != null);
        int fromPos = posAfter(fb);
        // no? Todo:check: assert(!moves[posAfter(fb)].isEmpty());
        List<Move> legalMoves = new ArrayList<>();
        //Todo: cache and reset after a real move
        //loop over all moves and take the ones that are legal now
        for (Move m : moves[fromPos]) {
            if (m.isALegalMoveAfter(fb))
                legalMoves.add(m);
        }
        return legalMoves.stream();
    }

    /**
     * almost the same as legalMovesAfter(), but also returns the "moves" that cover a (own) piece
     * @param fb
     * @return
     */
    public Stream<Move> coveringMovesAfter(VBoardInterface fb) {
        assert(moves != null);
        assert(moves[posAfter(fb)] != null);
        int fromPos = posAfter(fb);
        // no? Todo:check:  assert(!moves[posAfter(fb)].isEmpty());
        List<Move> legalMoves = new ArrayList<>();
        //Todo: cache and reset after a real move
        //loop over all moves and check if the moves are legal or covering an own piece
        for (Move m : moves[fromPos]) {
            if (m.isDefendingAfter(fb))
                legalMoves.add(m);
        }
        return legalMoves.stream();
    }

    private void clearMovesAndAllChances() {
        //TODO
        ;
    }

    public boolean isADirectMoveAfter(Move move, VBoardInterface fb) {
        if (move.toSq().hasPieceOfColorAfter(color(), fb))
            return false; // target already occupied
        return isDefendingTargetAfter(move, fb);
    }

    public boolean isDefendingTargetAfter(Move move, VBoardInterface fb) {
        // loop over all intermediate Sqs, if they are free
        for (Square iSq : move.intermedSqs()) {
            if (!iSq.isEmptyAfter(fb))
                return false;
        }
        return true;
    }

    /**
     * die() piece is EOL - clean up
     */
    public void die() {
        // little to clean up here...
        myPos = NOWHERE;
    }

    //// simple info

    @Override
    public String toString() {
        return pieceColorAndName(myPceType) + " on " + squareName(pos());
    }

    public int color() {
        return myColor;
    }

    int baseValue() {
        return pieceBaseValue(myPceType);
    }

    int reverseBaseEval() {
        return reversePieceBaseValue(myPceType);
    }

    public boolean isWhite() {
        return ChessBasics.isPieceTypeWhite(myPceType);
    }

    public char symbol() {
        return fenCharFromPceType(pieceType());
    }

    public int staysEval() {
        return board.getSquare(myPos).clashEval();
    }

//    public boolean canStayReasonably() {
//        return evalIsOkForColByMin(board.getBoardSquare(myPos).clashEval(), color());
//    }

    private Stream<Move> allMoves() {
        return moves[pos()].stream();
    }


    //// BoardChange dependent analogies for getters

    public int posAfter(VBoardInterface bc) {
        return bc.getPiecePos(this);
    }


    //// getter

    public int pieceType() {
        return myPceType;
    }

    public int id() {
        return myPceID;
    }

    public int pos() {
        return myPos;
    }

    public ChessBoard board() {
        return board;
    }


    //// setter

    public void setPos(int pos) {
        myPos = pos;
        resetPieceBasics();
    }


    /**
     * to be called only for directly (=1 move) reachable positions.
     * For 1hop pieces, this is just the position itself.
     * For sliding pieces, additionally all the squares in between
     * @param pos - target position (excluded)
     * @return list of squares able to block my way to pos, from this piece's myPos (included) to pos excluded
     */
    public int[] allPosOnWayTo(int pos) {
        int[] ret;
        if (isSlidingPieceType(myPceType)) {
            int dir = calcDirFromTo(myPos,pos);
            assert (dir!=NONE);
            ret = new int[distanceBetween(myPos,pos)];
            for (int i=0,p=myPos; p!=pos && i<ret.length; p+=dir, i++)
                ret[i]=p;
        }
        else {
            ret = new int[1];
            ret[0] = myPos;
        }
        return ret;
    }



    int[][] moveMatrix4King = new int[][] {
            /* rank 8: */ { 1, 9, 8 },  { 2, 0, 8, 10, 9 },  { 3, 1, 9, 11, 10 },  { 4, 2, 10, 12, 11 },  { 5, 3, 11, 13, 12 },  { 6, 4, 12, 14, 13 },  { 7, 5, 13, 15, 14 },  { 6, 14, 15 },
            /* rank 7: */ { 9, 1, 17, 16, 0 },  { 10, 8, 2, 16, 0, 18, 17, 1 },  { 11, 9, 3, 17, 1, 19, 18, 2 },  { 12, 10, 4, 18, 2, 20, 19, 3 },  { 13, 11, 5, 19, 3, 21, 20, 4 },  { 14, 12, 6, 20, 4, 22, 21, 5 },  { 15, 13, 7, 21, 5, 23, 22, 6 },  { 14, 22, 6, 23, 7 },
            /* rank 6: */ { 17, 9, 25, 24, 8 },  { 18, 16, 10, 24, 8, 26, 25, 9 },  { 19, 17, 11, 25, 9, 27, 26, 10 },  { 20, 18, 12, 26, 10, 28, 27, 11 },  { 21, 19, 13, 27, 11, 29, 28, 12 },  { 22, 20, 14, 28, 12, 30, 29, 13 },  { 23, 21, 15, 29, 13, 31, 30, 14 },  { 22, 30, 14, 31, 15 },
            /* rank 5: */ { 25, 17, 33, 32, 16 },  { 26, 24, 18, 32, 16, 34, 33, 17 },  { 27, 25, 19, 33, 17, 35, 34, 18 },  { 28, 26, 20, 34, 18, 36, 35, 19 },  { 29, 27, 21, 35, 19, 37, 36, 20 },  { 30, 28, 22, 36, 20, 38, 37, 21 },  { 31, 29, 23, 37, 21, 39, 38, 22 },  { 30, 38, 22, 39, 23 },
            /* rank 4: */ { 33, 25, 41, 40, 24 },  { 34, 32, 26, 40, 24, 42, 41, 25 },  { 35, 33, 27, 41, 25, 43, 42, 26 },  { 36, 34, 28, 42, 26, 44, 43, 27 },  { 37, 35, 29, 43, 27, 45, 44, 28 },  { 38, 36, 30, 44, 28, 46, 45, 29 },  { 39, 37, 31, 45, 29, 47, 46, 30 },  { 38, 46, 30, 47, 31 },
            /* rank 3: */ { 41, 33, 49, 48, 32 },  { 42, 40, 34, 48, 32, 50, 49, 33 },  { 43, 41, 35, 49, 33, 51, 50, 34 },  { 44, 42, 36, 50, 34, 52, 51, 35 },  { 45, 43, 37, 51, 35, 53, 52, 36 },  { 46, 44, 38, 52, 36, 54, 53, 37 },  { 47, 45, 39, 53, 37, 55, 54, 38 },  { 46, 54, 38, 55, 39 },
            /* rank 2: */ { 49, 41, 57, 56, 40 },  { 50, 48, 42, 56, 40, 58, 57, 41 },  { 51, 49, 43, 57, 41, 59, 58, 42 },  { 52, 50, 44, 58, 42, 60, 59, 43 },  { 53, 51, 45, 59, 43, 61, 60, 44 },  { 54, 52, 46, 60, 44, 62, 61, 45 },  { 55, 53, 47, 61, 45, 63, 62, 46 },  { 54, 62, 46, 63, 47 },
            /* rank 1: */ { 57, 49, 48 },  { 58, 56, 50, 48, 49 },  { 59, 57, 51, 49, 50 },  { 60, 58, 52, 50, 51 },  { 61, 59, 53, 51, 52 },  { 62, 60, 54, 52, 53 },  { 63, 61, 55, 53, 54 },  { 62, 54, 55 }
    };

    int[][] moveMatrix4Queen = new int[][] {
            /* rank 8: */ {  /*E*/ 1, 2, 3, 4, 5, 6, 7,  /*SE*/ 9, 18, 27, 36, 45, 54, 63,  /*S*/ 8, 16, 24, 32, 40, 48, 56 },
            {  /*E*/ 2, 3, 4, 5, 6, 7,  /*W*/ 0,  /*SW*/ 8,  /*SE*/ 10, 19, 28, 37, 46, 55,  /*S*/ 9, 17, 25, 33, 41, 49, 57 },
            {  /*E*/ 3, 4, 5, 6, 7,  /*W*/ 1, 0,  /*SW*/ 9, 16,  /*SE*/ 11, 20, 29, 38, 47,  /*S*/ 10, 18, 26, 34, 42, 50, 58 },
            {  /*E*/ 4, 5, 6, 7,  /*W*/ 2, 1, 0,  /*SW*/ 10, 17, 24,  /*SE*/ 12, 21, 30, 39,  /*S*/ 11, 19, 27, 35, 43, 51, 59 },
            {  /*E*/ 5, 6, 7,  /*W*/ 3, 2, 1, 0,  /*SW*/ 11, 18, 25, 32,  /*SE*/ 13, 22, 31,  /*S*/ 12, 20, 28, 36, 44, 52, 60 },
            {  /*E*/ 6, 7,  /*W*/ 4, 3, 2, 1, 0,  /*SW*/ 12, 19, 26, 33, 40,  /*SE*/ 14, 23,  /*S*/ 13, 21, 29, 37, 45, 53, 61 },
            {  /*E*/ 7,  /*W*/ 5, 4, 3, 2, 1, 0,  /*SW*/ 13, 20, 27, 34, 41, 48,  /*SE*/ 15,  /*S*/ 14, 22, 30, 38, 46, 54, 62 },
            {  /*W*/ 6, 5, 4, 3, 2, 1, 0,  /*SW*/ 14, 21, 28, 35, 42, 49, 56,  /*S*/ 15, 23, 31, 39, 47, 55, 63 },
            /* rank 7: */ {  /*E*/ 9, 10, 11, 12, 13, 14, 15,  /*NE*/ 1,  /*SE*/ 17, 26, 35, 44, 53, 62,  /*S*/ 16, 24, 32, 40, 48, 56,  /*N*/ 0 },
            {  /*E*/ 10, 11, 12, 13, 14, 15,  /*W*/ 8,  /*NE*/ 2,  /*SW*/ 16,  /*NW*/ 0,  /*SE*/ 18, 27, 36, 45, 54, 63,  /*S*/ 17, 25, 33, 41, 49, 57,  /*N*/ 1 },
            {  /*E*/ 11, 12, 13, 14, 15,  /*W*/ 9, 8,  /*NE*/ 3,  /*SW*/ 17, 24,  /*NW*/ 1,  /*SE*/ 19, 28, 37, 46, 55,  /*S*/ 18, 26, 34, 42, 50, 58,  /*N*/ 2 },
            {  /*E*/ 12, 13, 14, 15,  /*W*/ 10, 9, 8,  /*NE*/ 4,  /*SW*/ 18, 25, 32,  /*NW*/ 2,  /*SE*/ 20, 29, 38, 47,  /*S*/ 19, 27, 35, 43, 51, 59,  /*N*/ 3 },
            {  /*E*/ 13, 14, 15,  /*W*/ 11, 10, 9, 8,  /*NE*/ 5,  /*SW*/ 19, 26, 33, 40,  /*NW*/ 3,  /*SE*/ 21, 30, 39,  /*S*/ 20, 28, 36, 44, 52, 60,  /*N*/ 4 },
            {  /*E*/ 14, 15,  /*W*/ 12, 11, 10, 9, 8,  /*NE*/ 6,  /*SW*/ 20, 27, 34, 41, 48,  /*NW*/ 4,  /*SE*/ 22, 31,  /*S*/ 21, 29, 37, 45, 53, 61,  /*N*/ 5 },
            {  /*E*/ 15,  /*W*/ 13, 12, 11, 10, 9, 8,  /*NE*/ 7,  /*SW*/ 21, 28, 35, 42, 49, 56,  /*NW*/ 5,  /*SE*/ 23,  /*S*/ 22, 30, 38, 46, 54, 62,  /*N*/ 6 },
            {  /*W*/ 14, 13, 12, 11, 10, 9, 8,  /*SW*/ 22, 29, 36, 43, 50, 57,  /*NW*/ 6,  /*S*/ 23, 31, 39, 47, 55, 63,  /*N*/ 7 },
            /* rank 6: */ {  /*E*/ 17, 18, 19, 20, 21, 22, 23,  /*NE*/ 9, 2,  /*SE*/ 25, 34, 43, 52, 61,  /*S*/ 24, 32, 40, 48, 56,  /*N*/ 8, 0 },
            {  /*E*/ 18, 19, 20, 21, 22, 23,  /*W*/ 16,  /*NE*/ 10, 3,  /*SW*/ 24,  /*NW*/ 8,  /*SE*/ 26, 35, 44, 53, 62,  /*S*/ 25, 33, 41, 49, 57,  /*N*/ 9, 1 },
            {  /*E*/ 19, 20, 21, 22, 23,  /*W*/ 17, 16,  /*NE*/ 11, 4,  /*SW*/ 25, 32,  /*NW*/ 9, 0,  /*SE*/ 27, 36, 45, 54, 63,  /*S*/ 26, 34, 42, 50, 58,  /*N*/ 10, 2 },
            {  /*E*/ 20, 21, 22, 23,  /*W*/ 18, 17, 16,  /*NE*/ 12, 5,  /*SW*/ 26, 33, 40,  /*NW*/ 10, 1,  /*SE*/ 28, 37, 46, 55,  /*S*/ 27, 35, 43, 51, 59,  /*N*/ 11, 3 },
            {  /*E*/ 21, 22, 23,  /*W*/ 19, 18, 17, 16,  /*NE*/ 13, 6,  /*SW*/ 27, 34, 41, 48,  /*NW*/ 11, 2,  /*SE*/ 29, 38, 47,  /*S*/ 28, 36, 44, 52, 60,  /*N*/ 12, 4 },
            {  /*E*/ 22, 23,  /*W*/ 20, 19, 18, 17, 16,  /*NE*/ 14, 7,  /*SW*/ 28, 35, 42, 49, 56,  /*NW*/ 12, 3,  /*SE*/ 30, 39,  /*S*/ 29, 37, 45, 53, 61,  /*N*/ 13, 5 },
            {  /*E*/ 23,  /*W*/ 21, 20, 19, 18, 17, 16,  /*NE*/ 15,  /*SW*/ 29, 36, 43, 50, 57,  /*NW*/ 13, 4,  /*SE*/ 31,  /*S*/ 30, 38, 46, 54, 62,  /*N*/ 14, 6 },
            {  /*W*/ 22, 21, 20, 19, 18, 17, 16,  /*SW*/ 30, 37, 44, 51, 58,  /*NW*/ 14, 5,  /*S*/ 31, 39, 47, 55, 63,  /*N*/ 15, 7 },
            /* rank 5: */ {  /*E*/ 25, 26, 27, 28, 29, 30, 31,  /*NE*/ 17, 10, 3,  /*SE*/ 33, 42, 51, 60,  /*S*/ 32, 40, 48, 56,  /*N*/ 16, 8, 0 },
            {  /*E*/ 26, 27, 28, 29, 30, 31,  /*W*/ 24,  /*NE*/ 18, 11, 4,  /*SW*/ 32,  /*NW*/ 16,  /*SE*/ 34, 43, 52, 61,  /*S*/ 33, 41, 49, 57,  /*N*/ 17, 9, 1 },
            {  /*E*/ 27, 28, 29, 30, 31,  /*W*/ 25, 24,  /*NE*/ 19, 12, 5,  /*SW*/ 33, 40,  /*NW*/ 17, 8,  /*SE*/ 35, 44, 53, 62,  /*S*/ 34, 42, 50, 58,  /*N*/ 18, 10, 2 },
            {  /*E*/ 28, 29, 30, 31,  /*W*/ 26, 25, 24,  /*NE*/ 20, 13, 6,  /*SW*/ 34, 41, 48,  /*NW*/ 18, 9, 0,  /*SE*/ 36, 45, 54, 63,  /*S*/ 35, 43, 51, 59,  /*N*/ 19, 11, 3 },
            {  /*E*/ 29, 30, 31,  /*W*/ 27, 26, 25, 24,  /*NE*/ 21, 14, 7,  /*SW*/ 35, 42, 49, 56,  /*NW*/ 19, 10, 1,  /*SE*/ 37, 46, 55,  /*S*/ 36, 44, 52, 60,  /*N*/ 20, 12, 4 },
            {  /*E*/ 30, 31,  /*W*/ 28, 27, 26, 25, 24,  /*NE*/ 22, 15,  /*SW*/ 36, 43, 50, 57,  /*NW*/ 20, 11, 2,  /*SE*/ 38, 47,  /*S*/ 37, 45, 53, 61,  /*N*/ 21, 13, 5 },
            {  /*E*/ 31,  /*W*/ 29, 28, 27, 26, 25, 24,  /*NE*/ 23,  /*SW*/ 37, 44, 51, 58,  /*NW*/ 21, 12, 3,  /*SE*/ 39,  /*S*/ 38, 46, 54, 62,  /*N*/ 22, 14, 6 },
            {  /*W*/ 30, 29, 28, 27, 26, 25, 24,  /*SW*/ 38, 45, 52, 59,  /*NW*/ 22, 13, 4,  /*S*/ 39, 47, 55, 63,  /*N*/ 23, 15, 7 },
            /* rank 4: */ {  /*E*/ 33, 34, 35, 36, 37, 38, 39,  /*NE*/ 25, 18, 11, 4,  /*SE*/ 41, 50, 59,  /*S*/ 40, 48, 56,  /*N*/ 24, 16, 8, 0 },
            {  /*E*/ 34, 35, 36, 37, 38, 39,  /*W*/ 32,  /*NE*/ 26, 19, 12, 5,  /*SW*/ 40,  /*NW*/ 24,  /*SE*/ 42, 51, 60,  /*S*/ 41, 49, 57,  /*N*/ 25, 17, 9, 1 },
            {  /*E*/ 35, 36, 37, 38, 39,  /*W*/ 33, 32,  /*NE*/ 27, 20, 13, 6,  /*SW*/ 41, 48,  /*NW*/ 25, 16,  /*SE*/ 43, 52, 61,  /*S*/ 42, 50, 58,  /*N*/ 26, 18, 10, 2 },
            {  /*E*/ 36, 37, 38, 39,  /*W*/ 34, 33, 32,  /*NE*/ 28, 21, 14, 7,  /*SW*/ 42, 49, 56,  /*NW*/ 26, 17, 8,  /*SE*/ 44, 53, 62,  /*S*/ 43, 51, 59,  /*N*/ 27, 19, 11, 3 },
            {  /*E*/ 37, 38, 39,  /*W*/ 35, 34, 33, 32,  /*NE*/ 29, 22, 15,  /*SW*/ 43, 50, 57,  /*NW*/ 27, 18, 9, 0,  /*SE*/ 45, 54, 63,  /*S*/ 44, 52, 60,  /*N*/ 28, 20, 12, 4 },
            {  /*E*/ 38, 39,  /*W*/ 36, 35, 34, 33, 32,  /*NE*/ 30, 23,  /*SW*/ 44, 51, 58,  /*NW*/ 28, 19, 10, 1,  /*SE*/ 46, 55,  /*S*/ 45, 53, 61,  /*N*/ 29, 21, 13, 5 },
            {  /*E*/ 39,  /*W*/ 37, 36, 35, 34, 33, 32,  /*NE*/ 31,  /*SW*/ 45, 52, 59,  /*NW*/ 29, 20, 11, 2,  /*SE*/ 47,  /*S*/ 46, 54, 62,  /*N*/ 30, 22, 14, 6 },
            {  /*W*/ 38, 37, 36, 35, 34, 33, 32,  /*SW*/ 46, 53, 60,  /*NW*/ 30, 21, 12, 3,  /*S*/ 47, 55, 63,  /*N*/ 31, 23, 15, 7 },
            /* rank 3: */ {  /*E*/ 41, 42, 43, 44, 45, 46, 47,  /*NE*/ 33, 26, 19, 12, 5,  /*SE*/ 49, 58,  /*S*/ 48, 56,  /*N*/ 32, 24, 16, 8, 0 },
            {  /*E*/ 42, 43, 44, 45, 46, 47,  /*W*/ 40,  /*NE*/ 34, 27, 20, 13, 6,  /*SW*/ 48,  /*NW*/ 32,  /*SE*/ 50, 59,  /*S*/ 49, 57,  /*N*/ 33, 25, 17, 9, 1 },
            {  /*E*/ 43, 44, 45, 46, 47,  /*W*/ 41, 40,  /*NE*/ 35, 28, 21, 14, 7,  /*SW*/ 49, 56,  /*NW*/ 33, 24,  /*SE*/ 51, 60,  /*S*/ 50, 58,  /*N*/ 34, 26, 18, 10, 2 },
            {  /*E*/ 44, 45, 46, 47,  /*W*/ 42, 41, 40,  /*NE*/ 36, 29, 22, 15,  /*SW*/ 50, 57,  /*NW*/ 34, 25, 16,  /*SE*/ 52, 61,  /*S*/ 51, 59,  /*N*/ 35, 27, 19, 11, 3 },
            {  /*E*/ 45, 46, 47,  /*W*/ 43, 42, 41, 40,  /*NE*/ 37, 30, 23,  /*SW*/ 51, 58,  /*NW*/ 35, 26, 17, 8,  /*SE*/ 53, 62,  /*S*/ 52, 60,  /*N*/ 36, 28, 20, 12, 4 },
            {  /*E*/ 46, 47,  /*W*/ 44, 43, 42, 41, 40,  /*NE*/ 38, 31,  /*SW*/ 52, 59,  /*NW*/ 36, 27, 18, 9, 0,  /*SE*/ 54, 63,  /*S*/ 53, 61,  /*N*/ 37, 29, 21, 13, 5 },
            {  /*E*/ 47,  /*W*/ 45, 44, 43, 42, 41, 40,  /*NE*/ 39,  /*SW*/ 53, 60,  /*NW*/ 37, 28, 19, 10, 1,  /*SE*/ 55,  /*S*/ 54, 62,  /*N*/ 38, 30, 22, 14, 6 },
            {  /*W*/ 46, 45, 44, 43, 42, 41, 40,  /*SW*/ 54, 61,  /*NW*/ 38, 29, 20, 11, 2,  /*S*/ 55, 63,  /*N*/ 39, 31, 23, 15, 7 },
            /* rank 2: */ {  /*E*/ 49, 50, 51, 52, 53, 54, 55,  /*NE*/ 41, 34, 27, 20, 13, 6,  /*SE*/ 57,  /*S*/ 56,  /*N*/ 40, 32, 24, 16, 8, 0 },
            {  /*E*/ 50, 51, 52, 53, 54, 55,  /*W*/ 48,  /*NE*/ 42, 35, 28, 21, 14, 7,  /*SW*/ 56,  /*NW*/ 40,  /*SE*/ 58,  /*S*/ 57,  /*N*/ 41, 33, 25, 17, 9, 1 },
            {  /*E*/ 51, 52, 53, 54, 55,  /*W*/ 49, 48,  /*NE*/ 43, 36, 29, 22, 15,  /*SW*/ 57,  /*NW*/ 41, 32,  /*SE*/ 59,  /*S*/ 58,  /*N*/ 42, 34, 26, 18, 10, 2 },
            {  /*E*/ 52, 53, 54, 55,  /*W*/ 50, 49, 48,  /*NE*/ 44, 37, 30, 23,  /*SW*/ 58,  /*NW*/ 42, 33, 24,  /*SE*/ 60,  /*S*/ 59,  /*N*/ 43, 35, 27, 19, 11, 3 },
            {  /*E*/ 53, 54, 55,  /*W*/ 51, 50, 49, 48,  /*NE*/ 45, 38, 31,  /*SW*/ 59,  /*NW*/ 43, 34, 25, 16,  /*SE*/ 61,  /*S*/ 60,  /*N*/ 44, 36, 28, 20, 12, 4 },
            {  /*E*/ 54, 55,  /*W*/ 52, 51, 50, 49, 48,  /*NE*/ 46, 39,  /*SW*/ 60,  /*NW*/ 44, 35, 26, 17, 8,  /*SE*/ 62,  /*S*/ 61,  /*N*/ 45, 37, 29, 21, 13, 5 },
            {  /*E*/ 55,  /*W*/ 53, 52, 51, 50, 49, 48,  /*NE*/ 47,  /*SW*/ 61,  /*NW*/ 45, 36, 27, 18, 9, 0,  /*SE*/ 63,  /*S*/ 62,  /*N*/ 46, 38, 30, 22, 14, 6 },
            {  /*W*/ 54, 53, 52, 51, 50, 49, 48,  /*SW*/ 62,  /*NW*/ 46, 37, 28, 19, 10, 1,  /*S*/ 63,  /*N*/ 47, 39, 31, 23, 15, 7 },
            /* rank 1: */ {  /*E*/ 57, 58, 59, 60, 61, 62, 63,  /*NE*/ 49, 42, 35, 28, 21, 14, 7,  /*N*/ 48, 40, 32, 24, 16, 8, 0 },
            {  /*E*/ 58, 59, 60, 61, 62, 63,  /*W*/ 56,  /*NE*/ 50, 43, 36, 29, 22, 15,  /*NW*/ 48,  /*N*/ 49, 41, 33, 25, 17, 9, 1 },
            {  /*E*/ 59, 60, 61, 62, 63,  /*W*/ 57, 56,  /*NE*/ 51, 44, 37, 30, 23,  /*NW*/ 49, 40,  /*N*/ 50, 42, 34, 26, 18, 10, 2 },
            {  /*E*/ 60, 61, 62, 63,  /*W*/ 58, 57, 56,  /*NE*/ 52, 45, 38, 31,  /*NW*/ 50, 41, 32,  /*N*/ 51, 43, 35, 27, 19, 11, 3 },
            {  /*E*/ 61, 62, 63,  /*W*/ 59, 58, 57, 56,  /*NE*/ 53, 46, 39,  /*NW*/ 51, 42, 33, 24,  /*N*/ 52, 44, 36, 28, 20, 12, 4 },
            {  /*E*/ 62, 63,  /*W*/ 60, 59, 58, 57, 56,  /*NE*/ 54, 47,  /*NW*/ 52, 43, 34, 25, 16,  /*N*/ 53, 45, 37, 29, 21, 13, 5 },
            {  /*E*/ 63,  /*W*/ 61, 60, 59, 58, 57, 56,  /*NE*/ 55,  /*NW*/ 53, 44, 35, 26, 17, 8,  /*N*/ 54, 46, 38, 30, 22, 14, 6 },
            {  /*W*/ 62, 61, 60, 59, 58, 57, 56,  /*NW*/ 54, 45, 36, 27, 18, 9, 0,  /*N*/ 55, 47, 39, 31, 23, 15, 7 }
    };

    int[][] moveMatrix4Rook = new int[][] {
            /* rank 8: */ {  /*E*/ 1, 2, 3, 4, 5, 6, 7,  /*S*/ 8, 16, 24, 32, 40, 48, 56 },
            {  /*E*/ 2, 3, 4, 5, 6, 7,  /*W*/ 0,  /*S*/ 9, 17, 25, 33, 41, 49, 57 },
            {  /*E*/ 3, 4, 5, 6, 7,  /*W*/ 1, 0,  /*S*/ 10, 18, 26, 34, 42, 50, 58 },
            {  /*E*/ 4, 5, 6, 7,  /*W*/ 2, 1, 0,  /*S*/ 11, 19, 27, 35, 43, 51, 59 },
            {  /*E*/ 5, 6, 7,  /*W*/ 3, 2, 1, 0,  /*S*/ 12, 20, 28, 36, 44, 52, 60 },
            {  /*E*/ 6, 7,  /*W*/ 4, 3, 2, 1, 0,  /*S*/ 13, 21, 29, 37, 45, 53, 61 },
            {  /*E*/ 7,  /*W*/ 5, 4, 3, 2, 1, 0,  /*S*/ 14, 22, 30, 38, 46, 54, 62 },
            {  /*W*/ 6, 5, 4, 3, 2, 1, 0,  /*S*/ 15, 23, 31, 39, 47, 55, 63 },
            /* rank 7: */ {  /*E*/ 9, 10, 11, 12, 13, 14, 15,  /*S*/ 16, 24, 32, 40, 48, 56,  /*N*/ 0 },
            {  /*E*/ 10, 11, 12, 13, 14, 15,  /*W*/ 8,  /*S*/ 17, 25, 33, 41, 49, 57,  /*N*/ 1 },
            {  /*E*/ 11, 12, 13, 14, 15,  /*W*/ 9, 8,  /*S*/ 18, 26, 34, 42, 50, 58,  /*N*/ 2 },
            {  /*E*/ 12, 13, 14, 15,  /*W*/ 10, 9, 8,  /*S*/ 19, 27, 35, 43, 51, 59,  /*N*/ 3 },
            {  /*E*/ 13, 14, 15,  /*W*/ 11, 10, 9, 8,  /*S*/ 20, 28, 36, 44, 52, 60,  /*N*/ 4 },
            {  /*E*/ 14, 15,  /*W*/ 12, 11, 10, 9, 8,  /*S*/ 21, 29, 37, 45, 53, 61,  /*N*/ 5 },
            {  /*E*/ 15,  /*W*/ 13, 12, 11, 10, 9, 8,  /*S*/ 22, 30, 38, 46, 54, 62,  /*N*/ 6 },
            {  /*W*/ 14, 13, 12, 11, 10, 9, 8,  /*S*/ 23, 31, 39, 47, 55, 63,  /*N*/ 7 },
            /* rank 6: */ {  /*E*/ 17, 18, 19, 20, 21, 22, 23,  /*S*/ 24, 32, 40, 48, 56,  /*N*/ 8, 0 },
            {  /*E*/ 18, 19, 20, 21, 22, 23,  /*W*/ 16,  /*S*/ 25, 33, 41, 49, 57,  /*N*/ 9, 1 },
            {  /*E*/ 19, 20, 21, 22, 23,  /*W*/ 17, 16,  /*S*/ 26, 34, 42, 50, 58,  /*N*/ 10, 2 },
            {  /*E*/ 20, 21, 22, 23,  /*W*/ 18, 17, 16,  /*S*/ 27, 35, 43, 51, 59,  /*N*/ 11, 3 },
            {  /*E*/ 21, 22, 23,  /*W*/ 19, 18, 17, 16,  /*S*/ 28, 36, 44, 52, 60,  /*N*/ 12, 4 },
            {  /*E*/ 22, 23,  /*W*/ 20, 19, 18, 17, 16,  /*S*/ 29, 37, 45, 53, 61,  /*N*/ 13, 5 },
            {  /*E*/ 23,  /*W*/ 21, 20, 19, 18, 17, 16,  /*S*/ 30, 38, 46, 54, 62,  /*N*/ 14, 6 },
            {  /*W*/ 22, 21, 20, 19, 18, 17, 16,  /*S*/ 31, 39, 47, 55, 63,  /*N*/ 15, 7 },
            /* rank 5: */ {  /*E*/ 25, 26, 27, 28, 29, 30, 31,  /*S*/ 32, 40, 48, 56,  /*N*/ 16, 8, 0 },
            {  /*E*/ 26, 27, 28, 29, 30, 31,  /*W*/ 24,  /*S*/ 33, 41, 49, 57,  /*N*/ 17, 9, 1 },
            {  /*E*/ 27, 28, 29, 30, 31,  /*W*/ 25, 24,  /*S*/ 34, 42, 50, 58,  /*N*/ 18, 10, 2 },
            {  /*E*/ 28, 29, 30, 31,  /*W*/ 26, 25, 24,  /*S*/ 35, 43, 51, 59,  /*N*/ 19, 11, 3 },
            {  /*E*/ 29, 30, 31,  /*W*/ 27, 26, 25, 24,  /*S*/ 36, 44, 52, 60,  /*N*/ 20, 12, 4 },
            {  /*E*/ 30, 31,  /*W*/ 28, 27, 26, 25, 24,  /*S*/ 37, 45, 53, 61,  /*N*/ 21, 13, 5 },
            {  /*E*/ 31,  /*W*/ 29, 28, 27, 26, 25, 24,  /*S*/ 38, 46, 54, 62,  /*N*/ 22, 14, 6 },
            {  /*W*/ 30, 29, 28, 27, 26, 25, 24,  /*S*/ 39, 47, 55, 63,  /*N*/ 23, 15, 7 },
            /* rank 4: */ {  /*E*/ 33, 34, 35, 36, 37, 38, 39,  /*S*/ 40, 48, 56,  /*N*/ 24, 16, 8, 0 },
            {  /*E*/ 34, 35, 36, 37, 38, 39,  /*W*/ 32,  /*S*/ 41, 49, 57,  /*N*/ 25, 17, 9, 1 },
            {  /*E*/ 35, 36, 37, 38, 39,  /*W*/ 33, 32,  /*S*/ 42, 50, 58,  /*N*/ 26, 18, 10, 2 },
            {  /*E*/ 36, 37, 38, 39,  /*W*/ 34, 33, 32,  /*S*/ 43, 51, 59,  /*N*/ 27, 19, 11, 3 },
            {  /*E*/ 37, 38, 39,  /*W*/ 35, 34, 33, 32,  /*S*/ 44, 52, 60,  /*N*/ 28, 20, 12, 4 },
            {  /*E*/ 38, 39,  /*W*/ 36, 35, 34, 33, 32,  /*S*/ 45, 53, 61,  /*N*/ 29, 21, 13, 5 },
            {  /*E*/ 39,  /*W*/ 37, 36, 35, 34, 33, 32,  /*S*/ 46, 54, 62,  /*N*/ 30, 22, 14, 6 },
            {  /*W*/ 38, 37, 36, 35, 34, 33, 32,  /*S*/ 47, 55, 63,  /*N*/ 31, 23, 15, 7 },
            /* rank 3: */ {  /*E*/ 41, 42, 43, 44, 45, 46, 47,  /*S*/ 48, 56,  /*N*/ 32, 24, 16, 8, 0 },
            {  /*E*/ 42, 43, 44, 45, 46, 47,  /*W*/ 40,  /*S*/ 49, 57,  /*N*/ 33, 25, 17, 9, 1 },
            {  /*E*/ 43, 44, 45, 46, 47,  /*W*/ 41, 40,  /*S*/ 50, 58,  /*N*/ 34, 26, 18, 10, 2 },
            {  /*E*/ 44, 45, 46, 47,  /*W*/ 42, 41, 40,  /*S*/ 51, 59,  /*N*/ 35, 27, 19, 11, 3 },
            {  /*E*/ 45, 46, 47,  /*W*/ 43, 42, 41, 40,  /*S*/ 52, 60,  /*N*/ 36, 28, 20, 12, 4 },
            {  /*E*/ 46, 47,  /*W*/ 44, 43, 42, 41, 40,  /*S*/ 53, 61,  /*N*/ 37, 29, 21, 13, 5 },
            {  /*E*/ 47,  /*W*/ 45, 44, 43, 42, 41, 40,  /*S*/ 54, 62,  /*N*/ 38, 30, 22, 14, 6 },
            {  /*W*/ 46, 45, 44, 43, 42, 41, 40,  /*S*/ 55, 63,  /*N*/ 39, 31, 23, 15, 7 },
            /* rank 2: */ {  /*E*/ 49, 50, 51, 52, 53, 54, 55,  /*S*/ 56,  /*N*/ 40, 32, 24, 16, 8, 0 },
            {  /*E*/ 50, 51, 52, 53, 54, 55,  /*W*/ 48,  /*S*/ 57,  /*N*/ 41, 33, 25, 17, 9, 1 },
            {  /*E*/ 51, 52, 53, 54, 55,  /*W*/ 49, 48,  /*S*/ 58,  /*N*/ 42, 34, 26, 18, 10, 2 },
            {  /*E*/ 52, 53, 54, 55,  /*W*/ 50, 49, 48,  /*S*/ 59,  /*N*/ 43, 35, 27, 19, 11, 3 },
            {  /*E*/ 53, 54, 55,  /*W*/ 51, 50, 49, 48,  /*S*/ 60,  /*N*/ 44, 36, 28, 20, 12, 4 },
            {  /*E*/ 54, 55,  /*W*/ 52, 51, 50, 49, 48,  /*S*/ 61,  /*N*/ 45, 37, 29, 21, 13, 5 },
            {  /*E*/ 55,  /*W*/ 53, 52, 51, 50, 49, 48,  /*S*/ 62,  /*N*/ 46, 38, 30, 22, 14, 6 },
            {  /*W*/ 54, 53, 52, 51, 50, 49, 48,  /*S*/ 63,  /*N*/ 47, 39, 31, 23, 15, 7 },
            /* rank 1: */ {  /*E*/ 57, 58, 59, 60, 61, 62, 63,  /*N*/ 48, 40, 32, 24, 16, 8, 0 },
            {  /*E*/ 58, 59, 60, 61, 62, 63,  /*W*/ 56,  /*N*/ 49, 41, 33, 25, 17, 9, 1 },
            {  /*E*/ 59, 60, 61, 62, 63,  /*W*/ 57, 56,  /*N*/ 50, 42, 34, 26, 18, 10, 2 },
            {  /*E*/ 60, 61, 62, 63,  /*W*/ 58, 57, 56,  /*N*/ 51, 43, 35, 27, 19, 11, 3 },
            {  /*E*/ 61, 62, 63,  /*W*/ 59, 58, 57, 56,  /*N*/ 52, 44, 36, 28, 20, 12, 4 },
            {  /*E*/ 62, 63,  /*W*/ 60, 59, 58, 57, 56,  /*N*/ 53, 45, 37, 29, 21, 13, 5 },
            {  /*E*/ 63,  /*W*/ 61, 60, 59, 58, 57, 56,  /*N*/ 54, 46, 38, 30, 22, 14, 6 },
            {  /*W*/ 62, 61, 60, 59, 58, 57, 56,  /*N*/ 55, 47, 39, 31, 23, 15, 7 }
    };

    int[][] moveMatrix4Bishop = new int[][] {
            /* rank 8: */ {  /*SE*/ 9, 18, 27, 36, 45, 54, 63 },
            {  /*SW*/ 8,  /*SE*/ 10, 19, 28, 37, 46, 55 },
            {  /*SW*/ 9, 16,  /*SE*/ 11, 20, 29, 38, 47 },
            {  /*SW*/ 10, 17, 24,  /*SE*/ 12, 21, 30, 39 },
            {  /*SW*/ 11, 18, 25, 32,  /*SE*/ 13, 22, 31 },
            {  /*SW*/ 12, 19, 26, 33, 40,  /*SE*/ 14, 23 },
            {  /*SW*/ 13, 20, 27, 34, 41, 48,  /*SE*/ 15 },
            {  /*SW*/ 14, 21, 28, 35, 42, 49, 56 },
            /* rank 7: */ {  /*NE*/ 1,  /*SE*/ 17, 26, 35, 44, 53, 62 },
            {  /*NW*/ 0,  /*NE*/ 2,  /*SW*/ 16,  /*SE*/ 18, 27, 36, 45, 54, 63 },
            {  /*NW*/ 1,  /*NE*/ 3,  /*SW*/ 17, 24,  /*SE*/ 19, 28, 37, 46, 55 },
            {  /*NW*/ 2,  /*NE*/ 4,  /*SW*/ 18, 25, 32,  /*SE*/ 20, 29, 38, 47 },
            {  /*NW*/ 3,  /*NE*/ 5,  /*SW*/ 19, 26, 33, 40,  /*SE*/ 21, 30, 39 },
            {  /*NW*/ 4,  /*NE*/ 6,  /*SW*/ 20, 27, 34, 41, 48,  /*SE*/ 22, 31 },
            {  /*NW*/ 5,  /*NE*/ 7,  /*SW*/ 21, 28, 35, 42, 49, 56,  /*SE*/ 23 },
            {  /*NW*/ 6,  /*SW*/ 22, 29, 36, 43, 50, 57 },
            /* rank 6: */ {  /*NE*/ 9, 2,  /*SE*/ 25, 34, 43, 52, 61 },
            {  /*NW*/ 8,  /*NE*/ 10, 3,  /*SW*/ 24,  /*SE*/ 26, 35, 44, 53, 62 },
            {  /*NW*/ 9, 0,  /*NE*/ 11, 4,  /*SW*/ 25, 32,  /*SE*/ 27, 36, 45, 54, 63 },
            {  /*NW*/ 10, 1,  /*NE*/ 12, 5,  /*SW*/ 26, 33, 40,  /*SE*/ 28, 37, 46, 55 },
            {  /*NW*/ 11, 2,  /*NE*/ 13, 6,  /*SW*/ 27, 34, 41, 48,  /*SE*/ 29, 38, 47 },
            {  /*NW*/ 12, 3,  /*NE*/ 14, 7,  /*SW*/ 28, 35, 42, 49, 56,  /*SE*/ 30, 39 },
            {  /*NW*/ 13, 4,  /*NE*/ 15,  /*SW*/ 29, 36, 43, 50, 57,  /*SE*/ 31 },
            {  /*NW*/ 14, 5,  /*SW*/ 30, 37, 44, 51, 58 },
            /* rank 5: */ {  /*NE*/ 17, 10, 3,  /*SE*/ 33, 42, 51, 60 },
            {  /*NW*/ 16,  /*NE*/ 18, 11, 4,  /*SW*/ 32,  /*SE*/ 34, 43, 52, 61 },
            {  /*NW*/ 17, 8,  /*NE*/ 19, 12, 5,  /*SW*/ 33, 40,  /*SE*/ 35, 44, 53, 62 },
            {  /*NW*/ 18, 9, 0,  /*NE*/ 20, 13, 6,  /*SW*/ 34, 41, 48,  /*SE*/ 36, 45, 54, 63 },
            {  /*NW*/ 19, 10, 1,  /*NE*/ 21, 14, 7,  /*SW*/ 35, 42, 49, 56,  /*SE*/ 37, 46, 55 },
            {  /*NW*/ 20, 11, 2,  /*NE*/ 22, 15,  /*SW*/ 36, 43, 50, 57,  /*SE*/ 38, 47 },
            {  /*NW*/ 21, 12, 3,  /*NE*/ 23,  /*SW*/ 37, 44, 51, 58,  /*SE*/ 39 },
            {  /*NW*/ 22, 13, 4,  /*SW*/ 38, 45, 52, 59 },
            /* rank 4: */ {  /*NE*/ 25, 18, 11, 4,  /*SE*/ 41, 50, 59 },
            {  /*NW*/ 24,  /*NE*/ 26, 19, 12, 5,  /*SW*/ 40,  /*SE*/ 42, 51, 60 },
            {  /*NW*/ 25, 16,  /*NE*/ 27, 20, 13, 6,  /*SW*/ 41, 48,  /*SE*/ 43, 52, 61 },
            {  /*NW*/ 26, 17, 8,  /*NE*/ 28, 21, 14, 7,  /*SW*/ 42, 49, 56,  /*SE*/ 44, 53, 62 },
            {  /*NW*/ 27, 18, 9, 0,  /*NE*/ 29, 22, 15,  /*SW*/ 43, 50, 57,  /*SE*/ 45, 54, 63 },
            {  /*NW*/ 28, 19, 10, 1,  /*NE*/ 30, 23,  /*SW*/ 44, 51, 58,  /*SE*/ 46, 55 },
            {  /*NW*/ 29, 20, 11, 2,  /*NE*/ 31,  /*SW*/ 45, 52, 59,  /*SE*/ 47 },
            {  /*NW*/ 30, 21, 12, 3,  /*SW*/ 46, 53, 60 },
            /* rank 3: */ {  /*NE*/ 33, 26, 19, 12, 5,  /*SE*/ 49, 58 },
            {  /*NW*/ 32,  /*NE*/ 34, 27, 20, 13, 6,  /*SW*/ 48,  /*SE*/ 50, 59 },
            {  /*NW*/ 33, 24,  /*NE*/ 35, 28, 21, 14, 7,  /*SW*/ 49, 56,  /*SE*/ 51, 60 },
            {  /*NW*/ 34, 25, 16,  /*NE*/ 36, 29, 22, 15,  /*SW*/ 50, 57,  /*SE*/ 52, 61 },
            {  /*NW*/ 35, 26, 17, 8,  /*NE*/ 37, 30, 23,  /*SW*/ 51, 58,  /*SE*/ 53, 62 },
            {  /*NW*/ 36, 27, 18, 9, 0,  /*NE*/ 38, 31,  /*SW*/ 52, 59,  /*SE*/ 54, 63 },
            {  /*NW*/ 37, 28, 19, 10, 1,  /*NE*/ 39,  /*SW*/ 53, 60,  /*SE*/ 55 },
            {  /*NW*/ 38, 29, 20, 11, 2,  /*SW*/ 54, 61 },
            /* rank 2: */ {  /*NE*/ 41, 34, 27, 20, 13, 6,  /*SE*/ 57 },
            {  /*NW*/ 40,  /*NE*/ 42, 35, 28, 21, 14, 7,  /*SW*/ 56,  /*SE*/ 58 },
            {  /*NW*/ 41, 32,  /*NE*/ 43, 36, 29, 22, 15,  /*SW*/ 57,  /*SE*/ 59 },
            {  /*NW*/ 42, 33, 24,  /*NE*/ 44, 37, 30, 23,  /*SW*/ 58,  /*SE*/ 60 },
            {  /*NW*/ 43, 34, 25, 16,  /*NE*/ 45, 38, 31,  /*SW*/ 59,  /*SE*/ 61 },
            {  /*NW*/ 44, 35, 26, 17, 8,  /*NE*/ 46, 39,  /*SW*/ 60,  /*SE*/ 62 },
            {  /*NW*/ 45, 36, 27, 18, 9, 0,  /*NE*/ 47,  /*SW*/ 61,  /*SE*/ 63 },
            {  /*NW*/ 46, 37, 28, 19, 10, 1,  /*SW*/ 62 },
            /* rank 1: */ {  /*NE*/ 49, 42, 35, 28, 21, 14, 7 },
            {  /*NW*/ 48,  /*NE*/ 50, 43, 36, 29, 22, 15 },
            {  /*NW*/ 49, 40,  /*NE*/ 51, 44, 37, 30, 23 },
            {  /*NW*/ 50, 41, 32,  /*NE*/ 52, 45, 38, 31 },
            {  /*NW*/ 51, 42, 33, 24,  /*NE*/ 53, 46, 39 },
            {  /*NW*/ 52, 43, 34, 25, 16,  /*NE*/ 54, 47 },
            {  /*NW*/ 53, 44, 35, 26, 17, 8,  /*NE*/ 55 },
            {  /*NW*/ 54, 45, 36, 27, 18, 9, 0 }
    };

    int[][] moveMatrix4Knight = new int[][] {
            /* rank 8: */ { 10, 17 },  { 11, 16, 18 },  { 8, 12, 17, 19 },  { 9, 13, 18, 20 },  { 10, 14, 19, 21 },  { 11, 15, 20, 22 },  { 12, 21, 23 },  { 13, 22 },
            /* rank 7: */ { 2, 18, 25 },  { 3, 19, 24, 26 },  { 0, 4, 16, 20, 25, 27 },  { 1, 5, 17, 21, 26, 28 },  { 2, 6, 18, 22, 27, 29 },  { 3, 7, 19, 23, 28, 30 },  { 4, 20, 29, 31 },  { 5, 21, 30 },
            /* rank 6: */ { 1, 10, 26, 33 },  { 0, 2, 11, 27, 32, 34 },  { 1, 3, 8, 12, 24, 28, 33, 35 },  { 2, 4, 9, 13, 25, 29, 34, 36 },  { 3, 5, 10, 14, 26, 30, 35, 37 },  { 4, 6, 11, 15, 27, 31, 36, 38 },  { 5, 7, 12, 28, 37, 39 },  { 6, 13, 29, 38 },
            /* rank 5: */ { 9, 18, 34, 41 },  { 8, 10, 19, 35, 40, 42 },  { 9, 11, 16, 20, 32, 36, 41, 43 },  { 10, 12, 17, 21, 33, 37, 42, 44 },  { 11, 13, 18, 22, 34, 38, 43, 45 },  { 12, 14, 19, 23, 35, 39, 44, 46 },  { 13, 15, 20, 36, 45, 47 },  { 14, 21, 37, 46 },
            /* rank 4: */ { 17, 26, 42, 49 },  { 16, 18, 27, 43, 48, 50 },  { 17, 19, 24, 28, 40, 44, 49, 51 },  { 18, 20, 25, 29, 41, 45, 50, 52 },  { 19, 21, 26, 30, 42, 46, 51, 53 },  { 20, 22, 27, 31, 43, 47, 52, 54 },  { 21, 23, 28, 44, 53, 55 },  { 22, 29, 45, 54 },
            /* rank 3: */ { 25, 34, 50, 57 },  { 24, 26, 35, 51, 56, 58 },  { 25, 27, 32, 36, 48, 52, 57, 59 },  { 26, 28, 33, 37, 49, 53, 58, 60 },  { 27, 29, 34, 38, 50, 54, 59, 61 },  { 28, 30, 35, 39, 51, 55, 60, 62 },  { 29, 31, 36, 52, 61, 63 },  { 30, 37, 53, 62 },
            /* rank 2: */ { 33, 42, 58 },  { 32, 34, 43, 59 },  { 33, 35, 40, 44, 56, 60 },  { 34, 36, 41, 45, 57, 61 },  { 35, 37, 42, 46, 58, 62 },  { 36, 38, 43, 47, 59, 63 },  { 37, 39, 44, 60 },  { 38, 45, 61 },
            /* rank 1: */ { 41, 50 },  { 40, 42, 51 },  { 41, 43, 48, 52 },  { 42, 44, 49, 53 },  { 43, 45, 50, 54 },  { 44, 46, 51, 55 },  { 45, 47, 52 },  { 46, 53 }
    };


    int[][] moveMatrix4PawnWhite = new int[][] {
            /* rank 8: */ {}, {}, {}, {}, {}, {}, {}, {},
            /* rank 7: */ { 0, 1 },  { 0, 1, 2 },  { 1, 2, 3 },  { 2, 3, 4 },  { 3, 4, 5 },  { 4, 5, 6 },  { 5, 6, 7 },  { 6, 7 },
            /* rank 6: */ { 8, 9 },  { 8, 9, 10 },  { 9, 10, 11 },  { 10, 11, 12 },  { 11, 12, 13 },  { 12, 13, 14 },  { 13, 14, 15 },  { 14, 15 },
            /* rank 5: */ { 16, 17 },  { 16, 17, 18 },  { 17, 18, 19 },  { 18, 19, 20 },  { 19, 20, 21 },  { 20, 21, 22 },  { 21, 22, 23 },  { 22, 23 },
            /* rank 4: */ { 24, 25 },  { 24, 25, 26 },  { 25, 26, 27 },  { 26, 27, 28 },  { 27, 28, 29 },  { 28, 29, 30 },  { 29, 30, 31 },  { 30, 31 },
            /* rank 3: */ { 32, 33 },  { 32, 33, 34 },  { 33, 34, 35 },  { 34, 35, 36 },  { 35, 36, 37 },  { 36, 37, 38 },  { 37, 38, 39 },  { 38, 39 },
            /* rank 2: */ { 40, 41 },  { 40, 41, 42 },  { 41, 42, 43 },  { 42, 43, 44 },  { 43, 44, 45 },  { 44, 45, 46 },  { 45, 46, 47 },  { 46, 47 },
            /* rank 1: */ {}, {}, {}, {}, {}, {}, {}, {},
    };

    int[][] moveMatrix4PawnBlack = new int[][] {
            /* rank 8: */ {}, {}, {}, {}, {}, {}, {}, {},
            /* rank 7: */ { 16, 17 },  { 16, 17, 18 },  { 17, 18, 19 },  { 18, 19, 20 },  { 19, 20, 21 },  { 20, 21, 22 },  { 21, 22, 23 },  { 22, 23 },
            /* rank 6: */ { 24, 25 },  { 24, 25, 26 },  { 25, 26, 27 },  { 26, 27, 28 },  { 27, 28, 29 },  { 28, 29, 30 },  { 29, 30, 31 },  { 30, 31 },
            /* rank 5: */ { 32, 33 },  { 32, 33, 34 },  { 33, 34, 35 },  { 34, 35, 36 },  { 35, 36, 37 },  { 36, 37, 38 },  { 37, 38, 39 },  { 38, 39 },
            /* rank 4: */ { 40, 41 },  { 40, 41, 42 },  { 41, 42, 43 },  { 42, 43, 44 },  { 43, 44, 45 },  { 44, 45, 46 },  { 45, 46, 47 },  { 46, 47 },
            /* rank 3: */ { 48, 49 },  { 48, 49, 50 },  { 49, 50, 51 },  { 50, 51, 52 },  { 51, 52, 53 },  { 52, 53, 54 },  { 53, 54, 55 },  { 54, 55 },
            /* rank 2: */ { 56, 57 },  { 56, 57, 58 },  { 57, 58, 59 },  { 58, 59, 60 },  { 59, 60, 61 },  { 60, 61, 62 },  { 61, 62, 63 },  { 62, 63 },
            /* rank 1: */ {}, {}, {}, {}, {}, {}, {}, {},
    };

}
