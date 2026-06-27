package challengecodificadas;

import java.util.Scanner;

public class Insectologia {

  public static void main(String[] args) {

    Scanner sc = new Scanner(System.in);

    int n = sc.nextInt();

    long[] x = new long[n];
    long[] y = new long[n];

    for (int i = 0; i < n; i++) {
      x[i] = sc.nextLong();
      y[i] = sc.nextLong();
    }

    long minX = x[0];
    long maxX = x[0];
    long minY = y[0];
    long maxY = y[0];

    for (int i = 1; i < n; i++) {
      if (x[i] < minX) {
        minX = x[i];
      }

      if (x[i] > maxX) {
        maxX = x[i];
      }

      if (y[i] < minY) {
        minY = y[i];
      }

      if (y[i] > maxY) {
        maxY = y[i];
      }
    }

    long a = maxX - minX;
    long b = maxY - minY;
    long A = a * b;

    System.out.println(A + " " + a + " " + b);

    sc.close();
  }
}