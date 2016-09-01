package com.test.BTClient;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Scroller;

public class MyImageTopView extends ViewGroup {//自定义控件，从ViewGroup继承而来
	private GestureDetector gesDetector;// 手势检测器
	private Scroller scroller;// 滚动对象
	private int currentImageIndex=0;//记录当前显示的图片的序号
	private boolean fling = false;// 添加标志，防止底层的onTouch事件重复处理UP事件	
	private Handler mHandler;//handler对象，用于发送、接收和处理消息
	private Context context;//上下文对象	
	public MyImageTopView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		this.context=context;
		init();	//执行初始化操作
		this.setOnTouchListener(new MyOnTouchListener());//添加触摸事件处理
	}	
	public void init() {
		scroller = new Scroller(context);//创建滚动条
		mHandler = new Handler(){//创建Handler对象，并重写其处理消息的方法
			public void handleMessage(Message msg) {
				if(msg.what==0x11){
					//收到消息后，切换到指定的图片
					scrollToImage((currentImageIndex + 1) % getChildCount());
				}
			}
		};
		gesDetector = new GestureDetector(context, new OnGestureListener() {			
			public boolean onSingleTapUp(MotionEvent e) {// 手指离开触摸屏的那一刹那调用该方法
				return false;
			}			
			public void onShowPress(MotionEvent e) {// 手指按在触摸屏上，它的时间范围在按下起效，在长按之前
			}
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY) { // 手指在触摸屏上滑动
				// 如果滑动范围在第一页和最后一页之间，distanceX>0表示向右滑动，distanceX < 0表示向左滑动
				//如果超出了这两个范围，则不做任何操作
				if ((distanceX > 0 && getScrollX() < getWidth()
						* (getChildCount() - 1))
						|| (distanceX < 0 && getScrollX() > 0)) {
					scrollBy((int) distanceX, 0);//滚动的距离，在此只需要水平滚动，垂直方向滚动为0
				}
				return true;
			}
			public void onLongPress(MotionEvent e) {// 手指按在屏幕上持续一段时间，并且没有松开
			}
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {// 手指在触摸屏上迅速移动，并松开的动作
				// 判断是否达到最小滑动速度，取绝对值
				if (Math.abs(velocityX) > ViewConfiguration.get(context)
						.getScaledMinimumFlingVelocity()) {//如果速度超过最小速度
					if (velocityX > 0 && currentImageIndex >= 0) {
						fling = true;//velocityX>0表示向左滑动
						scrollToImage((currentImageIndex - 1 + getChildCount())
								% getChildCount());
					} else if (velocityX < 0
							&& currentImageIndex <= getChildCount() - 1) {						
						fling = true;//velocityX<0表示向右滑动
						scrollToImage((currentImageIndex + 1)
								% getChildCount());
					}
				}
				return true;
			}
			public boolean onDown(MotionEvent e) {// 手指刚刚接触到触摸屏的那一刹那，就是触的那一下
				return false;
			}
		});
	}
	
	public void scrollToImage(int targetIndex){//跳转到目标图片
		if (targetIndex != currentImageIndex && getFocusedChild() != null
				&& getFocusedChild() == getChildAt(currentImageIndex)) {
			getFocusedChild().clearFocus(); //当前图片清除焦点
		}
		final int delta =targetIndex * getWidth() - getScrollX();//需要滑动的距离
		int time =Math.abs(delta) * 5;//time表示滑动的时间，单位为毫秒，滑动的时间是滑动的距离的5倍
		scroller.startScroll(getScrollX(), 0, delta, 0, time);		
		invalidate();//刷新页面
		currentImageIndex = targetIndex; //改变当前图片的索引
		((BTClient)context).resetImg();
		//改变下方圆圈的状态
		((BTClient)context).imgViews[currentImageIndex].setImageResource(R.drawable.choosed);			
	}	
	public void computeScroll() {//重写父类的方法，记录滚动条的新位置
		super.computeScroll();
		if (scroller.computeScrollOffset()) {
			scrollTo(scroller.getCurrX(), 0);
			postInvalidate();
		}
	}
	private class MyOnTouchListener implements OnTouchListener{	//触摸事件监听器	
		public boolean onTouch(View v, MotionEvent event) {
			gesDetector.onTouchEvent(event);// 将触摸事件交由GestureDetector来处理
			if (event.getAction() == MotionEvent.ACTION_UP) {				
				if (!fling) {// 当用户停止拖动
					snapToDestination();
				}
				fling = false;
			}
			return true;
		}		
	}
	//该方法从ViewGroup中继承而来，是它的一个抽象方法，该方法用于指定容器里的控件如何摆放，当控件大小发生变化时，会回调该方法
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {		
		for (int i = 0; i < getChildCount(); i++) {// 设置布局，将子视图顺序横向排列
			View child = getChildAt(i);// 获取到每一个子控件
			child.setVisibility(View.VISIBLE);// 设置该控件为可见
			child.measure(right - left, bottom - top);
			child.layout(i * getWidth(), 0, (i + 1) * getWidth(), getHeight());
		}
	}	
	private void snapToDestination() {//滑动到指定图片
		scrollToImage((getScrollX() + (getWidth() / 2)) / getWidth());//四舍五入，超过一半进入下一张图片
	}
	
	public void initImages(int[] imgIds) {// 初始化显示的图片
		int num = imgIds.length;// 获取图片集合的长度
		this.removeAllViews();// 清空所有的控件
		for (int i = 0; i < num; i++) {// 循环逐个添加图片控件
			ImageView imageView = new ImageView(getContext());
			imageView.setImageResource(imgIds[i]);// 设置每个图片控件的图片
			this.addView(imageView);// 将图片添加到自定义的控件中
		}
	}
}
