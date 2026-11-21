package com.oopteama.chess.proto.front2;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class LessonScreen extends JPanel {
    private final DemoState state;
    private final JPanel lessonButtons=DemoTheme.transparent(new GridLayout(0,1,0,8));
    private final JLabel heading=DemoTheme.label("",25,DemoTheme.CREAM,true);
    private final JLabel tag=DemoTheme.label("",14,DemoTheme.MUTED,false);
    private final JTextArea lessonBody = new JTextArea();
    private final JPanel example = DemoTheme.transparent(new BorderLayout(0, 6));
    private final JPanel middle = DemoTheme.transparent(new BorderLayout(12, 12));
    private final JPanel info = DemoTheme.card(new BorderLayout(0, 16));
    private final JScrollPane explanation;
    private final JScrollPane rookExplanation;
    private final JLabel stepIndex=DemoTheme.label("",13,DemoTheme.GOLD,true);
    private final JButton back=DemoTheme.button("이전 단계",false);
    private final JButton next=DemoTheme.button("다음 단계",true);
    private final JLabel status=DemoTheme.label("",13,DemoTheme.GREEN,false);
    private int selected = DemoData.ROOK_LESSON_INDEX, step = 0;
    private final Runnable onChange;

    public LessonScreen(DemoState state,Runnable onChange) {
        this.state=state;this.onChange=onChange;
        setLayout(new BorderLayout(20,0));setOpaque(false);DemoTheme.inset(this,24,28,28,28);
        JPanel left=DemoTheme.transparent(new BorderLayout(0,16)); left.setPreferredSize(new Dimension(160,500));
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
        JPanel legend = DemoTheme.transparent(new FlowLayout(FlowLayout.LEADING, 8, 0));
        legend.add(DemoTheme.label("● 이동 가능", 13, DemoTheme.GREEN, true));
        legend.add(DemoTheme.label("× 이동 불가", 13, DemoTheme.RED, true));
        JPanel boardFooter = DemoTheme.transparent(new BorderLayout(0, 6));
        boardFooter.add(legend, BorderLayout.NORTH);
        JButton replay = DemoTheme.button("이동 예시 다시 보기", false);
        replay.addActionListener(event -> board.replayPreview());
        boardFooter.add(replay, BorderLayout.SOUTH);
        example.add(boardFooter, BorderLayout.SOUTH);

        info.setBackground(DemoTheme.PANEL2);
        styleText(lessonBody, 20, DemoTheme.CREAM);
        explanation = new JScrollPane(lessonBody,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        explanation.setBorder(null);
        explanation.setOpaque(false);
        explanation.getViewport().setOpaque(false);
        explanation.getVerticalScrollBar().setUnitIncrement(16);
        JPanel notes = DemoTheme.transparent(new GridBagLayout());
        GridBagConstraints note = new GridBagConstraints();
        note.gridx = 0;
        note.weightx = 1;
        note.fill = GridBagConstraints.HORIZONTAL;
        note.insets = new Insets(0, 0, 8, 0);
        addNote(notes, note, "룩의 이동 방향", "룩은 가로와 세로로 움직여요.");
        addNote(notes, note, "갈 수 있는 칸", "d4의 룩은 위로 d5까지 갈 수 있어요.");
        addNote(notes, note, "d7은 왜 못 갈까요?", "d6의 아군 폰이 길을 막고 있어요. 룩은 기물을 뛰어넘을 수 없어요.");
        JPanel rookNotes = DemoTheme.transparent(new BorderLayout());
        JScrollPane noteScroll = new JScrollPane(rookNotes,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        noteScroll.setBorder(null);
        noteScroll.setOpaque(false);
        noteScroll.getViewport().setOpaque(false);
        noteScroll.getVerticalScrollBar().setUnitIncrement(16);
        rookNotes.add(notes, BorderLayout.NORTH);
        rookExplanation = noteScroll;

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

    private static void addNote(JPanel notes, GridBagConstraints position, String title, String description) {
        JPanel item = DemoTheme.transparent(new BorderLayout(0, 6));
        JButton toggle = DemoTheme.button("+ " + title, false);
        toggle.setFont(DemoTheme.font(16, true));
        toggle.setForeground(DemoTheme.GOLD);
        toggle.setFocusPainted(true);
        toggle.setHorizontalAlignment(SwingConstants.LEFT);
        DemoTheme.inset(toggle, 9, 7, 9, 7);
        toggle.setToolTipText("<html><div width='215'>" + description + "</div></html>");

        JTextArea body = new JTextArea(description);
        styleText(body, 15, DemoTheme.CREAM);
        body.setSize(170, 1);
        DemoTheme.inset(body, 0, 7, 5, 7);
        body.setVisible(false);
        toggle.addActionListener(event -> {
            boolean expanded = !body.isVisible();
            body.setVisible(expanded);
            toggle.setText((expanded ? "− " : "+ ") + title);
            item.revalidate();
            item.repaint();
        });
        item.add(toggle, BorderLayout.NORTH);
        item.add(body, BorderLayout.CENTER);
        notes.add(item, position);
    }

    private void refreshButtons() {
        lessonButtons.removeAll();
        for(int i=0;i<DemoData.LESSONS.length;i++) {
            final int index=i;
            String prefix=state.doneLessons[i]?"✓  ":"0"+(i+1)+"  ";
            String title = DemoData.LESSONS[i].title;
            String formatted = switch (title) {
                case "나이트 집중 학습" -> "나이트<br>집중 학습";
                case "체크와 체크메이트" -> "체크와<br>체크메이트";
                default -> title;
            };
            JButton b = DemoTheme.button("<html>" + prefix + formatted + "</html>", i == selected);
            b.getAccessibleContext().setAccessibleName(title);
            b.setFocusPainted(true);
            b.setPreferredSize(new Dimension(160, 56));
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
        info.removeAll();
        info.add(showRook ? rookExplanation : explanation, BorderLayout.CENTER);
        info.setPreferredSize(showRook ? new Dimension(216, 0) : null);
        middle.remove(info);
        middle.remove(example);
        middle.add(info, showRook ? BorderLayout.EAST : BorderLayout.CENTER);
        if (showRook) {
            middle.add(example, BorderLayout.CENTER);
        }
        lessonBody.setText(l.steps[step]);
        lessonBody.setCaretPosition(0);
        stepIndex.setText("학습 단계  "+(step+1)+" / "+l.steps.length);
        status.setText(state.doneLessons[selected]?"✓ 학습 완료":"다음 단계에서 계속");
        back.setEnabled(step>0);next.setText(step==l.steps.length-1?"레슨 완료 처리":"다음 단계  →");
        refreshButtons();
        revalidate();
        repaint();
    }
}
