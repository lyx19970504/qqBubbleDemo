package com.example.myapplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PointFEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Nullable;

public class DragBubbleView extends View {

    private final int BUBBLE_STATE_DEFAULT = 0;    //气泡默认状态
    private final int BUBBLE_STATE_CONNECT = 1;    //气泡连接状态
    private final int BUBBLE_STATE_APART = 2;      //气泡分离状态
    private final int BUBBLE_STATE_DISMISS = 3;    //气泡消失状态

    private float mBubbleRadius;    //气泡半径
    private int mBubbleColor;  //气泡颜色
    private String mTextStr;    //气泡消息文字
    private int mTextColor;    //气泡消息文字颜色
    private float mTextSize;   //气泡消息文字大小
    private float mFixedBubbleRadius;   //不动气泡半径
    private float mMovedBubbleRadius;   //可动气泡半径
    private PointF mFixedBubbleCenter;  //不定气泡圆心
    private PointF mMovedBubbleCenter;  //可动气泡圆心
    private Paint mBubblePaint;   //气泡画笔
    private Path mBezierPath;     //贝塞尔曲线
    private Paint mTextPaint;     //消息文字画笔

    private Rect mTextRect;   //文字绘制区域

    private Paint mBurstPaint;   //爆炸气泡画笔
    private Rect mBurstRect;    //爆炸气泡绘制区域

    private int mBubbleState = BUBBLE_STATE_DEFAULT;    //气泡状态

    private float mDist;   //气泡圆心距离

    private float mMaxDist;   //气泡圆心最大距离

    private float MOVE_OFFSET;  //手指偏移量

    private Bitmap[] mBurstBitmapsArray;   //爆炸图像数组

    private boolean mIsBurstAnimStart = false;   //是否执行气泡爆炸动画

    private int mCurrentDrawableIndex;  //爆炸图像索引

    private static final float mDefaultBubbleRadius = 12;
    private static final int mDefaultBubbleColor = Color.RED;
    private static final int mDefaultTextColor = Color.WHITE;
    private static final float mDefaultTextSize = 12;

    private float centerX;
    private float centerY;

    private int[] mBurstDrawableArray
            = {R.mipmap.burst_1,R.mipmap.burst_2,R.mipmap.burst_3,R.mipmap.burst_4,R.mipmap.burst_5};



    public DragBubbleView(Context context) {
        this(context,null);
    }

