package com.personallibrary.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.personallibrary.models.Book;
import com.personallibrary.repository.BookRepository;

import java.util.Collections;
import java.util.List;

public class SearchViewModel extends AndroidViewModel {

    /**
     * [ISPRAVKA L4] Umjesto dva LiveData-a (showStart, showEmpty) čiji su se observeri
     * međusobno pregazivali, postoji jedno stanje ekrana.
     */
    public enum State { START, EMPTY, RESULTS }

    private final BookRepository repo;
    private final MutableLiveData<List<Book>> results = new MutableLiveData<>(Collections.<Book>emptyList());
    private final MutableLiveData<State> state = new MutableLiveData<>(State.START);
    private String currentQuery = "";

    public SearchViewModel(@NonNull Application app) {
        super(app);
        repo = new BookRepository(app);
    }

    public LiveData<List<Book>> getResults() { return results; }
    public LiveData<State> getState()         { return state; }
    public String getCurrentQuery()           { return currentQuery; }

    public void search(String query) {
        final String q = query == null ? "" : query.trim();
        currentQuery = q;

        if (q.isEmpty()) {
            results.setValue(Collections.<Book>emptyList());   // briše stare rezultate
            state.setValue(State.START);
            return;
        }

        repo.searchBooks(q, list -> {
            // [ISPRAVKA L4] Zakašnjeli odgovor za stari upit se ignoriše. Ranije je upit "a",
            // brzo obrisan, mogao stići nakon brisanja i vratiti rezultate preko START ekrana.
            if (!q.equals(currentQuery)) return;
            results.setValue(list);
            state.setValue(list.isEmpty() ? State.EMPTY : State.RESULTS);
        });
    }
}
