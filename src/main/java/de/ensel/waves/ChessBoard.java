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
 *     This file is based on class ChessBoard of TideEval v0.48.
 */

package de.ensel.waves;

import java.util.*;
import java.util.stream.Stream;

import static de.ensel.chessbasics.ChessBasics.*;
import static de.ensel.waves.Move.addMoveToSortedListOfCol;
import static de.ensel.waves.SimpleMove.getMoves;
import static de.ensel.waves.VBoardInterface.GameState.*;
import static java.lang.Math.*;
import static java.text.MessageFormat.format;

public class ChessBoard extends VBoard {
    public static final ResourceBundle chessBoardRes = ResourceBundle.getBundle("de.ensel.chessboardres");

    private ChessEngineParams engParams = new ChessEngineParams();

    /**
     * configure here which debug messages should be printed
     * (not final, to be able to change it during debugging)
     */
    public static boolean DEBUGMSG_DISTANCES = false;
    public static boolean DEBUGMSG_CLASH_CALCULATION = false;
    public static boolean DEBUGMSG_MOVEEVAL = false;   // <-- best for checking why moves are evaluated the way they are
    public static boolean DEBUGMSG_MOVESELECTION = false || DEBUGMSG_MOVEEVAL;

    public static boolean SHOW_REASONS = true;  // DEBUGMSG_MOVESELECTION;

    public static int DEBUGFOCUS_SQ = coordinateString2Pos("e1");   // changeable globally, just for debug output and breakpoints+watches
    public static int DEBUGFOCUS_VP = 0;   // changeable globally, just for debug output and breakpoints+watches
    private final ChessBoard board = this;       // only exists to make naming in debug evaluations easier (unified across all classes)

    private long boardHash;
    private List<List<Long>> boardHashHistory;  // 2 for the colors - then ArrayList<>(50) for 50 Hash values:

    private int whiteKingPos;
    private int blackKingPos;

    private int currentDistanceCalcLimit;

    public static int MAX_INTERESTING_NROF_HOPS = 3;
    // one move with a check, making a 2nd move with the tempo and thus cover another place - so dist of 3 is doable in one move so to speak

    private int[] nrOfLegalMoves = new int[2];
    protected Move bestMove;

    private boolean gameOver;

    private int repetitions;

    private Square[] boardSquares;
    private String fenPosAndMoves;

    private static int engineP1 = 0;  // engine option - used at varying places for optimization purposes.

    /**
     * keep all Pieces on Board
     */
    private ChessPiece[] piecesOnBoard;
    private int nextFreePceID;
    public static final int NO_PIECE_ID = -1;  //todo: why not using EMPTY from ChessBasics piece types?

    int countOfWhitePieces;  // todo: make array with colorindex
    private int countOfBlackPieces;
    private int[] countBishops = new int[2];  // count bishops for colorIndex
    private int[] countKnights = new int[2];  // count knights for colorIndex
    private int[][] countPawnsInFile = new int[2][NR_FILES];  // count pawns in particular file for colorIndex

    private int turn;  // colorIndex of who's turn it is now
    private int fullMoves;
    private int countBoringMoves;

    private StringBuffer boardName;



    /**
     * Constructor
     * for a fresh ChessBoard in Starting-Position
     */
//    public ChessBoard() {
//        initChessBoard(new StringBuffer(chessBoardRes.getString("chessboard.initialName")), FENPOS_STARTPOS);
//    }

    public ChessBoard(String fenBoard) {
        initChessBoard(new StringBuffer(chessBoardRes.getString("chessboard.initialName")), fenBoard);
    }

    public ChessBoard(String boardName, String fenBoard) {
        initChessBoard(new StringBuffer(boardName), fenBoard);
    }

    public ChessBoard(String boardName, String fenBoard, ChessEngineParams engParams) {
        this.engParams = engParams;
        initChessBoard(new StringBuffer(boardName), fenBoard);
    }

    public static int engineP1() {
        return engineP1;
    }

    public static void setEngineP1(int i) {
        engineP1 = i;
    }

    private void initChessBoard(StringBuffer boardName, String fenBoard) {
        this.boardName = boardName;
        setCurrentDistanceCalcLimit(0);
        updateBoardFromFEN(fenBoard);
        calcBestMove();
    }


    private void emptyBoard() {
        piecesOnBoard = new ChessPiece[MAX_PIECES];
        countOfWhitePieces = 0;
        countOfBlackPieces = 0;
        for (int ci=0; ci<=1; ci++) {
            countBishops[ci] = 0;
            countKnights[ci] = 0;
            for (int f=0; f<NR_FILES; f++)
                countPawnsInFile[ci][f] = 0;
        }
        nextFreePceID = 0;
        boardSquares = new Square[NR_SQUARES];
        for (int p = 0; p < NR_SQUARES; p++) {
            boardSquares[p] = new Square(this, p);
        }
    }

    private void setDefaultBoardState() {
        countBoringMoves = 0;
        kingsideCastlingAllowed[CIWHITE] = false;  /// s.o.
        queensideCastlingAllowed[CIWHITE] = false;
        kingsideCastlingAllowed[CIBLACK] = false;
        queensideCastlingAllowed[CIBLACK] = false;
        enPassantFile = -1;    // -1 = not possible,   0 to 7 = possible to beat pawn of opponent on col A-H
        turn = CIWHITE;
        fullMoves = 0;
        whiteKingPos = NOWHERE;
        blackKingPos = NOWHERE;
        resetHashHistory();
    }

    public ChessPiece getPiece(int pceID) {
        assert (pceID < nextFreePceID);
        return piecesOnBoard[pceID];
    }

    /**
     * give ChessPiece at pos
     *
     * @param pos board position
     * @return returns ChessPiece or null if quare is empty
     */
    @Override
    public ChessPiece getPieceAt(int pos) {
        int pceID = getPieceIdAt(pos);
        if (pceID == NO_PIECE_ID)
            return null;
        return piecesOnBoard[pceID];
    }

    @Override
    public int getPiecePos(ChessPiece pce) {
        return pce.pos();
        // note: looks a bit strange, but is due to the fact that the newer VBoardInterface is not Id-Based,
        // but the older CHessBoard is.
    }

    @Override
    public int depth() {
        return 0;
    }

    int getPieceIdAt(int pos) {
        return boardSquares[pos].myPieceID();
    }

//    @Override
//    public boolean isCaptured(ChessPiece pce) {
//        return pce.pos() == NOWHERE;
//    }

    @Override
    public boolean hasPieceOfColorAt(int color, int pos) {
        if (boardSquares[pos].isEmpty() || getPieceAt(pos) == null)   // Todo-Option:  use assert(getPiecePos!=null)
            return false;
        return (getPieceAt(pos).color() == color);
    }

    public int distanceToKing(int pos, int kingColor) {
        if (isWhite(kingColor))
            return distanceBetween(pos, whiteKingPos);
        else
            return distanceBetween(pos, blackKingPos);
    }

    @Override
    public boolean isCheck(int color) {
        return nrOfChecks(color) > 0;
    }

    @Override
    public int getNrOfRepetitions() { return repetitions; }


    public int nrOfChecks(int color) {
        int kingPos = getKingPos(color);
        if (kingPos < 0)
            return 0;  // king does not exist... should not happen, but is part of some test-positions
        Square kingSquare = getSquare(kingPos);
        return kingSquare.countDirectAttacksWithout2ndRowWithColor(opponentColor(color));
    }

    /////
    ///// the Chess Game as such
    /////

    private void checkAndEvaluateGameOver() {    // called to check+evaluate if there are no more moves left or 50-rules-move is violated
        if (countOfWhitePieces <= 0 || countOfBlackPieces <= 0) {
            gameOver = true;
        } else if (isCheck(getTurnCol()) && nrOfLegalMoves(getTurnCol()) == 0) {
            gameOver = true;
        } else
            gameOver = false;
    }

    private static final String[] evalLabels = {
            "game state",
            "piece values",
            "pceVals + best move[0]",
    };

    protected static final int EVAL_INSIGHT_LEVELS = evalLabels.length;


    static String getEvaluationLevelLabel(int level) {
        return evalLabels[level];
    }

    // [EVAL_INSIGHT_LEVELS];

    /**
     * calculates board evaluation according to several "insight levels"
     *
     * @param levelOfInsight: 1 - sum of plain standard figure values,
     *                        2 - take piece position into account
     *                        3 - add best move evaluation
     *                        -1 - take best algorithm currently implemented
     * @return board evaluation in centipawns (+ for white, - for an advantage of black)
     */
    public int boardEvaluation(int levelOfInsight) {
        if (levelOfInsight >= EVAL_INSIGHT_LEVELS || levelOfInsight < 0)
            levelOfInsight = EVAL_INSIGHT_LEVELS - 1;
        int[] eval = new int[EVAL_INSIGHT_LEVELS];
        // first check if its over...
        checkAndEvaluateGameOver();
        if (isGameOver()) {                         // gameOver
            if (isCheck(CIWHITE))
                eval[0] = WHITE_IS_CHECKMATE;
            else if (isCheck(CIBLACK))
                eval[0] = BLACK_IS_CHECKMATE;
            eval[0] = 0;
        } else if (isWhite(getTurnCol()))       // game is running
            eval[0] = +1;
        else
            eval[0] = -1;
        if (levelOfInsight == 0)
            return eval[0];
        // even for gameOver we try to calculate the other evaluations "as if"
        int l = 0;
        eval[++l] = evaluateAllPiecesBasicValueSum(); /*1*/
        if (levelOfInsight == l)
            return eval[1] + eval[l];
        eval[++l] = evaluateAllPiecesValueSum(); /*2*/
        if (levelOfInsight == l)
            return eval[1] + eval[l];
        eval[++l] = getBestMove() != null ? (getBestMove()).getSingleValueEval()/10 : 0;  /*3*/
        if (levelOfInsight == l)
            return eval[1] + eval[l];

        assert (false);
        return 0;
    }

