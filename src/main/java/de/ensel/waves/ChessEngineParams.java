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

public record ChessEngineParams(String paramSetName, int searchMaxDepth, int searchMaxNrOfBestMovesPerPly) {
    public static final int LEVEL_EASY = 1;
    public static final int LEVEL_LOW = 3;
    public static final int LEVEL_MID = 5;
    public static final int LEVEL_GOOD = 6;
    public static final int LEVEL_BETTER = 8;
    public static final int LEVEL_TEST_QUICK = 4;
    public static final int LEVEL_TEST_MED = 7;
    public static final int LEVEL_TEST_LONG = 9;

    public static final int MAX_SEARCH_DEPTH = 14;
    public static final int LEVEL_DEFAULT = LEVEL_TEST_QUICK;

    public static final ChessEngineParams[] levels = new ChessEngineParams[]{
            new ChessEngineParams("zero", 0, 100),
            new ChessEngineParams("easy", 1, 4),
            new ChessEngineParams("easy2", 1, 20),
            new ChessEngineParams("low", 4, 4),
            new ChessEngineParams("test-q", 6, 4),    // LEVEL_TEST_QUICK
            new ChessEngineParams("mid", 6, 6),
            new ChessEngineParams("good", 8, 6),
            new ChessEngineParams("test-m", 10, 6),   // LEVEL_TEST_MID
            new ChessEngineParams("better", 11, 10),
            new ChessEngineParams("test-max", 12, 10),  // LEVEL_TEST_LONG
            new ChessEngineParams("max/30", MAX_SEARCH_DEPTH, 30),
            new ChessEngineParams("max/100", MAX_SEARCH_DEPTH, 100),
    };

    ChessEngineParams() {
        this("default="+levels[LEVEL_DEFAULT].paramSetName(), levels[LEVEL_DEFAULT].searchMaxDepth(), levels[LEVEL_DEFAULT].searchMaxNrOfBestMovesPerPly());
    }

    ChessEngineParams(int level) {
        this("level="+levels[level].paramSetName(), levels[level].searchMaxDepth(), levels[level].searchMaxNrOfBestMovesPerPly());
    }
}
