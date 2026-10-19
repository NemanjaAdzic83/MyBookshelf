package com.personallibrary.utils;

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
                .centerCrop()
                .into(target);
    }
}