    public int boardEvaluation() {
        // for a game that has ended, the official evaluation is in level 0 (so that the others remain available "as if")
        if (isGameOver())
            return boardEvaluation(0);
        return boardEvaluation(EVAL_INSIGHT_LEVELS - 1);
    }

    private int evaluateAllPiecesBasicValueSum() {
        int pceValSum = 0;
        for (ChessPiece pce : piecesOnBoard)
            if (pce != null)
                pceValSum += pce.baseValue();
        return pceValSum;
    }

    private int evaluateAllPiecesValueSum() {
        int pceValSum = 0;
        for (ChessPiece pce : piecesOnBoard)
            if (pce != null)
                pceValSum += pce.getValue();
        return pceValSum;
    }

    /**
     * triggers distance calculation for all pieces, stepwise up to toLimit
     * this eventually does the breadth distance propagation
     *
     * @param toLimit final value of currentDistanceCalcLimit.
     */
    private void continueDistanceCalcUpTo(int toLimit) {
        for (int currentLimit = 1; currentLimit <= toLimit; currentLimit++) {
            setCurrentDistanceCalcLimit(currentLimit);
            nextUpdateClockTick();
            /*int processed;
            int emergencyBreak = 0;
            do {
                processed = 0;
             */
                for (ChessPiece pce : piecesOnBoard)
                    if (pce != null)
                        ; // TODOpce.continueDistanceCalc();
                nextUpdateClockTick();

            // update calc, of who can go where safely
            for (Square sq : boardSquares)
                ; // TODO sq.updateClashResultAndRelEvals();
        }

    }

    private void markCheckBlockingSquares() {
        for (Square sq : getBoardSquares())
            sq.resetBlocksChecks();
        for (int color = 0; color <= 1; color++) { // for both colors
            int kingpos = getKingPos(color);
            if (kingpos < 0)
                continue;          // in some test-cases boards without kings are used, so skip this (instead of error/abort)
            List<ChessPiece> attackers = getSquare(kingpos).directAttackersWithout2ndRowWithColor(opponentColor(color));
            for (ChessPiece a : attackers) {
                for (int pos : a.allPosOnWayTo(kingpos))
                    boardSquares[pos].setBlocksCheckFor(color);
            }
        }
    }

    /**
     * triggers all open calculation for all pieces
     */
    void completeCalc() {
        resetBestMove();

        continueDistanceCalcUpTo(MAX_INTERESTING_NROF_HOPS);

    }

    private void resetBestMove() {
        bestMove = null;
    }


    /**
     * Get (simple) fen string from the current board
     *
     * @return String in FEN notation representing the current board and game status
     */
    String getBoardFEN() {
        StringBuilder fenString = new StringBuilder();
        for (int rank = 0; rank < NR_RANKS; rank++) {
            if (rank > 0)
                fenString.append("/");
            int spaceCounter = 0;
            for (int file = 0; file < NR_FILES; file++) {
                int pceType = getPieceTypeAt(rank * 8 + file);
                if (pceType == EMPTY) {
                    spaceCounter++;
                } else {
                    if (spaceCounter > 0) {
                        fenString.append(spaceCounter);
                        spaceCounter = 0;
                    }
                    fenString.append(fenCharFromPceType(pceType));
                }
            }
            if (spaceCounter > 0) {
                fenString.append(spaceCounter);
            }
        }
        return fenString + "" + getFENBoardPostfix();
    }
    //StringBuffer[] getBoard8StringsFromPieces();


    String getFENBoardPostfix() {
        return (isWhite(turn) ? " w " : " b ")
                + (isKingsideCastleAllowed(CIWHITE) ? "K" : "") + (isQueensideCastleAllowed(CIWHITE) ? "Q" : "")
                + (isKingsideCastleAllowed(CIBLACK) ? "k" : "") + (isQueensideCastleAllowed(CIBLACK) ? "q" : "")
                + ((!isKingsideCastleAllowed(CIWHITE) && !isQueensideCastleAllowed(CIWHITE)
                && !isKingsideCastleAllowed(CIBLACK) && !isQueensideCastleAllowed(CIBLACK)) ? "- " : " ")
                + (getEnPassantFile() == -1 ? "- " : (Character.toString(getEnPassantFile() + 'a') + (isWhite(turn) ? "6" : "3")) + " ")
                + countBoringMoves
                + " " + fullMoves;
    }


    /**
     * create a new Piece on the board
     *
     * @param pceType type of white or black piece - according to type constants in ChessBasics
     * @param pos     square position on board, where to spawn that piece
     * @return returns pieceID of the new Piece
     */
    int spawnPieceAt(final int pceType, final int pos) {
        final int newPceID = nextFreePceID++;
        assert (nextFreePceID <= MAX_PIECES);
        assert (pos >= 0 && pos < NR_SQUARES);
        if (isPieceTypeWhite(pceType)) {
            countOfWhitePieces++;
            if (pceType == KING)
                whiteKingPos = pos;
        } else {
            countOfBlackPieces++;
            if (pceType == KING_BLACK)
                blackKingPos = pos;
        }

        switch (colorlessPieceType(pceType)) {
            case BISHOP -> countBishops[colorIndexOfPieceType(pceType)]++;
            case KNIGHT -> countKnights[colorIndexOfPieceType(pceType)]++;
            case PAWN   -> countPawnsInFile[colorIndexOfPieceType(pceType)][fileOf(pos)]++;
        }

        piecesOnBoard[newPceID] = ChessPiece.newPiece(this, pceType, newPceID, pos);

        // tell all squares about this new piece
        for (Square sq : boardSquares)
            sq.prepareNewPiece(newPceID);

        // finally, add the new piece at its place
        boardSquares[pos].spawnPiece(newPceID);
        //updateHash
        return newPceID;
    }

    public void removePiece(int pceID) {
        piecesOnBoard[pceID] = null;
        for (Square sq : boardSquares)
            sq.removePiece(pceID);
    }

    protected boolean updateBoardFromFEN(String fenString) {
        if (fenString == null || fenString.length() == 0)
            fenString = FENPOS_STARTPOS;
        SimpleMove[] movesToDo = null;
        boolean changed = true;
        if (fenPosAndMoves != null
                && fenString.startsWith(fenPosAndMoves)) {
            if (fenString.equals(fenPosAndMoves))
                changed = false; // it seems we are called with the same fenString again!
            movesToDo = getMoves(fenString.substring(fenPosAndMoves.length()));
        } else {
            // it seems the fenString is a later position of my current position or a totally different one
            movesToDo = initBoardFromFEN(fenString);
        }
        if (movesToDo != null) {
            for (int i = 0; i < movesToDo.length; i++) {
                completeCalc();
                if (!doMove(movesToDo[i])) {
                    System.err.println("Error in fenstring moves: invalid move " + movesToDo[i] + " on " + this.getBoardFEN() + "");
                    // try manually
                    basicMoveFromTo(movesToDo[i].from(), movesToDo[i].to());
                }
            }
        }
        if (!fenString.equalsIgnoreCase(fenPosAndMoves)) {
            //System.err.println("Inconsistency in fen string: " + fenPosAndMoves
            //        + " instead of " + fenString);
            // still we continue...
        }
        fenPosAndMoves = fenString;
        completeCalc();
        return changed;
    }

