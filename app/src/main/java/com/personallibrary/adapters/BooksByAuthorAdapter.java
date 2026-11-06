package com.personallibrary.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.personallibrary.R;
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
    public interface OnBoughtClickListener{
        void onBought(Book book);
    }

    private OnBoughtClickListener boughtListener;
    public void setOnBoughtClickListener(OnBoughtClickListener l){
        this.boughtListener = l;
        notifyDataSetChanged();
    }

    private final List<Object> items = new ArrayList<>();   // String = zaglavlje autora, Book = knjiga
    private final OnBookClickListener listener;
    private boolean grouped = true;

    public BooksByAuthorAdapter(OnBookClickListener listener) { this.listener = listener; }

    public void updateBooks(List<Book> books) { updateBooks(books, true); }

    /**
     * grouped = true : author headers + books (default sort).
     * grouped = false: plain list in the given order, rows show the author name.
     */
    public void updateBooks(List<Book> books, boolean grouped) {
        this.grouped = grouped;
        items.clear();
        if (books != null) {
            if (grouped) {
                Map<String, List<Book>> byAuthor = new LinkedHashMap<>();
                for (Book b : books) {
                    byAuthor.computeIfAbsent(b.getAuthorLastFirst(), k -> new ArrayList<>()).add(b);
                }
                for (Map.Entry<String, List<Book>> e : byAuthor.entrySet()) {
                    items.add(e.getKey());
                    items.addAll(e.getValue());
                }
            } else {
                items.addAll(books);
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

        if (grouped){
            v.tvAuthor.setVisibility(View.GONE);
        }else {
            v.tvAuthor.setText(book.getFullAuthorName());
            v.tvAuthor.setVisibility(View.VISIBLE);
        }

        if (book.hasGenre()) {
            v.tvGenreBadge.setText(book.getGenre());
            v.tvGenreBadge.setVisibility(View.VISIBLE);
        } else {
            v.tvGenreBadge.setVisibility(View.GONE);
        }
        v.tvWishlistBadge.setVisibility(book.isInWishlist() ? View.VISIBLE : View.GONE);
        v.ivReadStamp.setVisibility(book.isRead() ? View.VISIBLE : View.GONE);

        if (boughtListener != null) {
            v.ivChevron.setVisibility(View.GONE);
            v.btnBought.setVisibility(View.VISIBLE);
            v.btnBought.setContentDescription(
                    holder.itemView.getContext().getString(R.string.bought_description, book.getTitle()));
            v.btnBought.setOnClickListener(x -> boughtListener.onBought(book));
        } else {
            v.ivChevron.setVisibility(View.VISIBLE);
            v.btnBought.setVisibility(View.GONE);
        }

        CoverLoader.load(v.ivCover, book.getCoverImagePath(), book.getTitle());
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
