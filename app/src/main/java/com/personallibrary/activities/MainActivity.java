package com.personallibrary.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.personallibrary.R;
import com.personallibrary.databinding.ActivityMainBinding;
import com.personallibrary.fragments.HomeFragment;
import com.personallibrary.fragments.LibraryFragment;
import com.personallibrary.fragments.WishlistFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) load(new HomeFragment());

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_search) {
                startActivity(new Intent(this, SearchActivity.class));
                return false;    // [ISPRAVKA L19] kartica se ne označava, pa nije potreban post(setSelectedItemId)
            }
            if (id == R.id.nav_home)     { load(new HomeFragment());     return true; }
            if (id == R.id.nav_library)  { load(new LibraryFragment());  return true; }
            if (id == R.id.nav_wishlist) { load(new WishlistFragment()); return true; }
            return false;
        });

        // [ISPRAVKA L19] ponovni dodir na već izabranu karticu ne pravi novi fragment
        binding.bottomNavigation.setOnItemReselectedListener(item -> { });

        binding.fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AddBookActivity.class)));
        binding.fabScan.setOnClickListener(v -> startActivity(new Intent(this, BarcodeScannerActivity.class)));
    }

    // [ISPRAVKA L7] UKLONJEN onResume() koji je radio detach().attach() trenutnog fragmenta.
    // To je pri svakom povratku u aplikaciju uništavalo i ponovo gradilo cijeli View fragmenta,
    // a fragmenti već sami osvježavaju podatke u svom onResume().

    private void load(Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f)
                .commit();
    }
}
