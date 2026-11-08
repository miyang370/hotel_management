package com.hotel.ui;

import com.hotel.service.StayService;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class StayPanel extends JPanel {
    private final StayService service=new StayService();
    private final JTextField keyword=new JTextField(18);
    private final DefaultTableModel model=new DefaultTableModel(new String[]{"订单号","房号","姓名","电话","入住日期","离店日期","状态"},0) {
        public boolean isCellEditable(int r,int c) { return false; }
    };
    private final JTable table=new JTable(model);
    private final AuthStyle.Notice notice=new AuthStyle.Notice("查询预订后选择订单，可取消预订或办理当天入住");
    private final java.util.List<JButton> buttons=new java.util.ArrayList<>();
    private List<StayService.Order> rows=List.of();
    public StayPanel() {
        super(new BorderLayout(0,16)); setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        JPanel top=new JPanel(new BorderLayout(0,16));
        JLabel title=new JLabel("预订与入住"); title.setFont(title.getFont().deriveFont(Font.BOLD,20)); top.add(title,BorderLayout.NORTH);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,10,10));
        actions.add(new JLabel("订单号 / 姓名 / 房号")); actions.add(keyword);
        actions.add(button("查询",this::reload));
        actions.add(button("取消预订",()->change(false))); actions.add(button("办理入住",()->change(true)));
        actions.add(button("直接入住",()->{
            JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(this),"直接入住",Dialog.ModalityType.APPLICATION_MODAL);
            dialog.add(new BookingPanel(true)); dialog.setSize(950,720); dialog.setLocationRelativeTo(this); dialog.setVisible(true); reload();
        }));
        top.add(actions,BorderLayout.CENTER); add(top,BorderLayout.NORTH);
        UiTheme.table(table); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); add(new JScrollPane(table),BorderLayout.CENTER); add(notice,BorderLayout.SOUTH);
        reload();
    }
    private JButton button(String name,Runnable action) { JButton b=new JButton(name); UiTheme.button(b,name.equals("查询")||name.equals("办理入住")); b.addActionListener(e->action.run()); buttons.add(b); return b; }
    private void reload() { String text=keyword.getText().trim(); task(()->service.search(text), result->{rows=result; model.setRowCount(0); for(var r:rows) model.addRow(new Object[]{r.number(),r.room(),r.guest(),r.phone(),r.arrival(),r.departure(),label(r.status())}); notice.info("查询完成，共"+rows.size()+"条记录");}); }
    private String label(String status) { return switch(status) { case "RESERVED"->"已预订"; case "CHECKED_IN"->"已入住"; case "CANCELLED"->"已取消"; default->"已退房"; }; }
    private void change(boolean checkIn) {
        int index=table.getSelectedRow(); if(index<0) {notice.error("请先选择订单"); return;}
        var row=rows.get(table.convertRowIndexToModel(index));
        if(!row.status().equals("RESERVED")) {notice.error("仅已预订订单可执行此操作"); return;}
        if(JOptionPane.showConfirmDialog(this,(checkIn?"确认办理入住？":"确认取消此预订？"),"操作确认",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        task(()->{if(checkIn)service.checkIn(row.id());else service.cancel(row.id());return true;},ignored->reload());
    }
    private <T> void task(java.util.concurrent.Callable<T> work,java.util.function.Consumer<T> success) {
        buttons.forEach(b->b.setEnabled(false)); notice.info("正在处理……");
        new SwingWorker<T,Void>() {
            protected T doInBackground() throws Exception{return work.call();}
            protected void done(){buttons.forEach(b->b.setEnabled(true));try{success.accept(get());}catch(Exception e){Throwable c=e instanceof java.util.concurrent.ExecutionException?e.getCause():e;notice.error(c instanceof IllegalArgumentException?c.getMessage():"操作失败，请检查数据库连接并刷新记录核查结果");}}
        }.execute();
    }
}
