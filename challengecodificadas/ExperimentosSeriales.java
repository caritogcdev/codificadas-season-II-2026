package challengecodificadas;

import java.io.*;
import java.util.*;

public class ExperimentosSeriales {

    static int n;
    static int[] fixed;
    static int[] bestArray;
    static long bestScore = -1;
    static int missingTotal = 0;

    static long deadline;

    public static void main(String[] args) throws Exception {
        FastScanner fs = new FastScanner(System.in);

        n = fs.nextInt();
        fixed = new int[n];

        for (int i = 0; i < n; i++) {
            fixed[i] = fs.nextInt();
            if (fixed[i] == -1) {
                missingTotal++;
            }
        }

        if (missingTotal == 0) {
            printArray(fixed);
            return;
        }

        deadline = System.nanoTime() + 1_850_000_000L;

        consider(fillConstant(0));

        ArrayList<Integer> periods = buildPeriods();

        for (int p : periods) {
            if (timeIsOver()) break;

            ArrayList<Integer> phases = buildPhases(p);

            for (int phase : phases) {
                if (timeIsOver()) break;

                consider(periodicPattern(p, phase, false, false));
                consider(periodicPattern(p, phase, true, false));

                consider(periodicPattern(p, phase, false, true));
                consider(periodicPattern(p, phase, true, true));
            }
        }

        consider(rulerPattern(false));
        consider(rulerPattern(true));

        consider(runPattern(0.25, false));
        consider(runPattern(0.25, true));
        consider(runPattern(0.376, false));
        consider(runPattern(0.376, true));
        consider(runPattern(0.50, false));
        consider(runPattern(0.50, true));

        int greedyLimit = Math.min(n, 70);
        consider(greedy(false, greedyLimit));
        consider(greedy(true, greedyLimit));

        localImproveForSmallCases();

        printArray(bestArray);
    }

    static ArrayList<Integer> buildPeriods() {
        LinkedHashSet<Integer> set = new LinkedHashSet<>();

        addPeriod(set, Math.round(0.376f * n));
        addPeriod(set, Math.round(0.38f * n));
        addPeriod(set, Math.round(0.36f * n));
        addPeriod(set, Math.round(0.40f * n));

        addPeriod(set, Math.round(0.376f * missingTotal));
        addPeriod(set, Math.round(0.38f * missingTotal));
        addPeriod(set, Math.round(0.36f * missingTotal));
        addPeriod(set, Math.round(0.40f * missingTotal));

        double[] ratios = {
                0.05, 0.08, 0.10, 0.125, 0.15,
                0.20, 0.25, 0.30, 1.0 / 3.0,
                0.45, 0.50, 0.60, 0.75, 1.0
        };

        for (double r : ratios) {
            addPeriod(set, (int) Math.round(r * n));
            addPeriod(set, (int) Math.round(r * missingTotal));
        }

        int sq = (int) Math.sqrt(n);
        addPeriod(set, sq);
        addPeriod(set, 2 * sq);
        addPeriod(set, 3 * sq);

        for (int p = 1; p <= Math.min(n, 15); p++) {
            addPeriod(set, p);
        }

        return new ArrayList<>(set);
    }

    static void addPeriod(LinkedHashSet<Integer> set, int p) {
        if (p < 1) p = 1;
        if (p > n) p = n;

        set.add(p);

        if (p - 2 >= 1) set.add(p - 2);
        if (p - 1 >= 1) set.add(p - 1);
        if (p + 1 <= n) set.add(p + 1);
        if (p + 2 <= n) set.add(p + 2);
    }

    static ArrayList<Integer> buildPhases(int p) {
        LinkedHashSet<Integer> phases = new LinkedHashSet<>();

        addPhase(phases, 0, p);
        addPhase(phases, p / 4, p);
        addPhase(phases, p / 2, p);
        addPhase(phases, (3 * p) / 4, p);

        int added = 0;

        for (int i = 0; i < n && added < 4; i++) {
            if (fixed[i] >= 0 && fixed[i] < p) {
                addPhase(phases, fixed[i] - i, p);
                addPhase(phases, fixed[i] - (n - 1 - i), p);
                added++;
            }
        }

        return new ArrayList<>(phases);
    }

    static void addPhase(LinkedHashSet<Integer> phases, int phase, int p) {
        int value = phase % p;
        if (value < 0) value += p;
        phases.add(value);
    }

    static int[] fillConstant(int value) {
        int[] ans = new int[n];

        for (int i = 0; i < n; i++) {
            if (fixed[i] == -1) {
                ans[i] = value;
            } else {
                ans[i] = fixed[i];
            }
        }

        return ans;
    }

    static int[] periodicPattern(int period, int phase, boolean reverse, boolean byMissingIndex) {
        int[] ans = new int[n];
        int missingIndex = 0;

        for (int i = 0; i < n; i++) {
            if (fixed[i] != -1) {
                ans[i] = fixed[i];
            } else {
                int position;

                if (byMissingIndex) {
                    if (reverse) {
                        position = missingTotal - 1 - missingIndex;
                    } else {
                        position = missingIndex;
                    }

                    missingIndex++;
                } else {
                    if (reverse) {
                        position = n - 1 - i;
                    } else {
                        position = i;
                    }
                }

                ans[i] = mod(position + phase, period);
            }
        }

        return ans;
    }

