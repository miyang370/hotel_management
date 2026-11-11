package com.hotel.ui;

import com.hotel.service.OrderQueryService;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class OrderQueryPanel extends JPanel {
    private final JTextField name=new JTextField(10),phone=new JTextField(10),document=new JTextField(10),room=new JTextField(10),from=new JTextField(10),to=new JTextField(10);
    private final JComboBox<String> status=new JComboBox<>(new String[]{"全部","已预订","已入住","已退房","已取消"});
    private final JButton search=new JButton("查询"),reset=new JButton("重置"),detail=new JButton("查看详情");
    private final AuthStyle.Notice notice=new AuthStyle.Notice("日期按预计入住日期筛选，格式 yyyy-MM-dd，可留空；各项条件同时生效。");
    private final DefaultTableModel model=new DefaultTableModel(new String[]{"订单号","房号","客人姓名","手机号","证件号（脱敏）","预计入住","预计离店","订单状态","结算金额"},0){public boolean isCellEditable(int r,int c){return false;}};
    private final JTable table=new JTable(model);
    private List<OrderQueryService.Record> rows=List.of();
    public OrderQueryPanel(){
        super(new BorderLayout(0,16));setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        JPanel top=new JPanel(new BorderLayout(0,16));JLabel title=new JLabel("客人与订单查询");title.setFont(title.getFont().deriveFont(Font.BOLD,20));top.add(title,BorderLayout.NORTH);
        JPanel fields=new JPanel(new GridLayout(0,4,12,12));fields.setBackground(Color.WHITE);fields.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        String[] labels={"姓名","手机号","证件号","房号","入住开始日期","入住结束日期","订单状态"};
        JComponent[] inputs={name,phone,document,room,from,to,status};
        for(int i=0;i<labels.length;i++){fields.add(new JLabel(labels[i],SwingConstants.RIGHT));fields.add(inputs[i]);}
        top.add(fields,BorderLayout.CENTER);JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,12,0));
        UiTheme.button(search,true);UiTheme.button(reset,false);UiTheme.button(detail,false);actions.add(search);actions.add(reset);actions.add(detail);top.add(actions,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
        UiTheme.table(table);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.getTableHeader().setReorderingAllowed(false);add(new JScrollPane(table),BorderLayout.CENTER);add(notice,BorderLayout.SOUTH);
        search.addActionListener(e->reload());reset.addActionListener(e->{for(var f:new JTextField[]{name,phone,document,room,from,to})f.setText("");status.setSelectedIndex(0);reload();});detail.addActionListener(e->showDetail());reload();
    }
    private LocalDate date(JTextField field){return field.getText().isBlank()?null:LocalDate.parse(field.getText().trim());}
    private void reload(){
        OrderQueryService.Filter filter;
        try{filter=new OrderQueryService.Filter(name.getText().trim(),phone.getText().trim(),document.getText().trim(),room.getText().trim(),date(from),date(to),new String[]{"","RESERVED","CHECKED_IN","CHECKED_OUT","CANCELLED"}[status.getSelectedIndex()]);
            if(filter.from()!=null&&filter.to()!=null&&filter.to().isBefore(filter.from()))throw new IllegalArgumentException("结束日期不能早于开始日期");
        }catch(java.time.format.DateTimeParseException e){notice.error("日期格式须为 yyyy-MM-dd，请输入有效日期");return;}catch(IllegalArgumentException e){notice.error(e.getMessage());return;}
        search.setEnabled(false);reset.setEnabled(false);detail.setEnabled(false);notice.info("正在查询……");
        new SwingWorker<List<OrderQueryService.Record>,Void>(){
            protected List<OrderQueryService.Record> doInBackground()throws Exception{return new OrderQueryService().search(filter);}
            protected void done(){try{rows=get();model.setRowCount(0);for(var r:rows)model.addRow(new Object[]{r.number(),r.room(),r.name(),r.phone(),OrderQueryService.mask(r.document()),r.arrival(),r.departure(),OrderQueryService.statusLabel(r.status()),r.total()==null?"未结算":r.total().toPlainString()});notice.info("查询完成，共"+rows.size()+"条记录；日期范围按预计入住日期筛选。");}catch(Exception e){rows=List.of();model.setRowCount(0);notice.error("查询失败，请检查数据库连接和数据表");}finally{search.setEnabled(true);reset.setEnabled(true);detail.setEnabled(true);}}
        }.execute();
    }
    private void showDetail(){
        int index=table.getSelectedRow();if(index<0){notice.error("请先选择一条订单");return;}var r=rows.get(table.convertRowIndexToModel(index));
        JTextArea text=new JTextArea("订单号："+r.number()+"\n房号："+r.room()+"\n客人姓名："+r.name()+"\n手机号："+r.phone()+"\n证件号："+OrderQueryService.mask(r.document())+"\n预计入住："+r.arrival()+"\n预计离店："+r.departure()+"\n实际入住："+(r.actualArrival()==null?"未入住":r.actualArrival())+"\n实际退房："+(r.actualDeparture()==null?"未退房":r.actualDeparture())+"\n成交房价："+r.price()+" 元/晚\n结算金额："+(r.total()==null?"未结算":r.total()+" 元")+"\n订单状态："+OrderQueryService.statusLabel(r.status()));
        text.setEditable(false);text.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));text.setFont(new Font("Microsoft YaHei UI",Font.PLAIN,14));text.setForeground(UiTheme.TEXT);
        JScrollPane pane=new JScrollPane(text);pane.setPreferredSize(new Dimension(600,430));JOptionPane.showMessageDialog(this,pane,"订单详情",JOptionPane.PLAIN_MESSAGE);
    }
}
