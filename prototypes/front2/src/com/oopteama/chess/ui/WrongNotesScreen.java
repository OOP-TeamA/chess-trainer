package com.oopteama.chess.ui;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class WrongNotesScreen extends JPanel {
    private final DemoState state;private final Runnable onChange;
    private final JPanel items=DemoTheme.transparent(new GridLayout(0,1,0,12));
    private final JLabel count=DemoTheme.label("",14,DemoTheme.GOLD,true);
    public WrongNotesScreen(DemoState state,Runnable onChange) {
        this.state=state;this.onChange=onChange;
        setOpaque(false);setLayout(new BorderLayout(0,16));DemoTheme.inset(this,27,35,30,35);
        JPanel header=DemoTheme.transparent(new GridLayout(3,1,0,8));
        header.add(DemoTheme.label("REVIEW CENTER",12,DemoTheme.GOLD,true));
        header.add(DemoTheme.label("오답 노트",26,DemoTheme.CREAM,true));header.add(count);
        add(header,BorderLayout.NORTH);
        JPanel listWrap=DemoTheme.transparent(new BorderLayout());listWrap.add(items,BorderLayout.NORTH);
        add(DemoTheme.scroll(listWrap),BorderLayout.CENTER);refresh();
    }
    public void refresh() {
        items.removeAll();int total=state.wrongQuiz.size()+state.wrongPuzzles.size();
        count.setText("기록된 오답 "+total+"개  ·  이 데모는 실행 중 메모리에만 저장됩니다.");
        for(int index:new ArrayList<>(state.wrongQuiz)) {
            DemoData.Question q=DemoData.QUESTIONS[index];
            addItem("QUIZ  "+(index+1),q.q,"정답: "+q.options[q.correct]+"  |  "+q.explanation,()->{state.wrongQuiz.remove(index);refresh();onChange.run();});
        }
        for(int index:new ArrayList<>(state.wrongPuzzles)) {
            DemoData.Puzzle p=DemoData.PUZZLES[index];
            addItem("PUZZLE  "+(index+1),p.title,"목표 수: "+p.from+" → "+p.to+"  |  "+p.explanation,()->{state.wrongPuzzles.remove(index);refresh();onChange.run();});
        }
        if(total==0) {
            JPanel empty=DemoTheme.card(new BorderLayout());empty.add(DemoTheme.label("아직 기록된 오답이 없습니다. 퀴즈나 퍼즐을 풀어 보세요!",17,DemoTheme.CREAM,false));items.add(empty);
        }
        items.revalidate();items.repaint();
    }
    private void addItem(String category,String title,String detail,Runnable remove) {
        JPanel card=DemoTheme.card(new BorderLayout(0,10));
        JPanel top=DemoTheme.transparent(new BorderLayout());top.add(DemoTheme.label(category,12,DemoTheme.GOLD,true),BorderLayout.WEST);
        JButton delete=DemoTheme.button("기록 삭제",false);delete.addActionListener(e->remove.run());top.add(delete,BorderLayout.EAST);card.add(top,BorderLayout.NORTH);
        JPanel main=DemoTheme.transparent(new GridLayout(2,1,0,10));main.add(DemoTheme.label(title,17,DemoTheme.CREAM,true));
        JTextArea explanation=new JTextArea(detail);explanation.setLineWrap(true);explanation.setWrapStyleWord(true);explanation.setEditable(false);explanation.setOpaque(false);explanation.setForeground(DemoTheme.MUTED);explanation.setFont(DemoTheme.font(13,false));main.add(explanation);
        card.add(main,BorderLayout.CENTER);items.add(card);
    }
}
