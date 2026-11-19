package com.oopteama.chess.proto.front2;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class LessonScreen extends JPanel {
    private final DemoState state;
    private final JPanel lessonButtons=DemoTheme.transparent(new GridLayout(0,1,0,10));
    private final JLabel heading=DemoTheme.label("",25,DemoTheme.CREAM,true);
    private final JLabel tag=DemoTheme.label("",14,DemoTheme.MUTED,false);
    private final JTextArea lessonBody = new JTextArea();
    private final JPanel example = DemoTheme.transparent(new BorderLayout(0, 6));
    private final JPanel middle = DemoTheme.transparent(new BorderLayout(12, 12));
    private final JPanel info = DemoTheme.card(new BorderLayout(0, 16));
    private final JTextArea feedback = new JTextArea();
    private final JLabel stepIndex=DemoTheme.label("",13,DemoTheme.GOLD,true);
    private final JButton back=DemoTheme.button("이전 단계",false);
    private final JButton next=DemoTheme.button("다음 단계",true);
    private final JLabel status=DemoTheme.label("",13,DemoTheme.GREEN,false);
    private int selected = DemoData.ROOK_LESSON_INDEX, step = 0;
    private final Runnable onChange;

    public LessonScreen(DemoState state,Runnable onChange) {
        this.state=state;this.onChange=onChange;
        setLayout(new BorderLayout(20,0));setOpaque(false);DemoTheme.inset(this,24,28,28,28);
        JPanel left=DemoTheme.transparent(new BorderLayout(0,16)); left.setPreferredSize(new Dimension(152,500));
        left.add(DemoTheme.label("학습 커리큘럼",18,DemoTheme.CREAM,true),BorderLayout.NORTH);
        left.add(lessonButtons,BorderLayout.NORTH);add(left,BorderLayout.WEST);
        JPanel detail=DemoTheme.card(new BorderLayout(0,12));
        JPanel top=DemoTheme.transparent(new BorderLayout(0, 4));
        top.add(heading, BorderLayout.NORTH);
        top.add(tag, BorderLayout.SOUTH);
        detail.add(top,BorderLayout.NORTH);
        BoardView board = new BoardView();
        board.setBoard("4k3/8/3P4/8/3R4/8/8/4K3");
        board.showPreview("d4", Set.of("a4", "b4", "c4", "e4", "f4", "g4", "h4",
                "d1", "d2", "d3", "d5"), "d6", "d7");
        board.getAccessibleContext().setAccessibleName("룩 이동 예시. d4의 백 룩, d6의 아군 폰, 이동할 수 없는 d7.");
        example.add(board, BorderLayout.CENTER);
        example.add(DemoTheme.label("● 이동 가능    × 이동 불가", 14, DemoTheme.CREAM, true), BorderLayout.SOUTH);

        info.setBackground(DemoTheme.PANEL2);
        styleText(lessonBody, 20, DemoTheme.CREAM);
        styleText(feedback, 17, DemoTheme.CREAM);
        feedback.setRows(5);
        feedback.setText("d5까지 갈 수 있어요. d6의 아군 폰이 길을 막아서 d7로는 갈 수 없어요.");
        info.add(lessonBody, BorderLayout.CENTER);
        info.add(feedback, BorderLayout.SOUTH);

        middle.add(stepIndex, BorderLayout.SOUTH);
        detail.add(middle, BorderLayout.CENTER);
        JPanel foot=DemoTheme.transparent(new BorderLayout(10,0));
        JPanel controls=DemoTheme.transparent(new FlowLayout(FlowLayout.RIGHT,10,0));
        controls.add(back);controls.add(next); foot.add(status,BorderLayout.CENTER);foot.add(controls,BorderLayout.EAST);
        detail.add(foot,BorderLayout.SOUTH);add(detail,BorderLayout.CENTER);
        back.addActionListener(e->{if(step>0){step--;refresh();}});
        next.addActionListener(e->{if(step<DemoData.LESSONS[selected].steps.length-1){step++;refresh();} else {state.doneLessons[selected]=true;onChange.run();refresh();}});
        refresh();
    }
    private static void styleText(JTextArea text, int size, Color color) {
        text.setEditable(false);
        text.setOpaque(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setForeground(color);
        text.setFont(DemoTheme.font(size, false));
        text.setFocusable(false);
    }

    private void refreshButtons() {
        lessonButtons.removeAll();
        for(int i=0;i<DemoData.LESSONS.length;i++) {
            final int index=i;
            String prefix=state.doneLessons[i]?"✓  ":"0"+(i+1)+"  ";
            JButton b=DemoTheme.button(prefix+DemoData.LESSONS[i].title,i==selected);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.addActionListener(e->{selected=index;step=0;refresh();});
            lessonButtons.add(b);
        }
        lessonButtons.revalidate();lessonButtons.repaint();
    }
    private void refresh() {
        DemoData.Lesson l=DemoData.LESSONS[selected];
        heading.setText(l.title);tag.setText(l.tagline);
        boolean showRook = selected == DemoData.ROOK_LESSON_INDEX && step == 0;
        feedback.setVisible(showRook);
        info.setPreferredSize(showRook ? new Dimension(210, 0) : null);
        middle.remove(info);
        middle.remove(example);
        middle.add(info, showRook ? BorderLayout.EAST : BorderLayout.CENTER);
        if (showRook) {
            middle.add(example, BorderLayout.CENTER);
        }
        lessonBody.setText(showRook
                ? "룩은 가로와 세로로 움직여요. 초록 점은 d4의 룩이 갈 수 있는 칸이에요."
                : l.steps[step]);
        stepIndex.setText("학습 단계  "+(step+1)+" / "+l.steps.length);
        status.setText(state.doneLessons[selected]?"✓ 학습 완료":"다음 단계에서 계속");
        back.setEnabled(step>0);next.setText(step==l.steps.length-1?"레슨 완료 처리":"다음 단계  →");
        refreshButtons();
        revalidate();
        repaint();
    }
}
