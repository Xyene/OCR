package tk.ivybits.neural.ocr;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class GlyphGenerator {
    public static BufferedImage getGlyphImage(Font font, char c) {
        Rectangle2D bounds = font.getStringBounds(Character.toString(c), new FontRenderContext(null, false, false));
        BufferedImage image = new BufferedImage((int) bounds.getWidth(), (int) bounds.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics g = image.getGraphics();
        g.setColor(new Color(-1));
        g.fillRect(0, 0, (int) bounds.getWidth(), (int) bounds.getHeight());
        g.setColor(Color.BLACK);
        g.setFont(font);
        g.drawString(Character.toString(c), 0, (int) (bounds.getHeight() - bounds.getMaxY()));
        Rectangle2D sub = GlyphBounds.getBoundingBoxes(image).get(0);
        return image.getSubimage((int) sub.getX(), (int) sub.getY(), (int) sub.getWidth(), (int) sub.getHeight());
    }
}
