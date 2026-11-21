package com.oopteama.chess.proto.front2;

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
    private final JButton explanation = DemoTheme.button("+ 해설 보기", false);
    private final JTextArea solution = new JTextArea(3, 0);
    private final JScrollPane solutionScroll = new JScrollPane(solution,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
        JLabel help = DemoTheme.label("플레이 방법 ⓘ", 14, DemoTheme.MUTED, true);
        help.setToolTipText("<html>체스판에서 기물을 고른 뒤 도착 칸을 클릭하세요.<br>"
                + "이 데모에서는 지정된 목표 수를 기준으로 채점합니다.</html>");
        JPanel details = DemoTheme.transparent(new BorderLayout(0, 10));
        JPanel result = DemoTheme.transparent(new GridLayout(0, 1, 0, 6));
        result.add(help);
        result.add(message);
        result.add(explanation);
        details.add(result, BorderLayout.NORTH);
        solution.setEditable(false);
        solution.setLineWrap(true);
        solution.setWrapStyleWord(true);
        solution.setFont(DemoTheme.font(16, false));
        solution.setForeground(DemoTheme.CREAM);
        solution.setBackground(DemoTheme.PANEL2);
        DemoTheme.inset(solution, 9, 12, 9, 12);
        solutionScroll.setBorder(null);
        details.add(solutionScroll, BorderLayout.CENTER);
        center.add(details, BorderLayout.CENTER);
        right.add(center, BorderLayout.CENTER);

        explanation.setForeground(DemoTheme.GOLD);
        explanation.setFocusPainted(true);
        explanation.setHorizontalAlignment(SwingConstants.LEFT);
        explanation.addActionListener(event -> {
            boolean expanded = !solutionScroll.isVisible();
            solutionScroll.setVisible(expanded);
            explanation.setText(expanded ? "− 해설 접기" : "+ 해설 보기");
            right.revalidate();
            right.repaint();
        });
        JPanel controls=DemoTheme.transparent(new GridLayout(2,2,10,10));controls.add(prev);controls.add(next);controls.add(hint);controls.add(reset);
        right.add(controls, BorderLayout.SOUTH);
        add(right,BorderLayout.EAST);
        prev.addActionListener(e->{if(index>0){index--;load();}}); next.addActionListener(e->{if(index<DemoData.PUZZLES.length-1){index++;load();}});
        hint.addActionListener(e->{DemoData.Puzzle p=DemoData.PUZZLES[index];message.setForeground(DemoTheme.GOLD);message.setText("힌트: "+p.from+" → ?");});
        reset.addActionListener(e->load());
        board.setMoveHandler((from,to)->{
            DemoData.Puzzle p=DemoData.PUZZLES[index];
            if (solved) return;
            if (from.equals(p.from) && to.equals(p.to)) {
                solved = true;
                board.animateMove(from, to);
                board.setEnabled(false);
                state.solvedPuzzles.add(index);
                message.setForeground(DemoTheme.GREEN);
                message.setText("✓ 정답");
                explanation.setVisible(true);
                hint.setEnabled(false);
            } else {
                state.wrongPuzzles.add(index);
                board.clearSelection();
                message.setForeground(DemoTheme.RED);
                message.setText("다시 시도해 보세요.");
            }
            onChange.run();
        });load();
    }
    private void load() {
        DemoData.Puzzle p=DemoData.PUZZLES[index];solved=false;board.setBoard(p.board);
        board.setEnabled(true);
        heading.setText(p.title);counter.setText("퍼즐 "+(index+1)+" / "+DemoData.PUZZLES.length);
        prompt.setText(p.hint);message.setText("기물을 선택한 뒤 도착 칸을 클릭하세요.");message.setForeground(DemoTheme.MUTED);
        solution.setText(p.explanation);
        solution.setCaretPosition(0);
        solutionScroll.setVisible(false);
        explanation.setText("+ 해설 보기");
        explanation.setToolTipText("<html><div width='235'>" + p.explanation + "</div></html>");
        explanation.setVisible(false);
        hint.setEnabled(true);
        prev.setEnabled(index>0);next.setEnabled(index<DemoData.PUZZLES.length-1);
    }
}
