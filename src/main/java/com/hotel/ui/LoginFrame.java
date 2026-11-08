package com.hotel.ui;

import com.hotel.service.AuthService;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

public final class LoginFrame extends JFrame {
    private final JTextField username = new JTextField(20);
    private final JPasswordField password = new JPasswordField(20);
    private final JButton login = new JButton("登录");
    private final AuthStyle.Notice message = new AuthStyle.Notice("请输入管理员账号和密码");

    public LoginFrame() {
        super("酒店预订管理系统 · 管理员登录");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; form.add(new JLabel("账号"), c);
        c.gridx = 1; form.add(username, c);
        c.gridx = 0; c.gridy = 1; form.add(new JLabel("密码"), c);
        c.gridx = 1; form.add(password, c);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; form.add(message, c);
        c.gridy = 3; form.add(login, c);
        JButton register = new JButton("注册账号");
        c.gridy = 4; form.add(register, c);
        register.addActionListener(event -> new RegisterDialog(this, name -> {
            username.setText(name);
            password.setText("");
            message.info("注册成功，请输入密码登录");
            password.requestFocusInWindow();
        }).setVisible(true));
        setContentPane(AuthStyle.shell("欢迎登录", "登录管理员账号，开始处理酒店业务", form));
        getRootPane().setDefaultButton(login);
        login.addActionListener(event -> submit());
        pack(); setSize(Math.max(760, getWidth()), Math.max(620, getHeight()));
        setMinimumSize(getSize()); setLocationRelativeTo(null);
    }

    private void submit() {
        String name = username.getText().trim();
        char[] secret = password.getPassword();
        if (name.isEmpty() || secret.length == 0 || name.length() > 50 || secret.length > 128) {
            Arrays.fill(secret, '\0');
            message.error("请输入有效账号和密码（账号≤50，密码≤128位）");
            return;
        }
        login.setEnabled(false); username.setEnabled(false); password.setEnabled(false);
        message.info("正在验证，请稍候……");
        new SwingWorker<Boolean, Void>() {
            protected Boolean doInBackground() throws Exception {
                try { return new AuthService().login(name, secret); }
                finally { Arrays.fill(secret, '\0'); }
            }
            protected void done() {
                try {
                    if (get()) {
                        new MainFrame(name).setVisible(true);
                        dispose();
                    } else {
                        message.error("账号或密码错误");
                    }
                } catch (Exception e) {
                    message.error("连接失败，请检查数据库配置、驱动和数据表");
                } finally {
                    login.setEnabled(true); username.setEnabled(true); password.setEnabled(true);
                    password.setText(""); password.requestFocusInWindow();
                }
            }
        }.execute();
    }
}
