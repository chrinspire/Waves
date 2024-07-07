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

import de.ensel.chessbasics.ChessBasics;

import java.util.Arrays;

import static de.ensel.chessbasics.ChessBasics.*;

public abstract class CoverageBitMap {

    // Idea: Do not save too much memory,
    //       but make the difference between White+Black coverage computable
    // Use "bit piles" to count number of same figures covering a field.
    // 13 bits:                                     PP_NNB_RRQQ_LTTK
    static final int NOTCOVERED = 0;
    static final int COVER_MASK          = 0b11_111_1111_1111;
    static final int PAWN_COVER_MASK     = 0b11_000_0000_0000;
    static final int PAWN_COVER_1        = 0b01_000_0000_0000;
    static final int KNIGHT_COVER_MASK   = 0b00_110_0000_0000;
    static final int KNIGHT_COVER_1      = 0b00_010_0000_0000;
    static final int BISHOP_COVER_MASK   = 0b00_001_0000_0000;
    static final int BISHOP_COVER_1      = 0b00_001_0000_0000;
    static final int BISHOP_SHIFT = 8;
    static final int ROOK_COVER_MASK     = 0b00_000_1100_0000;
    static final int ROOK_COVER_1        = 0b00_000_0100_0000;
    static final int ROOK_SHIFT = 6;
    static final int QUEEN_COVER_MASK    = 0b00_000_0011_0000;  // max 3 Queens...
    static final int QUEEN_COVER_1       = 0b00_000_0001_0000;
    static final int BISHOP_BQ_COVER_MASK= 0b00_000_0000_1000;
    static final int BISHOP_BQ_COVER_1   = 0b00_000_0000_1000;
    static final int BISHOP_BQ_SHIFT = 3;
    static final int ROOK_BQ_COVER_MASK  = 0b00_000_0000_0110;
    static final int ROOK_BQ_COVER_1     = 0b00_000_0000_0010;
    static final int ROOK_BQ_SHIFT = 1;
    static final int KING_COVER_MASK     = 0b00_000_0000_0001;
    static final int KING_COVER_1        = 0b00_000_0000_0001;
    static final int CBM_LENGTH = 13;

    static final int PT_SOMEOFMINE   = 7;
    static final int ROOK_BEHIND_QUEEN = 3;
    static final int BISHOP_BEHIND_QUEEN = 5;

    // careful, content of these two arrays relies on the values of constants (from ChessBasics) as array index:
    static final int[] pceTypeCoverMask = { NOTCOVERED, KING_COVER_MASK, ROOK_COVER_MASK, ROOK_BQ_COVER_MASK, BISHOP_COVER_MASK, BISHOP_BQ_COVER_MASK, QUEEN_COVER_MASK, -1, KNIGHT_COVER_MASK, -1, -1, -1, -1, -1, -1, -1,  PAWN_COVER_MASK, COVER_MASK };
    static final int[] pceTypeCover1 = { NOTCOVERED, KING_COVER_1,    ROOK_COVER_1,    ROOK_BQ_COVER_1,    BISHOP_COVER_1,    BISHOP_BQ_COVER_1,    QUEEN_COVER_1,    -1, KNIGHT_COVER_1,    -1, -1, -1, -1, -1, -1, -1,  PAWN_COVER_1,    0,         };


    public static int addPieceOfTypeToCoverage(int posPceType, int/*CBM*/ oldcoveredvalue) {
        if (posPceType < 0) {
            System.err.println("*** Fehler: Versuche gegnerische Figur zu CBM hinzufügen.");
            return oldcoveredvalue;
        } else if (posPceType == EMPTY ) {
            return oldcoveredvalue;
        }
        posPceType = colorlessPieceType(posPceType);
        int coverMask = pceTypeCoverMask[posPceType];
        int cover1 = pceTypeCover1[posPceType];
        if ( (oldcoveredvalue & coverMask) == coverMask ) {  // already full...
            System.err.format("*** Fehler: Versuche zu viele Figuren zu CBM hinzufügen: %d zu %s.\n", posPceType, cbm2fullString(oldcoveredvalue) );
            return oldcoveredvalue;
        }
        return oldcoveredvalue + cover1;
    }

