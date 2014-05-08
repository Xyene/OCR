package tk.ivybits.neural.ocr;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import static java.lang.Math.*;

public class GlyphBounds {
    private static final int OFF = Color.WHITE.getRGB();

    public static List<Rectangle2D> getBoundingBoxes(BufferedImage buffer) {
        return getBoundingBoxes(buffer, 5);
    }

    public static List<Rectangle2D> getBoundingBoxes(BufferedImage buffer, int threshold) {
        List<LinkedList<Point>> chars = new ArrayList<>();
        int srcW = buffer.getWidth();
        int srcH = buffer.getHeight();

        // Visited pixel nodes
        boolean[][] visited = new boolean[srcW][srcH];

        // Scanline on y axis
        for (int y = 0; y != srcH; y++) {
            for (int x = 0; x != srcW; x++) {
                // We've found a nonvisited black pixel, floodfill through it
                if (buffer.getRGB(x, y) != OFF && !visited[x][y]) {
                    // Will hold all the points of the discovered character
                    LinkedList<Point> character = new LinkedList<>();
                    // Perform a BFS to explore entire character
                    _fill(buffer, x, y, character, visited);
                    chars.add(character);
                }
            }
        }

        List<Rectangle2D> bounds = new ArrayList<>();
        // Compute bounding boxes of each found character
        for (LinkedList<Point> pts : chars) {
            // Bounding box corners
            int left = Integer.MAX_VALUE;
            int top = Integer.MAX_VALUE;
            int right = 0;
            int bottom = 0;

            for (Point pt : pts) {
                // Find extremities
                left = min(left, pt.x);
                top = min(top, pt.y);
                right = max(right, pt.x);
                bottom = max(bottom, pt.y);
            }

            bounds.add(new Rectangle2D.Float(left, top, right - left, bottom - top));
        }

        // Sort from left to right
        Collections.sort(bounds, new Comparator<Rectangle2D>() {
            @Override
            public int compare(Rectangle2D o1, Rectangle2D o2) {
                return (int) (o1.getCenterX() - o2.getCenterX());
            }
        });

        // Iteratively collapse bounding boxes
        LinkedList<Rectangle2D> finalBounds = new LinkedList<>();
        boolean changed;
        do {
            changed = false;
            finalBounds.clear();
            for (int i = 0; i < bounds.size(); i++) {
                Rectangle2D p = bounds.get(i);
                // Any intersecting bounding boxes will always be to the right of this bounding box
                for (int j = i + 1; j < bounds.size(); j++) {
                    Rectangle2D c = bounds.get(j);
                    // Check if the two bounding boxes intersect on the x-axis within a threshold
                    if ((p.getMaxX() < c.getMaxX() + threshold && p.getMinX() > c.getMinX() - threshold) ||
                            (c.getMaxX() < p.getMaxX() + threshold && c.getMinX() > p.getMinX() - threshold)) {
                        // Collapse the bounding boxes
                        finalBounds.add(p.createUnion(c));
                        // And remove the original ones
                        bounds.remove(p);
                        bounds.remove(c);
                        changed = true;
                    }
                }
            }

            bounds.addAll(finalBounds);
        } while (changed);
        return Collections.unmodifiableList(bounds);
    }

    private static void _fill(BufferedImage buffer, int _x, int _y, LinkedList<Point> character, boolean[][] visited) {
        LinkedList<Point> Q = new LinkedList<>();

        // All possible directions - ordinal
        int[] dx = {1, -1, -1, 1, 1, -1, 0, 0};
        int[] dy = {-1, 1, -1, 1, 0, 0, 1, -1};

        Point n = new Point(_x, _y);
        Q.add(n);
        character.add(n);
        visited[_x][_y] = true;
        while (!Q.isEmpty()) {
            Point next = Q.pop();
            int x = next.x;
            int y = next.y;
            for (int i = 0; i != dx.length; i++) {
                _expand(buffer, Q, character, visited, x + dx[i], y + dy[i]);
            }
        }
    }

    private static void _expand(BufferedImage buffer, LinkedList<Point> Q, LinkedList<Point> character, boolean[][] visited, int x, int y) {
        if ((x >= 0 && x < buffer.getWidth() && y >= 0 && y < buffer.getHeight()) // Within bounds
                && !visited[x][y] // And hasn't been visited
                && buffer.getRGB(x, y) != OFF) { // And is on
            Point n = new Point(x, y);
            Q.add(n);
            character.add(n);
            visited[x][y] = true;
        }
    }
}
