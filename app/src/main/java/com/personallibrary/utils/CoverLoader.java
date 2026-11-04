package com.personallibrary.utils;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.personallibrary.R;

/**
 * [ISPRAVKA L6] NOVA KLASA - jedno mjesto za učitavanje naslovnica.
 *
 * Stari kod je u RecyclerView adapterima radio:
 *     if (path != null) Glide...into(iv);  else iv.setImageResource(placeholder);
 * Grana "else" NE otkazuje Glide zahtjev koji je prethodni (reciklirani) red još učitavao,
 * pa je mogao stići sa zakašnjenjem i prepisati placeholder tuđom naslovnicom.
 * Glide.into() uvijek otkazuje stari zahtjev za isti ImageView, a fallback() pokriva null putanju.
 */
public final class CoverLoader {

    private CoverLoader() { }

    public static void load(ImageView target, String path) {
        String model = (path == null || path.isEmpty()) ? null : path;
        Glide.with(target)
                .load(model)
                .placeholder(R.drawable.ic_book_placeholder)
                .fallback(R.drawable.ic_book_placeholder)
                .error(R.drawable.ic_book_placeholder)
                //scale whole image down so it fits inside the frame
                .fitCenter()
                .into(target);
    }

    // add: import android.graphics.drawable.Drawable;

    /**
     * Same as load(target, path), but books without a picture (or whose picture fails to load)
     * get a generated cover with the title instead of the grey placeholder icon.
     * A new CoverDrawable is made for every call because a Drawable must not be shared
     * between ImageViews.
     */
    public static void load(ImageView target, String path, String title) {
        String model = (path == null || path.isEmpty()) ? null : path;
        Drawable generated = new CoverDrawable(title, target.getResources().getDisplayMetrics().density);
        Glide.with(target)
                .load(model)
                .placeholder(generated)    // shown while loading
                .fallback(generated)       // shown when there is no path at all
                .error(generated)          // shown when the picture cannot be loaded
                .fitCenter()              // use the same scale type as in your old load() method
                .into(target);
    }

    /**
     * For large background areas (e.g. behind the cover on the detail screen): same colour as the
     * generated cover of this book, but without the title text.
     */
    public static void loadBackground(ImageView target, String path, String title) {
        String model = (path == null || path.isEmpty()) ? null : path;
        Drawable generated = new CoverDrawable(title,
                target.getResources().getDisplayMetrics().density, false);
        Glide.with(target)
                .load(model)
                .placeholder(generated)
                .fallback(generated)
                .error(generated)
                .centerCrop()
                .into(target);
    }
}