    /**
     * inits empty chessboard with pieces and parameters from a FEN string
     *
     * @param fenString FEN String according to Standard with board and game attributes
     * @return returns list of (still open) moves that were appended to the fen string
     */
    private SimpleMove[] initBoardFromFEN(String fenString) {
        //fenPosAndMoves = fenString;
        setDefaultBoardState();
        emptyBoard();
        int pceId;
        int i = 0;
        int rank = 0;
        int file = 0;
        int pos = 0;
        while (i < fenString.length() && rank < 8) {
            int emptyfields = 0;
            switch (fenString.charAt(i)) {
                case '*', 'p', '♟' -> pceId = PAWN_BLACK;
                case 'o', 'P', '♙' -> pceId = PAWN;
                case 'L', 'B', '♗' -> pceId = BISHOP;
                case 'l', 'b', '♝' -> pceId = BISHOP_BLACK;
                case 'T', 'R', '♖' -> pceId = ROOK;
                case 't', 'r', '♜' -> pceId = ROOK_BLACK;
                case 'S', 'N', '♘' -> pceId = KNIGHT;
                case 's', 'n', '♞' -> pceId = KNIGHT_BLACK;
                case 'K', '♔' -> pceId = KING;
                case 'k', '♚' -> pceId = KING_BLACK;
                case 'D', 'Q', '♕' -> pceId = QUEEN;
                case 'd', 'q', '♛' -> pceId = QUEEN_BLACK;
                case '/' -> {
                    if (file != 8)
                        internalErrorPrintln("**** Inkorrekte Felder pro Zeile gefunden beim Parsen an Position " + i + " des FEN-Strings " + fenString);
                    pos += 8 - file; // statt pos++, um ggf. den Input-Fehler zu korrigieren
                    file = 0;
                    i++;
                    rank++;
                    continue;
                }
                case ' ', '_' -> {  // signals end of board in fen notation, but we also want to accept it as empty field
                    if (fenString.charAt(i) == ' ' && file == 8 && rank == 7) {
                        i++;
                        rank++;
                        pos++;
                        continue;
                    }
                    pceId = EMPTY;
                    emptyfields = 1;
                }
                default -> {
                    pceId = EMPTY;
                    if (fenString.charAt(i) >= '1' && fenString.charAt(i) <= '8')
                        emptyfields = fenString.charAt(i) - '1' + 1;
                    else {
                        internalErrorPrintln("**** Fehler beim Parsen an Position " + i + " des FEN-Strings " + fenString);
                    }
                }
            }
            if (pceId != EMPTY) {
                spawnPieceAt(pceId, pos);
                file++;
                pos++;
            } else {
                //spawn nothing
                pos += emptyfields;
                file += emptyfields;
                emptyfields = 0;
            }
            if (file > 8)
                System.err.println("**** Überlange Zeile gefunden beim Parsen an Position " + i + " des FEN-Strings " + fenString);
            // kann nicht vorkommen if (rank>8)
            //    System.err.println("**** Zu viele Zeilen gefunden beim Parsen an Position "+i+" des FEN-Strings "+fenString);
            i++;
        }
        // set board params from fen appendix
        // TODO: implementation is quite old, should use split etc...
        while (i < fenString.length() && fenString.charAt(i) == ' ')
            i++;
        SimpleMove[] postMoves = null;
        if (i < fenString.length()) {
            if (fenString.charAt(i) == 'w' || fenString.charAt(i) == 'W')
                turn = CIWHITE;
            else if (fenString.charAt(i) == 'b' || fenString.charAt(i) == 'B')
                turn = CIBLACK;
            else
                System.err.println("**** Fehler beim Parsen der Spieler-Angabe an Position " + i + " des FEN-Strings " + fenString);
            i++;
            while (i < fenString.length() && fenString.charAt(i) == ' ')
                i++;
            // castle indicators
            int nextSeperator = i;
            while (nextSeperator < fenString.length() && fenString.charAt(nextSeperator) != ' ')
                nextSeperator++;
            String whiteKcastleSymbols = ".*[" + ( (char)((int) 'A' + fileOf(whiteKingPos)) ) + "-H].*";
            String whiteQcastleSymbols = ".*[A-" + ( (char)((int) 'A' + fileOf(whiteKingPos)) ) + "].*";
            String blackKcastleSymbols = ".*[" + ( (char)((int) 'a' + fileOf(blackKingPos)) ) + "-h].*";
            String blackQcastleSymbols = ".*[a-" + ( (char)((int) 'a' + fileOf(blackKingPos)) ) + "].*";
            String castleIndicators = fenString.substring(i, nextSeperator);
            queensideCastlingAllowed[CIBLACK] = castleIndicators.contains("q") || ( castleIndicators.matches(blackQcastleSymbols));
            kingsideCastlingAllowed[CIBLACK] = castleIndicators.contains("k") || ( castleIndicators.matches(blackKcastleSymbols));
            queensideCastlingAllowed[CIWHITE] = castleIndicators.contains("Q") || ( castleIndicators.matches(whiteQcastleSymbols));
            kingsideCastlingAllowed[CIWHITE] = castleIndicators.contains("K") || ( castleIndicators.matches(whiteKcastleSymbols));
            // enPassant
            i = nextSeperator;
            while (i < fenString.length() && fenString.charAt(i) == ' ')
                i++;
            nextSeperator = i;
            while (nextSeperator < fenString.length() && fenString.charAt(nextSeperator) != ' ')
                nextSeperator++;
            if (fenString.substring(i, nextSeperator).matches("[a-h]([1-8]?)"))
                enPassantFile = fenString.charAt(i) - 'a';
            else {
                enPassantFile = -1;
                if (fenString.charAt(i) != '-')
                    System.err.println("**** Fehler beim Parsen der enPassant-Spalte an Position " + i + " des FEN-Strings " + fenString);
            }
            // halfMoveClock
            i = nextSeperator;
            while (i < fenString.length() && fenString.charAt(i) == ' ')
                i++;
            nextSeperator = i;
            while (nextSeperator < fenString.length() && fenString.charAt(nextSeperator) != ' ')
                nextSeperator++;
            if (fenString.substring(i, nextSeperator).matches("[0-9]+"))
                countBoringMoves = Integer.parseInt(fenString.substring(i, nextSeperator));
            else {
                countBoringMoves = 0;
                System.err.println("**** Fehler beim Parsen der halfMoveClock an Position " + i + " des FEN-Strings " + fenString);
            }
            // nr of full moves
            i = nextSeperator;
            while (i < fenString.length() && fenString.charAt(i) == ' ')
                i++;
            nextSeperator = i;
            while (nextSeperator < fenString.length() && fenString.charAt(nextSeperator) != ' ')
                nextSeperator++;
            if (fenString.substring(i, nextSeperator).matches("[0-9]+"))
                fullMoves = Integer.parseInt(fenString.substring(i, nextSeperator));
            else {
                fullMoves = 1;
            }

            // parse+collect appending move strings
            i = nextSeperator;
            fenPosAndMoves = fenString.substring(0, i);
            while (i < fenString.length() && fenString.charAt(i) == ' ')
                i++;
            postMoves = getMoves(fenString.substring(i));
        }
        initHash();
        // else no further board parameters available, stay with defaults
        return postMoves;
    }

    ///// Hash
    //private long boardFigureHash;
    //long getBoardHash();
    //long getBoardAfterMoveHash(int frompos, int topos);


    ///// MOVES

    protected boolean[] kingsideCastlingAllowed = new boolean[2];
    protected boolean[] queensideCastlingAllowed = new boolean[2];
    protected int enPassantFile;   // -1 = not possible,   0 to 7 = possible to beat pawn of opponent on col A-H

    public boolean isKingsideCastleAllowed(int color) {
        return kingsideCastlingAllowed[color];
    }

    public boolean isQueensideCastleAllowed(int color) {
        return queensideCastlingAllowed[color];
    }

    public int getEnPassantFile() {
        return enPassantFile;
    }

    public int getEnPassantPosForTurnColor(int color) {
        if (enPassantFile == -1)
            return -1;
        return fileRank2Pos(enPassantFile, isWhite(color) ? 5 : 2);
    }

    public int getCountBoringMoves() {
        return countBoringMoves;
    }

    public int getFullMoves() {
        return fullMoves;
    }

    @Override
    public int captureEvalSoFar() {
        return 0;
    }

    /**
     * does calcBestMove when necessary (incl. checkAndEvaluateGameOver())
     * @return a string describing a hopefully good move (format "a1b2")
     */
    public String getMove() {
        Move m = getBestMove();
        if (m == null || !m.isMove())
            return "-";
        else
            return m.toString();
    }

    /**
     * does calcBestMove() when necessary (incl. checkAndEvaluateGameOver())
     * @return a hopefully good Move
     */
    public Move getBestMove() {
        if (bestMove==null)
            calcBestMove();
        return bestMove;
    }


    /**
     * the actual calculation... includes checkAndEvaluateGameOver()
     */
    private void calcBestMove() {
        final int lowest = (isWhite(getTurnCol()) ? WHITE_IS_CHECKMATE : BLACK_IS_CHECKMATE);
        int[] bestEvalSoFar = new int[MAX_INTERESTING_NROF_HOPS + 1];
        int[] bestOpponentEval = new int[MAX_INTERESTING_NROF_HOPS + 1];
        Arrays.fill(bestEvalSoFar, lowest);
        Arrays.fill(bestOpponentEval, -lowest);
        Arrays.fill(nrOfLegalMoves, 0);

        //System.err.println("Getting best move for: " + board.toString());
        // Compare all moves returned by all my pieces and find the best.
        //Stream<Move> bestOpponentMoves = getBestMovesForColAfter( opponentColor(getTurnCol()), NOCHANGE );
        countCalculatedBoards = 0;
        //setEngParams(new ChessEngineParams(engineP1));
        Stream<Move> bestMoves    = getBestMovesForColAfter( getTurnCol(), engParams, this, Integer.MIN_VALUE, Integer.MAX_VALUE);
        bestMove = bestMoves.findFirst().orElse(null);
        //System.err.println("  --> " + bestMove );
        if (true || DEBUGMSG_MOVESELECTION) {
            debugPrintln(DEBUGMSG_MOVESELECTION, "=> My best move (after looking at "
                    + countCalculatedBoards + " positions and " + VBoard.usageCounter + " moves"
                    + "): " + bestMove +".");
            //debugPrintln(DEBUGMSG_MOVESELECTION, "(opponents best moves: " + bestOpponentMoves.findFirst().orElse(null) + ").");
        }
        checkAndEvaluateGameOver();
    }

