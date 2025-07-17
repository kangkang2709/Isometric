package ctu.game.isometric.util;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

public class LogWindow {
    private static JTextArea logArea;

    public static void init() {
        JFrame frame = new JFrame("Chrono Veil Logs");
        logArea = new JTextArea(20, 80);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Redirect System.out and System.err
        PrintStream logStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append(String.valueOf((char) b));
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }
        }, true);

        System.setOut(logStream);
        System.setErr(logStream);
    }
}
