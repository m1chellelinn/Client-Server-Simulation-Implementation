//We ran out of time :(
//RIP we're not getting an A





























/*package cpen221.mp3.server;

import java.util.*;

public class Predictor {

    public static List<Double> predict(List<Double> sequence, int steps) {
        double lastNum = 0;
        boolean ifIncreasing = true;
        boolean ifDecreasing = true;

        for (int i = 1; i < sequence.size(); i++) {
            if (sequence.get(i) < sequence.get(i - 1)) {
                ifIncreasing = false;
                break;
            }
            if (sequence.get(i) > sequence.get(i - 1)) {
                ifDecreasing = false;
            }
        }

        if ( !ifIncreasing && !ifDecreasing ) {
            return predictBiggestPattern(sequence, steps);
        }
        else {
            return predictExponentialMovingAverage(sequence, steps);
        }
    }

    private static List<Double> predictBiggestPattern(List<Double> sequence, int steps) {
        int maxOccurrence = 0;
        int maxLength = 0;
        List<Double> maxPattern = new ArrayList<>();

        for (int patternLength = 1; patternLength <= sequence.size(); patternLength++) {
            int currentOccurrence = 0;

            List<Double> currentPattern = new ArrayList<>();
            for (int i = sequence.size() - patternLength; i < sequence.size(); i++) {
                currentPattern.add(sequence.get(i));
            }

            for (int i = sequence.size(); i - patternLength >= 0; i -= patternLength) {
                for(int j = 0; j < patternLength; j++) {
                    if (sequence.get(i+j) != currentPattern.get(j)) {
                        break;
                    }
                    else currentOccurrence++;
                }
            }
            if (currentOccurrence > maxOccurrence) {
                maxOccurrence = currentOccurrence;
                maxLength = patternLength;
                maxPattern = currentPattern;
            }
            else if (currentOccurrence == maxOccurrence) {
                if (patternLength > maxLength) {
                    maxLength = patternLength;
                    maxPattern = currentPattern;
                }
            }
        }

        List<Double> newSequence = new ArrayList<>();
        while (steps > 0) {
            for (int i = 0; i < maxPattern.size(); i++) {
                newSequence.add(maxPattern.get(i));
                steps--;
                if (i == maxPattern.size() - 1) {
                    i = -1;
                }
            }
        }
        return null;
    }

    private static List<Double> predictExponentialMovingAverage(List<Double> sequence, int steps) {

        double lastNum = sequence.get(sequence.size() - 2);
        double currentNum = sequence.get(sequence.size() - 1);
        double nextNum;

        List<Double> newSequence = new ArrayList<>();
        for (int n = 0; n < steps; n++) {
            nextNum = lastNum + (currentNum - lastNum) * (currentNum - lastNum);
            newSequence.add(nextNum);
            lastNum = currentNum;
            currentNum = nextNum;
        }
        return newSequence;
    }
}*/
