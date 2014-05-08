package tk.ivybits.neural.ocr;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class ZhangSuen {
    public static final int ON = Color.BLACK.getRGB(), OFF = Color.WHITE.getRGB();

    public static void perform(BufferedImage buffer) {
        LinkedList<Point> P = new LinkedList<>();
        boolean hasChanged;
        do {
            hasChanged = false;
            for (int x = 1; x + 1 < buffer.getWidth(); x++) {
                for (int y = 1; y + 1 < buffer.getHeight(); y++) {
                    int a = A(buffer, x, y);
                    int b = B(buffer, x, y);
                    if (buffer.getRGB(x, y) == 1 && 2 <= b && b <= 6 && a == 1
                            && (!(buffer.getRGB(x - 1, y) == ON && buffer.getRGB(x, y + 1) == ON && buffer.getRGB(x + 1, y) == ON))
                            && (!(buffer.getRGB(x, y + 1) == ON && buffer.getRGB(x + 1, y) == ON && buffer.getRGB(x, y - 1) == ON))) {
                        P.add(new Point(x, y));
                        hasChanged = true;
                    }
                }
            }

            for (Point point : P) {
                buffer.setRGB(point.x, point.y, OFF);
            }

            P.clear();

            for (int x = 1; x + 1 < buffer.getWidth(); x++) {
                for (int y = 1; y + 1 < buffer.getHeight(); y++) {
                    int a = A(buffer, x, y);
                    int b = B(buffer, x, y);
                    if (buffer.getRGB(x, y) == ON && 2 <= b && b <= 6 && a == 1
                            && (!(buffer.getRGB(x - 1, y) == ON && buffer.getRGB(x, y + 1) == ON && buffer.getRGB(x, y - 1) == ON))
                            && (!(buffer.getRGB(x - 1, y) == ON && buffer.getRGB(x + 1, y) == ON && buffer.getRGB(x, y - 1) == ON))) {
                        P.add(new Point(x, y));
                        hasChanged = true;
                    }
                }
            }

            for (Point point : P) {
                buffer.setRGB(point.x, point.y, OFF);
            }

            P.clear();

        } while (hasChanged);
    }

    private static int A(BufferedImage binaryImage, int x, int y) {
        int[][][] transitions = {
                {{-1, 0}, {-1, 1}},
                {{-1, 1}, {0, 1}},
                {{0, 1}, {1, 1}},
                {{1, 1}, {1, 0}},
                {{1, 0}, {1, -1}},
                {{1, -1}, {0, -1}},
                {{0, -1}, {-1, -1}},
                {{-1, -1}, {-1, 0}}
        };
        int a = 0;

        for (int[][] t : transitions) {
            if (binaryImage.getRGB(x + t[0][0], y + t[0][1]) == OFF && binaryImage.getRGB(x + t[1][0], y + t[1][1]) == ON) {
                a++;
            }
        }
        return a;
    }

    public static int B(BufferedImage buf, int x, int y) {
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int b = 0;
        for (int i = 0; i != dy.length; i++) {
            b += buf.getRGB(x + dx[i], y + dy[i]) == ON ? 1 : 0;
        }
        return b;
    }
}
