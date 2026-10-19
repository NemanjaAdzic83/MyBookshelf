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
import androidx.recyclerview.widget.GridLayoutManager;

import com.personallibrary.R;
import com.personallibrary.activities.BookDetailActivity;
import com.personallibrary.adapters.BookGridAdapter;
import com.personallibrary.databinding.FragmentHomeBinding;
import com.personallibrary.viewmodel.HomeViewModel;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private BookGridAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        adapter = new BookGridAdapter(book -> {
            Intent i = new Intent(requireContext(), BookDetailActivity.class);
            i.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getId());
            startActivity(i);
        });
        binding.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerView.setAdapter(adapter);

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.shimmerLayout.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading) {
                binding.shimmerLayout.startShimmer();
                binding.recyclerView.setVisibility(View.GONE);
                binding.emptyView.setVisibility(View.GONE);
            } else {
                binding.shimmerLayout.stopShimmer();
            }
        });

        viewModel.getRecentBooks().observe(getViewLifecycleOwner(), books -> {
            adapter.updateBooks(books);
            boolean empty = books == null || books.isEmpty();
            binding.emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.getLibraryCount().observe(getViewLifecycleOwner(), c -> {
            binding.tvLibraryCount.setText(String.valueOf(c));
            binding.tvBookCount.setText(getString(R.string.book_count_format, c));
        });
        viewModel.getWishlistCount().observe(getViewLifecycleOwner(),
                c -> binding.tvWishlistCount.setText(String.valueOf(c)));

        // [ISPRAVKA L8] ovdje se podaci više NE učitavaju - onResume() se uvijek poziva odmah poslije
        // onViewCreated(), pa je ranije svaki upit išao dvaput.
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;     // obavezno kod Fragmenta: View se uništava, a Fragment živi dalje
    }
}
