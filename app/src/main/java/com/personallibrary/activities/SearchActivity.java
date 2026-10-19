package com.personallibrary.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.personallibrary.R;
import com.personallibrary.adapters.BookListAdapter;
import com.personallibrary.databinding.ActivitySearchBinding;
import com.personallibrary.viewmodel.SearchViewModel;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private SearchViewModel viewModel;
    private BookListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new BookListAdapter(new ArrayList<>(), book -> {
            Intent i = new Intent(this, BookDetailActivity.class);
            i.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getId());
            startActivity(i);
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                binding.btnClear.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                viewModel.search(q);
            }
        });

        // [ISPRAVKA L22] tipka "Traži" na tastaturi sada zaista nešto radi (zatvara tastaturu)
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { hideKeyboard(); return true; }
            return false;
        });

        binding.btnClear.setOnClickListener(v -> binding.etSearch.setText(""));

        viewModel.getResults().observe(this, adapter::updateBooks);

        // [ISPRAVKA L4] jedno stanje ekrana umjesto dva LiveData-a čiji su se observeri pregazivali
        viewModel.getState().observe(this, state -> {
            binding.startView.setVisibility(state == SearchViewModel.State.START ? View.VISIBLE : View.GONE);
            binding.emptyView.setVisibility(state == SearchViewModel.State.EMPTY ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(state == SearchViewModel.State.RESULTS ? View.VISIBLE : View.GONE);
            if (state == SearchViewModel.State.EMPTY) {
                binding.tvEmpty.setText(getString(R.string.search_no_results, viewModel.getCurrentQuery()));
            }
        });

        binding.etSearch.requestFocus();   // tastaturu otvara windowSoftInputMode="stateVisible" iz manifesta
    }

    @Override
    protected void onResume() {
        super.onResume();
        // [ISPRAVKA L15] osvježi rezultate (npr. knjiga obrisana na ekranu detalja)
        viewModel.search(binding.etSearch.getText() != null ? binding.etSearch.getText().toString() : "");
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
    }
}
