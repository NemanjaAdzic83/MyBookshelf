package com.personallibrary.activities;

import android.content.Intent;
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
import com.personallibrary.databinding.ActivityBookDetailBinding;
import com.personallibrary.models.Book;
import com.personallibrary.utils.CoverLoader;
import com.personallibrary.viewmodel.BookDetailViewModel;

import java.util.zip.GZIPOutputStream;

public class BookDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BOOK_ID = "book_id";

    private ActivityBookDetailBinding binding;
    private BookDetailViewModel viewModel;
    private int bookId = -1;

    // Nakon povratka sa ekrana izmjene (snimljeno ili ne) ponovo učitaj knjigu da se vide nove vrijednosti
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
            getSupportActionBar().setDisplayShowTitleEnabled(false);   // naslov crta CollapsingToolbarLayout
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        bookId = getIntent().getIntExtra(EXTRA_BOOK_ID, -1);
        if (bookId == -1) { finish(); return; }

        viewModel.getBook().observe(this, this::bindBook);
        viewModel.getOpSuccess().observe(this, ok -> { if (ok != null && ok) finish(); });
        viewModel.loadBook(bookId);
    }

    // ----- ikona olovke u traci -----
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
        return super.onOptionsItemSelected(item);
    }

    private void bindBook(Book book) {
        if (book == null) { finish(); return; }

        // [ISPRAVKA L13] naslov se postavlja na CollapsingToolbarLayout (za njega su i definisani
        // atributi expandedTitleMargin*), a ne na ActionBar
        binding.collapsingToolbar.setTitle(book.getTitle());
        binding.tvTitle.setText(book.getTitle());
        binding.tvAuthor.setText(book.getFullAuthorName());

        if (book.isInWishlist()) {
            binding.tvStatus.setText(R.string.in_wishlist);
            binding.tvStatus.setBackgroundResource(R.drawable.bg_status_wishlist);
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.wishlist_color));
            binding.btnMoveToLibrary.setVisibility(View.VISIBLE);
            binding.btnMoveToWishlist.setVisibility(View.GONE);
            binding.btnMarkRead.setVisibility(View.GONE);
            binding.ivReadStamp.setVisibility(View.GONE);
        } else {
            binding.tvStatus.setText(R.string.in_library);
            binding.tvStatus.setBackgroundResource(R.drawable.bg_status_library);
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.library_color));
            binding.btnMoveToLibrary.setVisibility(View.GONE);
            binding.btnMoveToWishlist.setVisibility(View.VISIBLE);
            binding.btnMarkRead.setVisibility(View.VISIBLE);
            binding.btnMarkRead.setText(book.isRead() ? R.string.mark_as_unread : R.string.mark_as_read);
            binding.ivReadStamp.setVisibility(book.isRead() ? View.VISIBLE : View.GONE);
        }

        if (book.hasGenre()) {
            binding.tvGenreBadge.setText(book.getGenre());
            binding.tvGenreBadge.setVisibility(View.VISIBLE);
        } else {
            binding.tvGenreBadge.setVisibility(View.GONE);
        }

        CoverLoader.load(binding.ivCover, book.getCoverImagePath());
        CoverLoader.load(binding.ivCoverBg, book.getCoverImagePath());

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

        binding.btnMoveToWishlist.setOnClickListener( v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_move_wishlist_title)
                        .setMessage(getString(R.string.dialog_move_wishlist_message,book.getTitle()))
                        .setPositiveButton(R.string.dialog_move_positive, (d,w) -> {
                            Toast.makeText(this, R.string.toast_moved,Toast.LENGTH_SHORT).show();
                            viewModel.moveToWishlist();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show()

        );

        binding.btnMarkRead.setOnClickListener( v -> viewModel.toggleRead());


        binding.btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_delete_title)      // [ISPRAVKA L16] "Obriši knjige" -> "Obriši knjigu"
                        .setMessage(getString(R.string.dialog_delete_message, book.getTitle()))
                        .setPositiveButton(R.string.dialog_delete_positive, (d, w) -> {
                            Toast.makeText(this, R.string.toast_deleted, Toast.LENGTH_SHORT).show();
                            viewModel.deleteBook();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show());
    }
}
