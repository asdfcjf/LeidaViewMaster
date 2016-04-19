package zdl.softinfo.com.leidaviewmaster;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LeidaView extends View {

    private Context context;

    private final int SHOW = 1;
    private final int DISS = 2;
    private final int DOWN = 3;
    private final int NONE = 4;
    private final int DESTORY = 5;

    //自己的图标
    private Bitmap myBitmap;
    //指针的bitmap
    private Bitmap zhizhen;
    //坐标的bitmap
    private Bitmap zuobiao;
    //旋转角度
    private int angle;
    //主线程停止标记
    private boolean isStop;
    //主线程
    private SearchThread thread;
    //显示ico线程
    private ShowThread showThread;
    private SetHeadIcoThread setHead;
    private SetIcoThread setIco;

    private float bitmapX, bitmapY;
    private int width, height, centerX, centerY, radius;
    private Paint paint;
    //bitmap宽度
    private float myBitmapWidth;
    private float jiao_x;
    //绘制自己bitmap 的位置
    private RectF oval1;
    //设置随机描点的线程指示器
    private boolean isInitPoints = false;
    //宽度一半
    private float halfWidth;
    //显示的ico个数
    private int ico_number;
    //已经加载完成的ico下标记
    private int load_ico_number = -1;
    //ico的描点
    private MyPoint[] points = null;
    //icos
    private Bitmap[] bitmaps = null;
    //控制开关
    private int model;
    //当前选中
    private int select_position = -1;
    //加载icos 线程指示器
    private boolean isLoadIcos = false;
    //加载自己头像 线程指示器
    private boolean isLoadHead = false;

    //消失时的移动速度
    private final int speed = 10;
    //移动间距
    private float chax, chay;
    //最后动画时移动的坐标
    private float nowx = 0, nowy = 0;
    //最后动画 预定移动到的坐标
    private float downX, downY;
    //所有动画完成时的回调事件
    private OnFinished callback;

    //线程池
    private ExecutorService service;

    public void setOnFinishCallback(OnFinished callback) {
        this.callback = callback;
    }

    public LeidaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        myBitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
        myBitmapWidth = myBitmap.getWidth();
        zhizhen = BitmapFactory.decodeResource(context.getResources(), R.drawable.leidazhen);
        zuobiao = BitmapFactory.decodeResource(context.getResources(), R.drawable.leidazuobiao);
        angle = 0;
        isStop = false;
        paint = new Paint();
        thread = new SearchThread();
        showThread = new ShowThread();
        ico_number = 0;
        model = NONE;
        service = Executors.newFixedThreadPool(5);
    }

    //主线程
    class SearchThread extends Thread {
        public void run() {
            int jiange = 2;
            while (!isStop) {
                angle++;
                if (model == DISS && select_position >= 0) {
                    diss();
                }
                if (model == DISS && select_position == -1) {
                    if (nowx == 0 && nowy == 0) {
                        setIcoXY();
                        model = DOWN;
                        nowx = centerX - halfWidth;
                        nowy = centerY - halfWidth;
                    }
                }
                if (model == DOWN) {
                    dissIco();
                    if (Math.sqrt(Math.pow(nowx - downX, 2) + Math.pow(nowy - downY, 2)) < speed / 2 + 1) {
                        Log.w("test", "in down done");
                        nowx = downX;
                        nowy = downY;
                        chax = 0;
                        chay = 0;
                        stopSearch();
                        if (callback != null) {
                            callback.onFinish();
                        }
                    }
                }
                postInvalidate();
                try {
                    Thread.sleep(jiange);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //icos 消失时的动画
    private void diss() {
        if (select_position > ico_number - 1) {
            select_position = ico_number - 1;
            setChaXY(select_position);
        }
        if (select_position == -1) {
            return;
        }
        float x = points[select_position].x;
        float y = points[select_position].y;
        x += chax;
        y += chay;
        points[select_position].x = x;
        points[select_position].y = y;

        if (Math.sqrt(Math.pow(x - bitmapX, 2) + Math.pow(y - bitmapY, 2)) < speed / 2 + 1) {
            x = bitmapX;
            y = bitmapY;
            points[select_position].x = x;
            points[select_position].y = y;
            select_position--;
            setChaXY(select_position);
        }

    }

    private void dissIco() {
        nowx += chax;
        nowy += chay;
    }

    //设置 icos 消失时每个ico的移动间距
    private void setChaXY(int selectPosition) {
        if (selectPosition < 0) {
            return;
        }
        float x = points[select_position].x;
        float y = points[select_position].y;
        double juli = Math.sqrt(Math.pow(x - bitmapX, 2) + Math.pow(y - bitmapY, 2));
        double time = juli / speed;
        chax = (float) ((Math.abs(x - bitmapX)) / time);
        chay = (float) ((Math.abs(y - bitmapY)) / time);
        if (x > bitmapX) {
            chax *= -1;
        }
        if (y > bitmapY) {
            chay *= -1;
        }
    }

    //设置最后动画移动式的移动间距
    private void setIcoXY() {
        Display play = ((MainActivity) context).getWindowManager().getDefaultDisplay();
        int srceenHeight = play.getHeight();
        //当x = 0 或 y = 0 或 y < 3/4屏幕高度  说明没取到应该移动到的目标位置，这样可以预防最后动画乱跑
        if (downX == 0 || downY == 0 || downY <= srceenHeight / 4 * 3) {
            downX = 30;
            downY = srceenHeight - 200;
        }
        nowx = bitmapX;
        nowy = bitmapY;

        double juli = Math.sqrt(Math.pow(nowx - downX, 2) + Math.pow(nowy - downY, 2));
        double time = juli / speed;
        chax = (float) ((Math.abs(nowx - downX)) / time);
        chay = (float) ((Math.abs(nowy - downY) / time));
        if (nowx > downX) {
            chax *= -1;
        }
        if (nowy > downY) {
            chay *= -1;
        }

    }

    //显示icos的线程
    class ShowThread extends Thread {
        public void run() {
            while (isLoadIcos && isLoadHead) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            model = SHOW;
            while (!isStop && select_position < ico_number) {
                if (select_position < load_ico_number) {
                    select_position++;
                    postInvalidate();
                } else if (select_position == load_ico_number && load_ico_number == ico_number - 1) {
                    select_position = ico_number;
                }
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            select_position--;
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                e.printStackTrace();
            }
            model = DISS;
            select_position = ico_number;
        }
    }

    public void startSearch() {
//		thread.start();
        if (bitmaps == null) {
            Log.w("test_bug", "in getDefault icos");
            //如果没有促销信息，就显示几个默认头像
            String[] urls = {
                    "http://daogou.zdl123.com:80/daogou/upload/image/7af13621-99f2-413f-a3f9-147247bd6054.jpg",
                    "	http://daogou.zdl123.com:80/daogou/upload/image/3f50dcf8-5bc0-4ae2-9d6d-0e97e3ac49be.jpg",
                    "	http://daogou.zdl123.com:80/daogou/upload/image/849529cf-6d57-4111-98dc-197a7119917d.jpg",
                    "	http://daogou.zdl123.com:80/daogou/upload/image/95c146bd-d63c-48fc-b5b8-b106915572cb.jpg",
                    "	http://daogou.zdl123.com:80/daogou/upload/image/0bd06ace-1389-498f-85eb-4ff829c54569.jpg",
                    "	http://daogou.zdl123.com:80/daogou/upload/image/c1c658cc-92ee-475f-a3e1-d5154f8a3199.jpg",
                    "	http://daogou.zdl123.com:80/daogou/upload/image/a16d2911-c14c-4b09-8712-94d9100c87e1.jpg",
                    "	http://daogou.zdl123.com:80/daogou/upload/image/634c8034-ea40-4399-a375-e12a2655969a.jpg",
                    "	http://daogou.zdl123.com:80/daogou/upload/image/8448d07c-7c09-44ae-8c1c-b03a771bb0fd.jpg",
                    "	http://daogou.zdl123.com:80/daogou/upload/image/f89c4afd-1e2d-45c5-a406-d25c2c48b08d.jpg"
            };
            setIcos(urls);
        }
        thread.setPriority(10);
        service.execute(thread);
    }

    public void stopSearch() {
        isStop = true;
    }

    //设置自己头像的resource
    public void setMyHead(int resource) {
        if (myBitmap != null) {
            myBitmap.recycle();
        }

        myBitmap = BitmapFactory.decodeResource(context.getResources(), resource);
        myBitmapWidth = myBitmap.getWidth();
    }

    //设置自己头像的url
    public void setMyHead(String url) {
        isLoadHead = true;
        if (myBitmap != null) {
            myBitmap.recycle();
        }

//		new SetHeadIcoThread(url).start();
        setHead = new SetHeadIcoThread(url);
        setHead.setPriority(10);
        service.execute(setHead);
    }

    //从网络获取自己头像的线程
    class SetHeadIcoThread extends Thread {
        String url;

        public SetHeadIcoThread(String url) {
            this.url = url;
        }

        public void run() {
            myBitmap = getBitmapFromUrl(url);
        }
    }

    //设置icos
    public void setIcos(String[] urls) {
        ico_number = urls.length;
        bitmaps = new Bitmap[ico_number];
        isLoadIcos = true;
        setIco = new SetIcoThread(urls);
        setIco.setPriority(9);
        service.execute(setIco);
        new SetIcoListenerThread(setIco).start();
    }

    class SetIcoListenerThread extends Thread {
        SetIcoThread setIco;

        public SetIcoListenerThread(SetIcoThread setIco) {
            this.setIco = setIco;
        }

        public void run() {
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (load_ico_number < ico_number - 1) {
                ico_number = load_ico_number + 1;
            }
            Log.w("test_bug", "isShutdown");
            setIco = null;
        }
    }

    //从网络获取icos头像的线程
    class SetIcoThread extends Thread {
        String[] urls;

        public SetIcoThread(String[] urls) {
            this.urls = urls;
        }

        public void run() {
            for (int i = 0; i < ico_number; i++) {
                bitmaps[i] = getBitmapFromUrl(urls[i]);
                if (load_ico_number == ico_number - 1) {
                    return;
                }
                load_ico_number = i;
                if (i == 0) {
                    isLoadIcos = false;
                }
            }
        }
    }

    //获取bitmap
    private Bitmap getBitmapFromUrl(String url) {
        Bitmap b = null;
        try {
            InputStream in;
            URL u = new URL(url);
            in = u.openStream();
            if (in == null) {
                b = BitmapFactory.decodeResource(context.getResources(), R.drawable.touxiang70);
            } else {
//                b = BitmapFactory.decodeStream(in);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(in,null,options);
                in.close();
                in = u.openStream();
                int width = options.outWidth;
                int size = width / 100;
                options.inJustDecodeBounds = false;
                options.inSampleSize = size;
                b = BitmapFactory.decodeStream(in,null,options);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getRoundBitmap(b);
    }

    /**
     * 获取裁剪后的圆形图片
     */
    public Bitmap getRoundBitmap(Bitmap bmp) {
        if (bmp == null) {
            bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.touxiang70);
        }
        Bitmap output = Bitmap.createBitmap(bmp.getWidth(),
                bmp.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bmp.getWidth(),
                bmp.getHeight());

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(bmp.getWidth() / 2,
                bmp.getHeight() / 2, bmp.getWidth() / 2,
                paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bmp, rect, rect, paint);
        try{
            bmp.recycle();
            bmp = null;
        }catch (Exception e){
            e.printStackTrace();
        }
        return output;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);

        centerX = width / 2;
        centerY = height / 2;
        radius = width / 9;
        myBitmapWidth = radius;
        bitmapX = centerX - myBitmapWidth / 2;
        bitmapY = centerY - myBitmapWidth / 2;
        halfWidth = myBitmapWidth / 2;
        jiao_x = (float) Math.sqrt(Math.pow(radius * 5, 2) / 2);
        //自己头像大小
        oval1 = new RectF(centerX - halfWidth - 30, centerY - halfWidth - 30, centerX + halfWidth + 30, centerY + halfWidth + 30);
        if (!isInitPoints) {
//			service.execute(new Thread(){
//				public void run(){
//				}
//			});
            initPoints();
        }
    }

    //最后动画移动时的控制移动
    public void setDown(float x, float y) {
        this.downX = x;
        this.downY = y;
    }

    //初始化描点
    private void initPoints() {
        isInitPoints = true;
        points = new MyPoint[ico_number];
        for (int i = 0; i < ico_number; i++) {
            MyPoint p = new MyPoint();
            double x;
            double y;
            do {
                x = Math.random() * width;
                y = Math.random() * height;
            } while (!validate(x, y));
            p.x = (float) x;
            p.y = (float) y;
            points[i] = p;
        }
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!showThread.isAlive()) {
            try {
                showThread.setPriority(10);
//				showThread.start();
                service.execute(showThread);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //验证描点位置是否合格
    private boolean validate(double x, double y) {
        if (Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2)) < (5 * radius - myBitmapWidth)) {
            return true;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);

        if(model == DESTORY){
            return;
        }

        RectF oval = new RectF(centerX - 5 * radius, centerY - 5 * radius, centerX + 5 * radius, centerY + 5 * radius);
        canvas.drawBitmap(zuobiao, null, oval, paint);

        if (model == DOWN) {
            RectF oval1 = new RectF(nowx, nowy, nowx + myBitmapWidth, nowy + myBitmapWidth);
            if (myBitmap != null) {
                canvas.drawBitmap(myBitmap, null, oval1, paint);
            }
            return;
        }

        canvas.save();
        canvas.rotate(angle, centerX, centerY);
        canvas.drawBitmap(zhizhen, null, oval, paint);
        canvas.restore();
        if (select_position != -1 && model != NONE) {
            if (model == SHOW) {
                showIcos(canvas);
            } else if (model == DISS) {
                dissIcos(canvas);
            }
        }
        //绘制自己头像
        try {
            canvas.drawBitmap(myBitmap, null, oval1, paint);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showIcos(Canvas canvas) {
        for (int i = 0; i <= select_position && select_position < ico_number; i++) {
            float x = points[i].x;
            float y = points[i].y;
            RectF oval = new RectF(x, y, x + myBitmapWidth, y + myBitmapWidth);
            canvas.drawBitmap(bitmaps[i], null, oval, paint);
        }
    }

    private void dissIcos(Canvas canvas) {
        for (int i = 0; i < ico_number && i > -1; i++) {
            float x = points[i].x;
            float y = points[i].y;
            RectF oval = new RectF(x, y, x + myBitmapWidth, y + myBitmapWidth);
            canvas.drawBitmap(bitmaps[i], null, oval, paint);
        }
    }

    class MyPoint {
        private float x;
        private float y;
    }

    public interface OnFinished {
         void onFinish();
    }

    public void recycle() {
        model = DESTORY;
        recyclerBitmap(myBitmap);
        myBitmap = null;
        recyclerBitmap(zhizhen);
        zhizhen = null;
        recyclerBitmap(zuobiao);
        zuobiao = null;
        for (int i = 0; i < bitmaps.length; i++) {
            if (bitmaps[i] != null && !bitmaps[i].isRecycled()) {
                recyclerBitmap(bitmaps[i]);
                bitmaps[i] = null;
            }
        }
        bitmaps = null;
        for(int i = 0; i < points.length;i++){
            points[i] = null;
        }
        points = null;
        oval1 = null;
        points = null;
        thread = null;
        showThread = null;
        setHead = null;
        setIco = null;
        callback = null;
        if (service != null) {
            service.shutdownNow();
            service = null;
        }
    }

    private void recyclerBitmap(Bitmap bitmap){
        if(bitmap != null && !bitmap.isRecycled()){
            try {
                bitmap.recycle();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