    public int countCalculatedBoards;
    Stream<Move> getBestMovesForColAfter(final int color, ChessEngineParams engParams, VBoard upToNowBoard, int alpha, int beta) {
        final int maxBestMoves = engParams.searchMaxNrOfBestMovesPerPly();  // only the top moves are sorted
        List<Move> bestMoveCandidates = new ArrayList<>(maxBestMoves+(maxBestMoves>>1));
        List<Move> bestMoves = new ArrayList<>(maxBestMoves);
        List<Move> restMoves = new ArrayList<>();
        //nrOfLegalMoves[color] = 0;
        boolean[] alphabetabreak = new boolean[]{false};  // array to be accessible from inside lambda :-/
        int[] alphaS = new int[]{alpha};
        int[] betaS = new int[]{beta};
        boolean doABcheckInPresearch = upToNowBoard.futureLevel() >= engParams.searchMaxDepth()-1;
        final int[] countOppMoves = {0};
        upToNowBoard.getPieces()
            .filter(p -> p.color() == color)
            .forEach( p -> {
                p.legalMovesAfter(upToNowBoard).forEach( move -> {
                    if (!alphabetabreak[0]) { // like a for-break
                        Move evaluatedMove = p.getEvaluatedMoveToAfter(move, upToNowBoard);
                        evaluatedMove.addEval(upToNowBoard.captureEvalSoFar(),0);
                        addMoveToSortedListOfCol(evaluatedMove, bestMoveCandidates, color, maxBestMoves + (maxBestMoves >> 1), restMoves);
                        countCalculatedBoards++;
                        countOppMoves[0]++;
                        // alpha-beta-break-check (code duplication from below,... sorry :^)
                        if (doABcheckInPresearch) {
                            int bestEval0 = bestMoveCandidates.get(0).getEval().getEvalAt(0);
                            if (isWhite(move.piece().color())) {
                                alphaS[0] = max(alphaS[0] , bestEval0);
                                if (bestEval0 >= betaS[0] )
                                    alphabetabreak[0] = true;
                            }
                            else {
                                betaS[0]  = min(betaS[0] , bestEval0);
                                if (bestEval0 <= alphaS[0] )
                                    alphabetabreak[0] = true;
                            }
                        }
                    }
                    else
                        countOppMoves[0]++;
                });
            });

        if (countOppMoves[0] == 0) {
            // game over
            return Stream.empty();
        }
        if (doABcheckInPresearch) {
            // end of recursion, we treat the results of the pre-evaluation as final result.
            return Stream.concat( bestMoveCandidates.stream(), restMoves.stream());
        }
        // reevaluate moves by move simulation
        for (Move move : bestMoveCandidates) {
            if (!alphabetabreak[0]) {
                if ( abs(move.getEval().getEvalAt(0)) >= 3  // capturing moves
                        || upToNowBoard.futureLevel() == 0            // all my first moves
                        || (upToNowBoard.futureLevel() <= 4           // or early moves have a good followup
                            && abs(move.getEval().getEvalAt(2)) >= 6)
                        || (upToNowBoard.futureLevel() <= 1          // deep dive also for more opponent moves?
                            && abs(move.getEval().getEvalAt(1)) >= 6)
                        || (upToNowBoard.futureLevel() <= 3          // deep dive also for more opponent moves?
                            && abs(move.getEval().getEvalAt(1)) >= 9)
                ) {
                    VBoard nextBoard = VBoard.createNext(upToNowBoard, move);
                    Move bestOppMove = getBestMovesForColAfter(opponentColor(color), engParams, nextBoard, alpha, beta)
                            .findFirst().orElse(null);
                    if (bestOppMove != null) {
                        move.addEval(bestOppMove.getEval());
                        move.addEval(upToNowBoard.captureEvalSoFar(),0);
                        move.getEval().setReason( upToNowBoard + " " + move.toString()
                                + move.getEval() + " "
                                + move.getEval().getReason() + " <-- "
                                + "!{" + bestOppMove.getEval().getReason()+"} ");
                        if (DEBUGMSG_MOVESELECTION && upToNowBoard.futureLevel() == 0)
                            debugPrintln(DEBUGMSG_MOVESELECTION, "Reevaluated " /*+ move + " to " + move.getEval()
                                    + " reason: " */ + move.getEval().getReason());
                    }
                    else if (!nextBoard.hasLegalMoves(opponentColor(move.piece().color()))) {  // be sure it was not null due to not wanting to calc deeper any more
                        if (nextBoard.gameState() == DRAW ) {
                            //TODO: use -piece-value-sum or other board evaluation here, so we do not like draws with more pieces on the board
                            Evaluation drawEval = new Evaluation(0, 0);
                            move.addEval(drawEval);
                            move.getEval().setReason(upToNowBoard + " !draw!("+ drawEval + ") ");
                        }
                        else {
                            move.addEval(new Evaluation(checkmateEvalIn(nextBoard.getTurnCol(), upToNowBoard.futureLevel()), 0));
                            move.getEval().setReason(upToNowBoard + " !checkmate!");
                        }
                        if (DEBUGMSG_MOVESELECTION && upToNowBoard.futureLevel() == 0)
                            debugPrintln(DEBUGMSG_MOVESELECTION, "Reevaluated " /*+ move + " to " + move.getEval()
                                    + " reason: " */ + move.getEval().getReason());
                    }
                }
            }
            addMoveToSortedListOfCol(move, bestMoves, color, maxBestMoves, restMoves);
            // alpha-beta-break-check
            int bestEval0 = bestMoves.get(0).getEval().getEvalAt(0);
            if (isWhite(move.piece().color())) {
                alpha = max(alpha, bestEval0);
                if (bestEval0 >= beta)
                    alphabetabreak[0] = true;
            }
            else {
                beta = min(beta, bestEval0);
                if (bestEval0 <= alpha)
                    alphabetabreak[0] = true;
            }
        }
        return Stream.concat( bestMoves.stream(), restMoves.stream());
    }

    public static int checkmateEvalIn(int color, int nrOfPlys) {
        return isWhite(color) ? WHITE_IS_CHECKMATE + CHECK_IN_N_DELTA*nrOfPlys
                : BLACK_IS_CHECKMATE - CHECK_IN_N_DELTA*nrOfPlys;
    }

    boolean doMove (SimpleMove m) {
        return doMove(m.from(), m.to(), m.promotesTo());
    }

