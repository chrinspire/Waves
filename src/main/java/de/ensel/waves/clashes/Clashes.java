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
 *     This file is "adopted and adapted" from GlubschFish by Christian Ensel.
 */

package de.ensel.waves.clashes;
//import java.util.concurrent.TimeUnit;

import static de.ensel.chessbasics.ChessBasics.*;
import static de.ensel.waves.clashes.CoverageBitMap.*;
import static java.lang.Math.abs;

public abstract class Clashes {
    // derived from the coverage bit-representation in 13 bits per color (see CoverageBitMap)
    // a concat of the black.white representation is  used to represent and resolve clashes.

    //
    static final int DEBUG_VERBOSITY_CLASHCALC = 0;
    private static final boolean CACHE_ACTIVATED = true; // default: true;

    //
    private static final int cacheSize = 67108864; // 2^(2*13) = 67.108.864
    private static final short[] clashEvalResultCache = new short[cacheSize];  // Array of Evaluations
    private static final short[] clashPceIdResultCache = new short[cacheSize];  // Array of PieceIDs (of piece remaining after evaluation)
    private static int countCacheHits = 0;
    private static int countCacheMisses = 0;
    /*clashBitRep*/

    // Initialize cache
    static {
      if (CACHE_ACTIVATED) {
        if (DEBUG_VERBOSITY_CLASHCALC > 1)
            System.out.println("Initializing Cache with size " + cacheSize);
        long startTime = System.nanoTime();
        clashEvalResultCache[0] = 0;
        for (int i = 1; i < cacheSize; i++) {
            clashEvalResultCache[i] = Short.MIN_VALUE;
            clashPceIdResultCache[i] = EMPTY;
        }
        /*for (int i = 0; i < 8192; i++) {
            for (int j = 0; j < 8192; j++)
                calcClashResultFromCurrentPlayerPerspective(-PT_SOMEOFMINE,i,j);
            System.out.println(Clashes.getCacheStatistics());
        }*/
        long elapsedTime = System.nanoTime() - startTime;
        if (DEBUG_VERBOSITY_CLASHCALC > 0)
            System.out.println("************** Zeit für Cache-Initialisierung: " + elapsedTime / 1000000 + " Millisekunden");
      }
    }

    private static void addResultToCache(int result, int playerCBM, int oppCBM, int/*PceType*/ remainingPceType) {
        if (!CACHE_ACTIVATED) return;
        if (DEBUG_VERBOSITY_CLASHCALC > 1)
            System.out.println("Füge ein: Cache" + CoverageBitMap.cbm2fullString(playerCBM) + CoverageBitMap.cbm2fullString(oppCBM) + "=" + result);
        int cbr = (playerCBM << CBM_LENGTH) | oppCBM;
        clashEvalResultCache[cbr] = (short) result;
        clashPceIdResultCache[cbr] = (short) remainingPceType;
        // BLI: if one CBM==0, add all same results, which have same CBM but additional (unused) pieces in other CBM;
    }

    private static int getEvalCacheEntry(int playerCBM, int oppCBM) {
        int cbr = (playerCBM << CBM_LENGTH) | oppCBM;
        if (clashEvalResultCache[cbr] == Short.MIN_VALUE)
            return Integer.MIN_VALUE+1;
        if (DEBUG_VERBOSITY_CLASHCALC > 1)
            System.out.println("Lese aus Cache" + CoverageBitMap.cbm2fullString(playerCBM) + CoverageBitMap.cbm2fullString(oppCBM) + "=" + clashEvalResultCache[cbr]);
        return clashEvalResultCache[cbr];
    }

    private static int getPceTypeCacheEntry(int playerCBM, int oppCBM) {
        int cbr = (playerCBM << CBM_LENGTH) | oppCBM;
        return clashPceIdResultCache[cbr];
    }