    public static boolean containsPieceTypeInCoverage(int pceType, int/*CBM*/ cbm) {
        if (pceType < 0) {
            System.err.println("*** Fehler: Versuche gegnerische Figur in CBR zu suchen.");
            return false;
        } else if (pceType == EMPTY ) {
            return false;
        }
        int coverMask = pceTypeCoverMask[colorlessPieceType(pceType)];
        return (cbm & coverMask) != 0;
    }

    public static int removePieceOfTypeFromCoverageSaferWay(int pceType, int/*CBM*/ oldcoveredvalue) {
        // like removeFigureNrFromCoverage, but removes BbQ/RbQ if it is called with B/R but B/R is already empty.
        // this is necessary for cases, where (unlike in clashCalculation) it cannot be known if the piece is behind queen or not, like in calcMySupportFromPlayerPerspective
        if (pceType < 0) {
            System.err.println("*** Fehler: Versuche gegnerische Figur aus CBR zu entfernen.");
            return oldcoveredvalue;
        } else if (pceType == EMPTY ) {
            return oldcoveredvalue;
        }
        pceType = colorlessPieceType(pceType);
        if (pceType == BISHOP && ((oldcoveredvalue & pceTypeCoverMask[BISHOP]) == 0)
                && ((oldcoveredvalue & pceTypeCoverMask[BISHOP_BEHIND_QUEEN]) != 0)) {
            return removePieceOfTypeFromCoverage( BISHOP_BEHIND_QUEEN , oldcoveredvalue);
        } else if (pceType == ROOK && ((oldcoveredvalue & pceTypeCoverMask[ROOK]) == 0)
                && ((oldcoveredvalue & pceTypeCoverMask[ROOK_BEHIND_QUEEN]) != 0)) {
            return removePieceOfTypeFromCoverage( ROOK_BEHIND_QUEEN , oldcoveredvalue);
        }
        return removePieceOfTypeFromCoverage( pceType , oldcoveredvalue);
    }


    public static int removePieceOfTypeFromCoverage(int pceType, final int/*CBM*/ oldcoveredvalue) {
        if (pceType<0) {
            System.err.format("*** Fehler: Versuche gegnerische Figur aus CBM zu entfernen: %d aus %s.\n", pceType, cbm2fullString(oldcoveredvalue) );
            return oldcoveredvalue;
        } else if (pceType==EMPTY ) {
            return oldcoveredvalue;
        }
        pceType = colorlessPieceType(pceType);
        int coverMask = pceTypeCoverMask[pceType];
        int cover1 = pceTypeCover1[pceType];
        if ( (oldcoveredvalue & coverMask) == 0 ) {  // already empty ...
            // TODO!!!: dies kommt oft vor für:  *** Fehler: Versuche zu viele Figuren zu CBM hinzufügen: 4 zu [0000100000000]
            System.err.format("*** Fehler: Versuche zu viele Figuren aus CBM zu entfernen: %d von %s.\n", pceType, cbm2fullString(oldcoveredvalue) );
            return oldcoveredvalue;
        }
        int resCBM = oldcoveredvalue - cover1;   // this is the whole magic of removing a figure from the bit mask :-)
        if (pceType == QUEEN ) {
            // a Queen was removed, check if bishop-behind queen or rock-behind-queen needs to be counted as a normal bishop/rook now.
            //System.out.format("*** Entferne: %s aus %s ergibt %s. ", giveFigureNameByNr(pceType), toFullString(oldcoveredvalue), toFullString(resCBM) );
            int hiddenPieceCoverage =  ( resCBM & (pceTypeCoverMask[BISHOP_BEHIND_QUEEN]) ) >> BISHOP_BQ_SHIFT;
            //System.out.format(" hiddenPieceCoverage= %d from %s.  ", hiddenPieceCoverage, toFullString(resCBM) );
            if (hiddenPieceCoverage>0 && ( (resCBM & pceTypeCoverMask[BISHOP]) == 0 ) )  // es gibt mind einen BbQ, zähle das bei den Bishops dazu... daher prüfe vorher ob der 0 ist, denn es kann nicht 2 Bs eines Spielers mit der selben Feldfarbe geben
                resCBM = (resCBM + (hiddenPieceCoverage<<BISHOP_SHIFT) ) & (~(pceTypeCoverMask[BISHOP_BEHIND_QUEEN])) ;
            else {
                // es gab keine Läufer, versuche es nochmal mit Türmen.  (Wenn es Läufer gab, dann lass die Türme hinten bis zum Entfernen der nächsten Dame. Wg. den verschiedenen Richtungen kann die Dame eh nicht beide Fälle auf einmal freigeben)
                hiddenPieceCoverage = ( resCBM & (pceTypeCoverMask[ROOK_BEHIND_QUEEN]) ) >> ROOK_BQ_SHIFT;
                //System.out.format(" hiddenPieceCoverage= %d from %s. ", hiddenPieceCoverage, toFullString(resCBM) );
                if (hiddenPieceCoverage>0)
                    resCBM = (resCBM + (hiddenPieceCoverage<<ROOK_SHIFT) ) & (~(pceTypeCoverMask[ROOK_BEHIND_QUEEN]));
            }
            //System.out.format(" resCBM= %s.  ", toFullString(resCBM) );
        }
        return resCBM;
    }