    boolean doMove ( int frompos, int topos, int promoteToPceType){
        // sanity/range checks for move
        if (frompos < 0 || topos < 0
                || frompos >= NR_SQUARES || topos >= NR_SQUARES) { // || figuresOnBoard[frompos].getColor()!=turn  ) {
            internalErrorPrintln(String.format("Fehlerhafter Zug: %s%s ist außerhalb des Boards %s.\n", squareName(frompos), squareName(topos), getBoardFEN()));
            return false;
        }
        final int pceID = getPieceIdAt(frompos);
        final int pceType = getPieceTypeAt(frompos);
        if (pceID == NO_PIECE_ID) { // || figuresOnBoard[frompos].getColor()!=turn  ) {
            internalErrorPrintln(String.format("Fehlerhafter Zug: auf %s steht keine Figur auf Board %s.\n", squareName(frompos), getBoardFEN()));
            return false;
        }
        if (!boardSquares[topos].canMoveHere(pceID)
                && colorlessPieceType(pceType) != KING) { // || figuresOnBoard[frompos].getColor()!=turn  ) {
            // TODO: check king for allowed moves... excluded here, because castling is not obeyed in distance calculation, yet.
            internalErrorPrintln(String.format("Fehlerhafter Zug: %s -> %s nicht möglich auf Board %s.\n", squareName(frompos), squareName(topos), getBoardFEN()));
            //TODO: this allows illegal moves, but for now overcomes the bug to not allow en passant beating by the opponent...
            if (!(colorlessPieceType(pceType) == PAWN && fileOf(topos) == enPassantFile))
                return false;
        }
        final int toposPceID = getPieceIdAt(topos);
        final int toposType = getPieceTypeAt(topos);

        // take piece, but be careful, if the target is my own rook, it could be castling!
        boolean isBeatingSameColor = toposPceID != NO_PIECE_ID
                                     && colorOfPieceType(pceType) == colorOfPieceType(toposType);
        if (toposPceID != NO_PIECE_ID && !isBeatingSameColor ) {
            // if it is a rook, remove castling rights
            if ( toposType == ROOK ) {
                if ( fileOf(topos) > fileOf(getKingPos(CIWHITE)))
                    kingsideCastlingAllowed[CIWHITE] = false;
                else if ( fileOf(topos) < fileOf(getKingPos(CIWHITE)))
                    queensideCastlingAllowed[CIWHITE] = false;
            }
            else if ( toposType == ROOK_BLACK ) {
                if ( fileOf(topos) > fileOf(getKingPos(CIBLACK)))
                    kingsideCastlingAllowed[CIBLACK] = false;
                else if ( fileOf(topos) < fileOf(getKingPos(CIBLACK)))
                    queensideCastlingAllowed[CIBLACK] = false;
            }
            takePieceAway(topos);

        /*old code to update pawn-eval-parameters
        if (takenFigNr==NR_PAWN && toRow==getWhitePawnRowAtCol(tocolor))
            refindWhitePawnRowAtColBelow(toCol,toRow+1);  // try to find other pawn in column where the pawn was beaten
        else if (takenFigNr==NR_PAWN_BLACK && toRow==getBlackPawnRowAtCol(tocolor))
            refindBlackPawnRowAtColBelow(toCol,toRow-1);*/
        }

        // en-passant
        // is possible to occur in two notations to left/right (then taken pawn has already been treated above) ...
        if (pceType == PAWN
                && rankOf(frompos) == 4 && rankOf(topos) == 4
                && fileOf(topos) == enPassantFile) {
            topos += UP;
            //setFurtherWhitePawnRowAtCol(toCol, toRow);     // if the moved pawn was the furthest, remember new position, otherwise the function will leave it
        } else if (pceType == PAWN_BLACK
                && rankOf(frompos) == 3 && rankOf(topos) == 3
                && fileOf(topos) == enPassantFile) {
            topos += DOWN;
            //setFurtherBlackPawnRowAtCol(toCol, toRow);     // if the moved pawn was the furthest, remember new position, otherwise the function will leave it
        }
        // ... or diagonally als normal
        else if (pceType == PAWN
                && rankOf(frompos) == 4 && rankOf(topos) == 5
                && fileOf(topos) == enPassantFile) {
            takePieceAway(topos + DOWN);
        /*setFurtherWhitePawnRowAtCol(toCol, toRow);     // if the moved pawn was the furthest, remember new position, otherwise the function will leave it
        if (toRow+1==getBlackPawnRowAtCol(tocolor))
            setFurtherBlackPawnRowAtCol(toCol, NO_BLACK_PAWN_IN_color);*/
        } else if (pceType == PAWN_BLACK
                && rankOf(frompos) == 3 && rankOf(topos) == 2
                && fileOf(topos) == enPassantFile) {
            takePieceAway(topos + UP);
        /*setFurtherBlackPawnRowAtCol(toCol, toRow);     // if the moved pawn was the furthest, remember new position, otherwise the function will leave it
        if (toRow-1==getWhitePawnRowAtCol(tocolor))
            setFurtherWhitePawnRowAtCol(toCol, NO_WHITE_PAWN_IN_color);*/
        }

        //boardMoves.append(" ").append(squareName(frompos)).append(squareName(topos));

        // check if this is a 2-square pawn move ->  then enPassent is possible for opponent at next move
        if (pceType == PAWN && rankOf(frompos) == 1 && rankOf(topos) == 3
                || pceType == PAWN_BLACK && rankOf(frompos) == 6 && rankOf(topos) == 4)
            enPassantFile = fileOf(topos);
        else
            enPassantFile = -1;

        // castling:
        // i) also move rook  ii) update castling rights
        // test for Chess960 manually e.g. after: nbrkbrqn/pppppppp/8/8/8/8/PPPPPPPP/NBRKBRQN w CFcf - 0 1
        boolean didCastle = false;
        //TODO: check if squares where king moves along are free from check
        if (pceType == KING_BLACK) {
            if ( frompos<topos && isLastRank(frompos) && isLastRank(topos)
                    && ( toposType == ROOK_BLACK || topos == frompos+2 )
                    && allSquaresEmptyFromTo(frompos,topos)
                    && isKingsideCastlingPossible(CIBLACK)
            ) {
                if ( toposPceID == NO_PIECE_ID && topos == frompos+2 )  // seems to be std-chess notation (king moves 2 aside)
                    topos = findRook(frompos+1, coordinateString2Pos("h8"));
                if ( CASTLING_KINGSIDE_ROOKTARGET[CIBLACK] ==blackKingPos ) { // we have problem here, as this can happen in Chess960, but would kick our own king piece of the board
                    takePieceAway(topos); // eliminate rook :*\
                    basicMoveFromTo(frompos, CASTLING_KINGSIDE_KINGTARGET[CIBLACK]);  // move king instead
                    frompos = NOWHERE;
                    topos = CASTLING_KINGSIDE_ROOKTARGET[CIBLACK];
                }
                else {
                    basicMoveFromTo(ROOK_BLACK, getSquare(topos).myPieceID(), topos, CASTLING_KINGSIDE_ROOKTARGET[CIBLACK]);
                    //target position is always the same, even for chess960 - touches rook first, but nobody knows ;-)
                    topos = CASTLING_KINGSIDE_KINGTARGET[CIBLACK];
                }
                didCastle = true;
            }
            else if ( queensideCastlingAllowed[CIBLACK] && frompos>topos && isLastRank(frompos) && isLastRank(topos)
                    && ( toposType == ROOK_BLACK || topos == frompos-2 )
                    && allSquaresEmptyFromTo(frompos,topos)
                    && ( isSquareEmpty(CASTLING_QUEENSIDE_KINGTARGET[CIBLACK])
                    || getSquare(CASTLING_QUEENSIDE_KINGTARGET[CIBLACK]).myPieceType()==KING_BLACK
                    || getSquare(CASTLING_QUEENSIDE_KINGTARGET[CIBLACK]).myPieceType()==ROOK_BLACK )
                    && ( isSquareEmpty(CASTLING_QUEENSIDE_ROOKTARGET[CIBLACK])
                    || getSquare(CASTLING_QUEENSIDE_ROOKTARGET[CIBLACK]).myPieceType()==KING_BLACK
                    || getSquare(CASTLING_QUEENSIDE_ROOKTARGET[CIBLACK]).myPieceType()==ROOK_BLACK )
            ) {
                if (toposPceID == NO_PIECE_ID && topos == frompos - 2)  // seems to be std-chess notation (king moves 2 aside)
                    topos = findRook(coordinateString2Pos("a8"), frompos - 1);
                if (CASTLING_QUEENSIDE_ROOKTARGET[CIBLACK] == blackKingPos) { // we have problem here, as this can happen in Chess960, but would kick our own king piece of the board
                    takePieceAway(topos); // eliminate rook :*\
                    basicMoveFromTo(frompos, CASTLING_QUEENSIDE_KINGTARGET[CIBLACK]);  // move king instead
                    frompos = NOWHERE;
                    topos = CASTLING_QUEENSIDE_ROOKTARGET[CIBLACK];
                }
                else {
                    basicMoveFromTo(ROOK_BLACK, getSquare(topos).myPieceID(), topos, CASTLING_QUEENSIDE_ROOKTARGET[CIBLACK]);
                    //target position is always the same, even for chess960 - touches rook first, but nobody knows ;-)
                    topos = CASTLING_QUEENSIDE_KINGTARGET[CIBLACK];
                }
                didCastle = true;
            }
            kingsideCastlingAllowed[CIBLACK] = false;
            queensideCastlingAllowed[CIBLACK] = false;
        } else if (pceType == KING) {
            if ( kingsideCastlingAllowed[CIWHITE] && frompos<topos && isFirstRank(frompos) && isFirstRank(topos)
                    && ( toposType == ROOK || topos == frompos+2 )
                    && allSquaresEmptyFromTo(frompos,topos)
                    && isKingsideCastlingPossible(CIWHITE)
            ) {
                if ( toposPceID == NO_PIECE_ID && topos == frompos+2 )  // seems to be std-chess notation (king moves 2 aside)
                    topos = findRook(frompos+1, coordinateString2Pos("h1"));
                if ( CASTLING_KINGSIDE_ROOKTARGET[CIWHITE] ==whiteKingPos ) { // we have problem here, as this can happen in Chess960, but would kick our own king piece of the board
                    takePieceAway(topos); // eliminate rook :*\
                    basicMoveFromTo(frompos, CASTLING_KINGSIDE_KINGTARGET[CIWHITE]);  // move king instead
                    frompos = NOWHERE;
                    topos = CASTLING_KINGSIDE_ROOKTARGET[CIWHITE];
                } else {
                    basicMoveFromTo(ROOK, getSquare(topos).myPieceID(), topos, CASTLING_KINGSIDE_ROOKTARGET[CIWHITE]);
                    //target position is always the same, even for chess960 - touches rook first, but nobody knows ;-)
                    topos = CASTLING_KINGSIDE_KINGTARGET[CIWHITE];
                }
                didCastle = true;
            }
            else if ( queensideCastlingAllowed[CIWHITE] && frompos>topos && isFirstRank(frompos) && isFirstRank(topos)
                    && ( toposType == ROOK || topos == frompos-2 )
                    && allSquaresEmptyFromTo(frompos,topos)
                    && ( isSquareEmpty(CASTLING_QUEENSIDE_KINGTARGET[CIWHITE])
                         || getSquare(CASTLING_QUEENSIDE_KINGTARGET[CIWHITE]).myPieceType()==KING
                         || getSquare(CASTLING_QUEENSIDE_KINGTARGET[CIWHITE]).myPieceType()==ROOK )
                    && ( isSquareEmpty(CASTLING_QUEENSIDE_ROOKTARGET[CIWHITE])
                        || getSquare(CASTLING_QUEENSIDE_ROOKTARGET[CIWHITE]).myPieceType()==KING
                        || getSquare(CASTLING_QUEENSIDE_ROOKTARGET[CIWHITE]).myPieceType()==ROOK )
            ) {
                if (toposPceID == NO_PIECE_ID && topos == frompos - 2)  // seems to be std-chess notation (king moves 2 aside)
                    topos = findRook(coordinateString2Pos("a1"), frompos - 1);
                if (CASTLING_QUEENSIDE_ROOKTARGET[CIWHITE] == whiteKingPos) { // we have problem here, as this can happen in Chess960, but would kick our own king piece of the board
                    takePieceAway(topos); // eliminate rook :*\
                    basicMoveFromTo(frompos, CASTLING_QUEENSIDE_ROOKTARGET[CIWHITE]);  // move king instead
                    frompos = NOWHERE;
                    topos = CASTLING_QUEENSIDE_ROOKTARGET[CIWHITE];
                } else {
                    basicMoveFromTo(ROOK, getSquare(topos).myPieceID(), topos, CASTLING_QUEENSIDE_ROOKTARGET[CIWHITE]);
                    //target position is always the same, even for chess960 - touches rook first, but nobody knows ;-)
                    topos = CASTLING_QUEENSIDE_KINGTARGET[CIWHITE];
                }
                didCastle = true;
            }
            kingsideCastlingAllowed[CIWHITE] = false;
            queensideCastlingAllowed[CIWHITE] = false;
        } else if (kingsideCastlingAllowed[CIBLACK] && frompos == 7) {
            kingsideCastlingAllowed[CIBLACK] = false;
        } else if (queensideCastlingAllowed[CIBLACK] && frompos == 0) {
            queensideCastlingAllowed[CIBLACK] = false;
        } else if (kingsideCastlingAllowed[CIWHITE] && frompos == 63) {
            kingsideCastlingAllowed[CIWHITE] = false;
        } else if (queensideCastlingAllowed[CIWHITE] && frompos == 56) {
            queensideCastlingAllowed[CIWHITE] = false;
        }

        if (isBeatingSameColor && !didCastle) {
            internalErrorPrintln(String.format("Fehlerhafter Zug: %s%s schlägt eigene Figur auf %s.\n", squareName(frompos), squareName(topos), getBoardFEN()));
            return false;
        }

        if ( isPawn(pceType) && fileOf(frompos) != fileOf(topos) ) {
            // a beating pawn move
            countPawnsInFile[colorIndexOfPieceType(pceType)][fileOf(frompos)]--;
            countPawnsInFile[colorIndexOfPieceType(pceType)][fileOf(topos)]++;
        }

        if ( isPawn(pceType) || didCastle || toposPceID != NO_PIECE_ID ) {
            resetHashHistory();
            countBoringMoves = 0;
        }
        else {
            countBoringMoves++;
        }

        // move
        if (didCastle && frompos==NOWHERE ) {
            // special Chess960 case where Rook almost moved on kings position during castling and thus was eliminated instead
            spawnPieceAt(isWhite(getTurnCol()) ? ROOK : ROOK_BLACK, topos);
        }
        else
            basicMoveFromTo(pceType, pceID, frompos, topos);

        // promote to
        if ( isPawn(pceType)
                && (isLastRank(topos) || isFirstRank(topos))
        ) {
                if (promoteToPceType <= 0)
                    promoteToPceType = QUEEN;
                takePieceAway(topos);
                if (pceType == PAWN_BLACK)
                    spawnPieceAt(isPieceTypeWhite(promoteToPceType) ? promoteToPceType + BLACK_PIECE : promoteToPceType, topos);
                else
                    spawnPieceAt(isPieceTypeBlack(promoteToPceType) ? promoteToPceType - BLACK_PIECE : promoteToPceType, topos);
                completeCalc();
        } else {
            promoteToPceType = 0;
        }

        turn = opponentColor(turn);
        if (isWhite(turn))
            fullMoves++;

        fenPosAndMoves += " " + squareName(frompos) + squareName(topos)
                + (promoteToPceType > 0 ? (fenCharFromPceType(promoteToPceType | BLACK_PIECE)) : "");

        return true;
    }


