package com.personallibrary.activities;

import android.Manifest;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.personallibrary.R;
import com.personallibrary.databinding.ActivityBarcodeScannerBinding;
import com.personallibrary.models.Book;
import com.personallibrary.models.BookInfo;
import com.personallibrary.utils.ScanResultDialog;
import com.personallibrary.viewmodel.ScannerViewModel;

import java.util.Arrays;
import java.util.List;

public class BarcodeScannerActivity extends AppCompatActivity {

    private ActivityBarcodeScannerBinding binding;
    private ScannerViewModel viewModel;
    private Dialog activeDialog;
    private boolean hasCamera;

    // [ISPRAVKA L20] Moderni API umjesto zastarjelog onRequestPermissionsResult()
    private final ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startScanning();
                } else {
                    Toast.makeText(this, R.string.camera_permission_needed, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    // [ISPRAVKA K3] Jedan listener umjesto dva skoro identična (makeListener + anonimna klasa)
    private final ScanResultDialog.ScanResultListener dialogListener = new ScanResultDialog.ScanResultListener() {
        @Override public void onAddToWishlist(BookInfo info) { saveAndClose(info, true); }
        @Override public void onAddToLibrary(BookInfo info)  { saveAndClose(info, false); }

        @Override public void onViewExistingBook(Book book) {
            Intent i = new Intent(BarcodeScannerActivity.this, BookDetailActivity.class);
            i.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getId());
            startActivity(i);
            finish();
        }

        @Override public void onManualEntry() {
            startActivity(new Intent(BarcodeScannerActivity.this, AddBookActivity.class));
            finish();
        }

        @Override public void onSearchWeb(String isbn) {
            Uri uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode("ISBN " + isbn));
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(BarcodeScannerActivity.this, R.string.toast_no_browser, Toast.LENGTH_SHORT).show();
            }
        }

        @Override public void onDismiss() {
            activeDialog = null;
            viewModel.scanFinished();
            if (canScan()) binding.barcodeScanner.resume();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBarcodeScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(ScannerViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.scanner_title);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        if (!hasCamera) { showNoCameraDialog(); return; }

        binding.barcodeScanner.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(Arrays.asList(
                BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39, BarcodeFormat.QR_CODE, BarcodeFormat.DATA_MATRIX)));
        binding.barcodeScanner.setStatusText("");

        viewModel.getState().observe(this, this::render);

        if (hasCameraPermission()) startScanning();
        else cameraPermission.launch(Manifest.permission.CAMERA);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean canScan() {
        return binding != null && hasCamera && hasCameraPermission() && !viewModel.isBusy();
    }

    private void startScanning() {
        binding.barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override public void barcodeResult(BarcodeResult result) {
                if (result == null || result.getText() == null || viewModel.isBusy()) return;
                String code = result.getText().trim();
                if (code.isEmpty()) return;
                binding.barcodeScanner.pause();
                viewModel.onCodeScanned(code);
            }
            @Override public void possibleResultPoints(List<ResultPoint> points) { }
        });
    }

    /** Prikazuje stanje koje je ViewModel objavio (učitavanje / pronađeno / nije pronađeno / greška). */
    private void render(ScannerViewModel.ScanState s) {
        if (s == null) return;

        boolean loading = s.type == ScannerViewModel.Type.LOADING;
        binding.loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.tvScanHint.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        if (loading) return;

        switch (s.type) {
            case FOUND:
                activeDialog = ScanResultDialog.showBookFound(this, s.info, s.existing, dialogListener);
                break;
            case NOT_FOUND:
                activeDialog = ScanResultDialog.showNotFound(this, s.isbn, dialogListener);
                break;
            case ERROR:
                activeDialog = ScanResultDialog.showError(this, s.message, dialogListener);
                break;
            default:
                break;
        }
        viewModel.consumeState();   // dijalog se ne smije ponovo pojaviti nakon rekreiranja Activity-ja
    }

    private void saveAndClose(BookInfo info, boolean wishlist) {
        Context app = getApplicationContext();     // Toast ne smije držati referencu na Activity
        String ok = getString(wishlist ? R.string.toast_scan_added_wishlist : R.string.toast_scan_added_library,
                info.getTitle());
        String err = getString(R.string.toast_save_error);
        viewModel.saveBook(info, wishlist,
                success -> Toast.makeText(app, success ? ok : err, Toast.LENGTH_LONG).show());
        finish();
    }

    private void showNoCameraDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.no_camera_title)
                .setMessage(R.string.no_camera_message)
                .setCancelable(false)
                .setPositiveButton(R.string.enter_manually, (d, w) -> {
                    startActivity(new Intent(this, AddBookActivity.class));
                    finish();
                })
                .setNegativeButton(R.string.give_up, (d, w) -> finish())
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (canScan()) binding.barcodeScanner.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.barcodeScanner.pause();
    }

    @Override
    protected void onDestroy() {
        if (activeDialog != null) {
            activeDialog.setOnDismissListener(null);   // sprječava pristup View-ovima tokom uništavanja
            activeDialog.dismiss();
            activeDialog = null;
            if (!isFinishing()) viewModel.scanFinished();   // promjena konfiguracije: skener mora moći dalje
        }
        super.onDestroy();
    }
}
