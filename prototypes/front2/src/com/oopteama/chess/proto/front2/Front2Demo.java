package com.oopteama.chess.proto.front2;

import javax.swing.*;
import java.awt.*;

public class Front2Demo {
    private final JFrame frame=new JFrame("CHESS ACADEMY  |  프2 학습 화면 미리보기");
    private final DemoState state=new DemoState();
    private final CardLayout cards=new CardLayout();
    private final JPanel content=new JPanel(cards);
    private final JLabel stats=DemoTheme.label("",13,DemoTheme.MUTED,false);
    private WrongNotesScreen wrong;
    private final String[] names={"레슨 학습","용어 퀴즈","전술 퍼즐","오답 노트"};
    private final String[] keys={"LESSON","QUIZ","PUZZLE","WRONG"};
    private final JButton[] menu=new JButton[4];
    private int active=0;
    public Front2Demo() {
        DemoTheme.setDefaults();frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(980,680));frame.setSize(1260,830);frame.setLocationRelativeTo(null);
        JPanel root=new JPanel(new BorderLayout());root.setBackground(DemoTheme.BG);
        JPanel sidebar=new JPanel(new BorderLayout());sidebar.setBackground(DemoTheme.SIDEBAR);sidebar.setPreferredSize(new Dimension(245,800));
        DemoTheme.inset(sidebar,34,20,26,20);
        JPanel logo=DemoTheme.transparent(new GridLayout(3,1,0,3));
        logo.add(DemoTheme.label("♞  CHESS",27,DemoTheme.GOLD,true));
        logo.add(DemoTheme.label("ACADEMY",18,DemoTheme.CREAM,true));
        logo.add(DemoTheme.label("LEARNING STUDIO / FRONT 2",11,DemoTheme.MUTED,false));
        sidebar.add(logo,BorderLayout.NORTH);
        JPanel nav=DemoTheme.transparent(new GridLayout(4,1,0,12));
        for(int i=0;i<menu.length;i++) {
            final int n=i;menu[i]=DemoTheme.button(new String[]{"01   레슨 학습","02   용어 퀴즈","03   전술 퍼즐","04   오답 노트"}[i],i==0);
            menu[i].setHorizontalAlignment(SwingConstants.LEFT);
            menu[i].addActionListener(e->show(n));nav.add(menu[i]);
        }
        JPanel centerNav=DemoTheme.transparent(new BorderLayout());centerNav.add(nav,BorderLayout.NORTH);DemoTheme.inset(centerNav,42,0,0,0);
        sidebar.add(centerNav,BorderLayout.CENTER);
        JPanel footer=DemoTheme.transparent(new GridLayout(4,1,0,8));
        footer.add(DemoTheme.label("MY LEARNING",12,DemoTheme.GOLD,true));
        footer.add(stats);
        footer.add(DemoTheme.label("Java Swing · 독립 데모",12,DemoTheme.MUTED,false));
        footer.add(DemoTheme.label("팀 통합 전 UI 검증용",11,DemoTheme.MUTED,false));
        sidebar.add(footer,BorderLayout.SOUTH);root.add(sidebar,BorderLayout.WEST);
        JPanel main=new JPanel(new BorderLayout());main.setBackground(DemoTheme.BG);
        JPanel header=new JPanel(new BorderLayout());header.setBackground(DemoTheme.BG);DemoTheme.inset(header,25,30,15,30);
        header.add(DemoTheme.label("배우고 · 풀고 · 다시 익히는 체스",19,DemoTheme.CREAM,true),BorderLayout.WEST);
        header.add(DemoTheme.label("FRONTEND PREVIEW  v1.0",12,DemoTheme.GOLD,true),BorderLayout.EAST);
        main.add(header,BorderLayout.NORTH);
        content.setBackground(DemoTheme.BG);content.add(new LessonScreen(state,this::refresh),keys[0]);
        content.add(new TermQuizScreen(state,this::refresh),keys[1]);
        content.add(new PuzzleScreen(state,this::refresh),keys[2]);
        wrong=new WrongNotesScreen(state,this::refresh);content.add(wrong,keys[3]);main.add(content,BorderLayout.CENTER);
        root.add(main,BorderLayout.CENTER);frame.setContentPane(root);refresh();
    }
    private void refresh() {
        stats.setText("레슨 "+state.completedLessons()+"/"+DemoData.LESSONS.length+"   ·   퍼즐 "+state.solvedPuzzles.size()+"/"+DemoData.PUZZLES.length);
    }
    private void show(int index) {
        active=index;cards.show(content,keys[index]);
        for(int i=0;i<menu.length;i++){menu[i].setBackground(i==index?DemoTheme.GOLD:DemoTheme.PANEL2);menu[i].setForeground(i==index?DemoTheme.BG:DemoTheme.CREAM);}
        if(index==3)wrong.refresh();
    }
    public void display() {frame.setVisible(true);}
    public static void main(String[] args) {SwingUtilities.invokeLater(()->new Front2Demo().display());}
}