    public DragBubbleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public DragBubbleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs,R.styleable.DragBubbleView,defStyleAttr,0);
        mBubbleRadius = array.getDimension(R.styleable.DragBubbleView_bubble_radius,mDefaultBubbleRadius);
        mBubbleColor = array.getColor(R.styleable.DragBubbleView_bubble_color,mDefaultBubbleColor);
        mTextStr = array.getString(R.styleable.DragBubbleView_bubble_text);
        mTextSize = array.getDimension(R.styleable.DragBubbleView_bubble_textSize,mDefaultTextSize);
        mTextColor = array.getColor(R.styleable.DragBubbleView_bubble_textColor,mDefaultTextColor);
        array.recycle();

        mFixedBubbleRadius = mBubbleRadius;
        mMovedBubbleRadius = mFixedBubbleRadius;
        mMaxDist = 8 * mBubbleRadius;      //连接最大距离为8倍半径
        MOVE_OFFSET = mMaxDist / 4;

        //气泡画笔
        mBubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBubblePaint.setColor(mBubbleColor);
        mBubblePaint.setStyle(Paint.Style.FILL);

        //文本画笔
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
        mTextRect = new Rect();

        //爆炸画笔
        mBurstPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBurstPaint.setFilterBitmap(true);
        mBurstRect = new Rect();
        mBurstBitmapsArray = new Bitmap[mBurstDrawableArray.length];
        for (int i=0;i<mBurstDrawableArray.length;i++){
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(),mBurstDrawableArray[i]);
            mBurstBitmapsArray[i] = bitmap;
        }

        mBezierPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
        //不动气泡
        if(mFixedBubbleCenter == null){
            mFixedBubbleCenter = new PointF(centerX, centerY);
        }else{
            mFixedBubbleCenter.set(centerX, centerY);
        }

        //可动气泡
        if(mMovedBubbleCenter == null){
            mMovedBubbleCenter = new PointF(centerX, centerY);
        }else{
            mMovedBubbleCenter.set(centerX, centerY);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //1、默认状态
        if(mBubbleState != BUBBLE_STATE_DISMISS){
            canvas.drawCircle(mMovedBubbleCenter.x,mMovedBubbleCenter.y,mMovedBubbleRadius,mBubblePaint);
            mTextPaint.getTextBounds(mTextStr,0,mTextStr.length(),mTextRect);
            canvas.drawText(mTextStr,mMovedBubbleCenter.x - mTextRect.width() / 2f,
                    mMovedBubbleCenter.y + mTextRect.height() / 2f,mTextPaint);
        }
        //2、连接状态
        if(mBubbleState == BUBBLE_STATE_CONNECT){
            canvas.drawCircle(mFixedBubbleCenter.x,mFixedBubbleCenter.y,mFixedBubbleRadius,mBubblePaint);
            mBezierPath.reset();
            //画贝塞尔曲线
            //PE = p(y) - o(y)
            float PE = mFixedBubbleCenter.y - mMovedBubbleCenter.y;
            //OE = p(x) - o(x)
            float OE = mMovedBubbleCenter.x - mFixedBubbleCenter.x;
            float sinAngle = PE / mDist;
            float cosAngle = OE / mDist;

            //G
            float Gx = (mFixedBubbleCenter.x + mMovedBubbleCenter.x) / 2f;
            float Gy = (mFixedBubbleCenter.y + mMovedBubbleCenter.y) / 2f;

            //D
            float Dx = mFixedBubbleCenter.x + sinAngle * mFixedBubbleRadius;
            float Dy = mFixedBubbleCenter.y + cosAngle * mFixedBubbleRadius;

            //C
            float Cx = mMovedBubbleCenter.x + sinAngle * mMovedBubbleRadius;
            float Cy = mMovedBubbleCenter.y + cosAngle * mMovedBubbleRadius;

            //B
            float Bx = mMovedBubbleCenter.x - sinAngle * mMovedBubbleRadius;
            float By = mMovedBubbleCenter.y - cosAngle * mMovedBubbleRadius;

            //A
            float Ax = mFixedBubbleCenter.x - sinAngle * mFixedBubbleRadius;
            float Ay = mFixedBubbleCenter.y - cosAngle * mFixedBubbleRadius;

            mBezierPath.moveTo(Dx,Dy);
            mBezierPath.quadTo(Gx,Gy,Cx,Cy);

            mBezierPath.lineTo(Bx,By);
            mBezierPath.quadTo(Gx,Gy,Ax,Ay);
            mBezierPath.close();
            canvas.drawPath(mBezierPath,mBubblePaint);
        }
        //3、分离状态
        //4、消失状态
        if(mBubbleState == BUBBLE_STATE_DISMISS && mCurrentDrawableIndex < mBurstBitmapsArray.length){
            mBurstRect.set((int) (mMovedBubbleCenter.x - mMovedBubbleRadius),
                    (int) (mMovedBubbleCenter.y - mMovedBubbleRadius),
                    (int) (mMovedBubbleCenter.x + mMovedBubbleRadius),
                    (int) (mMovedBubbleCenter.y + mMovedBubbleRadius));
            canvas.drawBitmap(mBurstBitmapsArray[mCurrentDrawableIndex],null,mBurstRect,mBurstPaint);
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(mBubbleState != BUBBLE_STATE_DISMISS) {
                    mDist = (float) Math.hypot(event.getX() - mFixedBubbleCenter.x,
                            event.getY() - mFixedBubbleCenter.y);
                    if(mDist < mFixedBubbleRadius + MOVE_OFFSET){
                        mBubbleState = BUBBLE_STATE_CONNECT;
                    }else{
                        mBubbleState = BUBBLE_STATE_DEFAULT;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(mBubbleState != BUBBLE_STATE_DEFAULT){
                    mDist = (float) Math.hypot(event.getX() - mFixedBubbleCenter.x,
                            event.getY() - mFixedBubbleCenter.y);
                    mMovedBubbleCenter.x = event.getX();
                    mMovedBubbleCenter.y = event.getY();
                    if(mBubbleState == BUBBLE_STATE_CONNECT) {
                        if (mDist < mMaxDist - MOVE_OFFSET){
                            mFixedBubbleRadius = mMovedBubbleRadius - mDist / 8;   //固定气泡半径变化，可自定义更改
                        }else{
                            mBubbleState = BUBBLE_STATE_APART;
                        }
                    }
                    invalidate();    //调用onDraw方法
                }
                break;
            case MotionEvent.ACTION_UP:
                if(mBubbleState == BUBBLE_STATE_CONNECT){
                    //橡皮筋动画效果
                    startBubbleRestAnim();
                }else if(mBubbleState == BUBBLE_STATE_APART){
                    if(mDist < 2 * mBubbleRadius){
                        startBubbleRestAnim();
                    }else{
                        startBubbleBurstAnim();
                    }
                }
                break;
        }
        return true;
    }

    private void startBubbleRestAnim(){
        ValueAnimator anim = ValueAnimator.ofObject(new PointFEvaluator(),
                new PointF(mMovedBubbleCenter.x,mMovedBubbleCenter.y),
                new PointF(mFixedBubbleCenter.x,mFixedBubbleCenter.y));
        anim.setDuration(200);
        anim.setInterpolator(new OvershootInterpolator(5f));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mMovedBubbleCenter = (PointF) valueAnimator.getAnimatedValue();
                invalidate();  //反复调用onDraw
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mBubbleState = BUBBLE_STATE_DEFAULT;
            }
        });
        anim.start();
    }

    private void startBubbleBurstAnim(){
        mBubbleState = BUBBLE_STATE_DISMISS;
        ValueAnimator anim = ValueAnimator.ofInt(0,mBurstBitmapsArray.length);
        anim.setInterpolator(new LinearInterpolator());
        anim.setDuration(500);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mCurrentDrawableIndex = (int) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        anim.start();
    }

    public void init(){
        mBubbleState = BUBBLE_STATE_DEFAULT;
        mMovedBubbleCenter.x = centerX;
        mMovedBubbleCenter.y = centerY;
        mFixedBubbleRadius = mBubbleRadius;
        mMovedBubbleRadius = mFixedBubbleRadius;
        invalidate();
    }
}
