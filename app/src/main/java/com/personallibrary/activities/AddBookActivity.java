package com.personallibrary.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.personallibrary.R;
import com.personallibrary.databinding.ActivityAddBookBinding;
import com.personallibrary.utils.CoverLoader;
import com.personallibrary.utils.GenreConstants;
import com.personallibrary.viewmodel.AddBookViewModel;

/** Dodavanje nove knjige. Ako je proslijeđen EXTRA_EDIT_BOOK_ID, ekran radi u režimu IZMJENE knjige. */
public class AddBookActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_BOOK_ID = "edit_book_id";

    private ActivityAddBookBinding binding;
    private AddBookViewModel viewModel;
    private int editId = -1;

    // slika se odmah kopira u privatni direktorij (vidi AddBookViewModel.importCover)
    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) viewModel.importCover(uri);
            });

    private boolean isEditMode() { return editId != -1; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddBookBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(AddBookViewModel.class);
        editId = getIntent().getIntExtra(EXTRA_EDIT_BOOK_ID, -1);

        binding.btnBack.setOnClickListener(v -> finish());

        View.OnClickListener pickCover = v -> imagePicker.launch("image/*");
        binding.ivCoverPreview.setOnClickListener(pickCover);
        binding.layoutCoverHint.setOnClickListener(pickCover);
        binding.btnPickCover.setOnClickListener(pickCover);
        binding.btnRemoveCover.setOnClickListener(v -> viewModel.removeCover());

        buildGenreChips();

        binding.btnSaveLibrary.setOnClickListener(v -> attemptSave(false));
        binding.btnSaveWishlist.setOnClickListener(v -> attemptSave(true));

        if (isEditMode()) {
            // Režim izmjene: drugi naslov, jedno dugme "Sačuvaj izmjene", učitavanje postojeće knjige
            binding.tvHeaderTitle.setText(R.string.edit_book_title);
            binding.btnSaveLibrary.setText(R.string.save_changes);
            binding.btnSaveLibrary.setIcon(null);
            binding.btnSaveWishlist.setVisibility(View.GONE);
            viewModel.startEdit(editId);
        }

        observeViewModel();
    }

    private void buildGenreChips() {
        String selected = viewModel.getGenre();
        for (String genre : GenreConstants.ALL_GENRES) {
            Chip chip = new Chip(this);
            chip.setText(genre);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.chip_background_selector));
            chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text_selector));
            chip.setChecked(genre.equals(selected));
            chip.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) viewModel.setGenre(genre);
                else if (genre.equals(viewModel.getGenre())) viewModel.setGenre("");
            });
            binding.chipGroupGenre.addView(chip);
        }
    }

    /** Poslije učitavanja knjige za izmjenu označava njen žanr (čipovi su napravljeni prije učitavanja). */
    private void refreshGenreChips() {
        String genre = viewModel.getGenre();
        for (int i = 0; i < binding.chipGroupGenre.getChildCount(); i++) {
            View child = binding.chipGroupGenre.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChecked(chip.getText().toString().equals(genre));
            }
        }
    }

    private void observeViewModel() {
        viewModel.getEditBook().observe(this, book -> {
            if (book == null) return;
            binding.etTitle.setText(book.getTitle());
            binding.etAuthorFirst.setText(book.getAuthorFirstName());
            binding.etAuthorLast.setText(book.getAuthorLastName());
            refreshGenreChips();
            viewModel.consumeEditBook();   // bez ponovnog prepisivanja nakon rotacije (da se ne izgube unesene izmjene)
        });

        viewModel.getCoverPath().observe(this, path -> {
            boolean has = path != null && !path.isEmpty();
            binding.layoutCoverHint.setVisibility(has ? View.GONE : View.VISIBLE);
            binding.btnRemoveCover.setVisibility(has ? View.VISIBLE : View.GONE);
            CoverLoader.load(binding.ivCoverPreview, path);
        });

        viewModel.getBusy().observe(this, busy -> {
            boolean enabled = !Boolean.TRUE.equals(busy);
            binding.btnSaveLibrary.setEnabled(enabled);
            binding.btnSaveWishlist.setEnabled(enabled);
            binding.btnPickCover.setEnabled(enabled);
            binding.btnRemoveCover.setEnabled(enabled);
        });

        viewModel.getSaveResult().observe(this, result -> {
            if (result == null) return;
            String title = txt(binding.etTitle);
            switch (result) {
                case SUCCESS_LIBRARY:
                    Toast.makeText(this, R.string.toast_added_library, Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case SUCCESS_WISHLIST:
                    Toast.makeText(this, R.string.toast_added_wishlist, Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case SUCCESS_UPDATED:
                    Toast.makeText(this, R.string.toast_updated, Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case ERROR_EXISTS_LIBRARY:
                    Toast.makeText(this, getString(R.string.toast_exists_library, title), Toast.LENGTH_LONG).show();
                    break;
                case ERROR_EXISTS_WISHLIST:
                    Toast.makeText(this, getString(R.string.toast_exists_wishlist, title), Toast.LENGTH_LONG).show();
                    break;
                case ERROR_COVER:
                    Toast.makeText(this, R.string.toast_cover_error, Toast.LENGTH_SHORT).show();
                    break;
                case ERROR_NOT_FOUND:
                    Toast.makeText(this, R.string.toast_book_missing, Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case ERROR_SAVE:
                    Toast.makeText(this, R.string.toast_save_error, Toast.LENGTH_SHORT).show();
                    break;
            }
            viewModel.consumeSaveResult();
        });
    }

    private void attemptSave(boolean toWishlist) {
        binding.tilTitle.setError(null);
        binding.tilAuthorLast.setError(null);

        String title = txt(binding.etTitle);
        String lastName = txt(binding.etAuthorLast);

        if (TextUtils.isEmpty(title)) {
            binding.tilTitle.setError(getString(R.string.error_title_required));
            binding.etTitle.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            binding.tilAuthorLast.setError(getString(R.string.error_author_required));
            binding.etAuthorLast.requestFocus();
            return;
        }

        if (isEditMode()) viewModel.saveEdit(title, txt(binding.etAuthorFirst), lastName);
        else viewModel.saveBook(title, txt(binding.etAuthorFirst), lastName, toWishlist);
    }

    private String txt(EditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
