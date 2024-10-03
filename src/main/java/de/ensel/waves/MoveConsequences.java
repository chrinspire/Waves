package de.ensel.waves;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static de.ensel.chessbasics.ChessBasics.*;
import static java.lang.Math.max;

public class MoveConsequences {

    private final Set<Move>[] enabledMoves  = (Set<Move>[])new Set<?>[2];   // moves that are made legal or covering - unless contained in interestingMoves
    private final Set<Move>[] interestingMoves = (Set<Move>[])new Set<?>[2];   // interesting moves (capturing or todo: checking) - no matter if enabled or prepared
    private final Set<Move>[] preparedMoves = (Set<Move>[])new Set<?>[2];   // moves that will take more tempi in the future - unless contained in interestingMoves
    private final Set<Move>[] blockedMoves  = (Set<Move>[])new Set<?>[2];   // moves that are made no longer legal or covering  - either forever (because of killed piece) or delayed (then FullConseq-methods do not put it here, but in the following delayed-list)
    private final Set<Move>[] delayedMoves  = (Set<Move>[])new Set<?>[2];   // moves that will take more tempi in the future
    private final Set<Move>[] changedEvalMoves = (Set<Move>[])new Set<?>[2];// unchanged legality, but evaluation of move changes - should also be considered interesting
    private boolean foundInterestingBlock = false;

    public MoveConsequences() {
        for( int c = CIWHITE; c <= CIBLACK; c++) {
            enabledMoves[c] = new HashSet<>();
            interestingMoves[c] = new HashSet<>();
            preparedMoves[c] = new HashSet<>();
            blockedMoves[c] = new HashSet<>();
            delayedMoves[c] = new HashSet<>();
            changedEvalMoves[c] = new HashSet<>();
        }
    }

    @Deprecated
    static MoveConsequences calcMoveConsequences(final VBoard preBoard, Move move) {
        // todo!: it was easy for a first test, to "createNext" the postboard, but actually all the consequences
        //  should be derived without having to create the postBoard...
        VBoard postBoard = preBoard.createNext(move);
        if (postBoard == null)
            return null;  // may happen for not yet legal moves (enabled ones), which would still be non-legal. e.g. move king on occupied square
        return calcMoveConsequences(preBoard, move, postBoard);
    }

