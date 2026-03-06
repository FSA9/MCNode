package com.mine.geometry_node.client.ui.Viewport;

import com.mine.geometry_node.client.ui.UIConstants;
import com.mine.geometry_node.core.node.NodeRegistry;
import com.mine.geometry_node.core.node.nodes.NodeDef;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.TextWatcher;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.*;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * 视口右键菜单 (重构版 - 基于路径数组动态构建)
 */
public class ViewportMenu extends FrameLayout {

    private LinearLayout mContentLayout; // 菜单主体
    private LinearLayout mListContainer; // 列表容器
    private EditText mSearchBox;         // 搜索框

    // 数据部分：根目录
    private final MenuItem mRootFolder = new MenuItem("Root", true, null);
    private MenuItem mCurrentFolder = mRootFolder;

    private Viewport mViewport;
    private float mMenuX, mMenuY;

    public ViewportMenu(Context context) {
        super(context);
        initDataFromRegistry();
        initUI(context);
        refreshList(mRootFolder);
    }

    /** [核心逻辑] 从 NodeRegistry 动态读取 menuPath 并构建多级菜单树 */
    private void initDataFromRegistry() {
        Collection<NodeDef> definitions = NodeRegistry.INSTANCE.getAllDefinitions();

        for (NodeDef def : definitions) {
            String[] pathKeys = def.menuPath();
            MenuItem currentFolder = mRootFolder;

            // 1. 逐级遍历/创建路径文件夹
            if (pathKeys != null) {
                for (String key : pathKeys) {
                    MenuItem nextFolder = findChildFolder(currentFolder, key);

                    // 如果该层级目录不存在，则创建它
                    if (nextFolder == null) {
                        // 使用 Component 获取翻译文本作为显示名称
                        String displayName = Component.translatable(key).getString();
                        nextFolder = new MenuItem(displayName, true, currentFolder);
                        nextFolder.translationKey = key; // 记录原 Key，用于严格匹配同级目录
                        currentFolder.children.add(nextFolder);
                    }
                    currentFolder = nextFolder;
                }
            }

            // 2. 此时 currentFolder 是最深层目录，在此处加入具体的节点项
            MenuItem nodeItem = new MenuItem(def.displayName().getString(), false, currentFolder);
            nodeItem.nodeTypeId = def.typeId();
            currentFolder.children.add(nodeItem);
        }

        // 3. 对根目录及所有子目录进行按名称的字母排序
        sortMenuRecursive(mRootFolder);
    }

    /** 在父目录下根据 translationKey 查找是否已存在同名子目录 */
    private MenuItem findChildFolder(MenuItem parent, String key) {
        for (MenuItem child : parent.children) {
            if (child.isFolder && key.equals(child.translationKey)) {
                return child;
            }
        }
        return null;
    }

    /** 递归排序菜单项 */
    private void sortMenuRecursive(MenuItem folder) {
        folder.children.sort(Comparator.comparing(a -> a.name));
        for (MenuItem child : folder.children) {
            if (child.isFolder) {
                sortMenuRecursive(child);
            }
        }
    }

    private void initUI(Context context) {
        // 1. 设置根布局（全屏透明遮罩，点击关闭）
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        this.setOnClickListener(v -> dismiss());

        // 2. 初始化菜单主体容器
        mContentLayout = new LinearLayout(context);
        mContentLayout.setOrientation(LinearLayout.VERTICAL);
        mContentLayout.setBackground(createRectDrawable(
                UIConstants.ViewPort.NodeMenu.BG_COLOR,
                UIConstants.ViewPort.NodeMenu.BORDER_RADIUS));
        mContentLayout.setPadding(4, 4, 4, 4);
        mContentLayout.setOnClickListener(v -> {}); // 拦截点击

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(UIConstants.ViewPort.NodeMenu.ITEM_WEIGHT, LayoutParams.WRAP_CONTENT);
        mContentLayout.setLayoutParams(lp);

        // 3. 搜索框
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

        // 4. 列表滚动区
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

    private void refreshList(MenuItem folder) {
        mCurrentFolder = folder;
        mListContainer.removeAllViews();

        if (folder != mRootFolder && folder.parent != null) {
            addClickItem("← " + Component.translatable("menu.node.back").getString(), 0xFF888888, v -> refreshList(folder.parent));
        }

        List<MenuItem> target = (folder == null) ? mRootFolder.children : folder.children;

        for (MenuItem item : target) {
            String label = item.name + (item.isFolder ? "  ›" : "");
            addClickItem(label, UIConstants.ViewPort.NodeMenu.TEXT_COLOR, v -> {
                if (item.isFolder) {
                    refreshList(item);
                } else {
                    if (mViewport != null && item.nodeTypeId != null) {
                        mViewport.addNode(mMenuX, mMenuY, item.nodeTypeId);
                    }
                    dismiss();
                }
            });
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty()) { refreshList(mCurrentFolder); return; }
        mListContainer.removeAllViews();
        searchRecursive(mRootFolder.children, query.toLowerCase());
    }

    private void searchRecursive(List<MenuItem> list, String q) {
        for (MenuItem item : list) {
            if (item.isFolder) searchRecursive(item.children, q);
            else if (item.name.toLowerCase().contains(q)) {
                addClickItem(item.name, UIConstants.ViewPort.NodeMenu.TEXT_COLOR, v -> {
                    if (mViewport != null && item.nodeTypeId != null) mViewport.addNode(mMenuX, mMenuY, item.nodeTypeId);
                    dismiss();
                });
            }
        }
    }

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

    // [新增 translationKey 字段，支持精确合并]
    private static class MenuItem {
        String name;
        boolean isFolder;
        String nodeTypeId;
        String translationKey; // 用于比较的原始 Key
        MenuItem parent;
        List<MenuItem> children = new ArrayList<>();

        MenuItem(String n, boolean f, MenuItem p) {
            name = n; isFolder = f; parent = p;
        }
    }
}