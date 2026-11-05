package com.personallibrary.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.personallibrary.R;
import com.personallibrary.activities.BarcodeScannerActivity;
import com.personallibrary.activities.BookDetailActivity;
import com.personallibrary.activities.SearchActivity;
import com.personallibrary.adapters.BooksByAuthorAdapter;
import com.personallibrary.databinding.FragmentLibraryBinding;
import com.personallibrary.viewmodel.LibraryViewModel;

import java.util.List;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;
    private LibraryViewModel viewModel;
    private BooksByAuthorAdapter adapter;
    private boolean scrollToTop = false;

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
            boolean grouped = viewModel.getSortOrder().getValue() == LibraryViewModel.SortOrder.AUTHOR;
            adapter.updateBooks(books, grouped);
            if (scrollToTop) {                       // new sort order -> start from the top
                binding.recyclerView.scrollToPosition(0);
                scrollToTop = false;
            }
        });

        viewModel.getSortOrder().observe(getViewLifecycleOwner(), order ->
                binding.tvSort.setText(getString(R.string.sort_label, getString(sortLabel(order)))));

        binding.tvSort.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            menu.getMenuInflater().inflate(R.menu.menu_sort, menu.getMenu());
            menu.getMenu().findItem(sortMenuId(viewModel.getSortOrder().getValue())).setChecked(true);
            menu.setOnMenuItemClickListener(item -> {
                scrollToTop = true;
                viewModel.setSortOrder(sortFromMenuId(item.getItemId()));
                return true;
            });
            menu.show();
        });

        viewModel.getIsEmpty().observe(getViewLifecycleOwner(), empty -> {
            binding.emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
            boolean filtered = viewModel.getReadFilter().getValue() != LibraryViewModel.ReadFilter.ALL;
            binding.tvEmptyTitle.setText(filtered ? R.string.empty_filtered_books : R.string.empty_library_books);
            binding.tvSort.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.getGenres().observe(getViewLifecycleOwner(), this::rebuildChips);
        viewModel.getReadFilter().observe(getViewLifecycleOwner(), this::rebuildReadChips);

        binding.searchBar.getRoot().setOnClickListener( v ->
                startActivity(new Intent(requireContext(), SearchActivity.class)));
        binding.searchBar.btnScan.setOnClickListener( v ->
                startActivity(new Intent(requireContext(), BarcodeScannerActivity.class)));
    }


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

    /*
        Build the ALL / READ / UNREAD chips, the selected one always comes from the ViewModel state
     */
    private void rebuildReadChips(LibraryViewModel.ReadFilter current){
        ChipGroup group = binding.chipGroupRead;
        group.removeAllViews();
        group.addView(makeChip(getString(R.string.filter_all), current == LibraryViewModel.ReadFilter.ALL,
                v -> viewModel.setReadFilter(LibraryViewModel.ReadFilter.ALL)));
        group.addView(makeChip(getString(R.string.filter_read), current == LibraryViewModel.ReadFilter.READ,
                v -> viewModel.setReadFilter(LibraryViewModel.ReadFilter.READ)));
        group.addView(makeChip(getString(R.string.filter_unread), current == LibraryViewModel.ReadFilter.UNREAD,
                v -> viewModel.setReadFilter(LibraryViewModel.ReadFilter.UNREAD)));
    }

    private static int sortLabel(LibraryViewModel.SortOrder o) {
        if (o == LibraryViewModel.SortOrder.TITLE)  return R.string.sort_title;
        if (o == LibraryViewModel.SortOrder.NEWEST) return R.string.sort_newest;
        if (o == LibraryViewModel.SortOrder.OLDEST) return R.string.sort_oldest;
        return R.string.sort_author;
    }

    private static int sortMenuId(LibraryViewModel.SortOrder o) {
        if (o == LibraryViewModel.SortOrder.TITLE)  return R.id.sort_title;
        if (o == LibraryViewModel.SortOrder.NEWEST) return R.id.sort_newest;
        if (o == LibraryViewModel.SortOrder.OLDEST) return R.id.sort_oldest;
        return R.id.sort_author;
    }

    private static LibraryViewModel.SortOrder sortFromMenuId(int id) {
        if (id == R.id.sort_title)  return LibraryViewModel.SortOrder.TITLE;
        if (id == R.id.sort_newest) return LibraryViewModel.SortOrder.NEWEST;
        if (id == R.id.sort_oldest) return LibraryViewModel.SortOrder.OLDEST;
        return LibraryViewModel.SortOrder.AUTHOR;
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
