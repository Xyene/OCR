package tk.ivybits.neural.demo.ocr;

import lombok.Getter;
import tk.ivybits.neural.ocr.GlyphBounds;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class DrawPane extends JPanel {
    @Getter
    private BufferedImage buffer;
    private Graphics2D canvas;

    public DrawPane() {
        MouseAdapter drawer = new MouseAdapter() {
            int _x;
            int _y;

            @Override
            public void mouseClicked(MouseEvent e) {
                buffer.setRGB(e.getX(), e.getY(), Color.BLACK.getRGB());
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                _x = e.getX();
                _y = e.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                _x = -1;
                _y = -1;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                canvas.setColor(Color.BLACK);
                canvas.setStroke(new BasicStroke(2));
                canvas.drawLine(_x, _y, _x = e.getX(), _y = e.getY());
                canvas.setStroke(new BasicStroke(1));
                repaint();
            }
        };
        addMouseListener(drawer);
        addMouseMotionListener(drawer);
    }

    public void clearCanvas() {
        buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        canvas = buffer.createGraphics();
        canvas.setColor(Color.WHITE);
        canvas.fillRect(0, 0, getWidth(), getHeight());
        repaint();
    }

    public BufferedImage cloneBuffer() {
        BufferedImage clone = new BufferedImage(buffer.getWidth(), buffer.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = clone.getGraphics();
        g.drawImage(buffer, 0, 0, null);
        g.dispose();
        return clone;
    }

    @Override
    public void paintComponent(Graphics g) {
        if (canvas == null) {
            clearCanvas();
        }
        g.drawImage(buffer, 0, 0, getWidth(), getHeight(), null);
        g.setColor(Color.RED);
        for (Rectangle2D bound : GlyphBounds.getBoundingBoxes(buffer, 5)) {
            g.drawRect((int) bound.getX(), (int) bound.getY(), (int) bound.getWidth(), (int) bound.getHeight());
        }
    }
}

