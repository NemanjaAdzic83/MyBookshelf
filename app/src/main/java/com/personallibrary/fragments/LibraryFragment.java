package com.personallibrary.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.personallibrary.R;
import com.personallibrary.activities.BookDetailActivity;
import com.personallibrary.adapters.BooksByAuthorAdapter;
import com.personallibrary.databinding.FragmentLibraryBinding;
import com.personallibrary.viewmodel.LibraryViewModel;

import java.util.List;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;
    private LibraryViewModel viewModel;
    private BooksByAuthorAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LibraryViewModel.class);

        adapter = new BooksByAuthorAdapter(book -> {
            Intent i = new Intent(requireContext(), BookDetailActivity.class);
            i.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getId());
            startActivity(i);
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        viewModel.getBooks().observe(getViewLifecycleOwner(), books -> {
            binding.tvCountBadge.setText(String.valueOf(books != null ? books.size() : 0));
            adapter.updateBooks(books);      // grupisanje po autoru je sada unutar adaptera
        });

        viewModel.getIsEmpty().observe(getViewLifecycleOwner(), empty -> {
            binding.emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.getGenres().observe(getViewLifecycleOwner(), this::rebuildChips);
    }

    /**
     * [ISPRAVKA L2] Odabrani chip se uvijek računa iz stanja ViewModela (activeGenre).
     * Ranije su se chipovi pri svakom osvježavanju liste (npr. povratak sa ekrana detalja)
     * pravili ispočetka sa označenim "Sve", iako je lista i dalje bila filtrirana po žanru.
     */
    private void rebuildChips(List<String> genres) {
        ChipGroup group = binding.chipGroupFilter;
        group.removeAllViews();

        String active = viewModel.getActiveGenre().getValue();
        if (active == null) active = "";

        group.addView(makeChip(getString(R.string.filter_all), active.isEmpty(),
                v -> viewModel.clearFilter()));

        if (genres != null) {
            for (String genre : genres) {
                group.addView(makeChip(genre, genre.equals(active),
                        v -> viewModel.filterByGenre(genre)));
            }
        }
        binding.filterScrollView.setVisibility(genres != null && !genres.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private Chip makeChip(String text, boolean selected, View.OnClickListener onClick) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setChipBackgroundColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_filter_background_selector));
        chip.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector));
        chip.setChipStrokeColorResource(R.color.primary);
        chip.setChipStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.5f,
                getResources().getDisplayMetrics()));
        chip.setChecked(selected);
        chip.setOnClickListener(onClick);
        return chip;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadBooks();   // [ISPRAVKA L8] jedini poziv učitavanja
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