    @Deprecated
    static MoveConsequences calcMoveConsequences(final VBoard preBoard, Move move, final VBoard postBoard) {
        MoveConsequences conseqs = new MoveConsequences();
        Square fromSq = move.fromSq();
        Square toSq = move.toSq();
        int color = move.piece().color();
        int oppColor = opponentColor(color);

        // a) include moves that now could take this piece at the toPos
        // actually can only generate new moves for capturing pawns - all other pieces could already move here,
        toSq.getSingleMovesToHere(oppColor, postBoard)   // TODO: must later also include moves that are blocked multiple times by several pieces
                .forEach(oppMove -> {
                            if (isPawn(oppMove.piece().pieceType()))
                                conseqs.addEnabledMove(oppMove);
                            else
                                conseqs.addChangedEvalMove(oppMove);
                        });

        // b) counter effects of moves (resp. just of one move) that would have taken this piece here
        fromSq.getSingleMovesToHere(oppColor, postBoard)   // TODO: must later also include moves that are blocked multiple times by several pieces
                .forEach(conseqs::addChangedEvalMove);

        // x) also my own pieces can now move here
        fromSq.getSingleMovesToHere(color, postBoard)   // TODO: must later also include moves that are blocked multiple times by several pieces
                .forEach(conseqs::addEnabledMove);

        // c) enable moves that now can slide over this square here
        postBoard.getSingleMovesStreamSlidingOver(fromSq)
                .filter(slidingOverFromMove -> slidingOverFromMove.piece() != move.piece()
                        && slidingOverFromMove.to() != move.to()  // these cases were covered in a) already
                        && slidingOverFromMove.isCoveringAfter(postBoard))  // necessary? (yes, as long as we leave out moves blocked by several pieces in the way)
                .forEach(conseqs::addEnabledMove);

        // e+f) see where the moving piece can go to (move/capture/cover) from toPos onward
        // except, where it could already go to before
        Set<Move> unchangedCoverages = new HashSet<>();
        // todo: at the moment this leaves out pawn capture moves that are not possible (empty square), however it is covering there, so this might be missing at some point...
        postBoard.getSingleMovesStreamFromPce(move.piece())
                .forEach(nextmove -> {
                    Move prevMove2mTo = nextmove.piece().getMove(move.from(), nextmove.to());
                    if (prevMove2mTo != null && nextmove.piece().isCoveringTargetAfter(prevMove2mTo, preBoard)) {
                        // it was already covered before and also is now, so no difference
                        unchangedCoverages.add(prevMove2mTo);
                    }
                    else {
                        conseqs.addEnabledMove(nextmove);
                    }
                });

        // d) delay sliding moves that are now blocked at the toPos
        //Todo: check, does this treat the following correctly?:
        //  also process blocked 1-hop moves incl. formerly capturing pawns and straight moving pawns,
        //  as well as straight moving opponent pawns
        ChessPiece capturedPce = preBoard.getPieceAt(move.to());
        if (capturedPce == null) {
            // there was nobody there, so we really block the toSq
            postBoard.getSingleMovesStreamSlidingOver(toSq)
                    .filter(m -> m.piece() != move.piece()
                            // && m.to() != move.from()          // was, but why?: do not count blocking moves to my ex position
                            && m.isCoveringAfter(preBoard)      // it was covering before
                            && !m.isCoveringAfter(postBoard))     // but not any more
                    .forEach(conseqs::addBlockedMove);
        }
        else {
            // it was already blocked, but
            // h) we take away the chances of the captured piece
            capturedPce.coveringMovesStreamAfter(preBoard)
                    .forEach(conseqs::addBlockedMove);
        }

        // i+g) "leftovers" from f)... what prev. moves, coverings and captures of my piece are no more
        move.piece().coveringMovesStreamAfter(preBoard)
                .filter(m ->  m.to() != move.from()   // to my old self - should not occur anyway
                        // && m.to() != move.to()     // to my old self - this was covered, but is no longer, so this condition is commented out
                        && !unchangedCoverages.contains(m))
                .forEach(conseqs::addBlockedMove);
        return conseqs;
    }

