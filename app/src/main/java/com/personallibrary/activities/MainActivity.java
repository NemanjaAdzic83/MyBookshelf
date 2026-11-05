package com.personallibrary.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
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

        binding.bottomNavigation.setMenu(R.menu.bottom_nav_menu);
        binding.bottomNavigation.setOnItemSelectedListener(id -> {
            if (id == R.id.nav_home)          load(new HomeFragment());
            else if (id == R.id.nav_library)  load(new LibraryFragment());
            else if (id == R.id.nav_wishlist) load(new WishlistFragment());
        });

        binding.fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AddBookActivity.class)));


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.bottomNavigation.getSelectedItemId() != R.id.nav_home){
                    binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
                }else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
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
