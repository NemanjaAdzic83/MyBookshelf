package com.personallibrary.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.personallibrary.R;
import com.personallibrary.databinding.ItemBookSearchBinding;
import com.personallibrary.models.Book;
import com.personallibrary.utils.CoverLoader;

import java.util.ArrayList;
import java.util.List;

public class BookListAdapter extends RecyclerView.Adapter<BookListAdapter.ViewHolder> {

    public interface OnBookClickListener { void onBookClick(Book book); }

    private List<Book> books;
    private final OnBookClickListener listener;

    public BookListAdapter(List<Book> books, OnBookClickListener listener) {
        this.books = books != null ? books : new ArrayList<>();
        this.listener = listener;
    }

    public void updateBooks(List<Book> b) {
        books = b != null ? b : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemBookSearchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Book b = books.get(position);
        ItemBookSearchBinding v = h.binding;

        v.tvTitle.setText(b.getTitle());
        v.tvAuthor.setText(b.getFullAuthorName());

        boolean wish = b.isInWishlist();
        v.tvStatus.setText(wish ? R.string.badge_wishlist : R.string.status_library_short);
        v.tvStatus.setBackgroundResource(wish ? R.drawable.bg_status_wishlist : R.drawable.bg_status_library);
        v.tvStatus.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                wish ? R.color.wishlist_color : R.color.library_color));

        CoverLoader.load(v.ivCover, b.getCoverImagePath());   // [ISPRAVKA L6]
        h.itemView.setOnClickListener(x -> listener.onBookClick(b));
    }

    @Override
    public int getItemCount() { return books.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemBookSearchBinding binding;
        ViewHolder(ItemBookSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
