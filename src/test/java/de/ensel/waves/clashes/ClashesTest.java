package de.ensel.waves.clashes;

import org.junit.jupiter.api.Test;

import static de.ensel.chessbasics.ChessBasics.*;
import static java.lang.Math.min;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClashesTest {
    static boolean beVerbose = true;
    static final String outputPrefix = "[TEST] ";

    @Test
    void calcBiasedClashResultForClashPceTypeFromBoardPerspective_allEmpty_Test() {
        //                         -  K  Q  R  B  K  P  M  T  L
        assertTrue( doAllTestsWithCertainCBMs(0b00_000_0000_0000, 0b00_000_0000_0000) );
    }

    @Test
    void calcBiasedClashResultForClashPceTypeFromBoardPerspective_Kings_Test() {
        // mit Königen geht der Test auch :-)
        assertTrue( doAllTestsWithCertainCBMs(0b00_000_0000_0001, 0b00_000_0000_0001) );
    }

    @Test
    void calcBiasedClashResultForClashPceTypeFromBoardPerspective_Queens_Test() {
        // mit Damen geht der Test auch :-)
        assertTrue( doAllTestsWithCertainCBMs(0b00_000_0001_0000, 0b00_000_0001_0000) );
    }

    private static boolean doAllTestsWithCertainCBMs(int playerCBM, int oppCBM) {
        boolean testsuccess;
        long startTime = System.nanoTime();
        testsuccess = doAllTestsWithAllPceTypesOnSquare( playerCBM,  oppCBM );
        System.out.format("\n%s", outputPrefix);
        System.out.print("Testergebnis 1. Lauf nach "+(System.nanoTime() - startTime)/1000+" µs.");
        assertTrue(testsuccess);
        System.out.println();
        // repeat to ensure Cache functionality
        startTime = System.nanoTime();
        testsuccess = doAllTestsWithAllPceTypesOnSquare( playerCBM,  oppCBM );
        System.out.format("\n%s", outputPrefix);
        System.out.print("Testergebnis 2. Lauf nach "+(System.nanoTime() - startTime)/1000+" µs: ");
        assertTrue(testsuccess);
        System.out.println();
        return testsuccess;
    }

    private static boolean doAllTestsWithAllPceTypesOnSquare(int playerCBM, int oppCBM ) {
        boolean testsuccess = true;
        for (int pceOnSq = EMPTY; pceOnSq <= PAWN; pceOnSq++ ) {
            pceOnSq = skipEmptyPceTypes(pceOnSq);
            testsuccess &= doEightTestsWithAllPieceTypes(pceOnSq, playerCBM, oppCBM);      //Achtung, der Test kann derzeit nur für identische CMBs die Ergebnisse der Testfälle richtig berechnen!
        }
        return testsuccess;
    }

    private static int skipEmptyPceTypes(int pceOnSq) {
        if (pceOnSq == KING || pceOnSq == QUEEN+1 || pceOnSq == ROOK+1 || pceOnSq == BISHOP+1 )
            pceOnSq++;
        else if (pceOnSq == KNIGHT+1)
            pceOnSq = PAWN;
        return pceOnSq;
    }


    private static boolean doEightTestsWithAllPieceTypes(int pceOnSq, int playerCBM, int oppCBM ) {
        //BLI: Achtung, der Test kann derzeit nur für identische 0er CBRs die Ergebnisse der Testfälle richtig berechnen!
        int addedPceType;
        boolean testsuccess = true;
        for (addedPceType = ROOK; addedPceType <= PAWN; addedPceType++ ) {
            addedPceType = skipEmptyPceTypes(addedPceType);
            if ( !(CoverageBitMap.getSmallestPieceTypeFromCoverage(playerCBM) == BISHOP && addedPceType == BISHOP)   // keinen 2. Läufer hinzufügen, das kann nicht sein, ok kann schon, ist aber wg. der Seltenheit nicht im Dartenmodel vorgesehen...
                    && addedPceType > pceOnSq)
                testsuccess &= doFourAddAndClashTests( pceOnSq, playerCBM, addedPceType, oppCBM, addedPceType,
                        pceOnSq > 0 ? 1 : 0,
                        pceOnSq != EMPTY ? 1 : 0, //was (for old, fictive "a bishop gets beaten" result: :((positiveClashPieceBaseValue(KING) + positiveClashPieceBaseValue(BISHOP) - positiveClashPieceBaseValue(CoverageBitMap.getSmallestPieceTypeFromCoverage(CoverageBitMap.addPieceOfTypeToCoverage(addedPceType, playerCBM )))) >> 4)),
                        pceOnSq > 0 ? min(1, -Clashes.positiveClashPieceBaseValue(pceOnSq) + Clashes.positiveClashPieceBaseValue(addedPceType) + 1) : 0,
                        -Clashes.positiveClashPieceBaseValue(pceOnSq) - ((pceOnSq != EMPTY) ? 1 : 0)); // was: ((positiveClashPieceBaseValue(KING) + 30 - positiveClashPieceBaseValue(CoverageBitMap.getSmallestPieceTypeFromCoverage(CoverageBitMap.addPieceOfTypeToCoverage(addedPceType, oppCBM )))) >> 4)));
        }
        for (addedPceType = ROOK; addedPceType <= PAWN; addedPceType++ ) {
            addedPceType = skipEmptyPceTypes(addedPceType);
            if ( !(CoverageBitMap.getSmallestPieceTypeFromCoverage(playerCBM) == BISHOP && addedPceType == BISHOP)   // keinen 2. Läufer hinzufügen, das kann nicht sein
                    && addedPceType > pceOnSq)
                testsuccess &= doFourAddAndClashTests( -pceOnSq, playerCBM, addedPceType, oppCBM, addedPceType,
                        ( pceOnSq>0 ? -1 : 0),
                        Clashes.positiveClashPieceBaseValue(pceOnSq) + ((pceOnSq != EMPTY) ? 1 : 0), // ((positiveClashPieceBaseValue(KING) + 30 - positiveClashPieceBaseValue(CoverageBitMap.getSmallestPieceTypeFromCoverage(CoverageBitMap.addPieceOfTypeToCoverage(addedPceType, oppCBM )))) >> 4)),
                        ( pceOnSq>0 ? -min(1, -Clashes.positiveClashPieceBaseValue(pceOnSq) + Clashes.positiveClashPieceBaseValue(addedPceType) + 1) : 0) ,
                        ((pceOnSq!=EMPTY)?-1: 0)  // -((positiveClashPieceBaseValue(KING) + 30 - positiveClashPieceBaseValue(CoverageBitMap.getSmallestPieceTypeFromCoverage(CoverageBitMap.addPieceOfTypeToCoverage(addedPceType, playerCBM )))) >> 4))
                );
        }
        return testsuccess;
    }


    private static boolean doFourAddAndClashTests( int pceOnSq, int startPlayerCBM, int addPlayerPce, int startOppCBM, int addOppPce, int expectedResult, int expectedResultWithPlayer, int expectedResultWithBoth, int expectedResultWithOpp ) {
        int oppCBM = startOppCBM;
        int res;
        System.out.format("\n%s", outputPrefix);
        System.out.print("Teste vier Fälle mit " + CoverageBitMap.cbm2fullString(startPlayerCBM) + " und " + CoverageBitMap.cbm2fullString(startOppCBM) + " mit " + Clashes.clashPceType2String(pceOnSq) + " in der Mitte: ");
        int playerCBM = startPlayerCBM;

        if (beVerbose)
            System.out.print("\n" + outputPrefix +" 1. Teste Clash as is: " + CoverageBitMap.cbm2fullString(playerCBM) + " gegen " + CoverageBitMap.cbm2fullString(oppCBM) + " um " + Clashes.clashPceType2String(pceOnSq) + " " );
        res = Clashes.calcBiasedClashResultForClashPceTypeFromBoardPerspective( Clashes.clashPieceBaseValue(pceOnSq), pceOnSq, playerCBM, oppCBM );
        assertEquals(expectedResult, res);

        playerCBM = CoverageBitMap.addPieceOfTypeToCoverage( addPlayerPce, startPlayerCBM );
        if (beVerbose)
            System.out.print("\n" + outputPrefix +" 2. Teste Clash P+: " + pieceNameForType(addPlayerPce) + " in " + CoverageBitMap.cbm2fullString(playerCBM) + " gegen " + CoverageBitMap.cbm2fullString(oppCBM) + " um " + Clashes.clashPceType2String(pceOnSq) );
        res = Clashes.calcBiasedClashResultForClashPceTypeFromBoardPerspective( Clashes.clashPieceBaseValue(pceOnSq), pceOnSq, playerCBM, oppCBM );
        assertEquals(expectedResultWithPlayer, res);

        oppCBM = CoverageBitMap.addPieceOfTypeToCoverage( addOppPce, oppCBM );
        if (beVerbose)
            System.out.print("\n" + outputPrefix +" 3. Teste Clash P+O+: "
                    + pieceNameForType(addPlayerPce) + " in " + CoverageBitMap.cbm2fullString(playerCBM)
                    + " gegen " + Clashes.clashPceType2String(addOppPce) + " in " + CoverageBitMap.cbm2fullString(oppCBM)
                    + " um " + pieceNameForType(pceOnSq) );
        res = Clashes.calcBiasedClashResultForClashPceTypeFromBoardPerspective( Clashes.clashPieceBaseValue(pceOnSq), pceOnSq, playerCBM, oppCBM );
        assertEquals(expectedResultWithBoth, res);

        playerCBM = CoverageBitMap.removePieceOfTypeFromCoverage( addPlayerPce, playerCBM );
        if (beVerbose)
            System.out.print("\n" + outputPrefix +" 4. Teste Clash O+: " + CoverageBitMap.cbm2fullString(playerCBM) + " gegen " + Clashes.clashPceType2String(addOppPce) + " in " + CoverageBitMap.cbm2fullString(oppCBM) + " um " + Clashes.clashPceType2String(pceOnSq) );
        res = Clashes.calcBiasedClashResultForClashPceTypeFromBoardPerspective( Clashes.clashPieceBaseValue(pceOnSq), pceOnSq, playerCBM, oppCBM );
        assertEquals(expectedResultWithOpp, res);

        oppCBM = CoverageBitMap.removePieceOfTypeFromCoverage( addOppPce, oppCBM );
        assertEquals(startPlayerCBM, playerCBM);
        assertEquals(startOppCBM, oppCBM);
        return true;
    }


//    public static boolean myassert(int tobe, int testresult) {
//        System.out.format("%s ==> ", outputPrefix);
//        if (beVerbose || testresult != tobe)
//            System.out.print(" " + testresult + "=" + tobe + " ");
//        if ( testresult != tobe )
//            System.out.print(" [!!! Fehler !!!] ");
//        else if (beVerbose)
//            System.out.print(" [OK] ");
//        return testresult == tobe;
//    }
//
//    public static boolean myassert(boolean test, boolean tobe) {
//        return myassert(tobe?1:0, test?1:0);
//    }

}