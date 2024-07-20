package de.ensel.waves;

import org.junit.jupiter.api.Test;

import static de.ensel.chessbasics.ChessBasics.*;
import static org.junit.jupiter.api.Assertions.*;

class EvaluationTest {

    @Test
    void isGoodForColor_Test() {
    }

    @Test
    void isAboutZero_Test() {
    }

    @Test
    void isBetterForColorThan_Test() {
        Evaluation[] evals = new Evaluation[]{
                new Evaluation(-EVAL_HALFAPAWN, 0).addEval(-EVAL_TENTH, 1),
                new Evaluation(-EVAL_HALFAPAWN, 0),
                new Evaluation(-EVAL_HALFAPAWN, 0).addEval(EVAL_TENTH, 1),
                new Evaluation(-EVAL_TENTH, 0).addEval(-EVAL_TENTH, 1),
                new Evaluation(-EVAL_HALFAPAWN, 2),
                new Evaluation(-EVAL_TENTH, 0),
                new Evaluation(-EVAL_TENTH, 2),
                new Evaluation(),
                new Evaluation(EVAL_TENTH, 3),
                new Evaluation(EVAL_TENTH, 2),
                new Evaluation(EVAL_TENTH, 1).addEval(-EVAL_TENTH, 3),
                new Evaluation(EVAL_TENTH, 1),
                new Evaluation(EVAL_TENTH, 1).addEval(EVAL_TENTH, 3),
                new Evaluation(EVAL_TENTH, 0),
                new Evaluation(EVAL_TENTH<<1, 0),
                new Evaluation(EVAL_TENTH<<1, 0).addEval(EVAL_TENTH,1),
                new Evaluation(EVAL_HALFAPAWN, 0).addEval(-EVAL_TENTH, 1),
                new Evaluation(EVAL_HALFAPAWN, 0).addEval(-EVAL_TENTH, 2),
                new Evaluation(EVAL_HALFAPAWN, 0),
                new Evaluation(EVAL_HALFAPAWN, 0).addEval(EVAL_TENTH, 2),
                new Evaluation(EVAL_HALFAPAWN, 0).addEval(EVAL_TENTH, 1),
        };
        for (int i = 0; i < evals.length; i++) {
            for (int j = 0; j < evals.length; j++) {
                if (i == j)
                    continue;
                System.out.println("Checking " + i + ":"+ evals[i] + " and " + j + ":"+evals[j]);
                if (i < j) {
                    assertTrue(evals[j].isBetterForColorThan(CIWHITE, evals[i]));
                    assertFalse(evals[j].isBetterForColorThan(CIBLACK, evals[i]));
                }
                else {
                    assertTrue(evals[j].isBetterForColorThan(CIBLACK, evals[i]));
                    assertFalse(evals[j].isBetterForColorThan(CIWHITE, evals[i]));
                }
            }
        }
    }
}