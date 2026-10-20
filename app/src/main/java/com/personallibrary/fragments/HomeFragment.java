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

import android.net.Uri;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import com.personallibrary.utils.BackupManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private BookGridAdapter adapter;

     // The system file picker. CreateDocument lets the user choose WHERE to save (Downloads,
     // Google Drive...), OpenDocument lets them choose the file to restore. Neither needs a
    // storage permission because the user hands us one specific file.
    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/zip"),
            uri -> { if (uri != null) runExport(uri); });

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) runImport(uri); });

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
        //binding.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        int spanCount = Math.max(2,screenWidthDp / 180);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(),spanCount));
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

        binding.btnBackupMenu.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            menu.getMenuInflater().inflate(R.menu.menu_backup, menu.getMenu());
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_export_backup) {
                    exportLauncher.launch(defaultBackupName());
                    return true;
                }
                if (item.getItemId() == R.id.action_import_backup) {
                    confirmImport();
                    return true;
                }
                return false;
            });
            menu.show();
        });
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

    private String defaultBackupName() {
        return "MyBookshelf-backup-"
                + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()) + ".zip";
    }

    private void runExport(Uri uri) {
        Toast.makeText(requireContext(), R.string.backup_working, Toast.LENGTH_SHORT).show();
        BackupManager.exportTo(requireContext(), uri, r -> {
            if (!isAdded()) return;     // the user may have left the screen meanwhile
            String msg = r.ok
                    ? getString(R.string.backup_export_ok, r.count)
                    : getString(R.string.backup_export_fail, r.error);
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
        });
    }

    private void confirmImport() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.import_confirm_title)
                .setMessage(R.string.import_confirm_message)
                .setPositiveButton(android.R.string.ok, (d, w) -> importLauncher.launch(new String[]{"*/*"}))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void runImport(Uri uri) {
        Toast.makeText(requireContext(), R.string.backup_working, Toast.LENGTH_SHORT).show();
        BackupManager.importFrom(requireContext(), uri, r -> {
            if (!isAdded()) return;
            String msg = r.ok
                    ? getString(R.string.import_done_message, r.count, r.skipped)
                    : getString(R.string.import_fail, r.error);
            new AlertDialog.Builder(requireContext())
                    .setMessage(msg)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            if (r.ok) viewModel.loadData();     // refresh the counters and the grid
        });
    }
}