    public static String getCacheStatistics() {
        return "Cache has " + countCacheMisses + " Entries and resulted in " + countCacheHits + " hits.";
    }


    /** central piece clash calculation
     *
     * @param pceTypeOnSq piece type (from ChessBasics) on the square
     * @param whiteCBR white covering pieces - see CBR
     * @param blackCBR black covering pieces - see CBR
     * @return evaluation of the clash if carried out in a reasonable way
     */
    public static int calcBiasedClash(
            int/*ChessBasics.pceType*/ pceTypeOnSq,
            int/*CBR*/ whiteCBR,
            int/*CBR*/ blackCBR) {
        return calcBiasedClashResultForClashPceTypeFromBoardPerspective( pieceBaseValue(pceTypeOnSq), clashPcsType2CBPceType(pceTypeOnSq), whiteCBR, blackCBR);
    }

    static int calcBiasedClashResultForClashPceTypeFromBoardPerspective(
            int/*Eval0*/ bias,
            int/*!Clash-PceType!*/ clashPceTypeOnSq,
            int/*CBR*/ whiteCBR,
            int/*CBR*/ blackCBR) {
        // clashPceTypeOnSq:  neg for opponents TODO: comment prop. old, check if this method already works with + for w and - for black
        // bias: value of piece on this square (board perspektive as usual)  + possible additional bias .
        // cbr: see CBR :-)
        if (DEBUG_VERBOSITY_CLASHCALC > 0)
            System.out.print("calcClashResultFromBoardPerspective( " + bias + ", " + pieceNameForType(clashPceTypeOnSq)
                    + ", " + cbm2fullString(whiteCBR) + "<->" + cbm2fullString(blackCBR) + " ) ");
        int result;
        /*if (whiteCBR == 0 && blackCBR == 0) {
            if (beVerbose)
                System.out.print("calcClashResult() mit cbr 0 aufgerufen ");
            result = bias;
        }
        else */
        if (clashPceTypeOnSq == EMPTY ) {
            return 0;
//            was: // d.h. auf dem Feld steht keine Figur = fiktive Bewertung, zu welchem Preis man einen Läufer opfern könnte (und davon für weiß+schwarz das Delta/4)
//            if (DEBUG_VERBOSITY_CLASHCALC > 0)
//                System.out.print("<-- es steht keine Figur auf dem Feld. Bewerte, wer es besser abdeckt:  ");
//            result      = calcBiasedClashResultFromCurrentPlayerPerspective(-bias - positiveClashPieceBaseValue(BISHOP),
//                                                                            whiteCBR, blackCBR);
//            int resultB = calcBiasedClashResultFromCurrentPlayerPerspective(bias - positiveClashPieceBaseValue(BISHOP),
//                                                                            blackCBR, whiteCBR);
//            if (DEBUG_VERBOSITY_CLASHCALC > 0)
//                System.out.print(" (" + result + " vs " + resultB + ")  ");
//            if (result > resultB)
//                result = (positiveClashPieceBaseValue(QUEEN) + positiveClashPieceBaseValue(BISHOP) - positiveClashPieceBaseValue(CoverageBitMap.getSmallestPieceTypeFromCoverage(whiteCBR))) >> 5;
//            else if (result < resultB)
//                result = -((positiveClashPieceBaseValue(QUEEN) + positiveClashPieceBaseValue(BISHOP) - positiveClashPieceBaseValue(CoverageBitMap.getSmallestPieceTypeFromCoverage(blackCBR))) >> 5);
//            else
//                result = 0;
//            if (DEBUG_VERBOSITY_CLASHCALC > 0)
//                System.out.println(" Bewertung des leeren Feldes: " + result);
//            return result;
        } else if (isPieceTypeWhite(clashPceTypeOnSq)) { // d.h. auf dem Feld steht eine weiße Figur
            if (DEBUG_VERBOSITY_CLASHCALC > 0)
                System.out.print("<-- es steht eine weiße Figur auf dem Feld. Was kann Schwarz machen?  ");
            result = -calcBiasedClashResultFromCurrentPlayerPerspective( -bias,
                                                                         blackCBR, whiteCBR);
        } else { //schwarze Figur:   //  Das wird über den umgekehrten Weg einer weißen Figur berechnet.
            if (DEBUG_VERBOSITY_CLASHCALC > 0)
                System.out.print("<-- es steht eine schwarze Figur auf dem Feld. Schaue, was Weiß machen kann:  ");
            result = calcBiasedClashResultFromCurrentPlayerPerspective( bias,
                                                                        whiteCBR, blackCBR);
        }
        if (DEBUG_VERBOSITY_CLASHCALC > 0)
            System.out.println("==> Ok, Bewertung: " + result);
        return result;
    }

//
//    private static int calcClashResultFromCurrentPlayerPerspective(int/*NR*/alternatingPceTypeOnSq, int/*CBR*/ playerCBR, int/*CBR*/ oppCBR) {
//        // needs to be called for a square with an opponents piece on it.
//        // alternatingPceTypeOnSq:  different from the usual pceType interpretation:
//        // orig docu here said, it must be positive for player, but negative for his opponent. But actually it does not matter here, it is always abs()ed
//        //     "wird immer aufgerufen mit einer gegnerischen Figur am Feld!  (sonst könnte ich nicht schlagen!)"
//        // todo: re-find out, if this is player who's turn it is or player of piece at the square...
//        return calcBiasedClashResultFromCurrentPlayerPerspective(positiveClashPieceBaseValue(alternatingPceTypeOnSq), playerCBR, oppCBR);
//    }

