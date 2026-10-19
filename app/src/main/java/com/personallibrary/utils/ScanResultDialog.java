package com.personallibrary.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.personallibrary.R;
import com.personallibrary.databinding.DialogScanErrorBinding;
import com.personallibrary.databinding.DialogScanNotFoundBinding;
import com.personallibrary.databinding.DialogScanResultBinding;
import com.personallibrary.models.Book;
import com.personallibrary.models.BookInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * [ISPRAVKA VB1] Dijalozi su jedino mjesto u projektu koje je koristilo findViewById().
 * Sada koriste ViewBinding (DialogScanResultBinding, DialogScanNotFoundBinding, DialogScanErrorBinding).
 *
 * [ISPRAVKA K5] Sva tri dijaloga imaju OnDismissListener. Ranije dijalozi "nije pronađeno" i "greška"
 * nisu imali nijedan, pa bi dodir izvan dijaloga ili tipka Nazad ostavili skener trajno pauziran.
 * Dijalog "knjiga pronađena" je pozivao onDismiss() dva puta i čak nakon odabrane akcije.
 * Sada se onDismiss() poziva tačno jednom i to samo kada nije izabrana nijedna akcija.
 */
public final class ScanResultDialog {

    public interface ScanResultListener {
        void onAddToWishlist(BookInfo info);
        void onAddToLibrary(BookInfo info);
        void onViewExistingBook(Book existing);
        void onManualEntry();
        void onSearchWeb(String isbn);   // otvara web pretragu; dijalog ostaje otvoren          // zamjena za nejasno onAddToWishlist(null)
        void onDismiss();              // zatvoreno bez izbora akcije - skener može nastaviti
    }

    private ScanResultDialog() { }

    public static Dialog showBookFound(Context ctx, BookInfo info, Book existing, ScanResultListener l) {
        DialogScanResultBinding b = DialogScanResultBinding.inflate(LayoutInflater.from(ctx));
        Dialog d = createDialog(ctx, b.getRoot());
        AtomicBoolean actionTaken = new AtomicBoolean(false);

        b.tvBookTitle.setText(info.getTitle());
        b.tvBookAuthor.setText(info.getFullAuthorName());

        String isbn = info.getIsbn();
        setOptionalText(b.tvIsbn, isbn == null || isbn.isEmpty() ? null : ctx.getString(R.string.isbn_format, isbn));

        String date = info.getPublishedDate();
        String pub = info.getPublisher() == null ? "" : info.getPublisher();
        if (!pub.isEmpty() && date != null && !date.isEmpty()) pub = ctx.getString(R.string.publisher_format, pub, date);
        setOptionalText(b.tvPublisher, pub.isEmpty() ? null : pub);

        CoverLoader.load(b.ivBookCover, info.getCoverImageUrl());

        if (existing != null) {
            b.groupNewActions.setVisibility(View.GONE);
            b.groupExistingActions.setVisibility(View.VISIBLE);
            if (existing.isInWishlist()) {
                b.tvStatus.setText(R.string.scan_status_wishlist);
                b.tvStatus.setBackgroundResource(R.drawable.bg_status_wishlist);
                b.btnViewBook.setText(R.string.scan_view_in_wishlist);
            } else {
                b.tvStatus.setText(R.string.scan_status_library);
                b.tvStatus.setBackgroundResource(R.drawable.bg_status_library);
                b.btnViewBook.setText(R.string.scan_view_in_library);
            }
            b.btnViewBook.setOnClickListener(v -> {
                actionTaken.set(true); d.dismiss(); l.onViewExistingBook(existing);
            });
        } else {
            b.groupNewActions.setVisibility(View.VISIBLE);
            b.groupExistingActions.setVisibility(View.GONE);
            b.tvStatus.setText(R.string.scan_status_new);
            b.tvStatus.setBackgroundResource(R.drawable.bg_status_new);
            b.btnAddWishlist.setOnClickListener(v -> {
                actionTaken.set(true); d.dismiss(); l.onAddToWishlist(info);
            });
            b.btnAddLibrary.setOnClickListener(v -> {
                actionTaken.set(true); d.dismiss(); l.onAddToLibrary(info);
            });
        }

        b.btnClose.setOnClickListener(v -> d.dismiss());
        return show(d, l, actionTaken);
    }

    public static Dialog showNotFound(Context ctx, String isbn, ScanResultListener l) {
        DialogScanNotFoundBinding b = DialogScanNotFoundBinding.inflate(LayoutInflater.from(ctx));
        Dialog d = createDialog(ctx, b.getRoot());
        AtomicBoolean actionTaken = new AtomicBoolean(false);

        b.tvScannedIsbn.setText(ctx.getString(R.string.scan_code_format, isbn));
        b.btnManualAdd.setOnClickListener(v -> {
            actionTaken.set(true); d.dismiss(); l.onManualEntry();
        });
        // dijalog se ne zatvara: nakon pregleda u browseru korisnik se vraća i bira "Unesi podatke ručno"
        b.btnSearchWeb.setOnClickListener(v -> l.onSearchWeb(isbn));
        b.btnClose.setOnClickListener(v -> d.dismiss());
        return show(d, l, actionTaken);
    }

    public static Dialog showError(Context ctx, String msg, ScanResultListener l) {
        DialogScanErrorBinding b = DialogScanErrorBinding.inflate(LayoutInflater.from(ctx));
        Dialog d = createDialog(ctx, b.getRoot());

        b.tvErrorMessage.setText(msg);
        b.btnRetry.setOnClickListener(v -> d.dismiss());   // zatvaranje = onDismiss() = skener nastavlja
        b.btnClose.setOnClickListener(v -> d.dismiss());
        return show(d, l, new AtomicBoolean(false));
    }

    // ---------- pomoćne metode ----------
    private static Dialog show(Dialog d, ScanResultListener l, AtomicBoolean actionTaken) {
        d.setOnDismissListener(x -> { if (!actionTaken.get()) l.onDismiss(); });
        d.show();
        return d;
    }

    private static void setOptionalText(TextView tv, String text) {
        if (text == null || text.isEmpty()) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setText(text);
            tv.setVisibility(View.VISIBLE);
        }
    }

    private static Dialog createDialog(Context ctx, View content) {
        Dialog d = new Dialog(ctx);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(content);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.92);
            w.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return d;
    }
}
