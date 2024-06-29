package de.ensel.waves;

public record ChessEngineParams(String paramSetName, int searchMaxDepth, int searchMaxNrOfBestMovesPerPly) {
    public static final int LEVEL_EASY = 1;
    public static final int LEVEL_LOW = 3;
    public static final int LEVEL_MID = 5;
    public static final int LEVEL_HIGH = 7;
    public static final int LEVEL_TEST_QUICK = 4;
    public static final int LEVEL_TEST_MID = 6;
    public static final int LEVEL_TEST_LONG = 7;

    public static final ChessEngineParams[] levels = new ChessEngineParams[]{
            new ChessEngineParams("zero", 0, 100),
            new ChessEngineParams("easy", 1, 4),
            new ChessEngineParams("easy2", 1, 20),
            new ChessEngineParams("low", 4, 4),
            new ChessEngineParams("test-q", 6, 4),    // LEVEL_TEST_QUICK
            new ChessEngineParams("mid", 6, 6),
            new ChessEngineParams("test-m", 10, 6),   // LEVEL_TEST_MID
            new ChessEngineParams("test-l", 12, 10),  // LEVEL_TEST_LONG
            new ChessEngineParams("high", 12, 10),
    };

    ChessEngineParams() {
        this("default=easy", levels[LEVEL_EASY].searchMaxDepth, levels[LEVEL_EASY].searchMaxNrOfBestMovesPerPly);
    }

    ChessEngineParams(int level) {
        this("default=easy", levels[level].searchMaxDepth, levels[level].searchMaxNrOfBestMovesPerPly);
    }
}
