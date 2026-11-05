package com.personallibrary.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;

import com.personallibrary.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Floating, pill-shaped bottom navigation. The selected item is a yellow pill that shows
 * icon + label; the other items show only their icon.
 *
 * Why a custom view: Material's BottomNavigationView cannot put the label next to the icon
 * inside a pill. This class keeps the same small API (setSelectedItemId, getSelectedItemId,
 * a listener), so the rest of the app barely changes.
 *
 * The items come from a normal menu resource (setMenu), so ids, icons and titles stay in XML.
 */
public class PillNavigationView extends LinearLayout {

    public interface OnItemSelectedListener { void onItemSelected(int itemId); }

    // The selected item gets more room because it also shows its label.
    private static final float WEIGHT_SELECTED = 1.7f;
    private static final float WEIGHT_NORMAL = 1f;

    private static final class Item {
        final int id;
        final LinearLayout root;
        final ImageView icon;
        final TextView label;
        Item(int id, LinearLayout root, ImageView icon, TextView label) {
            this.id = id; this.root = root; this.icon = icon; this.label = label;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private int selectedId = View.NO_ID;
    private int rippleRes;
    private OnItemSelectedListener listener;

    public PillNavigationView(Context context) { this(context, null); }

    public PillNavigationView(Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }

    public PillNavigationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pad = dp(6);
        setPadding(pad, pad, pad, pad);
        setBackgroundResource(R.drawable.bg_pill_nav);

        // The theme's ripple, used on items that are not selected.
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true);
        rippleRes = tv.resourceId;
    }

    // ------------------------------------------------------------------ public API

    /** Builds the items from a menu resource. The first item starts selected (no callback). */
    public void setMenu(@MenuRes int menuRes) {
        removeAllViews();
        items.clear();

        // PopupMenu is only used as a convenient menu inflater, it is never shown.
        PopupMenu popup = new PopupMenu(getContext(), this);
        popup.inflate(menuRes);
        Menu menu = popup.getMenu();

        for (int i = 0; i < menu.size(); i++) {
            MenuItem mi = menu.getItem(i);
            if (!mi.isVisible()) continue;
            Item item = createItem(mi);
            items.add(item);
            addView(item.root);
        }
        selectedId = items.isEmpty() ? View.NO_ID : items.get(0).id;
        refreshAll();
    }

    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener l) { listener = l; }

    public int getSelectedItemId() { return selectedId; }

    /** Same as tapping the item: updates the look and notifies the listener. */
    public void setSelectedItemId(int id) { select(id, true); }

    // ------------------------------------------------------------------ internals

    private Item createItem(MenuItem mi) {
        final int id = mi.getItemId();

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(HORIZONTAL);
        root.setGravity(android.view.Gravity.CENTER);
        root.setPadding(dp(10), dp(12), dp(10), dp(12));
        root.setClickable(true);
        root.setFocusable(true);
        root.setContentDescription(mi.getTitle());      // TalkBack reads the title for icon-only items
        root.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, WEIGHT_NORMAL));

        ImageView icon = new ImageView(getContext());
        icon.setImageDrawable(mi.getIcon());
        icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        icon.setLayoutParams(new LayoutParams(dp(24), dp(24)));

        TextView label = new TextView(getContext());
        label.setText(mi.getTitle());
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        LayoutParams labelParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        labelParams.setMarginStart(dp(8));
        label.setLayoutParams(labelParams);

        root.addView(icon);
        root.addView(label);
        root.setOnClickListener(v -> setSelectedItemId(id));
        return new Item(id, root, icon, label);
    }

    private void select(int id, boolean notify) {
        if (id == selectedId || !contains(id)) return;
        TransitionManager.beginDelayedTransition(this);     // animates the pill growing / shrinking
        selectedId = id;
        refreshAll();
        if (notify && listener != null) listener.onItemSelected(id);
    }

    private boolean contains(int id) {
        for (Item it : items) if (it.id == id) return true;
        return false;
    }

    private void refreshAll() {
        for (Item it : items) applyState(it, it.id == selectedId);
    }

    private void applyState(Item it, boolean selected) {
        int dark = ContextCompat.getColor(getContext(), R.color.primary_dark);
        int muted = ContextCompat.getColor(getContext(), R.color.nav_unselected);

        it.label.setVisibility(selected ? View.VISIBLE : View.GONE);
        it.label.setTextColor(dark);
        it.icon.setImageTintList(ColorStateList.valueOf(selected ? dark : muted));

        LayoutParams lp = (LayoutParams) it.root.getLayoutParams();
        lp.weight = selected ? WEIGHT_SELECTED : WEIGHT_NORMAL;
        it.root.setLayoutParams(lp);

        it.root.setBackgroundResource(selected ? R.drawable.bg_pill_nav_selected : rippleRes);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------------------ keep the selection on rotation

    @Nullable @Override
    protected Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putParcelable("super", super.onSaveInstanceState());
        state.putInt("selected", selectedId);
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle b = (Bundle) state;
            super.onRestoreInstanceState(b.getParcelable("super"));
            // No callback: the FragmentManager restores the visible fragment by itself.
            select(b.getInt("selected", selectedId), false);
        } else {
            super.onRestoreInstanceState(state);
        }
    }
}
