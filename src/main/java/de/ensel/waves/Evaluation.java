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
 *     This file is based on class Evaluation of TideEval v0.48.
 */

package de.ensel.waves;

import java.util.Arrays;

import static de.ensel.chessbasics.ChessBasics.*;
import static de.ensel.waves.ChessBoard.*;
import static java.lang.Math.abs;

/** provides an Evaluation for a Piece going towards a target.
 * The evaluation representation is an array of ints (typically used as centipawns) in board
 * perspective for [now, in one move, in 2 moves,...] called future levels.
 */
public class Evaluation {
    public static final int MAX_EVALDEPTH = 5;
    private int[] rawEval;
    private String reason = null;

    //// Constructors
    public Evaluation() {
        rawEval = new int[MAX_EVALDEPTH];
    }

    public Evaluation(Evaluation oeval) {
        copy(oeval);
    }

    public Evaluation(int eval, int futureLevel) {
        this.rawEval = new int[MAX_EVALDEPTH];
        setEval(eval, futureLevel);
    }

    ////
    boolean isGoodForColor(int color) {
        for (int i = 0; i < MAX_EVALDEPTH; i++) {
            if (isWhite(color) ? rawEval[i] < 0 : rawEval[i] > 0)
                return false;
        }
        return true;
    }

    boolean isAboutZero() {
        return abs(rawEval[0])<=1 && abs(rawEval[1])<=2 && abs(rawEval[2])<=3;
    }

    boolean isBetterForColorThan(int color, Evaluation oEval) {
        int delta = 0;
        int insignificance = 1;
        for (int i = 0; i < MAX_EVALDEPTH; i++) {
            int iDelta = (rawEval[i] - oEval.rawEval[i]) / insignificance;
            insignificance += 1+i + abs(delta/EVAL_HALFAPAWN);
            delta += iDelta;
            if (abs(delta) >= EVAL_HALFAPAWN + (EVAL_HALFAPAWN>>1))
                break;
        }
        return evalIsOkForColByMin(delta, color, 0);
    }

    @Deprecated
    boolean old_isBetterForColorThan(int color, Evaluation oEval) {
        if (oEval == null)
            return true;  // anything is better than no move :-)
        int i = 0;
        //if (DEBUGMSG_MOVESELECTION)
        //    debugPrint(DEBUGMSG_MOVESELECTION, "  comparing move eval " + this + " at "+i + " with " + oEval +": ");
        int comparethreshold = 36; // 23 -> 34 -> 51
        int bias = isWhite(color) ? -4 : +4;
        boolean probablyBetter = false;
        boolean probablyALittleBetter = true;
        while (i < MAX_EVALDEPTH) {
            if (i==1)
                comparethreshold += 12;
            else if (i==2)
                comparethreshold += 8;
            else if (i==3)
                comparethreshold += 9;
            if (isWhite(color) ? rawEval[i] + bias - oEval.rawEval[i] > comparethreshold
                    : rawEval[i] + bias - oEval.rawEval[i] < -comparethreshold) {
                probablyBetter = true;
                break;
            }
            else if (isWhite(color) ? rawEval[i] + bias - oEval.rawEval[i] < -(comparethreshold>>1) // - lowthreshold
                    : rawEval[i] + bias - oEval.rawEval[i] > (comparethreshold>>1) ) {
                probablyBetter = false;
                probablyALittleBetter = false;
                break;
            }
            else if (isWhite(color) ? rawEval[i] + bias - oEval.rawEval[i] > (comparethreshold >> 1)
                    : rawEval[i] + bias - oEval.rawEval[i] < -(comparethreshold >> 1)) {
                probablyBetter = true;
                // tighten comparethreshold more if it was almost a full hit and leave it almost the same if it was close to similar
                // u76-u115: comparethreshold -= (comparethreshold>>2);
                comparethreshold -= ( abs(rawEval[i]- oEval.rawEval[i]) - (comparethreshold>>1) );
            }
            else if ( probablyALittleBetter
                    && (isWhite(color) ? rawEval[i] + bias - oEval.rawEval[i] < 0
                    : rawEval[i] + bias - oEval.rawEval[i] > 0) ) {
                probablyALittleBetter = false;
            }
            bias += (bias>>3) + rawEval[i]- oEval.rawEval[i];
            i++;  // almost same evals on the future levels so far, so continue comparing
        }
        if ( i >= MAX_EVALDEPTH && probablyALittleBetter ) {
            probablyBetter = true;
        }
        return probablyBetter;
    }

    private void copy(Evaluation oeval) {
        this.rawEval = Arrays.copyOf(oeval.rawEval, MAX_EVALDEPTH);
    }

    //// getter
    public int getEvalAt(int futureLevel) {
        return rawEval[futureLevel];
    }

    //// setter + advanced setter
    public void initEval(int initEval) {
        Arrays.fill(rawEval, initEval);
    }

    /**
     * sets an eval on a certain future level
     * beware: range is unchecked
     * @param evalValue
     * @param futureLevel the future level from 0..max
     * @return itself (but changed)
     */
    public Evaluation setEval(int evalValue, int futureLevel) {
        rawEval[futureLevel] = evalValue;
        return this;
    }

    /**
     * adds or substracts to/from an eval on a certain future level
     * beware: is unchecked
     * @param evalValue
     * @param futureLevel the future level from 0..max
     */
    public Evaluation addEval(int evalValue, int futureLevel) {
        rawEval[futureLevel] += evalValue;
        return this;
    }

    public Evaluation addEval(Evaluation addEval) {
        if (addEval != null) {
            for (int i = 0; i < MAX_EVALDEPTH; i++)
                this.rawEval[i] += addEval.rawEval[i];
        }
        return this;
    }

