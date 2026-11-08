package com.hotel.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

public final class UiTheme {
    public static final Color PRIMARY = Color.decode("#1677FF"), BACKGROUND = Color.decode("#F5F7FA"),
            TITLE = Color.decode("#333333"), TEXT = Color.decode("#666666"), MUTED = Color.decode("#999999"),
            SUCCESS = Color.decode("#52C41A"), WARNING = Color.decode("#FAAD14"), DANGER = Color.decode("#FF4D4F"),
            BORDER = Color.decode("#E8E8E8");
    private UiTheme() {}
    public static void install() {
        var font = new javax.swing.plaf.FontUIResource("Microsoft YaHei UI", Font.PLAIN, 14);
        for (Object key : java.util.Collections.list(UIManager.getDefaults().keys()))
            if (UIManager.get(key) instanceof Font) UIManager.put(key, font);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextField.foreground", TITLE);
        UIManager.put("PasswordField.foreground", TITLE);
    }
    public static void button(JButton button, boolean primary) {
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                Graphics2D copy = (Graphics2D) g.create();
                copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = primary ? PRIMARY : Color.WHITE;
                if (!button.isEnabled()) fill = Color.decode("#E8EDF3");
                else if (button.getModel().isPressed()) fill = primary ? PRIMARY.darker() : Color.decode("#DBEAFE");
                else if (button.getModel().isRollover()) fill = primary ? Color.decode("#4096FF") : Color.decode("#EFF6FF");
                copy.setColor(fill); copy.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10);
                if (!primary || button.hasFocus()) {
                    copy.setColor(button.hasFocus() ? PRIMARY : BORDER);
                    copy.drawRoundRect(0, 0, c.getWidth()-1, c.getHeight()-1, 10, 10);
                }
                copy.dispose(); super.paint(g, c);
            }
        });
        button.setOpaque(false); button.setContentAreaFilled(false); button.setBorderPainted(false);
        button.setFocusPainted(false); button.setRolloverEnabled(true);
        button.setForeground(primary ? Color.WHITE : TEXT);
        button.setBorder(BorderFactory.createEmptyBorder(7, 18, 7, 18));
        button.setPreferredSize(new Dimension(Math.max(110, button.getPreferredSize().width), 36));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    public static void table(JTable table) {
        table.setRowHeight(35); table.setShowVerticalLines(false); table.setShowHorizontalLines(true);
        table.setGridColor(BORDER); table.setSelectionBackground(Color.decode("#E6F4FF"));
        table.setSelectionForeground(TITLE); table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(Color.decode("#F0F2F5"));
        table.getTableHeader().setForeground(TITLE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                if (!selected) { setBackground(row % 2 == 0 ? Color.WHITE : Color.decode("#FAFBFC")); setForeground(TEXT); }
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12)); return this;
            }
        });
    }
}
