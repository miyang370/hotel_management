package com.hotel.ui;

import com.hotel.service.CheckoutService;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class CheckoutPanel extends JPanel {
    private final CheckoutService service=new CheckoutService();
    private final JTextField keyword=new JTextField(18);
    private final JButton search=new JButton("查询在住订单"), checkout=new JButton("确认收款并退房");
    private final AuthStyle.Notice notice=new AuthStyle.Notice("按实际入住至退房的日期差计费，至少一晚；请核对收款后确认退房。");
    private final DefaultTableModel model=new DefaultTableModel(new String[]{"订单号","房号","客人","实际入住","预计离店","每晚房价","计费晚数","应收金额"},0){public boolean isCellEditable(int r,int c){return false;}};
    private final JTable table=new JTable(model);
    private List<CheckoutService.Bill> rows=List.of();
    public CheckoutPanel(){
        super(new BorderLayout(0,16));setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        JPanel top=new JPanel(new BorderLayout(0,16));JLabel title=new JLabel("退房结算");title.setFont(title.getFont().deriveFont(Font.BOLD,20));top.add(title,BorderLayout.NORTH);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,12,12));actions.add(new JLabel("房号 / 姓名 / 订单号"));actions.add(keyword);
        UiTheme.button(search,true);UiTheme.button(checkout,true);actions.add(search);actions.add(checkout);top.add(actions,BorderLayout.CENTER);add(top,BorderLayout.NORTH);
        UiTheme.table(table);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);add(new JScrollPane(table),BorderLayout.CENTER);add(notice,BorderLayout.SOUTH);
        search.addActionListener(e->reload());checkout.addActionListener(e->settle());reload();
    }
    private void reload(){String text=keyword.getText().trim();task(()->service.search(text),result->{rows=result;model.setRowCount(0);for(var r:rows)model.addRow(new Object[]{r.number(),r.room(),r.guest(),r.arrival(),r.plannedDeparture(),r.price(),r.nights(),r.total()});notice.info("共"+rows.size()+"条在住订单；金额按当前日期预估，提交时再次核算。");});}
    private void settle(){
        int index=table.getSelectedRow();if(index<0){notice.error("请选择需要退房的订单");return;}
        var bill=rows.get(table.convertRowIndexToModel(index));
        String warning=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).isAfter(bill.plannedDeparture())?"已超过预计离店日期，请核对后续预订并与客人确认。\n":"";
        if(JOptionPane.showConfirmDialog(this,warning+"房号："+bill.room()+"\n客人："+bill.guest()+"\n计费："+bill.nights()+"晚\n应收："+bill.total()+"元\n确认已收款并办理退房？","结算确认",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        task(()->service.checkout(bill.id(),bill.total()),result->{rows=List.of();model.setRowCount(0);notice.info("退房成功，房间已恢复空闲。订单号："+result.number()+"，结算金额："+result.total()+"元。请查询刷新列表。");});
    }
    private <T> void task(java.util.concurrent.Callable<T> work,java.util.function.Consumer<T> success){
        search.setEnabled(false);checkout.setEnabled(false);notice.info("正在处理……");new SwingWorker<T,Void>(){
            protected T doInBackground()throws Exception{return work.call();}
            protected void done(){search.setEnabled(true);checkout.setEnabled(true);try{success.accept(get());}catch(Exception e){Throwable cause=e instanceof java.util.concurrent.ExecutionException?e.getCause():e;notice.error(cause instanceof IllegalArgumentException?cause.getMessage():"结算操作失败，请检查连接并刷新核查订单状态，勿重复收款");}}
        }.execute();
    }
}
