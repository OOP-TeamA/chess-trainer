package com.oopteama.chess.ui;

import javax.swing.*;
import java.awt.*;

public class PuzzleScreen extends JPanel {
    private final DemoState state; private final Runnable onChange;
    private final BoardView board=new BoardView();
    private final JLabel heading=DemoTheme.label("",24,DemoTheme.CREAM,true);
    private final JTextArea prompt=new JTextArea();
    private final JLabel message=DemoTheme.label("기물을 선택한 뒤 도착 칸을 클릭하세요.",14,DemoTheme.MUTED,false);
    private final JLabel counter=DemoTheme.label("",13,DemoTheme.GOLD,true);
    private final JButton prev=DemoTheme.button("← 이전",false),next=DemoTheme.button("다음 →",true),hint=DemoTheme.button("힌트",false),reset=DemoTheme.button("초기화",false);
    private int index=0; private boolean solved=false;
    public PuzzleScreen(DemoState state,Runnable onChange) {
        this.state=state;this.onChange=onChange;setOpaque(false);setLayout(new BorderLayout(20,0));DemoTheme.inset(this,24,28,26,28);
        JPanel boardCard=DemoTheme.card(new BorderLayout());boardCard.add(board,BorderLayout.CENTER);add(boardCard,BorderLayout.CENTER);
        JPanel right=DemoTheme.card(new BorderLayout(0,20));right.setPreferredSize(new Dimension(340,500));
        JPanel top=DemoTheme.transparent(new GridLayout(3,1,0,10));top.add(DemoTheme.label("TACTICAL PRACTICE",12,DemoTheme.GOLD,true));top.add(counter);top.add(heading);
        right.add(top,BorderLayout.NORTH);
        prompt.setEditable(false);prompt.setLineWrap(true);prompt.setWrapStyleWord(true);prompt.setFocusable(false);
        prompt.setFont(DemoTheme.font(17,false));prompt.setOpaque(false);prompt.setForeground(DemoTheme.CREAM);
        JPanel center=DemoTheme.transparent(new BorderLayout(0,18));center.add(prompt,BorderLayout.NORTH);
        JTextArea help=new JTextArea("[사용 방법]\n1. 체스판에서 기물을 클릭합니다.\n2. 도착할 칸을 클릭합니다.\n3. 정답 수와 일치하는지 확인합니다.\n\n※ 데모에서는 지정된 목표 수만 판정하며 전체 체스 규칙 엔진은 포함되지 않습니다.");
        help.setEditable(false);help.setFocusable(false);help.setOpaque(false);help.setForeground(DemoTheme.MUTED);help.setFont(DemoTheme.font(13,false));center.add(help,BorderLayout.CENTER);
        right.add(center,BorderLayout.CENTER);
        JPanel bottom=DemoTheme.transparent(new BorderLayout(0,15));
        JPanel controls=DemoTheme.transparent(new GridLayout(2,2,10,10));controls.add(prev);controls.add(next);controls.add(hint);controls.add(reset);
        bottom.add(message,BorderLayout.NORTH);bottom.add(controls,BorderLayout.SOUTH);right.add(bottom,BorderLayout.SOUTH);
        add(right,BorderLayout.EAST);
        prev.addActionListener(e->{if(index>0){index--;load();}}); next.addActionListener(e->{if(index<DemoData.PUZZLES.length-1){index++;load();}});
        hint.addActionListener(e->{DemoData.Puzzle p=DemoData.PUZZLES[index];message.setForeground(DemoTheme.GOLD);message.setText("힌트: "+p.from+" → ?");});
        reset.addActionListener(e->load());
        board.setMoveHandler((from,to)->{
            DemoData.Puzzle p=DemoData.PUZZLES[index];
            if(from.equals(p.from)&&to.equals(p.to)) {solved=true;board.showMove(from,to);state.solvedPuzzles.add(index);onChange.run();message.setForeground(DemoTheme.GREEN);message.setText("✓ "+p.explanation);}
            else {state.wrongPuzzles.add(index);onChange.run();board.clearSelection();message.setForeground(DemoTheme.RED);message.setText("다시 시도해 보세요. 힌트를 사용할 수 있어요.");}
        });load();
    }
    private void load() {
        DemoData.Puzzle p=DemoData.PUZZLES[index];solved=false;board.setBoard(p.board);
        heading.setText(p.title);counter.setText("퍼즐 "+(index+1)+" / "+DemoData.PUZZLES.length);
        prompt.setText(p.hint);message.setText("기물을 선택한 뒤 도착 칸을 클릭하세요.");message.setForeground(DemoTheme.MUTED);
        prev.setEnabled(index>0);next.setEnabled(index<DemoData.PUZZLES.length-1);
    }
}