    public boolean isKingsideCastlingPossible(int color) {
        int kingPos = getKingPos(color);
        return (kingsideCastlingAllowed[color]
                && !isCheck(color)
                && (isSquareEmpty(CASTLING_KINGSIDE_KINGTARGET[color])
                    || ( getSquare(CASTLING_KINGSIDE_KINGTARGET[color]).myPiece().color() == color
                         &&   (isKing(getSquare(CASTLING_KINGSIDE_KINGTARGET[color]).myPieceType())
                               || isRook(getSquare(CASTLING_KINGSIDE_KINGTARGET[color]).myPieceType()))) )
                && (isSquareEmpty(CASTLING_KINGSIDE_ROOKTARGET[color])
                    || (getSquare(CASTLING_KINGSIDE_ROOKTARGET[color]).myPiece().color() == color
                        && (isKing(getSquare(CASTLING_KINGSIDE_ROOKTARGET[color]).myPieceType())
                               || isRook(getSquare(CASTLING_KINGSIDE_ROOKTARGET[color]).myPieceType()))) )
                && allSquaresEmptyOrRookFromTo(kingPos, CASTLING_KINGSIDE_KINGTARGET[color])
                && allSquaresFromToWalkable4KingOfColor(kingPos, CASTLING_KINGSIDE_KINGTARGET[color], color )
        );
    }


    /**
     * searches for a rook and returns position
     * @param fromPosIncl startpos inclusive
     * @param toPosIncl endpos inclusive
     * @return position of the first rook found; NOWHERE if not found
     */
    int findRook(int fromPosIncl, int toPosIncl) {
        int dir = calcDirFromTo(fromPosIncl, toPosIncl);
        if (dir==NONE)
            return NOWHERE;
        int p=fromPosIncl;
        while (p!=toPosIncl){
            if (colorlessPieceType(getSquare(p).myPieceType()) == ROOK)
                return p;
            p+=dir;
        }
        if (colorlessPieceType(getSquare(p).myPieceType()) == ROOK)
            return p;
        return NOWHERE;
    }

    boolean allSquaresEmptyFromTo(final int fromPosExcl, final int toPosExcl) {
        int dir = calcDirFromTo(fromPosExcl, toPosExcl);
        if (dir==NONE)
            return false;
        int p=fromPosExcl+dir;
        while (p!=toPosExcl) {
            if (!isSquareEmpty(p))
                return false;
            p += dir;
        }
        return true;
    }

    /**
     * beware, this is an inprecise hack, there are rare cases, where the wrong rook could already in the way between the castelling moves
     * @param fromPosExcl exclusive
     * @param toPosExcl exclusive
     * @return true if castelling seems possible and there are only empty squares or rooks.
     */
    private boolean allSquaresEmptyOrRookFromTo(final int fromPosExcl, final int toPosExcl) {
        int dir = calcDirFromTo(fromPosExcl, toPosExcl);
        if (dir==NONE)
            return false;
        int p=fromPosExcl+dir;
        while (p!=toPosExcl) {
            if (!isSquareEmpty(p) && !(colorlessPieceType(getPieceAt(p).pieceType())==ROOK) )
                return false;
            p += dir;
        }
        return true;
    }

    /**
     * no attacks from color here? even not from a king-pinned piece , so designed to check if the king can go here.
     * @param fromPosExcl exclusive
     * @param toPosIncl exclusive
     * @return true if castelling seems possible and there are only empty squares or rooks.
     */
    private boolean allSquaresFromToWalkable4KingOfColor(final int fromPosExcl, final int toPosIncl, int color) {
        int dir = calcDirFromTo(fromPosExcl, toPosIncl);
        if (dir==NONE)
            return false;
        int p=fromPosExcl;
        do  {
            p += dir;
            if ( !getSquare(p).walkable4king(color) )
                return false;
        } while (p!=toPosIncl);
        return true;
    }

