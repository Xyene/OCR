package tk.ivybits.neural.ocr;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;

public class Downsample {
    public static void downSample(BufferedImage source, double[][] data) {
        int srcW = source.getWidth(null);
        int srcH = source.getHeight(null);
        int dstW = data.length;
        int dstH = data[0].length;

        PixelGrabber grabber = new PixelGrabber(source, 0, 0, srcW, srcH, true);
        try {
            grabber.grabPixels();
        } catch (InterruptedException e) {
            throw new IllegalStateException("interrupted while grabbing pixels", e);
        }
        int[] pixelMap = (int[]) grabber.getPixels();
        int downSampleLeft = Integer.MAX_VALUE;
        int downSampleTop = Integer.MAX_VALUE;
        int downSampleRight = 0;
        int downSampleBottom = 0;

        for (int _x = 0; _x < srcW; _x++) {
            for (int _y = 0; _y < srcH; _y++) {
                if (pixelMap[_y * srcW + _x] != Color.WHITE.getRGB()) {
                    downSampleLeft = Math.min(downSampleLeft, _x);
                    downSampleTop = Math.min(downSampleTop, _y);
                    downSampleRight = Math.max(downSampleRight, _x);
                    downSampleBottom = Math.max(downSampleBottom, _y);
                }
            }
        }

        double ratioX = (double) (downSampleRight - downSampleLeft) / (double) dstW;
        double ratioY = (double) (downSampleBottom - downSampleTop) / (double) dstH;

        for (int y = 0; y < dstH; y++) {
            for (int x = 0; x < dstW; x++) {
                boolean contains = false;
                int startX = (int) (downSampleLeft + (x * ratioX));
                int startY = (int) (downSampleTop + (y * ratioY));
                int endX = (int) (startX + ratioX);
                int endY = (int) (startY + ratioY);

                _inner:
                for (int _y = startY; _y <= endY; _y++) {
                    for (int _x = startX; _x <= endX; _x++) {
                        if (pixelMap[_y * srcW + _x] != Color.WHITE.getRGB()) {
                            contains = true;
                            break _inner;
                        }
                    }
                }

                data[x][y] = contains ? .5 : -.5;
            }
        }
    }
}