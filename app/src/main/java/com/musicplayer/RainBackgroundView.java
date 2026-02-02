package com.musicplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class RainBackgroundView extends View {
    private ArrayList<Drop> drops = new ArrayList<>();
    private Paint paint = new Paint();
    private Random random = new Random();
    private int numDrops = 50;
    private int[] purpleColors = {
        Color.parseColor("#C77DFF"),
        Color.parseColor("#9D4EDD"),
        Color.parseColor("#E0AAFF"),
        Color.parseColor("#7209B7")
    };

    public RainBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setAlpha(100);
        paint.setStrokeWidth(2);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        drops.clear();
        for (int i = 0; i < numDrops; i++) {
            drops.add(new Drop(random.nextInt(w), random.nextInt(h)));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Drop drop : drops) {
            paint.setColor(drop.color);
            canvas.drawLine(drop.x, drop.y, drop.x, drop.y + drop.length, paint);
            drop.y += drop.speed;
            if (drop.y > getHeight()) {
                drop.y = -drop.length;
                drop.x = random.nextInt(getWidth());
            }
        }
        invalidate();
    }

    private class Drop {
        float x, y;
        float speed;
        float length;
        int color;

        Drop(float x, float y) {
            this.x = x;
            this.y = y;
            this.speed = 10 + random.nextFloat() * 20;
            this.length = 15 + random.nextFloat() * 20;
            this.color = purpleColors[random.nextInt(purpleColors.length)];
        }
    }
}
