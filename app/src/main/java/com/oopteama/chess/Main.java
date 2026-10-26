package com.oopteama.chess;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Chess Trainer");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setSize(800, 600);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}