    /**
     * !side effects: 1) sets/changes minTempi of enabled moves and
     *      2) sets at move if it enables interesting moves.
     *      3) informes/sets enabled moves about this move as precondition
     *      4) stores the identical consequences object as returned also in move
     * @param vBoard
     * @param move
     * @return filled moveConsequence for move
     */
    static MoveConsequences calcMoveFullConseq(final VBoard vBoard, Move move) {
        final MoveConsequences conseqs = new MoveConsequences();
        final Square fromSq = move.fromSq();
        final Square toSq = move.toSq();
        final int color = move.piece().color();
        final int oppColor = opponentColor(color);
        final ChessPiece capturedPce = vBoard.getPieceAt(move.to());

        // a) include moves that now could take this piece at the toPos
        // actually can only generate new moves for capturing pawns (but not even new coverages) - all other pieces could already move here,
        toSq.getMovesToHere(oppColor)
                .forEach(oppMove -> {
                    if (isPawn(oppMove.piece().pieceType())) {
                        if ( ((ChessPiecePawn)oppMove.piece()).pawnmoveOnPiecePossible(   // a pawn that could capture there
                                        oppMove.from(), oppMove.to(), move.piece()) ) {
                            conseqs.addInterestingEnabledOrPreparedMove(vBoard, oppMove, true);
                            minimizeIncreasedMinTempiOfFollowUpMoveByAfter(oppMove, 1, vBoard, move);
                            oppMove.addPreCond(move);
                        }
                    }
                    else {
                        conseqs.addChangedEvalMove(oppMove);
                        minimizeIncreasedMinTempiOfFollowUpMoveByAfter(oppMove, 1, vBoard, move);
                    }
                });

        // b) counter effects of moves (resp. just of one move) that would have taken this piece here
        fromSq.getMovesToHere(oppColor)
                .forEach(oppMove -> {
                    if (isPawn(oppMove.piece().pieceType())) {
                        if ( ((ChessPiecePawn)oppMove.piece()).pawnmoveOnPiecePossible(   // a pawn that could have captured the mover on fromPos
                                oppMove.from(), oppMove.to(), move.piece()) ) {
                            if (vBoard.getPiecePos(oppMove.piece()) == oppMove.from()) {  // a direct (not later) pawn move
                                conseqs.addBlockedMove(oppMove);
                                conseqs.setFoundInterestingBlock();
                            }
                            else
                                conseqs.addDelayedMove(oppMove);
                        }
                    }
                    else {
                        conseqs.addChangedEvalMove(oppMove);
                        minimizeIncreasedMinTempiOfFollowUpMoveByAfter(oppMove, 1, vBoard, move);
                    }
                });

        // x) also my own pieces can now move here
        fromSq.getMovesToHere(color)
                .forEach(sameColMove -> {
                    if (isPawn(sameColMove.piece().pieceType())) {
                        if ( ((ChessPiecePawn)sameColMove.piece()).pawnmoveOnPiecePossible(   // a pawn that could move straight to freed mover's fromPos
                                sameColMove.from(), sameColMove.to(), null) ) {
                            conseqs.addInterestingEnabledOrPreparedMove(vBoard, sameColMove, false);  // could become interesting if checking
                            minimizeIncreasedMinTempiOfFollowUpMoveByAfter(sameColMove, 2, vBoard, move);  // needs to wait for an intermediate opponent move
                        }
                    }
                    else {
                        conseqs.addChangedEvalMove(sameColMove);
                        minimizeIncreasedMinTempiOfFollowUpMoveByAfter(sameColMove, 2, vBoard, move);
                    }
                    sameColMove.addPreCond(move);
                });

        // c) enable moves that now can slide over this square here - there can be 2 cases:
        //  delayed ones can become direct ones or are at least prepared to get one step closer
        fromSq.getMovesSlidingOver()
                .filter(slidingOverFromMove -> slidingOverFromMove.piece() != move.piece()
                        && slidingOverFromMove.to() != move.to())  // these cases were covered in a) already
                .filter(slidingMove -> !isBetweenFromAndTo(move.to(), slidingMove.from(), slidingMove.to()) ) // does not re-block at its toSquare
                .forEach(slidingMove -> {
                    int[] nrOfBlockers = vBoard.countBlockerForMove(slidingMove);
                    if (nrOfBlockers[CIWHITE]+nrOfBlockers[CIBLACK] == 1) {   // mover was the only blocker
                        conseqs.addInterestingEnabledOrPreparedMove(vBoard, slidingMove, false);
                    }
                    else {
                        conseqs.addInterestingOrPreparedMove(vBoard, slidingMove);
                    }
                    final int delay = vBoard.getSlidingDelay(nrOfBlockers, slidingMove, color);
                    minimizeIncreasedMinTempiOfFollowUpMoveByAfter(slidingMove, delay, vBoard, move);
                    slidingMove.addPreCond(move);
                });

        // e+f) see where the moving piece can go to (move/capture/cover) from toPos onward
        // except, where it could already have gone to before
        Set<Move> unchangedCoverages = new HashSet<>();
        // todo: at the moment this leaves out pawn capture moves that are not possible (empty square), however it is covering there, so this might be missing at some point...
        move.piece().allMovesFrom(move.to())
                .forEach(nextmove -> {
                    int[] newBlockersCount = vBoard.countBlockerForMove(nextmove);
                    // todo-think: if target was already covered before and also is now, there is actually no difference in
                    //  consequences, but still one move enabled, one disabled? (as they could be blocked differently...)
                    //      Move prevMove2mTo = nextmove.piece().getMove(move.from(), nextmove.to());
                    //      if (prevMove2mTo != null) ...
                    if (newBlockersCount[CIWHITE] > 0 || newBlockersCount[CIBLACK] > 0)
                        conseqs.addInterestingOrPreparedMove(vBoard, nextmove);
                    else
                        conseqs.addInterestingOrEnabledMove(vBoard, nextmove);
                    final int delay = vBoard.getSlidingDelay(newBlockersCount, nextmove, color);               // opponent can slide directly after, but I myself have to wait until after opponents ply in any case
                    minimizeIncreasedMinTempiOfFollowUpMoveByAfter(nextmove, delay, vBoard, move);
                    nextmove.addPreCond(move);
                });

        // d) delay sliding moves that are now blocked at the toPos
        //Todo: check, does this treat the following correctly?:
        //  also process blocked 1-hop moves incl. formerly capturing pawns and straight moving pawns,
        //  as well as straight moving opponent pawns
        if (capturedPce == null) {
            // there was nobody there, so we really block the toSq
            toSq.getMovesSlidingOver()
                    .filter(slidingOverFromMove -> slidingOverFromMove.piece() != move.piece())  // unless myself  - todo: should these be blocked anyway? they are not possilbe any more - although the piece can still reach their target in the same way...
                    .forEach(slidingMove -> {
                        int[] newBlockersCount = vBoard.countBlockerForMove(slidingMove);
                        if (newBlockersCount[CIWHITE] + newBlockersCount[CIBLACK] == 0      // mover is now the first and only blocker
                                && !isBetweenFromAndTo(move.from(), slidingMove.from(), slidingMove.to())) {  // and was not already a blocker at fromSq before
                            conseqs.addBlockedMove(slidingMove);
                            if (isInteresting(vBoard, slidingMove))
                                conseqs.setFoundInterestingBlock();
                        }
                        else
                            conseqs.addDelayedMove(slidingMove);
                    });
        }
        else {
            // it was already blocked, but
            // h) we take away the chances of the captured piece
            capturedPce.allMovesFrom(move.to())
                    .forEach(conseqs::addBlockedMove);
            conseqs.setFoundInterestingBlock();
        }

        // i+g) "leftovers" from f)... what prev. moves, coverings and captures of my piece are no more
        move.piece().allMovesFrom(move.from())
                .filter(myOtherMove ->  !unchangedCoverages.contains(myOtherMove))
                .forEach(blockedMove -> {
                    conseqs.addBlockedMove(blockedMove);
                    if (isInteresting(vBoard, blockedMove))
                        conseqs.setFoundInterestingBlock();
                });
        move.setConsequences(conseqs);
        return conseqs;
    }