    public Evaluation subtractEval(Evaluation addEval) {
        if (addEval != null) {
            for (int i = 0; i < MAX_EVALDEPTH; i++)
                this.rawEval[i] -= addEval.rawEval[i];
        }
        return this;
    }

    public Evaluation invert() {
        for (int i = 0; i < MAX_EVALDEPTH; i++)
            rawEval[i] = -rawEval[i];
        return this;
    }

    public Evaluation maxEvalFor(Evaluation meval, int color) {
        if (meval.isBetterForColorThan(color,this)) {
            copy(meval);
        }
        return this;
    }

    public Evaluation maxEvalPerFutureLevelFor(Evaluation meval, boolean color) {
        if (meval != null) {
            for (int i = 0; i < MAX_EVALDEPTH; i++) {
                this.rawEval[i] = maxFor(meval.rawEval[i], rawEval[i], color);
            }
        }
        return this;
    }

    /**
     * like incEvaltoMax, but always "add" negative (=warning/fee) evaluations
     * @param meval
     * @param color
     * @return
     */
    public Evaluation incEvaltoMaxOrDecreaseFor(Evaluation meval, boolean color) {
        if (meval != null) {
            boolean isNegative = false;
            for (int i = 0; i < MAX_EVALDEPTH; i++) {
                if (!evalIsOkForColByMin(meval.rawEval[i], color, 0)) {
                    isNegative = true;
                    break;
                }
            }
            if (isNegative)
                return addEval(meval);
            for (int i = 0; i < MAX_EVALDEPTH; i++) {
                this.rawEval[i] = maxFor(meval.rawEval[i], rawEval[i], color);
            }
        }
        return this;
    }

    public Evaluation decEvaltoMinFor(Evaluation meval, boolean color) {
        if (meval != null) {
            for (int i = 0; i < MAX_EVALDEPTH; i++) {
                this.rawEval[i] = minFor(meval.rawEval[i], rawEval[i], color);
            }
        }
        return this;
    }

    public Evaluation onlyBeneficialFor(boolean color) {
        for (int i = 0; i < MAX_EVALDEPTH; i++) {
            if ( !evalIsOkForColByMin(rawEval[i], color, 0) )
                rawEval[i] = 0;
        }
        return this;
    }

    public Evaluation changeEvalHalfWayTowards(int towardsValue) {
        for (int i = 0; i < MAX_EVALDEPTH; i++)
            rawEval[i] = (towardsValue + rawEval[i]) >> 1;
        return this;
    }

    /** shifts evaluation timewise - note: the values at the arrays borders can fall off the edge of their world.
     * Shift left followed by shift right does not result in the original evaluation, but clears the value at futurelevel 0.
     * @param futureLevelDelta negative value = shift left, e.g. -1: every eval comes one move closer.
     *                        positive value = shift right, e.g. +1: every eval is postponed one move into the future.
     * @return this (but changed)
     */
    public Evaluation timeWarp(int futureLevelDelta) {
        if (futureLevelDelta<0) {
            if (futureLevelDelta<-MAX_EVALDEPTH)
                futureLevelDelta = -MAX_EVALDEPTH;
            for (int i = 0; i < MAX_EVALDEPTH; i++) {
                rawEval[i] = i >= MAX_EVALDEPTH+futureLevelDelta ? 0 : rawEval[i-futureLevelDelta];
            }
        }
        else if (futureLevelDelta>0) {
            if (futureLevelDelta>MAX_EVALDEPTH)
                futureLevelDelta = MAX_EVALDEPTH;
            for (int i = MAX_EVALDEPTH-1; i>=0; i--) {
                rawEval[i] = i < futureLevelDelta ? 0 : rawEval[i-futureLevelDelta];
            }
        }
        return this;
    }

    /** devide all evals on all future levels by div
     * @param div divisor
     * @return this (but changed)
     */
    public Evaluation devideBy(int div) {
        int shiftBy = switch (div) {   // hoping this is faster than calculating
            case 2 -> 1;
            case 4 -> 2;
            case 8 -> 3;
            case 16 -> 4;
            default -> 0;
        };
        for (int i = 0; i < MAX_EVALDEPTH; i++) {
            rawEval[i] = (shiftBy>0) ? (rawEval[i]>>shiftBy)    // has anyone tried if this is really faster :-) or are these just my old 8088-without-FPU instincts?
                                     : (rawEval[i] / div);
        }
        return this;
    }

    ////
    @Override
    public String toString() {
        int firstEntry = MAX_EVALDEPTH;
        for (int i = 0; i < MAX_EVALDEPTH; i++) {
            if (rawEval[i] != 0) {
                if (firstEntry == MAX_EVALDEPTH)
                    firstEntry = i;
                else {
                    firstEntry = -1;  // meaning multiple entries found
                    break;
                }
            }
        }
        StringBuilder result = new StringBuilder();
        if (firstEntry == -1)
            result.append(Arrays.toString(rawEval));
        else if (firstEntry == MAX_EVALDEPTH) {
            if (reason != null)
                result.append("[]");
        }
        else {
            result.append("[");
            result.append(rawEval[firstEntry]);
            result.append("@");
            result.append(firstEntry);
            result.append("]");
        }
        /*if (reason != null) {
            result.append("!");
            result.append(reason);
        }*/
        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Evaluation)) return false;
        Evaluation that = (Evaluation) o;
        return Arrays.equals(rawEval, that.rawEval);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(rawEval);
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void addReason(String reason) {
        if (this.reason == null)
            this.reason = reason;
        else
            this.reason += "; " + reason;
    }

    public String getReason() {
        if (reason == null)
            return "";
        return reason;
    }

}