    public boolean doMove(String move){
        int startpos = 0;
        // skip spaces
        while (startpos < move.length() && move.charAt(startpos) == ' ')
            startpos++;
        move = move.substring(startpos);
        // an empty move string is not a legal move
        if (move.isEmpty())
            return false;

        SimpleMove m = new SimpleMove(move);
        if (m.isMove()) {
            // primitive move string wa successfully interpreted
        } else if (move.charAt(0) >= 'a' && move.charAt(0) < ('a' + NR_RANKS)) {
            int promcharpos;
            if (move.charAt(1) == 'x') {
                // a pawn beats something
                if (move.length() == 3) {
                    // very short form like "cxd" is not supported, yet
                    return false;
                }
                // a pawn beats something, like "hxg4"
                m.setTo(coordinateString2Pos(move, 2));
                m.setFrom(fileRank2Pos(move.charAt(0) - 'a', rankOf(m.to()) + (isWhite(getTurnCol()) ? -1 : +1)));
                promcharpos = 4;
            } else {
                // simple pawn move, like "d4"
                m.setTo(coordinateString2Pos(move, 0));
                m.setFrom(m.to() + (isWhite(getTurnCol()) ? +NR_FILES : -NR_FILES));  // normally it should come from one square below
                if (isWhite(getTurnCol()) && rankOf(m.to()) == 3) {
                    // check if it was a 2-square move...
                    if (getPieceTypeAt(m.from()) == EMPTY)
                        m.setFrom(m.from() + NR_FILES);   // yes, it must be even one further down
                } else if (isBlack(getTurnCol()) && rankOf(m.to()) == NR_RANKS - 4) {
                    // check if it was a 2-square move...
                    if (getPieceTypeAt(m.from()) == EMPTY)
                        m.setFrom(m.from() - NR_FILES);   // yes, it must be even one further down
                }
                promcharpos = 2;
            }
            // promotion character indicates what a pawn should be promoted to
            if ((isBlack(getTurnCol()) && isFirstRank(m.to())
                    || isWhite(getTurnCol()) && isLastRank(m.to()))) {
                char promoteToChar = move.length() > promcharpos ? move.charAt(promcharpos) : 'q';
                if (promoteToChar == '=') // some notations use a1=Q instead of a1Q
                    promoteToChar = move.length() > promcharpos + 1 ? move.charAt(promcharpos + 1) : 'q';
                m.setPromotesTo(getPceTypeFromPromoteChar(promoteToChar));
            }
        } else if (move.length() >= 3 && move.charAt(1) == '-' &&
                (move.charAt(0) == '0' && move.charAt(2) == '0'
                        || move.charAt(0) == 'O' && move.charAt(2) == 'O'
                        || move.charAt(0) == 'o' && move.charAt(2) == 'o')) {
            // castling - 0-0(-0) notation does not work for chess960 here, but this should be ok
            if (isWhite(getTurnCol()))
                m.setFrom(A1SQUARE + 4);
            else   // black
                m.setFrom(4);
            if (move.length() >= 5 && move.charAt(3) == '-' && move.charAt(4) == move.charAt(0))
                m.setTo(m.from() - 2);  // long castling
            else
                m.setTo(m.from() + 2);  // short castling
        } else {
            // must be a normal, non-pawn move
            int movingPceType = pceTypeFromPieceSymbol(move.charAt(0));
            if (movingPceType == EMPTY)
                internalErrorPrintln(format(chessBasicRes.getString("errorMessage.moveParsingError") + " <{0}>", move.charAt(0)));
            if (isBlack(getTurnCol()))
                movingPceType += BLACK_PIECE;
            int fromFile = -1;
            int fromRank = -1;
            if (move.length()<=2) {
                internalErrorPrintln("Error in move String <" + move + ">. ");
                return false;
            }
            if (isFileChar(move.charAt(2))) {
                // the topos starts only one character later, so there must be an intermediate information
                if (move.charAt(1) == 'x') {   // its beating something - actually we do not care if this is true...
                } else if (isFileChar(move.charAt(1)))  // a starting file
                    fromFile = move.charAt(1) - 'a';
                else if (isRankChar(move.charAt(1)))  // a starting rank
                    fromRank = move.charAt(1) - '1';
                m.setTo(coordinateString2Pos(move, 2));
            } else if (move.charAt(2) == 'x') {
                // a starting file or rank + a beating x..., like "Rfxf2"
                if (isFileChar(move.charAt(1)))      // a starting file
                    fromFile = move.charAt(1) - 'a';
                else if (isRankChar(move.charAt(1))) // a starting rank
                    fromRank = move.charAt(1) - '1';
                m.setTo(coordinateString2Pos(move, 3));
            } else {
                m.setTo(coordinateString2Pos(move, 1));
            }
            // now the only difficulty is to find the piece and its starting position...
            m.setFrom(-1);
            for (ChessPiece p : piecesOnBoard) {
                // check if this piece matches the type and can move there in one hop.
                // TODO!!: it can still take wrong piece that is pinned to its king...
                if (p != null && movingPceType == p.pieceType()                                    // found Piece p that matches the wanted type
                        && (fromFile == -1 || fileOf(p.pos()) == fromFile)       // no extra file is specified or it is correct
                        && (fromRank == -1 || rankOf(p.pos()) == fromRank)       // same for rank
                        && boardSquares[m.to()].canMoveHere(p.id())         // p can move here directly (distance==1)
                        && moveIsNotBlockedByKingPin(p, m.to())                                         // p is not king-pinned or it is pinned but does not move out of the way.
                ) {
                    m.setFrom(p.pos());
                    break;
                }
            }
            if (!m.isMove())
                return false;  // no matching piece found
        }
        return doMove(m);  //frompos, topos, promoteToFigNr);
    }

    /**
     * p is not king-pinned or it is pinned but does not move out of the way.
     */
    public boolean moveIsNotBlockedByKingPin(ChessPiece p, int topos){
        if (isKing(p.pieceType()))
            return true;
        int sameColorKingPos = p.isWhite() ? whiteKingPos : blackKingPos;
        if (sameColorKingPos < 0)
            return true;  // king does not exist... should not happen, but is part of some test-positions
        if (!isPiecePinnedToPos(p, sameColorKingPos))
            return true;   // p is not king-pinned
        if (colorlessPieceType(p.pieceType()) == KNIGHT)
            return false;  // a king-pinned knight can never move away in a way that it still avoids the check
        // or it is pinned, but does not move out of the way.
        int king2PceDir = calcDirFromTo(sameColorKingPos, topos);
        int king2TargetDir = calcDirFromTo(sameColorKingPos, p.pos());
        return king2PceDir == king2TargetDir;
        // TODO?:  could also be solved by more intelligent condition stored in the distance to the king
    }


    public boolean isPiecePinnedToPos(ChessPiece p,int pos){
        int pPos = p.pos();
        List<Integer> listOfSquarePositionsCoveringPos = null; // TODO: boardSquares[pos].getPositionsOfPiecesThatBlockWayAndAreOfColor(p.color());
        for (Integer covpos : listOfSquarePositionsCoveringPos)
            if (covpos == pPos)
                return true;
        return false;
    }

    public boolean posIsBlockingCheck(int kingColor, int pos){
        return boardSquares[pos].blocksCheckFor(kingColor);
    }

    public int nrOfLegalMovesForPieceOnPos ( int pos){
        ChessPiece sqPce = boardSquares[pos].myPiece();
        if (sqPce == null)
            return -1;
        return 0; // TODO   sqPce.getLegalMovesAndChances().size();
    }


    int getPieceTypeAt(int pos){
        int pceID = boardSquares[pos].myPieceID();
        if (pceID == NO_PIECE_ID || piecesOnBoard[pceID] == null)
            return EMPTY;
        return piecesOnBoard[pceID].pieceType();
    }

    private void takePieceAway ( int topos){
        //decreasePieceNrCounter(takenFigNr);
        //updateHash(takenFigNr, topos);
        ChessPiece p = getPieceAt(topos);
        piecesOnBoard[p.id()] = null;
        if (p.isWhite())
            countOfWhitePieces--;
        else
            countOfBlackPieces--;

        int pceType = p.pieceType();
        switch (colorlessPieceType(pceType)) {
            case BISHOP -> countBishops[colorIndexOfPieceType(pceType)]--;
            case KNIGHT -> countKnights[colorIndexOfPieceType(pceType)]--;
            case PAWN   -> countPawnsInFile[colorIndexOfPieceType(pceType)][fileOf(topos)]--;
        }

        for (Square s : boardSquares)
            s.removePiece(p.id());
        p.die();
        emptySquare(topos);
    }

    private void basicMoveFromTo(final int pceType, final int pceID, final int frompos, final int topos){
        if (frompos==topos)
            return;  // this is ok, e.g. in chess960 castling, a rook or king might end up in the exact same square again...
        if (pceType == KING)
            whiteKingPos = topos;
        else if (pceType == KING_BLACK)
            blackKingPos = topos;
        updateHashWithMove(frompos, topos);
        // re-place piece on board
        emptySquare(frompos);
        piecesOnBoard[pceID].setPos(topos);
        // tell the square
        setCurrentDistanceCalcLimit(0);
        boardSquares[topos].movePieceHereFrom(pceID, frompos);
        // tell all Pieces to update their vPieces (to recalc the distances)
        ChessPiece mover = piecesOnBoard[pceID];

        // TODO
//        mover.updateDueToPceMove(frompos, topos);
//        for (ChessPiece chessPiece : piecesOnBoard)
//            if (chessPiece != null && chessPiece != mover)
//                chessPiece.updateDueToPceMove(frompos, topos);
        completeCalc();
    }

    @Override
    public boolean isSquareEmpty(final int pos){
        return (boardSquares[pos].isEmpty());
    }

