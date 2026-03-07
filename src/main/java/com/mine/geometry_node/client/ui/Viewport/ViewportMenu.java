package com.mine.geometry_node.client.ui.Viewport;

import com.mine.geometry_node.client.ui.UIConstants;
import com.mine.geometry_node.core.node.NodeCategory;
import com.mine.geometry_node.core.node.NodeRegistry;
import com.mine.geometry_node.core.node.nodes.BaseNode;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.TextWatcher;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.*;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class ViewportMenu extends FrameLayout {

    private LinearLayout mContentLayout;
    private LinearLayout mListContainer;
    private EditText mSearchBox;

    private Viewport mViewport;
    private float mMenuX, mMenuY;

    private final Stack<NodeCategory> mHistory = new Stack<>();
    private NodeCategory mCurrentFolder;

    public ViewportMenu(Context context) {
        super(context);
        initUI(context);
        navigateTo(NodeRegistry.INSTANCE.ROOT);
    }

    private void initUI(Context context) {
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        this.setOnClickListener(v -> dismiss());

        mContentLayout = new LinearLayout(context);
        mContentLayout.setOrientation(LinearLayout.VERTICAL);
        mContentLayout.setBackground(createRectDrawable(
                UIConstants.ViewPort.NodeMenu.BG_COLOR,
                UIConstants.ViewPort.NodeMenu.BORDER_RADIUS));
        mContentLayout.setPadding(4, 4, 4, 4);
        mContentLayout.setOnClickListener(v -> {});

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(UIConstants.ViewPort.NodeMenu.ITEM_WEIGHT, LayoutParams.WRAP_CONTENT);
        mContentLayout.setLayoutParams(lp);

        mSearchBox = new EditText(context);
        mSearchBox.setHint(Component.translatable("menu.node.search").getString());
        float searchFontSize = UIConstants.ViewPort.NodeMenu.HEIGHT_SEARCH_BOX * (float)UIConstants.ViewPort.NodeMenu.TEXT_SIZE;
        mSearchBox.setTextSize(0, searchFontSize);
        mSearchBox.setTextColor(UIConstants.ViewPort.NodeMenu.TEXT_COLOR_SEARCH);
        mSearchBox.setHintTextColor(0xFF666666);
        mSearchBox.setBackground(createRectDrawable(UIConstants.ViewPort.NodeMenu.SEARCH_BG_COLOR, 4));
        mSearchBox.setPadding(10, 0, 10, 0);

        mSearchBox.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) { performSearch(s.toString()); }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(-1, UIConstants.ViewPort.NodeMenu.HEIGHT_SEARCH_BOX);
        searchLp.setMargins(4, 4, 4, 6);
        mContentLayout.addView(mSearchBox, searchLp);

        ScrollView sv = new ScrollView(context);
        mListContainer = new LinearLayout(context);
        mListContainer.setOrientation(LinearLayout.VERTICAL);
        sv.addView(mListContainer);

        mContentLayout.addView(sv, new LinearLayout.LayoutParams(-1, 300));
        addView(mContentLayout);
    }

    public void showAt(float x, float y, ViewGroup parent) {
        if (parent instanceof Viewport) mViewport = (Viewport) parent;
        mMenuX = x; mMenuY = y;

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mContentLayout.getLayoutParams();
        lp.leftMargin = (int) x; lp.topMargin = (int) y;

        if (parent != null) {
            if (x + 200 > parent.getWidth()) lp.leftMargin = (int)(parent.getWidth() - 220);
            if (y + 300 > parent.getHeight()) lp.topMargin = (int)(parent.getHeight() - 320);
        }
        mContentLayout.setLayoutParams(lp);

        if (this.getParent() != null) ((ViewGroup)this.getParent()).removeView(this);
        parent.addView(this);
        mSearchBox.requestFocus();
    }

    private void dismiss() {
        if (getParent() != null) ((ViewGroup) getParent()).removeView(this);
    }

    // --- 核心导航&渲染逻辑 ---

    private void navigateTo(NodeCategory folder) {
        if (mCurrentFolder != null && folder != mCurrentFolder) {
            mHistory.push(mCurrentFolder); // 记录来时的路
        }
        mCurrentFolder = folder;
        renderCurrentFolder();
    }

    private void navigateBack() {
        if (!mHistory.isEmpty()) {
            mCurrentFolder = mHistory.pop();
            renderCurrentFolder();
        }
    }

    private void renderCurrentFolder() {
        mListContainer.removeAllViews();

        // 1. “返回”
        if (mCurrentFolder != NodeRegistry.INSTANCE.ROOT) {
            addClickItem("← " + Component.translatable("menu.node.back").getString(), 0xFF888888, v -> navigateBack());
        }

        // 2. 渲染子文件夹
        for (NodeCategory sub : mCurrentFolder.getSubCategories()) {
            String label = Component.translatable(sub.translationKey).getString() + "  ›";
            addClickItem(label, UIConstants.ViewPort.NodeMenu.TEXT_COLOR, v -> {
                mSearchBox.setText("");
                navigateTo(sub);
            });
        }

        // 3. 渲染节点
        for (BaseNode node : mCurrentFolder.getNodes()) {
            String label = node.getDefaultDefinition().displayName().getString();
            addClickItem(label, UIConstants.ViewPort.NodeMenu.TEXT_COLOR, v -> {
                if (mViewport != null) {
                    mViewport.addNode(mMenuX, mMenuY, node.getTypeId());
                }
                dismiss();
            });
        }
    }

    // --- 搜索逻辑 ---

    private void performSearch(String query) {
        if (query.trim().isEmpty()) {
            renderCurrentFolder();
            return;
        }
        mListContainer.removeAllViews();
        String q = query.toLowerCase().trim();

        for (com.mine.geometry_node.core.node.nodes.NodeDef def : NodeRegistry.INSTANCE.getAllDefinitions()) {
            String name = def.displayName().getString();
            if (name.toLowerCase().contains(q)) {
                addClickItem(name, UIConstants.ViewPort.NodeMenu.TEXT_COLOR, v -> {
                    if (mViewport != null) {
                        mViewport.addNode(mMenuX, mMenuY, def.typeId());
                    }
                    dismiss();
                });
            }
        }
    }

    // --- 底层 UI 组件 ---

    private void addClickItem(String text, int color, OnClickListener listener) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        float fontSize = UIConstants.ViewPort.NodeMenu.ITEM_HEIGHT * (float)UIConstants.ViewPort.NodeMenu.TEXT_SIZE;
        tv.setTextSize(0, fontSize);
        tv.setTextColor(color);
        tv.setPadding(12, 0, 12, 0);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setOnClickListener(listener);

        tv.setOnHoverListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                tv.setBackground(createRectDrawable(UIConstants.ViewPort.NodeMenu.HOVER_COLOR, 4));
                tv.setTextColor(UIConstants.ViewPort.NodeMenu.TEXT_COLOR_HOVER);
            } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                tv.setBackground(null);
                tv.setTextColor(color);
            }
            return false;
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, UIConstants.ViewPort.NodeMenu.ITEM_HEIGHT);
        lp.setMargins(2, 1, 2, 1);
        mListContainer.addView(tv, lp);
    }

    private ShapeDrawable createRectDrawable(int color, int radius) {
        ShapeDrawable d = new ShapeDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }
}