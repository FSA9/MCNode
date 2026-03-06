package com.mine.geometry_node.core.node;

import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.RectF;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;

/**
 * 节点 UI 组件：负责节点外观绘制、内部布局及选中状态表现
 */
public class TestNode extends LinearLayout {

    // --- 视觉布局常量 ---
    private static final int NODE_WIDTH = 120;
    private static final int HEADER_HEIGHT = 20;
    private static final int ROW_HEIGHT = 20;
    private static final float PORT_RADIUS = 5f;
    private static final float PORT_TOUCH_RADIUS = 5f;
    private static final float CORNER_RADIUS = 8f;
    private static final float TEXT_SIZE_RATIO = 0.5f;

    // --- 配色方案 ---
    private static final int COLOR_HEADER   = 0xFF555555;
    private static final int COLOR_BODY     = 0xFF303030;
    private static final int COLOR_OUTLINE  = 0xFF101010;
    private static final int COLOR_SELECTED = 0xFFFFFFFF; // 选中时的高亮边框颜色
    private static final int COLOR_TEXT     = 0xFFEEEEEE;
    private static final int COLOR_PORT_IN  = 0xFFE6E6E6;
    private static final int COLOR_PORT_OUT = 0xFFD95763;

    // --- 内部数据与工具 ---
    private final String[] mInputs = {"Input", "Size", "Attribute"};
    private final String[] mOutputs = {"Output", "Object"};
    private final Paint mPaint = new Paint();
    private final RectF mTempRect = new RectF();
    private boolean mIsSelected = false;

    public TestNode(Context context) {
        super(context);
        initViewProperties();
        initLayout(context);
    }

    /** 初始化组件基础属性 */
    private void initViewProperties() {
        setOrientation(VERTICAL);
        setWillNotDraw(false);
        setClipChildren(false);
        setLayoutParams(new LayoutParams(NODE_WIDTH, LayoutParams.WRAP_CONTENT));
        mPaint.setAntiAlias(true);
    }

    /** 构建节点内部的标题和行列 UI */
    private void initLayout(Context context) {
        // 1. 添加标题
        TextView title = new TextView(context);
        title.setText("Cube");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(0, HEADER_HEIGHT * (TEXT_SIZE_RATIO + 0.1f));
        title.setGravity(Gravity.CENTER);
        addView(title, new LayoutParams(LayoutParams.MATCH_PARENT, HEADER_HEIGHT));

        // 2. 混合添加输入输出行
        int maxRows = Math.max(mInputs.length, mOutputs.length);
        for (int i = 0; i < maxRows; i++) {
            LinearLayout row = createRowContainer(context);
            row.addView(createLabel(context, i < mInputs.length ? mInputs[i] : "", Gravity.LEFT));
            row.addView(createLabel(context, i < mOutputs.length ? mOutputs[i] : "", Gravity.RIGHT));
            addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, ROW_HEIGHT));
        }
    }

    /** 创建行容器 */
    private LinearLayout createRowContainer(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padding = (int) (PORT_RADIUS * 2);
        row.setPadding(padding, 0, padding, 0);
        return row;
    }

    /** 创建属性标签 */
    private TextView createLabel(Context context, String text, int gravity) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(COLOR_TEXT);
        tv.setTextSize(0, ROW_HEIGHT * TEXT_SIZE_RATIO);
        tv.setGravity(gravity | Gravity.CENTER_VERTICAL);
        return tv;
    }

    /** 设置选中状态并触发重绘 */
    public void setSelected(boolean selected) {
        if (mIsSelected != selected) {
            mIsSelected = selected;
            invalidate();
        }
    }

    public boolean isSelected() { return mIsSelected; }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();

        // 1. 绘制主体背景
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(COLOR_BODY);
        mTempRect.set(0, 0, w, h);
        canvas.drawRoundRect(mTempRect, CORNER_RADIUS, (int) CORNER_RADIUS, mPaint);

        // 2. 绘制标题栏背景
        canvas.save();
        canvas.clipRect(0, 0, w, HEADER_HEIGHT);
        mPaint.setColor(COLOR_HEADER);
        canvas.drawRoundRect(0, 0, w, HEADER_HEIGHT + CORNER_RADIUS, CORNER_RADIUS, (int) CORNER_RADIUS, mPaint);
        canvas.restore();

        // 3. 绘制外轮廓（根据选中状态切换粗细和颜色）
        mPaint.setStyle(Paint.Style.STROKE);
        if (mIsSelected) {
            mPaint.setStrokeWidth(3.0f);
            mPaint.setColor(COLOR_SELECTED);
        } else {
            mPaint.setStrokeWidth(1.5f);
            mPaint.setColor(COLOR_OUTLINE);
        }
        mTempRect.set(0, 0, w, h);
        canvas.drawRoundRect(mTempRect, CORNER_RADIUS, (int) CORNER_RADIUS, mPaint);

        // 4. 绘制连接端口
        drawPorts(canvas, w);

        super.onDraw(canvas);
    }

    /** 绘制左右侧的连接圆点 */
    private void drawPorts(Canvas canvas, float w) {
        mPaint.setStyle(Paint.Style.FILL);
        // 输入端口
        mPaint.setColor(COLOR_PORT_IN);
        for (int i = 0; i < mInputs.length; i++) {
            canvas.drawCircle(0, HEADER_HEIGHT + (i + 0.5f) * ROW_HEIGHT, PORT_RADIUS, mPaint);
        }
        // 输出端口
        mPaint.setColor(COLOR_PORT_OUT);
        for (int i = 0; i < mOutputs.length; i++) {
            canvas.drawCircle(w, HEADER_HEIGHT + (i + 0.5f) * ROW_HEIGHT, PORT_RADIUS, mPaint);
        }
    }

    /**
     * 获取指定端口相对于节点左上角的坐标
     * @param index 端口索引
     * @param isInput true为输入端口，false为输出端口
     * @return float[]{x, y}
     */
    public float[] getPortPosition(int index, boolean isInput) {
        float y = HEADER_HEIGHT + (index + 0.5f) * ROW_HEIGHT;
        float x = isInput ? 0 : getWidth(); // 输入在左(0)，输出在右(Width)
        return new float[]{x, y};
    }

    /**
     * 检测节点内部坐标是否命中端口
     * @param localX 节点内部 X 坐标
     * @param localY 节点内部 Y 坐标
     * @return 命中的端口信息索引，未命中返回 -1
     */
    public int hitTestPort(float localX, float localY, boolean checkInput) {
        int count = checkInput ? mInputs.length : mOutputs.length;
        float targetX = checkInput ? 0 : getWidth();

        for (int i = 0; i < count; i++) {
            float portY = HEADER_HEIGHT + (i + 0.5f) * ROW_HEIGHT;
            float dx = localX - targetX;
            float dy = localY - portY;
            // 简单的距离判定
            if (dx * dx + dy * dy <= PORT_TOUCH_RADIUS * PORT_TOUCH_RADIUS) {
                return i;
            }
        }
        return -1;
    }

    public int getInputCount() { return mInputs.length; }
    public int getOutputCount() { return mOutputs.length; }
}