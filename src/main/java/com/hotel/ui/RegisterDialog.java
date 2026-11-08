package com.hotel.ui;

import com.hotel.service.AuthService;
import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;

public final class RegisterDialog extends JDialog {
    private final JTextField username = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JPasswordField confirmation = new JPasswordField(22);
    private final JButton submit = new JButton("确认注册");
    private final AuthStyle.Notice message = new AuthStyle.Notice("账号为3～50位字母、数字或下划线，密码为8～128位");
    private boolean busy;

    public RegisterDialog(JFrame owner, Consumer<String> onRegistered) {
        super(owner, "注册管理员账号", true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent event) {
                if (!busy) dispose();
            }
        });
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        JComponent[] fields = {username, password, confirmation};
        String[] labels = {"账号", "密码", "确认密码"};
        for (int i = 0; i < fields.length; i++) {
            c.gridy = i; c.gridx = 0; form.add(new JLabel(labels[i]), c);
            c.gridx = 1; form.add(fields[i], c);
        }
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; form.add(message, c);
        c.gridy = 4; form.add(submit, c);
        JButton back = new JButton("返回登录");
        c.gridy = 5; form.add(back, c);
        back.addActionListener(event -> { if (!busy) dispose(); });
        submit.addActionListener(event -> register(onRegistered));
        setContentPane(AuthStyle.shell("创建管理员账号", "填写以下信息，注册后即可返回登录", form));
        getRootPane().setDefaultButton(submit);
        pack(); setSize(Math.max(800, getWidth()), Math.max(740, getHeight()));
        setMinimumSize(getSize()); setLocationRelativeTo(owner);
    }

    private void register(Consumer<String> onRegistered) {
        String name = username.getText().trim();
        char[] secret = password.getPassword();
        char[] repeated = confirmation.getPassword();
        String error = null;
        if (!name.matches("[A-Za-z0-9_]{3,50}")) error = "账号须为3～50位字母、数字或下划线";
        else if (secret.length < 8 || secret.length > 128) error = "密码长度须为8～128位";
        else if (!Arrays.equals(secret, repeated)) error = "两次输入的密码不一致";
        Arrays.fill(repeated, '\0');
        if (error != null) {
            Arrays.fill(secret, '\0');
            message.error(error);
            return;
        }
        busy = true;
        submit.setEnabled(false); username.setEnabled(false);
        password.setEnabled(false); confirmation.setEnabled(false);
        message.info("正在注册，请稍候……");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                try { new AuthService().createAdmin(name, secret); return null; }
                finally { Arrays.fill(secret, '\0'); }
            }
            @Override protected void done() {
                try {
                    get();
                    dispose();
                    onRegistered.accept(name);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    message.error("注册被中断，请稍后重试");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof SQLException sql && sql.getErrorCode() == 1062)
                        message.error("该账号已注册，请更换账号或返回登录");
                    else if (cause instanceof IllegalArgumentException)
                        message.error(cause.getMessage());
                    else message.error("注册失败，请检查数据库配置、驱动和数据表");
                } finally {
                    busy = false;
                    submit.setEnabled(true); username.setEnabled(true);
                    password.setEnabled(true); confirmation.setEnabled(true);
                    password.setText(""); confirmation.setText("");
                }
            }
        }.execute();
    }
}
