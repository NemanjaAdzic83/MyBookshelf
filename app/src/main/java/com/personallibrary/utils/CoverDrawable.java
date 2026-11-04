package com.personallibrary.utils;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * A generated book cover for books that have no picture: a coloured rectangle with the title.
 *
 * Why a Drawable and not a bitmap: it is drawn at exactly the size of the ImageView, so it is
 * always sharp, takes no memory for storing images, and works as a Glide placeholder.
 *
 * The colour is derived from the title, so the same book always gets the same colour.
 */
public final class CoverDrawable extends Drawable {

    // Muted, dark colours that keep the cream title text readable.
    private static final int[] PALETTE = {
            0xFF6D2B2B, 0xFF8B3A12, 0xFF1F4E79, 0xFF4A5A28, 0xFF8E2F20,
            0xFF1E4D5A, 0xFF5A2D5E, 0xFF2F5D50, 0xFF7A4B1E, 0xFF3F4A7A
    };
    private static final int TEXT_COLOR = 0xFFFFF4E0;

    // Below this width (in dp) the cover is just a thumbnail and the text would be unreadable.
    private static final float MIN_WIDTH_FOR_TEXT_DP = 80f;

    private final String title;
    private final float density;
    private final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    // StaticLayout is expensive to build, so it is cached and rebuilt only when the width changes.
    private StaticLayout layout;
    private int layoutWidth = -1;
    private final boolean showTitle;

    public CoverDrawable(@Nullable String title, float density){
        this(title,density, true);
    }

    public CoverDrawable(@Nullable String title, float density, boolean showTitle) {
        this.title = title == null ? "" : title.trim();
        this.density = density;
        this.showTitle = showTitle;

        // "& 0x7fffffff" clears the sign bit: Math.abs(Integer.MIN_VALUE) would stay negative.
        int hash = this.title.toLowerCase(Locale.ROOT).hashCode() & 0x7fffffff;
        background.setColor(PALETTE[hash % PALETTE.length]);

        textPaint.setColor(TEXT_COLOR);
        textPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        canvas.drawRect(bounds, background);

        if (!showTitle || title.isEmpty() || bounds.width() < MIN_WIDTH_FOR_TEXT_DP * density) return;

        // Everything is proportional to the width, so the cover looks the same at any size.
        int padding = Math.round(bounds.width() * 0.10f);
        int textWidth = bounds.width() - 2 * padding;

        if (layout == null || layoutWidth != textWidth) {
            textPaint.setTextSize(bounds.width() * 0.125f);
            layout = StaticLayout.Builder.obtain(title, 0, title.length(), textPaint, textWidth)
                    .setMaxLines(4)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build();
            layoutWidth = textWidth;
        }

        canvas.save();
        canvas.translate(bounds.left + padding, bounds.top + padding);
        layout.draw(canvas);
        canvas.restore();
    }

    @Override public void setAlpha(int alpha) {
        background.setAlpha(alpha);
        textPaint.setAlpha(alpha);
    }

    @Override public void setColorFilter(@Nullable ColorFilter colorFilter) {
        background.setColorFilter(colorFilter);
        textPaint.setColorFilter(colorFilter);
    }

    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
