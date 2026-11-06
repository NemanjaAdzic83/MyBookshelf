package com.personallibrary.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.personallibrary.R;
import com.personallibrary.activities.BookDetailActivity;
import com.personallibrary.adapters.BooksByAuthorAdapter;
import com.personallibrary.databinding.FragmentWishlistBinding;
import com.personallibrary.viewmodel.WishlistViewModel;

public class WishlistFragment extends Fragment {

    private FragmentWishlistBinding binding;
    private WishlistViewModel viewModel;
    private BooksByAuthorAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWishlistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WishlistViewModel.class);

        adapter = new BooksByAuthorAdapter(book -> {
            Intent i = new Intent(requireContext(), BookDetailActivity.class);
            i.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getId());
            startActivity(i);
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnBoughtClickListener(book -> {
            viewModel.moveToLibrary(book);

            Snackbar bar = Snackbar.make(binding.getRoot(),
                            getString(R.string.snackbar_bought, book.getTitle()), Snackbar.LENGTH_LONG)
                    .setAction(R.string.snackbar_undo, v -> viewModel.moveBackToWishlist(book));

            // The floating pill navigation is drawn over the fragment, so lift the snackbar above it
            View nav = requireActivity().findViewById(R.id.bottom_navigation);
            if (nav != null) bar.setAnchorView(nav);
            bar.show();
        });

        viewModel.getBooks().observe(getViewLifecycleOwner(), books -> {
            binding.tvCountBadge.setText(String.valueOf(books != null ? books.size() : 0));
            adapter.updateBooks(books);
        });
        viewModel.getIsEmpty().observe(getViewLifecycleOwner(), empty -> {
            binding.emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadBooks();   // [ISPRAVKA L8]
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