    public static int getSmallestPieceTypeFromCoverage(int/*CBM*/ cbm) {
        if ( (cbm & pceTypeCoverMask[PAWN]) > 0 )
            return PAWN;
        if ( (cbm & pceTypeCoverMask[KNIGHT]) > 0 )
            return KNIGHT;
        if ( (cbm & pceTypeCoverMask[BISHOP]) > 0 )
            return BISHOP;
        if ( (cbm & pceTypeCoverMask[ROOK]) > 0 )
            return ROOK;
        if ( (cbm & pceTypeCoverMask[QUEEN]) > 0 )
            return QUEEN;
        if ( (cbm & pceTypeCoverMask[KING]) > 0 )
            return KING;
        if ( cbm > 0)
            System.out.println("*** Fehler: keine Figur in CBM gefunden, obwohl diese nicht leer scheint.");
        return 0;  // nothing found, CBR must be 0
//        if ( (cbm & pceTypeCoverMask[BISHOP_BEHIND_QUEEN]) > 0 )
//            return 0; // should not happen. It is removed after Queen-move. BISHOP_BEHIND_QUEEN;
//        if ( (cbm & pceTypeCoverMask[ROOK_BEHIND_QUEEN]) > 0 )
//            return 0; // should not happen. It is removed after Queen-move. ROOK_BEHIND_QUEEN;
    }



    public static String cbm2pureString(int cbm1) {
        //String.format("%32s", Integer.toBinaryString(cmbl)).replace(' ', '0')
        //return Integer.toBinaryString(cbm1);
        return String.format("%13s", Integer.toBinaryString(cbm1)).replace(' ', '0');
    }

    public static String cbm2fullString(int cbm1) {
        return "["+ cbm2pureString(cbm1)+"]";
    }

    public static String cbm2fullString(int cbm1, boolean col) {
        return "["+ (col==WHITE ? "w" : "b") +  cbm2pureString(cbm1) + "]";
    }

    public static String cbm2String(int cbm1, boolean col) {
        //todo: use Letters fro ChessBasics resources
        if ( cbm1 == NOTCOVERED ) {
            return "_";
        }
        if ( col ) {
            switch (cbm1) {
            case PAWN_COVER_1:   return "P";
            case KNIGHT_COVER_1: return "N";
            case BISHOP_COVER_1: return "B";
            case ROOK_COVER_1:   return "R";
            case QUEEN_COVER_1:  return "Q";
            case KING_COVER_1:   return "K";
            default: return "X"; 
            }
        } else {
            switch (cbm1) {
            case PAWN_COVER_1:   return "p";
            case KNIGHT_COVER_1: return "n";
            case BISHOP_COVER_1: return "b";
            case ROOK_COVER_1:   return "r";
            case QUEEN_COVER_1:  return "q";
            case KING_COVER_1:   return "k";
            default: return "x"; 
            }
          
        }
    }

    public static String cbmArray2String(int[] cbms) {
        return Arrays.toString( Arrays.stream(cbms).mapToObj(cbm -> CoverageBitMap.cbm2String(cbm, ChessBasics.WHITE)).toArray());
    }
}
