package com.personallibrary.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.personallibrary.databinding.ItemBookMoreBinding;
import com.personallibrary.models.Book;
import com.personallibrary.utils.CoverLoader;

import java.util.ArrayList;
import java.util.List;

/** Horizontal row of covers ("More by this author") on the book detail screen. */
public class MoreByAuthorAdapter extends RecyclerView.Adapter<MoreByAuthorAdapter.ViewHolder> {

    public interface OnBookClickListener { void onBookClick(Book book); }

    private List<Book> books = new ArrayList<>();
    private final OnBookClickListener listener;

    public MoreByAuthorAdapter(OnBookClickListener listener) { this.listener = listener; }

    public void updateBooks(List<Book> b) {
        books = b != null ? b : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemBookMoreBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Book b = books.get(position);
        h.binding.tvTitle.setText(b.getTitle());
        CoverLoader.load(h.binding.ivCover, b.getCoverImagePath(), b.getTitle());
        h.itemView.setOnClickListener(v -> listener.onBookClick(b));
    }

    @Override
    public int getItemCount() { return books.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemBookMoreBinding binding;
        ViewHolder(ItemBookMoreBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
