package com.courtstory.app;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Original resolution-independent Court Story insignia. Meaning is in the adjacent native text. */
final class AchievementBadge extends View {
    final int design;final boolean earned;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Canvas c;
    AchievementBadge(Context context,int design,boolean earned){super(context);this.design=design;this.earned=earned;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setFocusable(false);}
    void line(float... xy){Path path=new Path();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);c.drawPath(path,p);}
    void circle(float x,float y,float r){c.drawCircle(x,y,r,p);}
    void star(float x,float y,float r){Path path=new Path();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float radius=i%2==0?r:r*.43f,px=x+(float)Math.cos(a)*radius,py=y+(float)Math.sin(a)*radius;if(i==0)path.moveTo(px,py);else path.lineTo(px,py);}path.close();c.drawPath(path,p);}
    void person(float x,float y){circle(x,y,5);c.drawArc(x-8,y+8,x+8,y+24,180,180,false,p);}
    void court(){c.drawRect(30,29,70,70,p);line(30,49,70,49);line(40,29,40,70);line(60,29,60,70);}
    void book(){line(50,35,34,30,29,32,29,64,50,70,71,64,71,32,66,30,50,35,50,70);}
    void trophy(){line(37,30,63,30,61,47,57,54,50,58,43,54,39,47,37,30);line(50,58,50,70);line(39,72,61,72);c.drawArc(28,31,44,50,90,180,false,p);c.drawArc(56,31,72,50,270,180,false,p);}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);c=canvas;int save=c.save();float scale=Math.min(getWidth(),getHeight())/100f;c.translate((getWidth()-100*scale)/2,(getHeight()-100*scale)/2);c.scale(scale,scale);
        p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(15,10,85,95,earned?0xff23573e:0xffeaf0e4,earned?0xff092e23:0xffcdd8c7,Shader.TileMode.CLAMP));circle(50,49,46);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setColor(earned?0xffddff28:0xff536a55);circle(50,49,40);p.setStrokeWidth(3);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        switch(design){
            case 0: line(50,72,50,43);c.drawOval(31,32,49,47,p);c.drawOval(51,26,69,41,p);break;
            case 1: line(29,66,41,66,41,53,53,53,53,40,68,40);star(67,28,7);break;
            case 2: line(27,68,42,39,51,54,60,33,75,68,27,68);break;
            case 3: line(49,26,37,43,34,56,40,69,51,73,64,66,68,51,60,37,56,53,49,26);break;
            case 4: circle(50,49,16);for(int i=0;i<8;i++){double a=i*Math.PI/4;line(50+(float)Math.cos(a)*25,49+(float)Math.sin(a)*25,50+(float)Math.cos(a)*31,49+(float)Math.sin(a)*31);}star(50,49,10);break;
            case 5: court();circle(52,40,5);break;
            case 6: line(54,25,35,53,48,53,44,74,65,44,52,44,54,25);break;
            case 7: line(35,73,35,28,67,28,59,40,67,51,35,51);break;
            case 8: trophy();star(50,42,7);break;
            case 9: person(50,39);line(29,69,71,69);break;
            case 10: person(38,40);person(62,40);line(39,69,61,69);break;
            case 11: star(50,46,22);line(39,70,61,70);break;
            case 12: star(37,44,13);star(65,44,13);line(36,67,64,67);break;
            case 13: line(27,36,41,29,59,36,73,29,73,64,59,71,41,64,27,71,27,36);line(41,29,41,64);line(59,36,59,71);break;
            case 14: circle(50,49,24);line(59,33,55,54,40,65,45,44,59,33);break;
            case 15: circle(50,44,17);line(39,59,35,77,50,70,65,77,61,59);star(50,44,10);break;
            case 16: line(25,51,35,51,41,37,50,66,58,45,65,51,75,51);break;
            case 17: c.save();c.rotate(-35,50,49);c.drawRoundRect(30,39,55,57,9,9,p);c.drawRoundRect(45,39,70,57,9,9,p);c.restore();break;
            case 18: circle(50,49,23);circle(50,49,12);circle(50,49,2);break;
            case 19: circle(46,53,21);circle(46,53,11);line(46,53,69,29,69,40);line(69,29,59,29);break;
            case 20: book();circle(50,21,3);break;
            case 21: book();line(36,43,43,45);line(36,51,43,53);line(57,45,64,43);line(57,53,64,51);break;
            case 22: c.drawRoundRect(29,29,71,62,8,8,p);line(39,62,37,74,52,62);star(50,45,11);break;
            case 23: c.drawRoundRect(31,30,69,71,5,5,p);line(41,26,41,36);line(59,26,59,36);line(32,42,68,42);line(40,55,47,62,61,48);break;
            case 24: person(50,31);person(31,52);person(69,52);line(43,73,57,73);break;
            default: throw new IllegalArgumentException("Unknown badge design");
        }
        p.setStyle(Paint.Style.FILL);p.setColor(earned?0xffddff28:0xff536a55);circle(80,81,12);p.setColor(earned?0xff123b2a:0xffffffff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);
        if(earned)line(74,81,78,85,86,77);else{c.drawRoundRect(76,80,84,87,1,1,p);c.drawArc(77,74,83,83,180,180,false,p);}c.restoreToCount(save);
    }
}
