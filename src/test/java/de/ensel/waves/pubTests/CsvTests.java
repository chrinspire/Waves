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
        doAndTestPuzzle(fen, moves, themes, false);
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
        doAndTestPuzzle(fen, moves, themes, false);
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
        doAndTestPuzzle(fen, moves, themes, false);
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
        doAndTestPuzzle(fen, moves, themes, false);
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
        doAndTestPuzzle(fen, moves, themes, false);
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
        doAndTestPuzzle(fen, moves, themes, false);
    }

//    @CsvFileSource(resources = "lichess_db_puzzle_230601_410-499-mateIn1.csv",
//            numLinesToSkip = 0);
//    @CsvFileSource(resources = "lichess_db_puzzle_230601_2k-16xx.csv",
//            resources = "lichess_db_puzzle_230601_2k-16xx.csv",
//            numLinesToSkip = 0)



}