    private void emptySquare(final int frompos){
        boardSquares[frompos].emptySquare();
    }

    private void basicMoveFromTo(final int frompos, final int topos){
        int pceID = getPieceIdAt(frompos);
        int pceType = getPieceTypeAt(frompos);
        basicMoveFromTo(pceType, pceID, frompos, topos);
    }

    public String getPieceFullName(int pceId){
        return getPiece(pceId).toString();
    }

    @Override
    public GameState gameState() {
        checkAndEvaluateGameOver();
        if (isGameOver()) {
            if (isCheck(CIWHITE))
                return BLACK_WON;
            else if (isCheck(CIBLACK))
                return WHITE_WON;
            return DRAW;
        }
        // ongoing
        if (getFullMoves() == 0)
            return NOTSTARTED;
        return ONGOING;
    }

    @Override
    public Stream<ChessPiece> getPieces() {
        return Arrays.stream(piecesOnBoard).filter(Objects::nonNull);
    }

    public int getNrOfPieces(int color) {
        return color == CIWHITE ? countOfWhitePieces : countOfBlackPieces;
    }


    // virtual non-linear, but continuously increasing "clock" used to remember update-"time"s and check if information is outdated
    private long updateClockFineTicks = 0;

    public int getNrOfPlys () {
        if (isWhite(turn))
            return fullMoves * 2;
        return fullMoves * 2 + 1;
    }

    public long getUpdateClock() {
        return getNrOfPlys() * 10000L + updateClockFineTicks;
    }

    public long nextUpdateClockTick() {
        ++updateClockFineTicks;
        return getUpdateClock();
    }

    public void internalErrorPrintln(String s){
        System.err.println(chessBasicRes.getString("errormessage.errorPrefix") + s);
        System.err.println( "Board: " + getBoardFEN() );
    }


    public static void debugPrint(boolean doPrint, String s){
        if (doPrint)
            System.err.print(s);
    }

    public static void debugPrintln(boolean doPrint, String s){
        if (doPrint)
            System.err.println(s);
    }

    public int currentDistanceCalcLimit() {
        return currentDistanceCalcLimit;
    }

    private void setCurrentDistanceCalcLimit ( int newLimit){
        currentDistanceCalcLimit = min(MAX_INTERESTING_NROF_HOPS, newLimit);
    }


    //// Hash methods

    // initialize random values once at startup
    static private final long[] randomSquareValues = new long[70];
    static {
        for (int i=0; i<randomSquareValues.length; i++)
            randomSquareValues[i] = (long)(random()*((double)(Long.MAX_VALUE>>1)));
    }

    private void resetHashHistory() {
        boardHashHistory = new ArrayList<List<Long>>(2);  // 2 for the colors
        for (int ci = 0; ci < 2; ci++) {
            boardHashHistory.add( new ArrayList<Long>(50) );  // then ArrayList<>(50) for 50 Hash values:
        }
        repetitions = 0;
    }

    /**
     * returns how many times the position reached by a move has been there before
     */
    int moveLeadsToRepetitionNr(int frompos, int topos) {
        int color = opponentColor(getTurnCol());
        long resultingHash = calcBoardHashAfterMove(frompos,topos);
        return countHashOccurrencesForColor(resultingHash, color)+1;
    }

    private int countHashOccurrencesForColor(long resultingHash, int color) {
        return (int) (boardHashHistory.get(color).stream()
                .filter(h -> (h.equals(resultingHash)))
                .count());
    }

    public long getBoardHash() {
        long hash= boardHash ^ randomSquareValues[68] * getEnPassantFile();
        if (isQueensideCastleAllowed(CIBLACK))
            hash ^= randomSquareValues[64];
        if (isKingsideCastleAllowed(CIBLACK))
            hash ^= randomSquareValues[65];
        if (isQueensideCastleAllowed(CIWHITE))
            hash ^= randomSquareValues[66];
        if (isKingsideCastleAllowed(CIWHITE))
            hash ^= randomSquareValues[67];
        /*if (turn)  // we do not need to hash the turn-flag, as we store hashHistroy seperately for both color turns
            hash ^= randomSquareValues[69];
        /*if (countBoringMoves>MAX_BORING_MOVES-3)
            hash ^= randomSquareValues[70]*(MAX_BORING_MOVES-countBoringMoves+1);*/  // move repetitions should lea to same hash value
        return boardHash;
    }

    private void initHash() {
        boardHash = 0;
        for (int p=0; p<64; p++) {
            long f = getPieceTypeAt(p);
            if (f!=NO_PIECE_ID)
                boardHash ^= randomSquareValues[p]*f;
        }
        repetitions = 0;
    }

    public long calcBoardHashAfterMove(int frompos, int topos) {
        // TODO: EnPassant und Rochade sind hier nicht implementiert!
        int fromPceType = getPieceTypeAt(frompos);
        int takenPceType = getPieceTypeAt(topos);
        long hash = rawUpdateAHash( getBoardHash(), fromPceType, frompos);
        if (takenPceType != NO_PIECE_ID)
            hash = rawUpdateAHash( hash, takenPceType, topos);
        hash = rawUpdateAHash( hash, fromPceType, topos);
        return hash;
    }

    private void updateHashWithMove(final int frompos, final int topos) {
        boardHash = calcBoardHashAfterMove(frompos, topos);
        // add Hash To History
        int nextci = opponentColor(getTurnCol());
        boardHashHistory.get( nextci ).add(getBoardHash());
        repetitions = countHashOccurrencesForColor(boardHash, nextci ) - 1;
    }

    static public long rawUpdateAHash(long hash, int pceType, int pos) {
        if (((long)pceType) != 0)
            hash ^= randomSquareValues[pos] * ((long)pceType);
        return hash;
    }


    //// getter

    public int getPieceCounter() {
        return countOfWhitePieces + countOfBlackPieces;
    }

    public int getPieceCounterForColor(int color){
        return isWhite(color) ? countOfWhitePieces
                : countOfBlackPieces;
    }

    public int getLightPieceCounterForPieceType(int pceType ){
        int ci = colorIndex(colorOfPieceType(pceType));
        if ( colorlessPieceType(pceType) == BISHOP )
            return countBishops[ci];
        //else  == KNIGHT
        return countKnights[ci];
    }

    public int getPawnCounterForColorInFileOfPos(int color, int pos ){
        int file = fileOf(pos);
        return countPawnsInFile[color][file];
    }

    public boolean isGameOver() {
        return gameOver;
    }

    @Override
    public int futureLevel() {
        return 0;
    }

    public int nrOfLegalMoves(int color){
        return nrOfLegalMoves[color];
    }

    @Override
    public boolean hasLegalMoves(int color){
        return nrOfLegalMoves[color] > 0;
    }

//    @Override
//    public VBoardInterface preBoard() {
//        return null;
//    }

    public int getKingPos(int color){
        return isWhite(color) ? whiteKingPos : blackKingPos;
    }

    public int getKingId(int color) {
        return getPieceIdAt(getKingPos(color));
    }

    public static int getMAX_INTERESTING_NROF_HOPS() {
        return MAX_INTERESTING_NROF_HOPS;
    }

    @Override
    public int getTurnCol() {
        return turn;
    }

    StringBuffer getBoardName() {
        StringBuffer n = new StringBuffer(boardName + "("+engineP1()+")");
        return n;
    }

    StringBuffer getShortBoardName() {
        return boardName;
    }

    @Deprecated
    public Square[] getBoardSquares() {
        return boardSquares;
    }

    public int getRepetitions() {
        return repetitions;
    }

    Square getSquare(int pos) {
        return boardSquares[pos];
    }


    //// setter

    public static void setMAX_INTERESTING_NROF_HOPS ( int RECONST_MAX_INTERESTING_NROF_HOPS){
        MAX_INTERESTING_NROF_HOPS = RECONST_MAX_INTERESTING_NROF_HOPS;
    }

    public void setEngParams(ChessEngineParams engParams) {
        this.engParams = engParams;
    }

    //void setTurn(boolean turn);

    /** like moveIsMoreOrLessHinderingMove(), but false for m moving out of the way of the target - unless this really
     * hinders the move (i.e. a taking pawn move can be hindered by moving out of the way)
     *
     * @param m
     * @param m2bBlocked
     * @return
     */
    private boolean moveIsReallyHinderingMove(SimpleMove m, SimpleMove m2bBlocked) {
        if (m.to() == m2bBlocked.from())
            return true;
        if ( m.from() == m2bBlocked.to()
                && isPawn((getSquare(m2bBlocked.from()).myPiece().pieceType()) )
                && !isSquareEmpty(m2bBlocked.to()) ) {
            return true; // pawn tries to take, but target moves away, so m hinders the pawn move
        }
        if (isBetweenFromAndTo(m.to(), m2bBlocked.from(), m2bBlocked.to()))
            return true;
        return false;
    }

    @Override
    public String toString() {
        return "ChessBoard{" +
                boardName + ": " +
                "" + getGameStateDescription() + ", " +
                "FEN=" + getBoardFEN() +
                '}';
    }

}