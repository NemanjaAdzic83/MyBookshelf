package com.personallibrary.activities;

import android.content.Intent;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.personallibrary.R;
import com.personallibrary.adapters.MoreByAuthorAdapter;
import com.personallibrary.databinding.ActivityBookDetailBinding;
import com.personallibrary.databinding.ItemInfoRowBinding;
import com.personallibrary.models.Book;
import com.personallibrary.utils.CoverLoader;
import com.personallibrary.viewmodel.BookDetailViewModel;

import java.util.List;

public class BookDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BOOK_ID = "book_id";

    private ActivityBookDetailBinding binding;
    private BookDetailViewModel viewModel;
    private MoreByAuthorAdapter moreAdapter;
    private int bookId = -1;

    // After returning from the edit screen (saved or not) reload the book to show new values
    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> { if (bookId != -1) viewModel.loadBook(bookId); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(BookDetailViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);   // the title is drawn by CollapsingToolbarLayout
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        // White three-dot icon (the default one is dark and invisible on the dark header)
        binding.toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.ic_more_vert));

        // Blurred background behind the cover. RenderEffect exists from Android 12 (API 31);
        // on older phones the background is simply the dimmed cover. The view is scaled up a bit
        // so the blurred edges are not visible.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.ivCoverBg.setRenderEffect(
                    RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP));
            binding.ivCoverBg.setScaleX(1.3f);
            binding.ivCoverBg.setScaleY(1.3f);
        }

        moreAdapter = new MoreByAuthorAdapter(other -> {
            Intent i = new Intent(this, BookDetailActivity.class);
            i.putExtra(EXTRA_BOOK_ID, other.getId());
            startActivity(i);
        });
        binding.rvMore.setAdapter(moreAdapter);

        bookId = getIntent().getIntExtra(EXTRA_BOOK_ID, -1);
        if (bookId == -1) { finish(); return; }

        viewModel.getBook().observe(this, this::bindBook);
        viewModel.getMoreByAuthor().observe(this, this::bindMoreByAuthor);
        viewModel.getOpSuccess().observe(this, ok -> { if (ok != null && ok) finish(); });
        viewModel.loadBook(bookId);
    }

    // ----- toolbar menu: pencil (edit) + three dots (delete) -----
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_book_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit && bookId != -1) {
            Intent i = new Intent(this, AddBookActivity.class);
            i.putExtra(AddBookActivity.EXTRA_EDIT_BOOK_ID, bookId);
            editLauncher.launch(i);
            return true;
        }
        if (item.getItemId() == R.id.action_delete) {
            Book book = viewModel.getBook().getValue();
            if (book != null) showDeleteDialog(book);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void bindBook(Book book) {
        if (book == null) { finish(); return; }

        binding.collapsingToolbar.setTitle(book.getTitle());
        binding.tvTitle.setText(book.getTitle());
        binding.tvAuthor.setText(book.getFullAuthorName());

        if (book.isInWishlist()) {
            binding.tvStatus.setText(R.string.chip_on_wishlist);
            binding.tvStatus.setBackgroundResource(R.drawable.bg_status_wishlist);
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.wishlist_color));
            binding.btnMoveToLibrary.setVisibility(View.VISIBLE);
            binding.btnMoveToWishlist.setVisibility(View.GONE);
            binding.btnMarkRead.setVisibility(View.GONE);
            binding.ivReadStamp.setVisibility(View.GONE);
            binding.tvReadChip.setVisibility(View.GONE);
        } else {
            binding.tvStatus.setText(R.string.chip_in_library);
            binding.tvStatus.setBackgroundResource(R.drawable.bg_status_library);
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.library_color));
            binding.btnMoveToLibrary.setVisibility(View.GONE);
            binding.btnMoveToWishlist.setVisibility(View.VISIBLE);
            binding.btnMarkRead.setVisibility(View.VISIBLE);
            binding.btnMarkRead.setText(book.isRead() ? R.string.mark_as_unread : R.string.mark_as_read);
            binding.ivReadStamp.setVisibility(book.isRead() ? View.VISIBLE : View.GONE);
            binding.tvReadChip.setVisibility(book.isRead() ? View.VISIBLE : View.GONE);
        }

        if (book.hasGenre()) {
            binding.tvGenreBadge.setText(book.getGenre());
            binding.tvGenreBadge.setVisibility(View.VISIBLE);
        } else {
            binding.tvGenreBadge.setVisibility(View.GONE);
        }

        // Info table. New fields (publisher, series, purchase date, read date) are added here later
        // with one addInfoRow(...) call each.
        binding.infoContainer.removeAllViews();
        addInfoRow(getString(R.string.info_author), book.getFullAuthorName());
        if (book.hasGenre()) addInfoRow(getString(R.string.info_genre), book.getGenre());

        CoverLoader.load(binding.ivCover, book.getCoverImagePath(), book.getTitle());
        CoverLoader.loadBackground(binding.ivCoverBg, book.getCoverImagePath(), book.getTitle());

        binding.btnMoveToLibrary.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_move_title)
                        .setMessage(getString(R.string.dialog_move_message, book.getTitle()))
                        .setPositiveButton(R.string.dialog_move_positive, (d, w) -> {
                            Toast.makeText(this, R.string.toast_moved, Toast.LENGTH_SHORT).show();
                            viewModel.moveToLibrary();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show());

        binding.btnMoveToWishlist.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_move_wishlist_title)
                        .setMessage(getString(R.string.dialog_move_wishlist_message, book.getTitle()))
                        .setPositiveButton(R.string.dialog_move_positive, (d, w) -> {
                            Toast.makeText(this, R.string.toast_moved, Toast.LENGTH_SHORT).show();
                            viewModel.moveToWishlist();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show());

        binding.btnMarkRead.setOnClickListener(v -> viewModel.toggleRead());
    }

    /** "More by this author" row, plus the "you already have N books" banner for wishlist books. */
    private void bindMoreByAuthor(List<Book> others) {
        Book book = viewModel.getBook().getValue();
        if (book == null) return;

        int n = others == null ? 0 : others.size();
        moreAdapter.updateBooks(others);
        binding.moreSection.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
        binding.tvMoreTitle.setText(getString(R.string.more_by_author, book.getFullAuthorName()));
        binding.tvMoreCount.setText(getResources().getQuantityString(R.plurals.more_by_count, n, n));

        if (book.isInWishlist() && n > 0) {
            binding.tvAuthorBanner.setText(getResources().getQuantityString(
                    R.plurals.you_have_by_author, n, n, book.getFullAuthorName()));
            binding.tvAuthorBanner.setVisibility(View.VISIBLE);
        } else {
            binding.tvAuthorBanner.setVisibility(View.GONE);
        }
    }

    private void addInfoRow(String label, String value) {
        ItemInfoRowBinding row = ItemInfoRowBinding.inflate(getLayoutInflater(), binding.infoContainer, false);
        row.tvLabel.setText(label);
        row.tvValue.setText(value);
        // no divider above the first row
        if (binding.infoContainer.getChildCount() == 0) row.vDivider.setVisibility(View.GONE);
        binding.infoContainer.addView(row.getRoot());
    }

    private void showDeleteDialog(Book book) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, book.getTitle()))
                .setPositiveButton(R.string.dialog_delete_positive, (d, w) -> {
                    Toast.makeText(this, R.string.toast_deleted, Toast.LENGTH_SHORT).show();
                    viewModel.deleteBook();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}