package com.hotel.ui;

import com.hotel.entity.Room;
import com.hotel.service.RoomService;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class RoomPanel extends JPanel {
    private final RoomService service = new RoomService();
    private final JTextField number = new JTextField(9), type = new JTextField(9);
    private final JComboBox<String> status = new JComboBox<>(new String[]{"全部", "空闲", "已入住"});
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"房号", "房型", "每日价格（元）", "房态"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);
    private final AuthStyle.Notice notice = new AuthStyle.Notice("请选择查询条件，或新增客房");
    private final java.util.List<JButton> buttons = new java.util.ArrayList<>();
    private List<Room> rooms = List.of();
    private boolean busy;

    public RoomPanel() {
        super(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JLabel title = new JLabel("客房管理");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20)); title.setForeground(UiTheme.TITLE);
        JPanel top = new JPanel(new BorderLayout(0, 16)); top.add(title, BorderLayout.NORTH);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filters.setBackground(Color.WHITE);
        filters.add(new JLabel("房号")); filters.add(number);
        filters.add(new JLabel("房型")); filters.add(type);
        filters.add(new JLabel("房态")); filters.add(status);
        filters.add(button("查询", true, this::reload));
        filters.add(button("重置", false, () -> { number.setText(""); type.setText(""); status.setSelectedIndex(0); reload(); }));
        top.add(filters, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.add(button("新增客房", true, () -> edit(null)));
        actions.add(button("修改", false, () -> { Room r = selected(); if (r != null) edit(r); }));
        actions.add(button("删除", false, this::delete)); top.add(actions, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        UiTheme.table(table); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        add(new JScrollPane(table), BorderLayout.CENTER); add(notice, BorderLayout.SOUTH);
        reload();
    }
    private JButton button(String text, boolean primary, Runnable action) {
        JButton b = new JButton(text); UiTheme.button(b, primary);
        b.addActionListener(e -> { if (!busy) action.run(); }); buttons.add(b); return b;
    }
    private Room selected() {
        int row = table.getSelectedRow();
        if (row < 0) { notice.error("请先选择一间客房"); return null; }
        return rooms.get(table.convertRowIndexToModel(row));
    }
    private void reload() {
        String n = number.getText().trim(), t = type.getText().trim();
        String s = switch (status.getSelectedIndex()) { case 1 -> "AVAILABLE"; case 2 -> "OCCUPIED"; default -> ""; };
        task(() -> service.search(n, t, s), result -> {
            rooms = result; model.setRowCount(0);
            for (Room r : rooms) model.addRow(new Object[]{r.roomNumber(), r.roomType(), r.dailyPrice().toPlainString(), r.status().equals("AVAILABLE") ? "空闲" : "已入住"});
            notice.info("查询完成，共 " + rooms.size() + " 间客房");
        });
    }
    private void edit(Room room) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), room == null ? "新增客房" : "修改客房", Dialog.ModalityType.APPLICATION_MODAL);
        JTextField no = new JTextField(room == null ? "" : room.roomNumber(), 20);
        JTextField kind = new JTextField(room == null ? "" : room.roomType(), 20);
        JTextField price = new JTextField(room == null ? "" : room.dailyPrice().toPlainString(), 20);
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 16));
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        form.add(new JLabel("房号", SwingConstants.RIGHT)); form.add(no);
        form.add(new JLabel("房型", SwingConstants.RIGHT)); form.add(kind);
        form.add(new JLabel("每日价格（元）", SwingConstants.RIGHT)); form.add(price);
        AuthStyle.Notice error = new AuthStyle.Notice("新客房默认空闲；房态由入住和退房业务维护");
        JButton save = new JButton("保存"); UiTheme.button(save, true);
        save.addActionListener(e -> {
            BigDecimal amount;
            try { amount = new BigDecimal(price.getText().trim()); }
            catch (NumberFormatException ex) { error.error("请输入有效的房价数字"); return; }
            String n = no.getText().trim(), t = kind.getText().trim();
            save.setEnabled(false); no.setEnabled(false); kind.setEnabled(false); price.setEnabled(false);
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            new SwingWorker<Void, Void>() {
                protected Void doInBackground() throws Exception { service.save(room == null ? null : room.id(), n, t, amount); return null; }
                protected void done() {
                    try { get(); dialog.dispose(); reload(); }
                    catch (Exception ex) { error.error(friendly(ex)); }
                    finally { save.setEnabled(true); no.setEnabled(true); kind.setEnabled(true); price.setEnabled(true); dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); }
                }
            }.execute();
        });
        JPanel footer = new JPanel(new BorderLayout(0, 12)); footer.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
        footer.add(error, BorderLayout.NORTH); footer.add(save, BorderLayout.SOUTH);
        dialog.add(form, BorderLayout.CENTER); dialog.add(footer, BorderLayout.SOUTH);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); dialog.pack(); dialog.setMinimumSize(new Dimension(650, 320));
        dialog.setSize(dialog.getMinimumSize()); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }
    private void delete() {
        Room r = selected(); if (r == null) return;
        if (JOptionPane.showConfirmDialog(this, "确认删除客房 " + r.roomNumber() + "？有关联订单的客房不能删除。", "删除确认", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        task(() -> { service.delete(r.id()); return true; }, ignored -> reload());
    }
    private <T> void task(java.util.concurrent.Callable<T> work, java.util.function.Consumer<T> success) {
        busy = true; buttons.forEach(b -> b.setEnabled(false)); notice.info("正在处理，请稍候……");
        new SwingWorker<T, Void>() {
            protected T doInBackground() throws Exception { return work.call(); }
            protected void done() {
                busy = false; buttons.forEach(b -> b.setEnabled(true));
                try { success.accept(get()); } catch (Exception e) { notice.error(friendly(e)); }
            }
        }.execute();
    }
    private static String friendly(Exception e) {
        Throwable cause = e instanceof java.util.concurrent.ExecutionException ? e.getCause() : e;
        return cause instanceof IllegalArgumentException ? cause.getMessage() : "操作失败，请检查数据库连接、驱动及数据表";
    }
}
