package com.oopteama.chess.ui;

import javax.swing.*;
import java.awt.*;

public class TermQuizScreen extends JPanel {
    private final DemoState state; private final Runnable onChange;
    private int index=0, score=0;private boolean answered=false,finished=false;
    private final JLabel progress=DemoTheme.label("",14,DemoTheme.GOLD,true);
    private final JLabel question=DemoTheme.label("",22,DemoTheme.CREAM,true);
    private final JLabel feedback=DemoTheme.label("",15,DemoTheme.MUTED,false);
    private final JPanel answers=DemoTheme.transparent(new GridLayout(4,1,0,12));
    private final JButton[] choices=new JButton[4];
    private final JButton next=DemoTheme.button("다음 문제  →",true);
    public TermQuizScreen(DemoState state,Runnable onChange) {
        this.state=state;this.onChange=onChange;
        setOpaque(false);setLayout(new BorderLayout());DemoTheme.inset(this,26,70,35,70);
        JPanel card=DemoTheme.card(new BorderLayout(0,20));
        JPanel top=DemoTheme.transparent(new GridLayout(3,1,0,10));
        top.add(DemoTheme.label("KNOWLEDGE CHECK",12,DemoTheme.GOLD,true));top.add(progress);top.add(question);
        card.add(top,BorderLayout.NORTH);
        for(int i=0;i<4;i++) { final int n=i; choices[i]=DemoTheme.button("",false);choices[i].setHorizontalAlignment(SwingConstants.LEFT);choices[i].addActionListener(e->answer(n));answers.add(choices[i]); }
        card.add(answers,BorderLayout.CENTER);
        JPanel foot=DemoTheme.transparent(new BorderLayout(10,10));
        foot.add(feedback,BorderLayout.CENTER);foot.add(next,BorderLayout.EAST);card.add(foot,BorderLayout.SOUTH);
        next.addActionListener(e->{if(finished){reset();}else if(answered){index++;if(index==DemoData.QUESTIONS.length)showResult();else showQuestion();}});
        add(card,BorderLayout.CENTER);showQuestion();
    }
    private void showQuestion() {
        answered=false;finished=false;
        DemoData.Question q=DemoData.QUESTIONS[index];
        progress.setText(String.format("문제 %02d / %02d   •   현재 정답 %d개",index+1,DemoData.QUESTIONS.length,score));
        question.setText("<html><body style='width:600px'>"+q.q+"</body></html>");
        for(int i=0;i<4;i++) {choices[i].setText("  "+(i+1)+".  "+q.options[i]);choices[i].setEnabled(true);choices[i].setBackground(DemoTheme.PANEL2);}
        feedback.setText("정답을 하나 선택하세요.");feedback.setForeground(DemoTheme.MUTED);next.setEnabled(false);
        next.setText(index==DemoData.QUESTIONS.length-1?"결과 보기  →":"다음 문제  →");
    }
    private void answer(int n) {
        if(answered||finished)return;answered=true;
        DemoData.Question q=DemoData.QUESTIONS[index];boolean correct=n==q.correct;
        if(correct){score++;feedback.setForeground(DemoTheme.GREEN);feedback.setText("✓ 정답!  "+q.explanation);} else {
            state.wrongQuiz.add(index);onChange.run();feedback.setForeground(DemoTheme.RED);
            feedback.setText("✕ 오답.  "+q.explanation);
        }
        for(int i=0;i<4;i++){choices[i].setEnabled(false);if(i==q.correct)choices[i].setBackground(new Color(58,111,78));else if(i==n)choices[i].setBackground(new Color(132,68,65));}
        next.setEnabled(true);
    }
    private void showResult() {
        finished=true;state.quizAttempts++;state.bestScore=Math.max(state.bestScore,score);onChange.run();
        progress.setText("QUIZ COMPLETED");
        question.setText("퀴즈 완료!   "+score+" / "+DemoData.QUESTIONS.length+"점");
        answers.removeAll();
        JLabel summary=DemoTheme.label("정답률 "+(score*100/DemoData.QUESTIONS.length)+"%  ·  오답은 오답 노트에서 복습할 수 있어요.",17,DemoTheme.CREAM,true);
        answers.setLayout(new BorderLayout());answers.add(summary,BorderLayout.CENTER);answers.revalidate();answers.repaint();
        feedback.setText("다시 풀면 점수를 새로 계산합니다.");feedback.setForeground(DemoTheme.MUTED);
        next.setText("다시 풀기  ↻");next.setEnabled(true);
    }
    private void reset() {
        index=0;score=0;answered=false;finished=false;
        answers.removeAll();answers.setLayout(new GridLayout(4,1,0,12));
        for(JButton button:choices)answers.add(button);
        answers.revalidate();showQuestion();
    }
}
