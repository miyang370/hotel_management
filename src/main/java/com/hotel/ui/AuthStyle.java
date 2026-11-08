package com.hotel.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Shared appearance for the login and registration forms. */
final class AuthStyle {
    static final Color INK = UiTheme.TITLE;
    static final Color BLUE = UiTheme.PRIMARY;
    private AuthStyle() {}

    static JPanel shell(String title, String subtitle, JPanel form) {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(UiTheme.BACKGROUND);
        outer.setBorder(new EmptyBorder(28, 38, 28, 38));
        JPanel card = new JPanel(new BorderLayout(0, 22));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(216, 225, 236)),
                new EmptyBorder(30, 36, 30, 36)));
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 10));
        heading.setOpaque(false);
        JLabel brand = new JLabel("HOTEL  /  酒店预订管理系统");
        brand.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 13)); brand.setForeground(new Color(102, 102, 102));
        brand.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel name = new JLabel(title);
        name.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 20)); name.setForeground(INK);
        name.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel note = new JLabel(subtitle);
        note.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        note.setForeground(new Color(153, 153, 153));
        note.setHorizontalAlignment(SwingConstants.CENTER);
        heading.add(brand); heading.add(name); heading.add(note);
        style(form);
        form.setBorder(new EmptyBorder(0, 0, 0, 0));
        card.add(heading, BorderLayout.NORTH); card.add(form, BorderLayout.CENTER);
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        outer.add(card, c);
        return outer;
    }

    private static void style(Component component) {
        component.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        component.setForeground(INK);
        if (component instanceof JPanel panel) panel.setOpaque(false);
        if (component instanceof JTextField field) {
            field.setPreferredSize(new Dimension(300, 44));
            field.setMinimumSize(new Dimension(180, 44));
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(182, 197, 215)),
                    new EmptyBorder(7, 12, 7, 12)));
        }
        if (component instanceof JButton button) {
            button.setPreferredSize(new Dimension(300, 46));
            button.setFocusPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            boolean primary = button.getText().equals("登录") || button.getText().equals("确认注册");
            button.setBackground(primary ? BLUE : new Color(238, 243, 249));
            button.setForeground(primary ? Color.WHITE : BLUE);
            button.setOpaque(true); button.setContentAreaFilled(true);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primary ? BLUE : new Color(202, 214, 229)),
                    new EmptyBorder(10, 18, 10, 18)));
        }
        if (component instanceof Container container)
            for (Component child : container.getComponents()) style(child);
        if (component instanceof JButton button)
            UiTheme.button(button, button.getText().equals("登录") || button.getText().equals("确认注册"));
        if (component instanceof JLabel label && !(component instanceof Notice)) label.setHorizontalAlignment(SwingConstants.RIGHT);
        if (component instanceof Notice notice) notice.refresh();
    }

    static final class Notice extends JLabel {
        private boolean error;
        Notice(String text) { super(text); refresh(); }
        void info(String text) { error = false; setText(text); refresh(); }
        void error(String text) { error = true; setText("提示：" + text); refresh(); }
        void refresh() {
            setOpaque(true);
            setForeground(error ? new Color(180, 35, 45) : new Color(62, 91, 122));
            setBackground(error ? new Color(255, 239, 240) : new Color(239, 245, 252));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(error ? new Color(242, 192, 196) : new Color(214, 227, 243)),
                    new EmptyBorder(12, 12, 12, 12)));
        }
    }
}