    /**
     * needs to be called for a square with an opponents piece on it, otherwise nothing could be captured.
     * @param bias hat damit normal den negativen Wert der gegnerischen Figur, die darauf steht.  (Dis kann bei Bedarf rauf oder runter verschoben werden)
     * @param playerCBR - see CBR
     * @param oppCBR _ see CBR
     * @return resulting evaluation after reasonable clash
     */
    private static int calcBiasedClashResultFromCurrentPlayerPerspective(int bias, int/*CBR*/ playerCBR, int/*CBR*/ oppCBR) {
        if (DEBUG_VERBOSITY_CLASHCALC > 1)
              System.out.print("calcBiasedClashResultFromCurrentPlayerPerspective( bias=" + bias
                    + ", " + cbm2fullString(playerCBR) + "<->" + cbm2fullString(oppCBR) + " ) ");
        /*if (figOnFieldNr > 0) { // d.h. auf dem Feld steht eine eigene Figur
              System.out.print("***** Fehler: es steht eine eigene Figur auf dem Feld. Hier kann man nichts nehmen ");
            return 0;
        }*/

        int result = Integer.MIN_VALUE+1;
        int remainingPceType;

        if (playerCBR == 0) {   // hab nix mehr
            return -1;            // Gegner hat die letzte Figur drauf (also "gewonnen", aber hier im letzten Moment gerade kein Material gewonnen)
        } else if (playerCBR == KING_COVER_1 && oppCBR > 0) {   // habe nur noch nur König, aber Gegner hat noch was
            return -1;            // Gegner hat noch Figuren drauf aber ich nur den König, kann daher nicht schlagen.
        } else {
            int usedPceType = CoverageBitMap.getSmallestPieceTypeFromCoverage(playerCBR);
            if (usedPceType == 0) {
                 System.err.println("**** Fehler: calcClashResult() mit playerCBR==0 Sollte an dieser Stelle nicht vorkommen!.");
                //throw new Exception(String.format("Error in call to calcBiasedClashResultFromCurrentPlayerPerspective(%+d,%s,%s)", bias, toFullString(playerCBR), toFullString(oppCBR) ));
                result = -1;
            } else {
                // mit gefundener nächster Figur "schlagen"
                // System.err.print(" ->" + toFullString(subCBR) + ".  ");
                // System.err.print(" -> Ziehe mit " + name(usedPceType) + " und rufe nächste Bewertung auf:   ");
                if (CACHE_ACTIVATED) {
                    result = getEvalCacheEntry(playerCBR, oppCBR);
                    remainingPceType = getPceTypeCacheEntry(playerCBR, oppCBR);
                    if (result > Integer.MIN_VALUE+1)  // Berechnung bereits im Cache
                        countCacheHits++;
                    else
                        countCacheMisses++;
                    //  System.out.println(" -> Result from Cache: " + result + " (" + countCacheHits + " hits, " + countCacheMisses + " misses)");
                }
                if (result <= Integer.MIN_VALUE+1 ) {
                    // neue Berechnung aufrufen (recursive call)
                    int subCBR = CoverageBitMap.removePieceOfTypeFromCoverage(usedPceType, playerCBR);
                    result = -calcBiasedClashResultFromCurrentPlayerPerspective(-positiveClashPieceBaseValue(usedPceType), oppCBR, subCBR);
                    if (result == 1)
                        addResultToCache(result, playerCBR, oppCBR, usedPceType);  // Gegner hat nicht mehr gezogen, meine Figur blieb stehen.
                    else
                        addResultToCache(result, playerCBR, oppCBR, -getPceTypeCacheEntry(oppCBR, subCBR));
                }
            }
            //  System.out.print("=> erhalte Bewertung " + result + " nach " + "Schlagen von Figur " + name(-figOnFieldNr) + " und speichere den Wert im Cache an Stelle " + toFullString(playerCBR)+ toFullString(oppCBR) + ". ");
        }

        if (result - bias < 0) {    // schlagen bleibt trotzdem schlecht, ich mache es also nicht...
            //  System.out.print("=> erhalte Bewertung " + result + " nach " + "Schlagen von Figur " + name(-figOnFieldNr) + ". Das sieht nicht gut aus, da ziehe ich lieber nicht.  ");
            result = -1;  // es wird mindestens -1 genommen, da bei schlechten Zügen besser nicht gezogen wird.
        } else
            result -= bias;   // bias ist normal negativ wg. dem Wert der gegnerischen Figur, d.h. das Schlagen verbessert hier den Wert.

        if (DEBUG_VERBOSITY_CLASHCALC > 1)
              System.out.println("==> Ok, bewerte das Ergebnis als " + result);

        return result;
    }

