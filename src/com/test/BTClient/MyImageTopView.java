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

public class MyImageTopView extends ViewGroup {//�Զ���ؼ�����ViewGroup�̳ж���
	private GestureDetector gesDetector;// ���Ƽ����
	private Scroller scroller;// ��������
	private int currentImageIndex=0;//��¼��ǰ��ʾ��ͼƬ�����
	private boolean fling = false;// ��ӱ�־����ֹ�ײ��onTouch�¼��ظ�����UP�¼�	
	private Handler mHandler;//handler�������ڷ��͡����պʹ�����Ϣ
	private Context context;//�����Ķ���	
	public MyImageTopView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		this.context=context;
		init();	//ִ�г�ʼ������
		this.setOnTouchListener(new MyOnTouchListener());//��Ӵ����¼�����
	}	
	public void init() {
		scroller = new Scroller(context);//����������
		mHandler = new Handler(){//����Handler���󣬲���д�䴦����Ϣ�ķ���
			public void handleMessage(Message msg) {
				if(msg.what==0x11){
					//�յ���Ϣ���л���ָ����ͼƬ
					scrollToImage((currentImageIndex + 1) % getChildCount());
				}
			}
		};
		gesDetector = new GestureDetector(context, new OnGestureListener() {			
			public boolean onSingleTapUp(MotionEvent e) {// ��ָ�뿪����������һɲ�ǵ��ø÷���
				return false;
			}			
			public void onShowPress(MotionEvent e) {// ��ָ���ڴ������ϣ�����ʱ�䷶Χ�ڰ�����Ч���ڳ���֮ǰ
			}
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY) { // ��ָ�ڴ������ϻ���
				// ���������Χ�ڵ�һҳ�����һҳ֮�䣬distanceX>0��ʾ���һ�����distanceX < 0��ʾ���󻬶�
				//�����������������Χ�������κβ���
				if ((distanceX > 0 && getScrollX() < getWidth()
						* (getChildCount() - 1))
						|| (distanceX < 0 && getScrollX() > 0)) {
					scrollBy((int) distanceX, 0);//�����ľ��룬�ڴ�ֻ��Ҫˮƽ��������ֱ�������Ϊ0
				}
				return true;
			}
			public void onLongPress(MotionEvent e) {// ��ָ������Ļ�ϳ���һ��ʱ�䣬����û���ɿ�
			}
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {// ��ָ�ڴ�������Ѹ���ƶ������ɿ��Ķ���
				// �ж��Ƿ�ﵽ��С�����ٶȣ�ȡ����ֵ
				if (Math.abs(velocityX) > ViewConfiguration.get(context)
						.getScaledMinimumFlingVelocity()) {//����ٶȳ�����С�ٶ�
					if (velocityX > 0 && currentImageIndex >= 0) {
						fling = true;//velocityX>0��ʾ���󻬶�
						scrollToImage((currentImageIndex - 1 + getChildCount())
								% getChildCount());
					} else if (velocityX < 0
							&& currentImageIndex <= getChildCount() - 1) {						
						fling = true;//velocityX<0��ʾ���һ���
						scrollToImage((currentImageIndex + 1)
								% getChildCount());
					}
				}
				return true;
			}
			public boolean onDown(MotionEvent e) {// ��ָ�ոսӴ�������������һɲ�ǣ����Ǵ�����һ��
				return false;
			}
		});
	}
	
	public void scrollToImage(int targetIndex){//��ת��Ŀ��ͼƬ
		if (targetIndex != currentImageIndex && getFocusedChild() != null
				&& getFocusedChild() == getChildAt(currentImageIndex)) {
			getFocusedChild().clearFocus(); //��ǰͼƬ�������
		}
		final int delta =targetIndex * getWidth() - getScrollX();//��Ҫ�����ľ���
		int time =Math.abs(delta) * 5;//time��ʾ������ʱ�䣬��λΪ���룬������ʱ���ǻ����ľ����5��
		scroller.startScroll(getScrollX(), 0, delta, 0, time);		
		invalidate();//ˢ��ҳ��
		currentImageIndex = targetIndex; //�ı䵱ǰͼƬ������
		((BTClient)context).resetImg();
		//�ı��·�ԲȦ��״̬
		((BTClient)context).imgViews[currentImageIndex].setImageResource(R.drawable.choosed);			
	}	
	public void computeScroll() {//��д����ķ�������¼����������λ��
		super.computeScroll();
		if (scroller.computeScrollOffset()) {
			scrollTo(scroller.getCurrX(), 0);
			postInvalidate();
		}
	}
	private class MyOnTouchListener implements OnTouchListener{	//�����¼�������	
		public boolean onTouch(View v, MotionEvent event) {
			gesDetector.onTouchEvent(event);// �������¼�����GestureDetector������
			if (event.getAction() == MotionEvent.ACTION_UP) {				
				if (!fling) {// ���û�ֹͣ�϶�
					snapToDestination();
				}
				fling = false;
			}
			return true;
		}		
	}
	//�÷�����ViewGroup�м̳ж�����������һ�����󷽷����÷�������ָ��������Ŀؼ���ΰڷţ����ؼ���С�����仯ʱ����ص��÷���
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {		
		for (int i = 0; i < getChildCount(); i++) {// ���ò��֣�������ͼ˳���������
			View child = getChildAt(i);// ��ȡ��ÿһ���ӿؼ�
			child.setVisibility(View.VISIBLE);// ���øÿؼ�Ϊ�ɼ�
			child.measure(right - left, bottom - top);
			child.layout(i * getWidth(), 0, (i + 1) * getWidth(), getHeight());
		}
	}	
	private void snapToDestination() {//������ָ��ͼƬ
		scrollToImage((getScrollX() + (getWidth() / 2)) / getWidth());//�������룬����һ�������һ��ͼƬ
	}
	
	public void initImages(int[] imgIds) {// ��ʼ����ʾ��ͼƬ
		int num = imgIds.length;// ��ȡͼƬ���ϵĳ���
		this.removeAllViews();// ������еĿؼ�
		for (int i = 0; i < num; i++) {// ѭ��������ͼƬ�ؼ�
			ImageView imageView = new ImageView(getContext());
			imageView.setImageResource(imgIds[i]);// ����ÿ��ͼƬ�ؼ���ͼƬ
			this.addView(imageView);// ��ͼƬ��ӵ��Զ���Ŀؼ���
		}
	}
}
