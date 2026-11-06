package com.hotel;

import com.hotel.service.AuthService;
import java.util.Arrays;

/** Run from a terminal so passwords are never passed as command-line arguments. */
public final class SetupAdmin {
    public static void main(String[] args) {
        var console = System.console();
        if (console == null) {
            System.err.println("请在 PowerShell 或 CMD 终端中运行管理员初始化命令。");
            System.exit(1);
        }
        String username = console.readLine("管理员账号（3～50位字母、数字、下划线）：");
        char[] password = console.readPassword("密码（8～128位）：");
        char[] confirmation = console.readPassword("再次输入密码：");
        try {
            if (username == null || password == null || confirmation == null)
                throw new IllegalArgumentException("输入已取消");
            if (!Arrays.equals(password, confirmation)) throw new IllegalArgumentException("两次密码不一致");
            new AuthService().createAdmin(username.trim(), password);
            System.out.println("管理员创建成功，可以启动系统登录。");
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage()); System.exit(1);
        } catch (Exception e) {
            System.err.println("创建失败：请检查数据库配置及数据表，并确认账号未被使用。");
            System.exit(1);
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }
}
