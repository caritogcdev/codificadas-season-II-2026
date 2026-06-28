package challengecodificadas;

import java.io.InputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.PriorityQueue;

public class BaulesLuzDePlata {

  static final long BASE = 1_000_000_000L;

  static int n;
  static long[] a;
  static long[] b;
  static long[] c;

  static class FastScanner {

    private final InputStream input = System.in;
    private final byte[] buffer = new byte[1 << 16];
    private int pointer = 0;
    private int length = 0;

    private int read() throws IOException {
      if (pointer >= length) {
        length = input.read(buffer);
        pointer = 0;

        if (length <= 0) {
          return -1;
        }
      }

      return buffer[pointer++];
    }

    long nextLong() throws IOException {
      int character = read();

      while (character <= ' ' && character != -1) {
        character = read();
      }

      long number = 0;

      while (character > ' ') {
        number = number * 10 + (character - '0');
        character = read();
      }

      return number;
    }
  }

  static class Baul {

    int i;
    int j;
    int k;

    long high;
    long low;

    Baul(int i, int j, int k) {
      this.i = i;
      this.j = j;
      this.k = k;

      calcularSuperficie();
    }

    void calcularSuperficie() {
      long ab = a[i] * b[j];
      long bc = b[j] * c[k];
      long ca = c[k] * a[i];

      long lowSum = ab % BASE + bc % BASE + ca % BASE;
      long highSum = ab / BASE + bc / BASE + ca / BASE;

      highSum = highSum + lowSum / BASE;
      lowSum = lowSum % BASE;

      lowSum = lowSum * 2;
      highSum = highSum * 2;

      highSum = highSum + lowSum / BASE;
      lowSum = lowSum % BASE;

      this.high = highSum;
      this.low = lowSum;
    }
  }

  static long obtenerClave(int i, int j, int k) {
    return ((long) i * n + j) * n + k;
  }

  static void agregarBaul(
      PriorityQueue<Baul> cola,
      HashSet<Long> visitados,
      int i,
      int j,
      int k
  ) {
    if (i < 0 || j < 0 || k < 0) {
      return;
    }

    long clave = obtenerClave(i, j, k);

    if (!visitados.contains(clave)) {
      visitados.add(clave);
      cola.add(new Baul(i, j, k));
    }
  }

  public static void main(String[] args) throws Exception {

    FastScanner fs = new FastScanner();

    n = (int) fs.nextLong();
    int objetivo = (int) fs.nextLong();

    a = new long[n];
    b = new long[n];
    c = new long[n];

    for (int i = 0; i < n; i++) {
      a[i] = fs.nextLong();
    }

    for (int i = 0; i < n; i++) {
      b[i] = fs.nextLong();
    }

    for (int i = 0; i < n; i++) {
      c[i] = fs.nextLong();
    }

    PriorityQueue<Baul> cola = new PriorityQueue<>((baul1, baul2) -> {

      if (baul1.high != baul2.high) {
        return Long.compare(baul2.high, baul1.high);
      }

      if (baul1.low != baul2.low) {
        return Long.compare(baul2.low, baul1.low);
      }

      if (baul1.i != baul2.i) {
        return Integer.compare(baul2.i, baul1.i);
      }

      if (baul1.j != baul2.j) {
        return Integer.compare(baul2.j, baul1.j);
      }

      return Integer.compare(baul2.k, baul1.k);
    });

    HashSet<Long> visitados = new HashSet<>();

    agregarBaul(cola, visitados, n - 1, n - 1, n - 1);

    Baul respuesta = null;

    for (int contador = 1; contador <= objetivo; contador++) {

      respuesta = cola.poll();

      agregarBaul(cola, visitados, respuesta.i - 1, respuesta.j, respuesta.k);
      agregarBaul(cola, visitados, respuesta.i, respuesta.j - 1, respuesta.k);
      agregarBaul(cola, visitados, respuesta.i, respuesta.j, respuesta.k - 1);
    }

    System.out.println(
        (respuesta.i + 1) + " " +
        (respuesta.j + 1) + " " +
        (respuesta.k + 1)
    );
  }
}