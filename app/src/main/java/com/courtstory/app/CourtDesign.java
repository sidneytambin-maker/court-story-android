package com.courtstory.app;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;

/** Brand surfaces drawn at device resolution, with no decorative accessibility nodes. */
final class CourtDesign {
    static Drawable surface(int fill,int border,int radius,int ripple){
        GradientDrawable shape=new GradientDrawable();shape.setColor(fill);shape.setCornerRadius(radius);
        if(border!=0)shape.setStroke(1,border);
        return new RippleDrawable(ColorStateList.valueOf(ripple),shape,null);
    }
    static final class Hero extends Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;
        Hero(float density){this.density=density;}
        @Override public void draw(Canvas canvas){
            Rect b=getBounds();RectF area=new RectF(b);float radius=22*density;
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(9,49,36));canvas.drawRoundRect(area,radius,radius,paint);
            int save=canvas.save();Path clip=new Path();clip.addRoundRect(area,radius,radius,Path.Direction.CW);canvas.clipPath(clip);
            paint.setColor(Color.argb(25,221,255,40));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(density);
            float x=b.right-72*density;canvas.drawLine(x,b.top,x,b.bottom,paint);
            canvas.drawLine(x-34*density,b.top,x-34*density,b.bottom,paint);
            canvas.drawLine(x,b.top+30*density,b.right,b.top+30*density,paint);
            canvas.drawLine(x,b.bottom-30*density,b.right,b.bottom-30*density,paint);
            canvas.drawLine(x,b.centerY(),b.right,b.centerY(),paint);
            canvas.restoreToCount(save);paint.setStyle(Paint.Style.FILL);
        }
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
