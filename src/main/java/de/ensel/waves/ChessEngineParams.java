package de.ensel.waves;

public record ChessEngineParams(int searchMaxDepth, int searchMaxNrOfBestMovesPerPly) {
    public static final int DEFAULT_SEARCH_MAX_DEPTH = 10;
    public static final int DEFAULT_SEARCH_MAX_NR_OF_BEST_MOVES_PER_PLY = 6;
    ChessEngineParams() {
        this(DEFAULT_SEARCH_MAX_DEPTH, DEFAULT_SEARCH_MAX_NR_OF_BEST_MOVES_PER_PLY);
    }
}
