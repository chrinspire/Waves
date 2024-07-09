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
 *     The test data in the .cvs files come form the great Lichess database.
 *     Many thanks to all lichess contributors!
 */

package de.ensel.waves.pubTests;

import de.ensel.waves.ChessBoard;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static de.ensel.waves.ChessBoardTest.doAndTestPuzzle;

public class CsvTests {

    //    @CsvFileSource(resources = "/home/christian/IdeaProjects/Waves/src/test/data/puzzles/",
//            files = "lichess_db_puzzle_230601_410-499-mateIn1.csv",
//            numLinesToSkip = 0);
//    files = {"src/test/resources/de/ensel/waves/lichess_db_puzzle_230601_2k-12xx.csv",
//            "src/test/resources/de/ensel/waves/lichess_db_puzzle_230601_410-499-NOTmateIn1.csv"},



    @ParameterizedTest
    @CsvFileSource( resources = { "/de/ensel/waves/lichess_db_puzzle_230601_410-499-mateIn1.csv" },
            numLinesToSkip = 0)
    public void ChessBoardGetBestMove_PuzzleMateIn1_Test(String puzzleId, String fen, String moves,
                                                         String rating, String ratingDeviation, String popularity,
                                                         String nbPlays,
                                                         String themes, String gameUrl, String openingTags) {
        ChessBoard.DEBUGMSG_MOVEEVAL = false;
        ChessBoard.DEBUGMSG_MOVESELECTION = false;
        doAndTestPuzzle(fen, moves, themes, false, false);
    }

    @ParameterizedTest
    @CsvFileSource( resources = { "/de/ensel/waves/must1.csv" },
            numLinesToSkip = 0)
    public void ChessBoardGetBestMove_PuzzleMust1_Test(String puzzleId, String fen, String moves,
                                           String rating, String ratingDeviation, String popularity,
                                           String nbPlays,
                                           String themes, String gameUrl, String openingTags) {
        ChessBoard.DEBUGMSG_MOVEEVAL = false;
        ChessBoard.DEBUGMSG_MOVESELECTION = false;
        doAndTestPuzzle(fen, moves, themes, false, false);
    }

    @Disabled
    @ParameterizedTest
    @CsvFileSource( resources = { "/de/ensel/waves/lichess_db_puzzle_230601_410-499-NOTmateIn1.csv" },
            numLinesToSkip = 0)
    public void ChessBoardGetBestMove_Puzzle4xxExceptMateIn1_Test(String puzzleId, String fen, String moves,
                                                  String rating, String ratingDeviation, String popularity,
                                                  String nbPlays,
                                                  String themes, String gameUrl, String openingTags) {
        ChessBoard.DEBUGMSG_MOVEEVAL = false;
        ChessBoard.DEBUGMSG_MOVESELECTION = false;
        doAndTestPuzzle(fen, moves, themes, false, false);
    }

    @Disabled
    @ParameterizedTest
    @CsvFileSource( resources = { "/de/ensel/waves/lichess_db_puzzle_230601_2k-5xx.csv" },
            numLinesToSkip = 0)
    public void ChessBoardGetBestMove_Puzzle5xx_Test(String puzzleId, String fen, String moves,
                                           String rating, String ratingDeviation, String popularity,
                                           String nbPlays,
                                           String themes, String gameUrl, String openingTags) {
        ChessBoard.DEBUGMSG_MOVEEVAL = false;
        ChessBoard.DEBUGMSG_MOVESELECTION = false;
        doAndTestPuzzle(fen, moves, themes, false, false);
    }

    @Disabled
    @ParameterizedTest
    @CsvFileSource( resources = { "/de/ensel/waves/lichess_db_puzzle_230601_2k-12xx.csv" },
            numLinesToSkip = 0)
    public void ChessBoardGetBestMove_Puzzle12xx_Test(String puzzleId, String fen, String moves,
                                                  String rating, String ratingDeviation, String popularity,
                                                  String nbPlays,
                                                  String themes, String gameUrl, String openingTags) {
        ChessBoard.DEBUGMSG_MOVEEVAL = false;
        ChessBoard.DEBUGMSG_MOVESELECTION = false;
        doAndTestPuzzle(fen, moves, themes, false, false);
    }

    @Disabled
    @ParameterizedTest
    @CsvFileSource( resources = { "/de/ensel/waves/lichess_db_puzzle_230601_2k-16xx.csv" },
            numLinesToSkip = 0)
    public void ChessBoardGetBestMove_Puzzle16xx_Test(String puzzleId, String fen, String moves,
                                                  String rating, String ratingDeviation, String popularity,
                                                  String nbPlays,
                                                  String themes, String gameUrl, String openingTags) {
        ChessBoard.DEBUGMSG_MOVEEVAL = false;
        ChessBoard.DEBUGMSG_MOVESELECTION = false;
        doAndTestPuzzle(fen, moves, themes, false, false);
    }

//    @CsvFileSource(resources = "lichess_db_puzzle_230601_410-499-mateIn1.csv",
//            numLinesToSkip = 0);
//    @CsvFileSource(resources = "lichess_db_puzzle_230601_2k-16xx.csv",
//            resources = "lichess_db_puzzle_230601_2k-16xx.csv",
//            numLinesToSkip = 0)



}
