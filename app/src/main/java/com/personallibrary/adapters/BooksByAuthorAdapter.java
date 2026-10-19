package com.personallibrary.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.personallibrary.databinding.ItemAuthorHeaderBinding;
import com.personallibrary.databinding.ItemBookListBinding;
import com.personallibrary.models.Book;
import com.personallibrary.utils.CoverLoader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BooksByAuthorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_BOOK = 1;

    public interface OnBookClickListener { void onBookClick(Book book); }

    private final List<Object> items = new ArrayList<>();   // String = zaglavlje autora, Book = knjiga
    private final OnBookClickListener listener;

    public BooksByAuthorAdapter(OnBookClickListener listener) { this.listener = listener; }

    /**
     * [ISPRAVKA L23] Grupisanje po autoru je premješteno ovdje. Identična metoda groupByAuthor()
     * je ranije bila kopirana u LibraryFragment i WishlistFragment.
     */
    public void updateBooks(List<Book> books) {
        items.clear();
        if (books != null) {
            Map<String, List<Book>> byAuthor = new LinkedHashMap<>();
            for (Book b : books) {
                byAuthor.computeIfAbsent(b.getAuthorLastFirst(), k -> new ArrayList<>()).add(b);
            }
            for (Map.Entry<String, List<Book>> e : byAuthor.entrySet()) {
                items.add(e.getKey());
                items.addAll(e.getValue());
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_BOOK;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderVH(ItemAuthorHeaderBinding.inflate(inflater, parent, false));
        }
        return new BookVH(ItemBookListBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).binding.tvAuthorName.setText((String) items.get(position));
            return;
        }

        Book book = (Book) items.get(position);
        ItemBookListBinding v = ((BookVH) holder).binding;

        v.tvTitle.setText(book.getTitle());

        if (book.hasGenre()) {
            v.tvGenreBadge.setText(book.getGenre());
            v.tvGenreBadge.setVisibility(View.VISIBLE);
        } else {
            v.tvGenreBadge.setVisibility(View.GONE);
        }
        v.tvWishlistBadge.setVisibility(book.isInWishlist() ? View.VISIBLE : View.GONE);

        CoverLoader.load(v.ivCover, book.getCoverImagePath());   // [ISPRAVKA L6]
        holder.itemView.setOnClickListener(x -> listener.onBookClick(book));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class HeaderVH extends RecyclerView.ViewHolder {
        final ItemAuthorHeaderBinding binding;
        HeaderVH(ItemAuthorHeaderBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }

    static class BookVH extends RecyclerView.ViewHolder {
        final ItemBookListBinding binding;
        BookVH(ItemBookListBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
}