    private static void minimizeIncreasedMinTempiOfFollowUpMoveByAfter(final Move followUpMove, final int delta, final VBoard vBoard, final Move move) {
        if (vBoard.getPiecePos(move.piece()) == move.from())
            followUpMove.minimizeMinTempi(move.getMinTempi() + delta);  // needs to wait for an intermediate opponent move
        else ; // TODO: count still blocking pieces and add to nextMinTempi
    }

//    private List<Move> clashMoves = new ArrayList<>();
//
//    public void setClashMoves(List<Move> clashMoves) {
//        this.clashMoves = clashMoves;
//    }


    /// getter
    public Set<Move> getEnabledMoves(int color) {
        return enabledMoves[color];
    }

    public Stream<Move> getEnabledMoves() {
        return Stream.concat(enabledMoves[CIWHITE].stream(), enabledMoves[CIBLACK].stream());
    }

    public Set<Move> getInterestingMoves(int color) {
        return interestingMoves[color];
    }

    public Stream<Move> getInterestingMoves() {
        return Stream.concat(interestingMoves[CIWHITE].stream(), interestingMoves[CIBLACK].stream());
    }

    public Set<Move> getBlockedMoves(int color) {
        return blockedMoves[color];
    }

    public Stream<Move> getBlockedMoves() {
        return Stream.concat(blockedMoves[CIWHITE].stream(), blockedMoves[CIBLACK].stream());
    }