    static int[] runPattern(double ratio, boolean reverseInsideRun) {
        int[] ans = new int[n];

        int i = 0;

        while (i < n) {
            if (fixed[i] != -1) {
                ans[i] = fixed[i];
                i++;
            } else {
                int start = i;

                while (i < n && fixed[i] == -1) {
                    i++;
                }

                int end = i;
                int len = end - start;

                int period = (int) Math.round(ratio * len);
                if (period < 1) period = 1;
                if (period > n) period = n;

                for (int j = start; j < end; j++) {
                    int offset = j - start;

                    if (reverseInsideRun) {
                        offset = len - 1 - offset;
                    }

                    ans[j] = offset % period;
                }
            }
        }

        return ans;
    }

    static int[] rulerPattern(boolean reverse) {
        int[] ans = new int[n];

        for (int i = 0; i < n; i++) {
            if (fixed[i] != -1) {
                ans[i] = fixed[i];
            } else {
                int position;

                if (reverse) {
                    position = n - i;
                } else {
                    position = i + 1;
                }

                int value = Integer.numberOfTrailingZeros(position);

                if (value > n) {
                    value = n;
                }

                ans[i] = value;
            }
        }

        return ans;
    }

    static int[] greedy(boolean reverse, int limit) {
        int[] ans = new int[n];
        int[] last = new int[limit + 1];

        for (int step = 0; step < n; step++) {
            if (timeIsOver()) break;

            int index;

            if (reverse) {
                index = n - 1 - step;
            } else {
                index = step;
            }

            int value;

            if (fixed[index] != -1) {
                value = fixed[index];
            } else {
                long bestLocalScore = -1;
                int bestValue = 0;

                for (int candidate = 0; candidate <= limit; candidate++) {
                    int old = last[candidate];

                    last[candidate] = step + 1;
                    long currentScore = endingContribution(last, limit);
                    last[candidate] = old;

                    if (currentScore > bestLocalScore) {
                        bestLocalScore = currentScore;
                        bestValue = candidate;
                    }
                }

                value = bestValue;
            }

            ans[index] = value;

            if (value >= 0 && value <= limit) {
                last[value] = step + 1;
            }
        }

        for (int i = 0; i < n; i++) {
            if (fixed[i] != -1) {
                ans[i] = fixed[i];
            }
        }

        return ans;
    }

    static long endingContribution(int[] last, int limit) {
        long sum = 0;
        int minimumLastPosition = Integer.MAX_VALUE;

        for (int value = 0; value <= limit; value++) {
            minimumLastPosition = Math.min(minimumLastPosition, last[value]);

            if (minimumLastPosition == 0) {
                break;
            }

            sum += minimumLastPosition;
        }

        return sum;
    }

    static void consider(int[] candidate) {
        if (candidate == null || timeIsOver()) return;

        long currentScore = score(candidate);

        if (currentScore > bestScore) {
            bestScore = currentScore;
            bestArray = candidate.clone();
        }
    }

    static long score(int[] arr) {
        int[] last = new int[n + 1];
        long total = 0;

        for (int right = 1; right <= n; right++) {
            if ((right & 127) == 0 && timeIsOver()) {
                return Long.MIN_VALUE;
            }

            int value = arr[right - 1];

            if (value >= 0 && value <= n) {
                last[value] = right;
            }

            int minimumLastPosition = Integer.MAX_VALUE;

            for (int x = 0; x <= n; x++) {
                minimumLastPosition = Math.min(minimumLastPosition, last[x]);

                if (minimumLastPosition == 0) {
                    break;
                }

                total += minimumLastPosition;
            }
        }

        return total;
    }

    static void localImproveForSmallCases() {
        if (n > 180 || bestArray == null || timeIsOver()) {
            return;
        }

        int maxValue = Math.min(n, 80);

        boolean improved = true;
        int rounds = 0;

        while (improved && rounds < 3 && !timeIsOver()) {
            improved = false;
            rounds++;

            for (int i = 0; i < n && !timeIsOver(); i++) {
                if (fixed[i] != -1) continue;

                int oldValue = bestArray[i];
                int bestValue = oldValue;
                long currentBestScore = bestScore;

                for (int value = 0; value <= maxValue && !timeIsOver(); value++) {
                    if (value == oldValue) continue;

                    bestArray[i] = value;
                    long candidateScore = score(bestArray);

                    if (candidateScore > currentBestScore) {
                        currentBestScore = candidateScore;
                        bestValue = value;
                    }
                }

                bestArray[i] = bestValue;

                if (currentBestScore > bestScore) {
                    bestScore = currentBestScore;
                    improved = true;
                }
            }
        }
    }

    static boolean timeIsOver() {
        return System.nanoTime() > deadline;
    }

    static int mod(int a, int b) {
        int result = a % b;

        if (result < 0) {
            result += b;
        }

        return result;
    }

    static void printArray(int[] arr) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(' ');
            sb.append(arr[i]);
        }

        System.out.println(sb);
    }

    static class FastScanner {
        private final InputStream in;
        private final byte[] buffer = new byte[1 << 16];
        private int ptr = 0;
        private int len = 0;

        FastScanner(InputStream is) {
            in = is;
        }

        private int read() throws IOException {
            if (ptr >= len) {
                len = in.read(buffer);
                ptr = 0;

                if (len <= 0) {
                    return -1;
                }
            }

            return buffer[ptr++];
        }

        int nextInt() throws IOException {
            int c;

            do {
                c = read();
            } while (c <= ' ' && c != -1);

            int sign = 1;

            if (c == '-') {
                sign = -1;
                c = read();
            }

            int value = 0;

            while (c > ' ') {
                value = value * 10 + (c - '0');
                c = read();
            }

            return value * sign;
        }
    }
}
