package tk.ivybits.neural.ocr;

import lombok.Getter;
import lombok.Setter;
import tk.ivybits.neural.network.kohonen.KohonenNetwork;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

import static tk.ivybits.neural.ocr.GlyphGenerator.getGlyphImage;

public class GlyphRecognizer {
    @Getter
    protected final int width;
    @Getter
    protected final int height;
    @Getter
    protected final char[] glyphs;
    @Getter
    protected KohonenNetwork<Character> net;
    @Getter
    @Setter
    protected boolean doThin;

    public GlyphRecognizer(int width, int height, boolean doThin, char[] glyphs) {
        this.width = width;
        this.height = height;
        this.glyphs = glyphs;
        this.doThin = doThin;
    }

    public GlyphRecognizer(int width, int height, boolean doThin) {
        this(width, height, doThin, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ012345679".toCharArray());
    }

    public GlyphRecognizer(int width, int height) {
        this(width, height, true);
    }

    public void train(Font... fonts) {
        HashMap<Character, BufferedImage> chars = new HashMap<>();

        for (Font font : fonts) {
            for (char glyph : glyphs) {
                chars.put(glyph, getGlyphImage(font, glyph));
            }
        }

        train(chars);
    }

    public void train(HashMap<Character, BufferedImage> chars) {
        int inputNeuron = width * height;
        int outputNeuron = chars.size();

        net = new KohonenNetwork<>(inputNeuron, outputNeuron);
        for (Map.Entry<Character, BufferedImage> pair : chars.entrySet()) {
            int idx = 0;
            double[][] ds = getSampleFor(pair.getValue());
            double[] set = new double[inputNeuron];
            for (int y = 0; y < ds[0].length; y++) {
                for (double[] d : ds) {
                    set[idx++] = d[y];
                }
            }
            net.queueData(pair.getKey(), set);
        }

        net.learn();
    }

    public char recognize(BufferedImage img) {
        return net.recall(getInputFor(getSampleFor(img)));
    }

    protected double[][] getSampleFor(BufferedImage img) {
        if (doThin)
            ZhangSuen.perform(img);
        double[][] sample = new double[width][height];
        Downsample.downSample(img, sample);
        return sample;
    }

    protected double[] getInputFor(double[][] ds) {
        double input[] = new double[ds.length * ds[0].length];
        int idx = 0;
        for (int y = 0; y < ds[0].length; y++) {
            for (double[] d : ds) {
                input[idx++] = d[y];
            }
        }
        return input;
    }
}