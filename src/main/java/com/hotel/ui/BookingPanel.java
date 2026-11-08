package com.hotel.ui;

import com.hotel.entity.Room;
import com.hotel.service.BookingService;
import java.awt.*;
import java.time.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class BookingPanel extends JPanel {
    private final JTextField arrival = new JTextField(LocalDate.now(ZoneId.of("Asia/Shanghai")).toString(), 10);
    private final JTextField departure = new JTextField(LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1).toString(), 10);
    private final JTextField guest = new JTextField(12), phone = new JTextField(12), document = new JTextField(16);
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"房号", "房型", "每日价格（元）"}, 0) {
        public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JButton search = new JButton("查询空房"), reserve = new JButton("确认预订");
    private final AuthStyle.Notice notice = new AuthStyle.Notice("日期格式：2026-11-08。请先查询空房，再选择客房填写客人信息。");
    private List<Room> rooms = List.of();
    private LocalDate queriedIn, queriedOut;
    private final BookingService service = new BookingService();

    public BookingPanel() {
        super(new BorderLayout(0, 16)); setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JPanel heading = new JPanel(new BorderLayout(0, 16));
        JLabel title = new JLabel("空房查询与预订"); title.setFont(title.getFont().deriveFont(Font.BOLD, 20)); title.setForeground(UiTheme.TITLE);
        heading.add(title, BorderLayout.NORTH);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12)); filters.setBackground(Color.WHITE);
        filters.add(new JLabel("入住日期")); filters.add(arrival); filters.add(new JLabel("离店日期")); filters.add(departure);
        UiTheme.button(search, true); filters.add(search); heading.add(filters, BorderLayout.CENTER); add(heading, BorderLayout.NORTH);
        UiTheme.table(table); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.getTableHeader().setReorderingAllowed(false);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12)); form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16)); form.setBackground(Color.WHITE);
        form.add(new JLabel("客人姓名", SwingConstants.RIGHT)); form.add(guest);
        form.add(new JLabel("联系电话", SwingConstants.RIGHT)); form.add(phone);
        form.add(new JLabel("证件号码", SwingConstants.RIGHT)); form.add(document);
        UiTheme.button(reserve, true); reserve.setEnabled(false);
        bottom.add(form, BorderLayout.NORTH); bottom.add(reserve, BorderLayout.CENTER); bottom.add(notice, BorderLayout.SOUTH); add(bottom, BorderLayout.SOUTH);
        search.addActionListener(e -> query()); reserve.addActionListener(e -> book());
        javax.swing.event.DocumentListener changed = new javax.swing.event.DocumentListener() {
            private void invalidateResults() { rooms = List.of(); model.setRowCount(0); reserve.setEnabled(false); queriedIn = null; queriedOut = null; }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { invalidateResults(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { invalidateResults(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { invalidateResults(); }
        };
        arrival.getDocument().addDocumentListener(changed); departure.getDocument().addDocumentListener(changed);
    }
    private void query() {
        try {
            LocalDate in = LocalDate.parse(arrival.getText().trim()), out = LocalDate.parse(departure.getText().trim());
            BookingService.validateDates(in, out);
            task(() -> service.available(in, out), result -> {
                rooms = result; queriedIn = in; queriedOut = out; model.setRowCount(0);
                for (Room r : rooms) model.addRow(new Object[]{r.roomNumber(), r.roomType(), r.dailyPrice().toPlainString()});
                reserve.setEnabled(!rooms.isEmpty()); notice.info("找到 " + rooms.size() + " 间可订客房，请选择客房后提交预订");
            });
        } catch (Exception e) { notice.error(e instanceof java.time.format.DateTimeParseException ? "请按 yyyy-MM-dd 输入有效日期" : e.getMessage()); }
    }
    private void book() {
        int row = table.getSelectedRow();
        if (row < 0 || queriedIn == null) { notice.error("请先查询并选择一间客房"); return; }
        Room room = rooms.get(table.convertRowIndexToModel(row));
        String name = guest.getText().trim(), tel = phone.getText().trim(), id = document.getText().trim();
        LocalDate in = queriedIn, out = queriedOut;
        task(() -> service.reserve(room, in, out, name, tel, id), number -> {
            rooms = List.of(); model.setRowCount(0); reserve.setEnabled(false);
            guest.setText(""); phone.setText(""); document.setText("");
            notice.info("预订成功，订单号：" + number + "。继续预订请重新查询。");
        });
    }
    private <T> void task(java.util.concurrent.Callable<T> work, java.util.function.Consumer<T> success) {
        search.setEnabled(false); reserve.setEnabled(false); arrival.setEnabled(false); departure.setEnabled(false);
        notice.info("正在处理，请稍候……");
        new SwingWorker<T, Void>() {
            protected T doInBackground() throws Exception { return work.call(); }
            protected void done() {
                search.setEnabled(true); arrival.setEnabled(true); departure.setEnabled(true); reserve.setEnabled(!rooms.isEmpty());
                try { success.accept(get()); }
                catch (Exception e) {
                    Throwable cause = e instanceof java.util.concurrent.ExecutionException ? e.getCause() : e;
                    notice.error(cause instanceof IllegalArgumentException ? cause.getMessage() : "操作失败，请检查数据库连接、驱动及数据表；可通过订单号核查预订结果");
                }
            }
        }.execute();
    }
}