    static int clashPieceBaseValue(int pceOnSq) {
        return pieceBaseValue( clashPcsType2CBPceType(pceOnSq) );
    }

    static int positiveClashPieceBaseValue(int pceOnSq) {
        return positivePieceBaseValue( clashPcsType2CBPceType(pceOnSq) );
    }

    static String clashPceType2String(int pceOnSq) {
        return ((pceOnSq>0) ? "weißer " : "schwarzer " ) + pieceNameForType( abs(pceOnSq));
    }

    private static int clashPcsType2CBPceType(int pceOnSq) {
        return  (pceOnSq < 0) ? -pceOnSq + BLACK_PIECE
                              : pceOnSq;
    }


//    public static void main(String[] args) {
//        boolean testsuccess = true;
//
//        addResultToCache(123, 0b0000000010000, 0b0000000010000, PAWN );
//        testsuccess &= TestClashes.myassert ( getEvalCacheEntry(0b0000000010000, 0b0000000010000), 123 );
//
//        addResultToCache(-123, 0b0000000010000, 0b0000000010000, PAWN);
//        testsuccess &= TestClashes.myassert ( getEvalCacheEntry(0b0000000010000, 0b0000000010000), -123 );
//
//        System.out.format("\n\n%s", TestClashes.outputPrefix);
//        System.out.print("Testergebnis: ");
//        TestClashes.myassert(testsuccess, true);
//        System.out.println();
//    }

}