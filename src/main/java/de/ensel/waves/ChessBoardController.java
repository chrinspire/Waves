/*
 *     TideEval - Wired New Chess Algorithm
 *     Copyright (C) 2023 Christian Ensel
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

import de.ensel.waves.UCI4ChessEngine.ChessEngine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import static de.ensel.chessbasics.ChessBasics.*;
import static de.ensel.waves.ChessBoard.NO_PIECE_ID;

public class ChessBoardController implements ChessEngine {
    ChessBoard board;

    @Override
    public boolean doMove(String move) {
        return board.doMove(move);
    }

    @Override
    public String getMove() {
        if (board.isGameOver())
            return null;
        //TODO: chessBoard.go();
        // should be replaced by async functions, see interface
        return board.getMove();
    }

    @Override
    public void setBoard(String fen) {
        if (board==null) {
            board = new ChessBoard(chessBasicRes.getString("chessboard.initialName"), fen);
        }
        else {
            if (!board.updateBoardFromFEN(fen) && !fen.equals(FENPOS_STARTPOS)) {
                // seems the fen ins repeated - maybe I answered with an illegal move? try a board reset.
                System.err.println("Board " + board.getBoardFEN() + " was called to update with equal FEN string: " + fen + ".");
                board = new ChessBoard(chessBasicRes.getString("chessboard.initialName"), board.getBoardFEN());
            }
        }
    }

    @Override
    public boolean setParam(String paramName, String value) {
        String param = paramName.toLowerCase(Locale.ROOT);
        switch (paramName) {
            case "hops", "nrofhops" -> {
                ChessBoard.setMAX_INTERESTING_NROF_HOPS(Integer.parseInt(value));
                return true;
            }
            case "engineP1" -> {
                ChessBoard.setEngineP1(Integer.parseInt(value));
                return true;
            }
        }
        return false;
    }

    @Override
    public String getBoard() {
        return board.getBoardFEN();
    }

    @Override
    public HashMap<String,String > getBoardInfo() {
        HashMap<String,String> boardInfo = new HashMap<>();
        boardInfo.put("BoardInfo of:", board.getBoardName().toString() + " {"+board.getBoardHash()+"}");
        //boardInfo.put("Nr. of moves & turn:", ""+chessBoard.getFullMoves()  );
        boardInfo.put("FEN:", board.getBoardFEN());
        boardInfo.put("Game state:", board.getGameState()
                + ( board.isCheck(board.getTurnCol()) ? " "+board.nrOfChecks(board.getTurnCol())+" checks" : "")
                + " --> " + ( board.isGameOver() ? "Game Over" : (" turn: " + colorName(board.getTurnCol()) + "" ) ) );
        boardInfo.put("Attack balance on opponent side, king area / defend own king:", "");
        boardInfo.put("Evaluation (overall - piece values, max clashes, mobility) -> move sugestion:", ""
                + board.boardEvaluation()+" - "
                + board.boardEvaluation(1) + ", "
                + board.boardEvaluation(4)
                + " -> " + board.getBestMove() );
        return boardInfo;
    }

    @Override
    public int getBoardEvaluation() {
        return board.boardEvaluation();
    }

    @Override
    public HashMap<String,String> getSquareInfo(String square, String squareFrom) {
        HashMap<String,String> squareInfo = new HashMap<>();
        int pos = coordinateString2Pos(square);
        int squareFromPos = squareFrom.length()<2 ? pos : coordinateString2Pos(squareFrom);
        int squareFromPceId = board.getPieceIdAt(squareFromPos);
        // basic square name (is now in headline)
        // does it contain a chess piece?
        ChessPiece pce = board.getPieceAt(pos);
        final String pceInfo;
        if (pce!=null) {
            pceInfo = pce.toString();
            squareInfo.put("Square's piece:", "" + (pce==null ? "-" : pce.toString() ));
        }
        else
            pceInfo = chessBasicRes.getString("pieceCharset.empty");
        // squareInfo.put("Piece:",pceInfo);
        Square sq = board.getSquare(pos);
        //squareInfo.put("SquareId:",""+pos+" = "+ squareName(pos));
        squareInfo.put("Base Value:",""+(pce==null ? "0" : pce.baseValue()));
        if (squareFromPceId!=NO_PIECE_ID) {
            int d = sq.getDistanceToPieceId(squareFromPceId);
            squareInfo.put("* Sel. piece's Distance:", "" + ( false ? -d : d )  );
            if (pce!=null)
                squareInfo.put("Moves+Evals: ", "" + pce.toString() );
        }

        // information specific to this square
        squareInfo.put("May block check:",""+ ( (sq.blocksCheckFor(CIWHITE)? 3:0) + (sq.blocksCheckFor(CIBLACK)? -2:0)) );
        //squareInfo.put("Attacks by white:",""+ sq.countDirectAttacksWithColor(WHITE) );
        //squareInfo.put("Attacks by black:",""+ sq.countDirectAttacksWithColor(BLACK) );
        squareInfo.put("Clash Eval:",""+sq.clashEval());
        squareInfo.put("Coverage by White:", " " +sq.getCoverageInfoByColorForLevel(WHITE, 1)
                +" "+sq.getCoverageInfoByColorForLevel(WHITE, 2));
        squareInfo.put("Coverage by Black:", " " +sq.getCoverageInfoByColorForLevel(BLACK, 1)
                +" "+sq.getCoverageInfoByColorForLevel(BLACK, 2));

        // distance info for alle pieces in relation to this square
        for (Iterator<ChessPiece> it = board.getPiecesIterator(); it.hasNext(); ) {
            ChessPiece p = it.next();
            if (p != null) {
                int pID = p.id();
                int distance = sq.getDistanceToPieceId(pID);

                if (distance<board.getMAX_INTERESTING_NROF_HOPS())
                    squareInfo.put("z " + p + " ("+pID+") Distance: ",
                                "" + ( false ? -distance : distance )
                                + " (" + sq.getDistanceToPieceId(pID)
                    );
            }
        }
        return squareInfo;
    }
}