    public Set<Move> getChangedEvalMoves(int color) {
        return changedEvalMoves[color];
    }

    public Stream<Move> getChangedEvalMoves() {
        return Stream.concat(changedEvalMoves[CIWHITE].stream(), changedEvalMoves[CIBLACK].stream());
    }

    public Set<Move> getPreparedMoves(int color) {
        return preparedMoves[color];
    }

    public Set<Move> getDelayedMoves(int color) {
        return delayedMoves[color];
    }

    public boolean hasFoundInterestingFollowUp() {
        return foundInterestingBlock
                || !interestingMoves[CIWHITE].isEmpty()
                || !interestingMoves[CIBLACK].isEmpty()
                || !changedEvalMoves[CIWHITE].isEmpty()
                || !changedEvalMoves[CIBLACK].isEmpty();
    }


    //// setter
    private void setFoundInterestingBlock() {
        foundInterestingBlock = true;
    }

    public void addBlockedMove(Move blockedMove) {
        blockedMoves[blockedMove.piece().color()].add(blockedMove);
    }

    public void addEnabledMove(Move enabledMove) {
        enabledMoves[enabledMove.piece().color()].add(enabledMove);
    }

    public void addInterestingMove(Move move) {
        interestingMoves[move.piece().color()].add(move);
    }

    public void addInterestingEnabledOrPreparedMove(VBoard vBoard, Move move, boolean interesting) {
        if (interesting || isInteresting(vBoard, move)) {
            // interesting is interesting, no matter if already legal or needs preparation
            addInterestingMove(move);
        }
        else if (vBoard.getPiecePos(move.piece()) == move.from()) {
            // a direct (not later) pawn move (*)=Todo: based on VBoard, but will need to consider conditions
            addEnabledMove(move);
        }
        else
            addPreparedMoves(move);
    }

    public void addInterestingOrPreparedMove(VBoard vBoard, Move move) {
        if (isInteresting(vBoard, move)) {
            // interesting is interesting, no matter if already legal or needs preparation
            addInterestingMove(move);
        }
        else
            addPreparedMoves(move);
    }

    public void addInterestingOrEnabledMove(VBoard vBoard, Move move) {
        if (isInteresting(vBoard, move)) {
            // interesting is interesting, no matter if already legal or needs preparation
            addInterestingMove(move);
        }
        else
            addEnabledMove(move);
    }

    public void addChangedEvalMove(Move chgMove) {
        changedEvalMoves[chgMove.piece().color()].add(chgMove);
    }

    public void addPreparedMoves(Move preparedMove) {
        preparedMoves[preparedMove.piece().color()].add(preparedMove);
    }

    public void addDelayedMove(Move deleyedMove) {
        delayedMoves[deleyedMove.piece().color()].add(deleyedMove);
    }


    static boolean isInteresting(VBoard vBoard, Move move) {
        return !vBoard.isSquareEmpty(move.to())        // todo!: extract and let it consider preconditions (*)
                || (isPawn(move.piece().pieceType()) && promotionDistanceForColor(move.to(), move.piece().color()) < 2);
    }

    @Override
    public String toString() {
        return "MoveConsequences{ white: " +
                "interestingMoves=" + interestingMoves[CIWHITE] +
                ", enabledMoves=" + enabledMoves[CIWHITE] +
                ", preparedMoves=" + preparedMoves[CIWHITE] +
                ", changedEvalMoves=" + changedEvalMoves[CIWHITE] +
                ", blockedMoves=" + blockedMoves[CIWHITE] +
                ", delayedMoves=" + delayedMoves[CIWHITE] +
                "; black: " +
                "interestingMoves=" + interestingMoves[CIBLACK] +
                ", enabledMoves=" + enabledMoves[CIBLACK] +
                ", preparedMoves=" + preparedMoves[CIBLACK] +
                ", changedEvalMoves=" + changedEvalMoves[CIBLACK] +
                ", blockedMoves=" + blockedMoves[CIBLACK] +
                ", delayedMoves=" + delayedMoves[CIBLACK] +
                '}';
    }
}
