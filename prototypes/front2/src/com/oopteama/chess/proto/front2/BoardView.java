package com.oopteama.chess.proto.front2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.BiConsumer;

public class BoardView extends JPanel {
    private final char[][] pieces = new char[8][8];
    private String selected;
    private BiConsumer<String,String> moveHandler;
    private String highlightFrom, highlightTo;
    private final Color light = new Color(222, 208, 175), dark = new Color(129, 104, 79);
    public BoardView() {
        setOpaque(false); setPreferredSize(new Dimension(460,460));
        addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) {
            int size=Math.min(getWidth(), getHeight())-28; int x0=(getWidth()-size)/2; int y0=(getHeight()-size)/2;
            if(e.getX()<x0 || e.getY()<y0 || e.getX()>=x0+size || e.getY()>=y0+size) return;
            int col=(e.getX()-x0)*8/size, row=(e.getY()-y0)*8/size;
            String cell=""+(char)('a'+col)+(char)('8'-row);
            if(selected==null) {
                if(pieces[row][col]==' ') return;
                selected=cell;
            } else {
                String previous=selected; selected=null;
                if(moveHandler!=null) moveHandler.accept(previous,cell);
            }
            repaint();
        }});
    }
    public void setBoard(String fen) {
        String[] rows=fen.split("/");
        if(rows.length!=8) throw new IllegalArgumentException("보드에는 8개 랭크가 필요합니다");
        for(int r=0;r<8;r++) {
            int c=0;
            for(char ch:rows[r].toCharArray()) {
                if(Character.isDigit(ch)) { for(int i=0;i<ch-'0';i++) pieces[r][c++]=' '; }
                else pieces[r][c++]=ch;
            }
            if(c!=8) throw new IllegalArgumentException("각 랭크는 8칸이어야 합니다");
        }
        selected=null; highlightFrom=highlightTo=null; repaint();
    }
    public void setMoveHandler(BiConsumer<String,String> handler) { moveHandler=handler; }
    public void clearSelection() { selected=null; repaint(); }
    public void showMove(String from,String to) { highlightFrom=from; highlightTo=to; repaint(); }
    private static String symbol(char ch) {
        switch (ch) {
            case 'K': return "♔"; case 'Q': return "♕"; case 'R': return "♖";
            case 'B': return "♗"; case 'N': return "♘"; case 'P': return "♙";
            case 'k': return "♚"; case 'q': return "♛"; case 'r': return "♜";
            case 'b': return "♝"; case 'n': return "♞"; case 'p': return "♟";
            default: return "";
        }
    }
    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g=(Graphics2D)g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size=Math.min(getWidth(), getHeight())-28;
        int x0=(getWidth()-size)/2,y0=(getHeight()-size)/2;
        for(int r=0;r<8;r++) for(int c=0;c<8;c++) {
            int x1=x0+c*size/8, x2=x0+(c+1)*size/8, y1=y0+r*size/8, y2=y0+(r+1)*size/8;
            int w=x2-x1,h=y2-y1;
            String coord=""+(char)('a'+c)+(char)('8'-r);
            g.setColor((r+c)%2==0?light:dark); g.fillRect(x1,y1,w,h);
            if(coord.equals(selected)) { g.setColor(new Color(226,179,64,130)); g.fillRect(x1,y1,w,h); }
            if(coord.equals(highlightFrom)||coord.equals(highlightTo)) { g.setColor(new Color(123,205,145,125)); g.fillRect(x1,y1,w,h); }
            if(c==0) { g.setColor((r+c)%2==0?dark:light); g.setFont(DemoTheme.font(Math.max(11,size/48),true)); g.drawString(""+(8-r),x1+4,y1+15); }
            if(r==7) { g.setColor((r+c)%2==0?dark:light); g.setFont(DemoTheme.font(Math.max(11,size/48),true)); g.drawString(""+(char)('a'+c),x2-14,y2-5); }
            char p=pieces[r][c];
            if(p!=' ') {
                String text=symbol(p);
                int fontSize=Math.max(23,(int)(w*0.77));
                g.setFont(new Font("Segoe UI Symbol", Font.PLAIN,fontSize));
                FontMetrics fm=g.getFontMetrics(); int tx=x1+(w-fm.stringWidth(text))/2,ty=y1+(h-fm.getHeight())/2+fm.getAscent();
                g.setColor(Character.isUpperCase(p)?new Color(255,247,218):new Color(32,31,36));
                g.drawString(text,tx,ty);
            }
        }
        g.dispose();
    }
}
