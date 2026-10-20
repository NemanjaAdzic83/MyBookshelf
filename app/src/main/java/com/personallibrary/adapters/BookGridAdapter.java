package com.personallibrary.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.personallibrary.databinding.ItemBookGridBinding;
import com.personallibrary.models.Book;
import com.personallibrary.utils.CoverLoader;

import java.util.ArrayList;
import java.util.List;

public class BookGridAdapter extends RecyclerView.Adapter<BookGridAdapter.ViewHolder> {

    public interface OnBookClickListener { void onBookClick(Book book); }

    private List<Book> books = new ArrayList<>();
    private final OnBookClickListener listener;

    public BookGridAdapter(OnBookClickListener listener) { this.listener = listener; }

    public void updateBooks(List<Book> b) {
        books = b != null ? b : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemBookGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Book b = books.get(position);
        h.binding.tvTitle.setText(b.getTitle());
        h.binding.tvAuthor.setText(b.getFullAuthorName());
        CoverLoader.load(h.binding.ivCover, b.getCoverImagePath());
        h.binding.ivReadStamp.setVisibility(b.isRead() ? View.VISIBLE : View.GONE);
        h.itemView.setOnClickListener(v -> listener.onBookClick(b));
    }

    @Override
    public int getItemCount() { return books.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemBookGridBinding binding;
        ViewHolder(ItemBookGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
