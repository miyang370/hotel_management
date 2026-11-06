package com.hotel.ui;

import java.awt.*;
import javax.swing.*;

public final class MainFrame extends JFrame {
    public MainFrame(String username) {
        super("酒店预订管理系统");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 650); setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        header.add(new JLabel("酒店预订管理系统    当前管理员：" + username), BorderLayout.CENTER);
        JButton logout = new JButton("退出登录");
        header.add(logout, BorderLayout.EAST);
        logout.addActionListener(event -> {
            if (JOptionPane.showConfirmDialog(this, "确定退出当前账号？", "退出登录",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                new LoginFrame().setVisible(true); dispose();
            }
        });
        add(header, BorderLayout.NORTH);
        CardLayout layout = new CardLayout();
        JPanel content = new JPanel(layout);
        JPanel menu = new JPanel(new GridLayout(0, 1, 0, 10));
        menu.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));
        String[] names = {"客房管理", "空房查询与预订", "预订与入住", "退房结算", "客人与订单查询"};
        String[] descriptions = {"客房新增、修改、删除及房态查询", "按日期查询空房、登记预订与检测日期冲突",
                "查询和取消预订、预订转入住及直接入住", "查询在住订单、计算房费及办理退房",
                "按客人姓名、手机号、证件号、房号与订单状态查询"};
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            JPanel page = new JPanel(new BorderLayout(0, 20));
            page.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
            JLabel title = new JLabel(name); title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
            page.add(title, BorderLayout.NORTH);
            page.add(new JLabel("<html>" + descriptions[i] + "<br><br>本页面将在后续开发阶段实现。</html>"), BorderLayout.CENTER);
            content.add(page, name);
            JButton button = new JButton(name);
            button.addActionListener(event -> layout.show(content, name));
            menu.add(button);
        }
        JPanel sidebar = new JPanel(new BorderLayout()); sidebar.add(menu, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST); add(content, BorderLayout.CENTER);
    }
}
