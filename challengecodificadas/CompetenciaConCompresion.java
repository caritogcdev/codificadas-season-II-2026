package challengecodificadas;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

public class CompetenciaConCompresion {

  static final char[] HEX = "0123456789ABCDEF".toCharArray();

  static final int MODE_ZLIB_RAW = 1;
  static final int MODE_PNG_FILTER = 2;
  static final int MODE_PALETTE = 3;
  static final int MODE_RLE = 4;

  static final int MAX_PALETTE = 65536;

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

    String next() throws IOException {
      int ch = read();

      while (ch <= ' ' && ch != -1) {
        ch = read();
      }

      StringBuilder sb = new StringBuilder();

      while (ch > ' ') {
        sb.append((char) ch);
        ch = read();
      }

      return sb.toString();
    }

    long nextLong() throws IOException {
      int ch = read();

      while (ch <= ' ' && ch != -1) {
        ch = read();
      }

      long number = 0;

      while (ch > ' ') {
        number = number * 10 + (ch - '0');
        ch = read();
      }

      return number;
    }

    int nextInt() throws IOException {
      return (int) nextLong();
    }

    int nextHexInt() throws IOException {
      int ch = read();

      while (ch <= ' ' && ch != -1) {
        ch = read();
      }

      int value = 0;

      while (ch > ' ') {
        int digit;

        if (ch >= '0' && ch <= '9') {
          digit = ch - '0';
        } else if (ch >= 'A' && ch <= 'F') {
          digit = ch - 'A' + 10;
        } else {
          digit = ch - 'a' + 10;
        }

        value = (value << 4) | digit;
        ch = read();
      }

      return value;
    }
  }

  static class Candidate {
    int mode;
    byte[] compressed;
    int k;

    Candidate(int mode, byte[] compressed) {
      this.mode = mode;
      this.compressed = compressed;
      this.k = 2 + (compressed.length + 3) / 4;
    }
  }

  public static void main(String[] args) throws Exception {
    FastScanner fs = new FastScanner();

    String action = fs.next();

    if (action.equals("compress")) {
      compress(fs);
    } else {
      decompress(fs);
    }
  }

  static void compress(FastScanner fs) throws Exception {
    int n = fs.nextInt();
    int m = fs.nextInt();

    int totalPixels = n * m;

    int[] pixels = new int[totalPixels];
    byte[] raw = new byte[totalPixels * 4];

    for (int i = 0; i < totalPixels; i++) {
      int pixel = fs.nextHexInt();

      pixels[i] = pixel;

      int position = i * 4;

      raw[position] = (byte) (pixel >>> 24);
      raw[position + 1] = (byte) (pixel >>> 16);
      raw[position + 2] = (byte) (pixel >>> 8);
      raw[position + 3] = (byte) pixel;
    }

    int level = chooseLevel(totalPixels);

    Candidate best = null;

    byte[] compressedRaw = deflate(raw, level, false);
    best = chooseBest(best, new Candidate(MODE_ZLIB_RAW, compressedRaw));

    byte[] filtered = makePngFiltered(raw, n, m);
    byte[] compressedFiltered = deflate(filtered, level, true);
    best = chooseBest(best, new Candidate(MODE_PNG_FILTER, compressedFiltered));

    byte[] paletteData = makePaletteData(pixels, totalPixels);

    if (paletteData != null) {
      byte[] compressedPalette = deflate(paletteData, level, false);
      best = chooseBest(best, new Candidate(MODE_PALETTE, compressedPalette));
    }

    byte[] rleData = makeRleData(pixels, totalPixels);

    if (rleData != null) {
      byte[] compressedRle = deflate(rleData, level, false);
      best = chooseBest(best, new Candidate(MODE_RLE, compressedRle));
    }

    if (best != null && best.k < totalPixels) {
      printCompressed(best);
    } else {
      printRaw(pixels);
    }
  }

  static int chooseLevel(int totalPixels) {
    if (totalPixels > 700000) {
      return 4;
    }

    if (totalPixels > 300000) {
      return 5;
    }

    return 7;
  }

  static Candidate chooseBest(Candidate best, Candidate current) {
    if (best == null || current.k < best.k) {
      return current;
    }

    return best;
  }

  static byte[] deflate(byte[] data, int level, boolean filteredStrategy) {
    Deflater deflater = new Deflater(level);

    if (filteredStrategy) {
      deflater.setStrategy(Deflater.FILTERED);
    }

    deflater.setInput(data);
    deflater.finish();

    ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(32, data.length / 2));
    byte[] temp = new byte[1 << 15];

    while (!deflater.finished()) {
      int count = deflater.deflate(temp);
      output.write(temp, 0, count);
    }

    deflater.end();

    return output.toByteArray();
  }

  static byte[] makePngFiltered(byte[] raw, int n, int m) {
    int rowLength = m * 4;
    byte[] result = new byte[n + raw.length];

    for (int row = 0; row < n; row++) {
      int rowStart = row * rowLength;

      long[] scores = new long[5];

      for (int col = 0; col < rowLength; col++) {
        int position = rowStart + col;
        int current = raw[position] & 255;

        int left = col >= 4 ? raw[position - 4] & 255 : 0;
        int up = row > 0 ? raw[position - rowLength] & 255 : 0;
        int upLeft = row > 0 && col >= 4 ? raw[position - rowLength - 4] & 255 : 0;

        scores[0] += absSignedByte(current);
        scores[1] += absSignedByte(current - left);
        scores[2] += absSignedByte(current - up);
        scores[3] += absSignedByte(current - ((left + up) >> 1));
        scores[4] += absSignedByte(current - paeth(left, up, upLeft));
      }

      int bestFilter = 0;

      for (int filter = 1; filter < 5; filter++) {
        if (scores[filter] < scores[bestFilter]) {
          bestFilter = filter;
        }
      }

      result[row] = (byte) bestFilter;

      int outputStart = n + rowStart;

      for (int col = 0; col < rowLength; col++) {
        int position = rowStart + col;
        int current = raw[position] & 255;

        int left = col >= 4 ? raw[position - 4] & 255 : 0;
        int up = row > 0 ? raw[position - rowLength] & 255 : 0;
        int upLeft = row > 0 && col >= 4 ? raw[position - rowLength - 4] & 255 : 0;

        int predicted = 0;

        if (bestFilter == 1) {
          predicted = left;
        } else if (bestFilter == 2) {
          predicted = up;
        } else if (bestFilter == 3) {
          predicted = (left + up) >> 1;
        } else if (bestFilter == 4) {
          predicted = paeth(left, up, upLeft);
        }

        result[outputStart + col] = (byte) ((current - predicted) & 255);
      }
    }

    return result;
  }

  static byte[] undoPngFiltered(byte[] data, int n, int m) {
    int rowLength = m * 4;
    byte[] raw = new byte[n * rowLength];

    for (int row = 0; row < n; row++) {
      int filter = data[row] & 255;

      int rowStart = row * rowLength;
      int inputStart = n + rowStart;

      for (int col = 0; col < rowLength; col++) {
        int position = rowStart + col;
        int value = data[inputStart + col] & 255;

        int left = col >= 4 ? raw[position - 4] & 255 : 0;
        int up = row > 0 ? raw[position - rowLength] & 255 : 0;
        int upLeft = row > 0 && col >= 4 ? raw[position - rowLength - 4] & 255 : 0;

        int predicted = 0;

        if (filter == 1) {
          predicted = left;
        } else if (filter == 2) {
          predicted = up;
        } else if (filter == 3) {
          predicted = (left + up) >> 1;
        } else if (filter == 4) {
          predicted = paeth(left, up, upLeft);
        }

        raw[position] = (byte) ((value + predicted) & 255);
      }
    }

    return raw;
  }

  static int absSignedByte(int value) {
    value = value & 255;

    if (value >= 128) {
      value -= 256;
    }

    if (value < 0) {
      return -value;
    }

    return value;
  }

  static int paeth(int left, int up, int upLeft) {
    int p = left + up - upLeft;

    int pa = Math.abs(p - left);
    int pb = Math.abs(p - up);
    int pc = Math.abs(p - upLeft);

    if (pa <= pb && pa <= pc) {
      return left;
    }

    if (pb <= pc) {
      return up;
    }

    return upLeft;
  }

  static byte[] makePaletteData(int[] pixels, int totalPixels) {
    IntIntMap map = new IntIntMap(262144);

    int[] colors = new int[MAX_PALETTE];
    int uniqueColors = 0;

    for (int i = 0; i < totalPixels; i++) {
      int color = pixels[i];
      int index = map.get(color);

      if (index == -1) {
        if (uniqueColors == MAX_PALETTE) {
          return null;
        }

        map.put(color, uniqueColors);
        colors[uniqueColors] = color;
        uniqueColors++;
      }
    }

    int bits;

    if (uniqueColors <= 2) {
      bits = 1;
    } else if (uniqueColors <= 4) {
      bits = 2;
    } else if (uniqueColors <= 16) {
      bits = 4;
    } else if (uniqueColors <= 256) {
      bits = 8;
    } else {
      bits = 16;
    }

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    writeInt(output, uniqueColors);
    output.write(bits);

    for (int i = 0; i < uniqueColors; i++) {
      writeInt(output, colors[i]);
    }

    BitWriter writer = new BitWriter(output);

    for (int i = 0; i < totalPixels; i++) {
      writer.write(map.get(pixels[i]), bits);
    }

    writer.flush();

    return output.toByteArray();
  }

  static int[] decodePalette(byte[] data, int totalPixels) {
    int position = 0;

    int uniqueColors = readInt(data, position);
    position += 4;

    int bits = data[position] & 255;
    position++;

    int[] colors = new int[uniqueColors];

    for (int i = 0; i < uniqueColors; i++) {
      colors[i] = readInt(data, position);
      position += 4;
    }

    int[] pixels = new int[totalPixels];

    BitReader reader = new BitReader(data, position);

    for (int i = 0; i < totalPixels; i++) {
      int index = reader.read(bits);
      pixels[i] = colors[index];
    }

    return pixels;
  }

  static class BitWriter {
    ByteArrayOutputStream output;
    int current = 0;
    int used = 0;

    BitWriter(ByteArrayOutputStream output) {
      this.output = output;
    }

    void write(int value, int bits) {
      for (int shift = bits - 1; shift >= 0; shift--) {
        current = (current << 1) | ((value >>> shift) & 1);
        used++;

        if (used == 8) {
          output.write(current);
          current = 0;
          used = 0;
        }
      }
    }

    void flush() {
      if (used > 0) {
        current = current << (8 - used);
        output.write(current);
      }
    }
  }

  static class BitReader {
    byte[] data;
    int position;
    int current = 0;
    int remaining = 0;

    BitReader(byte[] data, int position) {
      this.data = data;
      this.position = position;
    }

    int read(int bits) {
      int value = 0;

      for (int i = 0; i < bits; i++) {
        if (remaining == 0) {
          current = data[position] & 255;
          position++;
          remaining = 8;
        }

        value = (value << 1) | ((current >>> (remaining - 1)) & 1);
        remaining--;
      }

      return value;
    }
  }

  static byte[] makeRleData(int[] pixels, int totalPixels) {
    int runs = 0;
    int i = 0;

    while (i < totalPixels) {
      runs++;

      int color = pixels[i];

      while (i < totalPixels && pixels[i] == color) {
        i++;
      }
    }

    if (runs * 2 >= totalPixels) {
      return null;
    }

    ByteArrayOutputStream output = new ByteArrayOutputStream(runs * 8 + 4);

    writeInt(output, runs);

    i = 0;

    while (i < totalPixels) {
      int color = pixels[i];
      int start = i;

      while (i < totalPixels && pixels[i] == color) {
        i++;
      }

      int length = i - start;

      writeInt(output, color);
      writeInt(output, length);
    }

    return output.toByteArray();
  }

  static int[] decodeRle(byte[] data, int totalPixels) {
    int position = 0;

    int runs = readInt(data, position);
    position += 4;

    int[] pixels = new int[totalPixels];
    int outputPosition = 0;

    for (int r = 0; r < runs; r++) {
      int color = readInt(data, position);
      position += 4;

      int length = readInt(data, position);
      position += 4;

      for (int i = 0; i < length; i++) {
        pixels[outputPosition] = color;
        outputPosition++;
      }
    }

    return pixels;
  }

  static class IntIntMap {
    int[] keys;
    int[] values;
    boolean[] used;
    int mask;

    IntIntMap(int capacity) {
      int size = 1;

      while (size < capacity) {
        size = size << 1;
      }

      keys = new int[size];
      values = new int[size];
      used = new boolean[size];
      mask = size - 1;
    }

    int get(int key) {
      int position = hash(key) & mask;

      while (used[position]) {
        if (keys[position] == key) {
          return values[position];
        }

        position = (position + 1) & mask;
      }

      return -1;
    }

    void put(int key, int value) {
      int position = hash(key) & mask;

      while (used[position]) {
        if (keys[position] == key) {
          values[position] = value;
          return;
        }

        position = (position + 1) & mask;
      }

      used[position] = true;
      keys[position] = key;
      values[position] = value;
    }

    int hash(int x) {
      x ^= x >>> 16;
      x *= 0x7feb352d;
      x ^= x >>> 15;
      x *= 0x846ca68b;
      x ^= x >>> 16;

      return x;
    }
  }

  static void printCompressed(Candidate candidate) {
    StringBuilder sb = new StringBuilder();

    sb.append(candidate.k).append('\n');

    sb.append(candidate.mode).append(' ');
    sb.append(candidate.compressed.length);

    for (int i = 0; i < candidate.compressed.length; i += 4) {
      int word = 0;

      for (int j = 0; j < 4; j++) {
        word = word << 8;

        if (i + j < candidate.compressed.length) {
          word = word | (candidate.compressed[i + j] & 255);
        }
      }

      sb.append(' ').append(Integer.toUnsignedString(word));
    }

    System.out.println(sb);
  }

  static void printRaw(int[] pixels) {
    StringBuilder sb = new StringBuilder();

    sb.append(pixels.length).append('\n');

    for (int i = 0; i < pixels.length; i++) {
      if (i > 0) {
        sb.append(' ');
      }

      sb.append(Integer.toUnsignedString(pixels[i]));
    }

    System.out.println(sb);
  }

  static void decompress(FastScanner fs) throws Exception {
    int n = fs.nextInt();
    int m = fs.nextInt();

    int totalPixels = n * m;

    int k = fs.nextInt();

    int[] pixels;

    if (k == totalPixels) {
      pixels = new int[totalPixels];

      for (int i = 0; i < totalPixels; i++) {
        pixels[i] = (int) fs.nextLong();
      }
    } else {
      int mode = (int) fs.nextLong();
      int compressedLength = (int) fs.nextLong();

      byte[] compressed = readCompressedBytes(fs, k - 2, compressedLength);
      byte[] data = inflate(compressed);

      if (mode == MODE_ZLIB_RAW) {
        pixels = bytesToPixels(data, totalPixels);
      } else if (mode == MODE_PNG_FILTER) {
        byte[] raw = undoPngFiltered(data, n, m);
        pixels = bytesToPixels(raw, totalPixels);
      } else if (mode == MODE_PALETTE) {
        pixels = decodePalette(data, totalPixels);
      } else {
        pixels = decodeRle(data, totalPixels);
      }
    }

    printImage(pixels, n, m);
  }

  static byte[] readCompressedBytes(
      FastScanner fs,
      int words,
      int compressedLength) throws IOException {

    byte[] bytes = new byte[compressedLength];

    int position = 0;

    for (int i = 0; i < words; i++) {
      int word = (int) fs.nextLong();

      for (int shift = 24; shift >= 0 && position < compressedLength; shift -= 8) {
        bytes[position] = (byte) (word >>> shift);
        position++;
      }
    }

    return bytes;
  }

  static byte[] inflate(byte[] compressed) throws DataFormatException {
    Inflater inflater = new Inflater();

    inflater.setInput(compressed);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] temp = new byte[1 << 15];

    while (!inflater.finished()) {
      int count = inflater.inflate(temp);

      if (count == 0) {
        break;
      }

      output.write(temp, 0, count);
    }

    inflater.end();

    return output.toByteArray();
  }

  static int[] bytesToPixels(byte[] raw, int totalPixels) {
    int[] pixels = new int[totalPixels];

    for (int i = 0; i < totalPixels; i++) {
      int position = i * 4;

      pixels[i] = ((raw[position] & 255) << 24)
          | ((raw[position + 1] & 255) << 16)
          | ((raw[position + 2] & 255) << 8)
          | (raw[position + 3] & 255);
    }

    return pixels;
  }

  static void printImage(int[] pixels, int n, int m) {
    StringBuilder sb = new StringBuilder();

    for (int row = 0; row < n; row++) {
      for (int col = 0; col < m; col++) {
        if (col > 0) {
          sb.append(' ');
        }

        appendHex(sb, pixels[row * m + col]);
      }

      if (row + 1 < n) {
        sb.append('\n');
      }
    }

    System.out.println(sb);
  }

  static void appendHex(StringBuilder sb, int value) {
    for (int shift = 28; shift >= 0; shift -= 4) {
      sb.append(HEX[(value >>> shift) & 15]);
    }
  }

  static void writeInt(ByteArrayOutputStream output, int value) {
    output.write(value >>> 24);
    output.write(value >>> 16);
    output.write(value >>> 8);
    output.write(value);
  }

  static int readInt(byte[] data, int position) {
    return ((data[position] & 255) << 24)
        | ((data[position + 1] & 255) << 16)
        | ((data[position + 2] & 255) << 8)
        | (data[position + 3] & 255);
  }
}