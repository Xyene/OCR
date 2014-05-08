package tk.ivybits.neural.demo.ocr;

import tk.ivybits.neural.ocr.GlyphBounds;
import tk.ivybits.neural.ocr.ZhangSuen;
import tk.ivybits.neural.ocr.GlyphRecognizer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;

public class OCRDemo {
    public static void main(String[] argv) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
//        for (UIManager.LookAndFeelInfo lf : UIManager.getInstalledLookAndFeels()) {
//            if (lf.getName().contains("GTK")) {
//                UIManager.setLookAndFeel(lf.getClassName());
//            }
//        }
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        final JFrame demo = new JFrame("OCR Demo");
        demo.setLayout(new BorderLayout());
        final DrawPane draw = new DrawPane();
        Dimension drawSize = new Dimension(360, 120);
        draw.setPreferredSize(drawSize);
        draw.setMaximumSize(drawSize);
        draw.setSize(drawSize);
        demo.add(draw, BorderLayout.CENTER);

        final GlyphRecognizer rec = new GlyphRecognizer(7, 7);
        final HashMap<Character, BufferedImage> trainingSet = new HashMap<>();

        JButton recButton = new JButton("Recognize");
        recButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                BufferedImage buffer = draw.cloneBuffer();
                ZhangSuen.perform(buffer);
                StringBuilder strb = new StringBuilder();
                for (Rectangle2D bound : GlyphBounds.getBoundingBoxes(buffer, 5)) {
                    strb.append(rec.recognize(buffer.getSubimage(
                            (int) bound.getX(),
                            (int) bound.getY(),
                            (int) bound.getWidth(),
                            (int) bound.getHeight())
                    ));
                }
                JOptionPane.showMessageDialog(demo, "Recognized '" + strb + "'", "Recognized...", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JButton addButton = new JButton("Train");
        addButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JOptionPane option = new JOptionPane();
                option.setMessageType(JOptionPane.QUESTION_MESSAGE);
                option.setMessage("What character(s) did you draw?");
                option.setWantsInput(true);

                JDialog dialog = option.createDialog(demo, "What string did you enter?");
                dialog.setModal(true);
                dialog.setVisible(true);
                dialog.toFront();

                ZhangSuen.perform(draw.getBuffer());

                BufferedImage buffer = draw.cloneBuffer();
                ZhangSuen.perform(buffer);
                List<Rectangle2D> charBoxes = GlyphBounds.getBoundingBoxes(buffer, 5);

                String str = (String) option.getInputValue();
                if (str.length() != charBoxes.size())
                    JOptionPane.showMessageDialog(demo,
                            "Not equal number of characters, ignoring leftovers",
                            "...",
                            JOptionPane.WARNING_MESSAGE);
                for (int i = 0; i < charBoxes.size(); i++) {
                    Rectangle2D bound = charBoxes.get(i);
                    trainingSet.put(str.charAt(i),
                            buffer.getSubimage(
                                    (int) bound.getX(),
                                    (int) bound.getY(),
                                    (int) bound.getWidth(),
                                    (int) bound.getHeight()
                            )
                    );
                }
                rec.train(trainingSet);
            }
        });

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                draw.clearCanvas();
            }
        });

        JPanel controls = new JPanel(new GridLayout(0, 1));
        controls.add(recButton);
        controls.add(addButton);
        controls.add(clearButton);
        controls.setBorder(BorderFactory.createTitledBorder("Controls"));

        demo.add(controls, BorderLayout.WEST);

        demo.pack();
        demo.setResizable(false);
        demo.setLocationRelativeTo(null);
        demo.setVisible(true);
        demo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
