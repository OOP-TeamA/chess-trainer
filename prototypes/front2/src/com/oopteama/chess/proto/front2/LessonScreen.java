package com.oopteama.chess.proto.front2;

import javax.swing.*;
import java.awt.*;

public class LessonScreen extends JPanel {
    private final DemoState state;
    private final JPanel lessonButtons=DemoTheme.transparent(new GridLayout(0,1,0,10));
    private final JLabel heading=DemoTheme.label("",25,DemoTheme.CREAM,true);
    private final JLabel tag=DemoTheme.label("",14,DemoTheme.MUTED,false);
    private final JTextArea lessonBody=new JTextArea();
    private final JLabel stepIndex=DemoTheme.label("",13,DemoTheme.GOLD,true);
    private final JButton back=DemoTheme.button("이전 단계",false);
    private final JButton next=DemoTheme.button("다음 단계",true);
    private final JLabel status=DemoTheme.label("",13,DemoTheme.GREEN,false);
    private int selected=0,step=0;
    private final Runnable onChange;

    public LessonScreen(DemoState state,Runnable onChange) {
        this.state=state;this.onChange=onChange;
        setLayout(new BorderLayout(20,0));setOpaque(false);DemoTheme.inset(this,24,28,28,28);
        JPanel left=DemoTheme.transparent(new BorderLayout(0,16)); left.setPreferredSize(new Dimension(295,500));
        left.add(DemoTheme.label("학습 커리큘럼",18,DemoTheme.CREAM,true),BorderLayout.NORTH);
        left.add(lessonButtons,BorderLayout.CENTER);add(left,BorderLayout.WEST);
        JPanel detail=DemoTheme.card(new BorderLayout(0,20));
        JPanel top=DemoTheme.transparent(new GridLayout(3,1,0,5));
        top.add(DemoTheme.label("LESSON CONTENT",12,DemoTheme.GOLD,true));top.add(heading);top.add(tag);
        detail.add(top,BorderLayout.NORTH);
        JPanel middle=DemoTheme.transparent(new BorderLayout(0,20));
        JPanel info=DemoTheme.card(new BorderLayout()); info.setBackground(DemoTheme.PANEL2);
        lessonBody.setEditable(false); lessonBody.setOpaque(false); lessonBody.setLineWrap(true);lessonBody.setWrapStyleWord(true);
        lessonBody.setForeground(DemoTheme.CREAM);lessonBody.setFont(DemoTheme.font(19,false));
        lessonBody.setFocusable(false);info.add(lessonBody,BorderLayout.CENTER);
        middle.add(info,BorderLayout.CENTER);middle.add(stepIndex,BorderLayout.SOUTH);
        detail.add(middle,BorderLayout.CENTER);
        JPanel foot=DemoTheme.transparent(new BorderLayout(10,0));
        JPanel controls=DemoTheme.transparent(new FlowLayout(FlowLayout.RIGHT,10,0));
        controls.add(back);controls.add(next); foot.add(status,BorderLayout.CENTER);foot.add(controls,BorderLayout.EAST);
        detail.add(foot,BorderLayout.SOUTH);add(detail,BorderLayout.CENTER);
        back.addActionListener(e->{if(step>0){step--;refresh();}});
        next.addActionListener(e->{if(step<DemoData.LESSONS[selected].steps.length-1){step++;refresh();} else {state.doneLessons[selected]=true;onChange.run();refresh();}});
        refreshButtons();refresh();
    }
    private void refreshButtons() {
        lessonButtons.removeAll();
        for(int i=0;i<DemoData.LESSONS.length;i++) {
            final int index=i;
            String prefix=state.doneLessons[i]?"✓  ":"0"+(i+1)+"  ";
            JButton b=DemoTheme.button(prefix+DemoData.LESSONS[i].title,i==selected);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.addActionListener(e->{selected=index;step=0;refreshButtons();refresh();});
            lessonButtons.add(b);
        }
        lessonButtons.revalidate();lessonButtons.repaint();
    }
    private void refresh() {
        DemoData.Lesson l=DemoData.LESSONS[selected];
        heading.setText(l.title);tag.setText(l.tagline);
        lessonBody.setText(l.steps[step]);
        stepIndex.setText("학습 단계  "+(step+1)+" / "+l.steps.length);
        status.setText(state.doneLessons[selected]?"✓ 학습 완료":"내용을 읽고 다음 단계로 이동하세요");
        back.setEnabled(step>0);next.setText(step==l.steps.length-1?"레슨 완료 처리":"다음 단계  →");
        refreshButtons();
    }
}
