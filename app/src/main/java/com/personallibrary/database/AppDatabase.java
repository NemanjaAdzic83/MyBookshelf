package com.personallibrary.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.personallibrary.models.Book;
import com.personallibrary.utils.GenreConstants;
import java.util.List;

/**
 * AppDatabase v4.2
 *
 * Historija migracija:
 *   v1 → v2: dodani genre i rating (ALTER TABLE)
 *   v2 → v3: uklonjen rating — Room ne podržava DROP COLUMN
 *             na starijim SQLite verzijama, pa koristimo
 *             rekreaciju tabele (copy → drop → rename).
 *
 * Korisnici koji imaju v2 bazu zadržavaju sve podatke osim
 * kolone rating koja se briše bez gubitka knjiga.
 */
@Database(entities = {Book.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract BookDao bookDao();

    // v1 → v2: dodani genre i rating
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE books ADD COLUMN genre TEXT");
            db.execSQL("ALTER TABLE books ADD COLUMN rating REAL NOT NULL DEFAULT 0.0");
        }
    };

    // v2 → v3: uklanjamo kolonu rating (rekreacija tabele)
    //
    // [ISPRAVKA K2] Room nakon migracije poredi stvarnu šemu sa šemom iz @Entity klase.
    // Prethodna verzija je pravila:
    //   - indekse "idx_author_last" / "idx_wishlist"  (Room očekuje "index_books_author_last_name"
    //                                                   i "index_books_in_wishlist")
    //   - "title TEXT NOT NULL" i "author_last_name TEXT NOT NULL" (u Book.java ta polja su
    //                                                   nullable, pa Room očekuje "TEXT")
    // Rezultat: IllegalStateException "Migration didn't properly handle: books" pri nadogradnji.
    // Sada su tabela i indeksi identični onima koje Room sam generiše iz entiteta.
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE books_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "title TEXT, " +
                    "author_first_name TEXT, " +
                    "author_last_name TEXT, " +
                    "cover_image_path TEXT, " +
                    "in_wishlist INTEGER NOT NULL, " +
                    "date_added INTEGER NOT NULL, " +
                    "genre TEXT)");

            // COALESCE štiti od NULL vrijednosti u starim redovima (kolone su NOT NULL u novoj tabeli)
            db.execSQL("INSERT INTO books_new " +
                    "(id, title, author_first_name, author_last_name, cover_image_path, " +
                    "in_wishlist, date_added, genre) " +
                    "SELECT id, title, author_first_name, author_last_name, cover_image_path, " +
                    "COALESCE(in_wishlist, 0), COALESCE(date_added, 0), genre " +
                    "FROM books");

            db.execSQL("DROP TABLE books");
            db.execSQL("ALTER TABLE books_new RENAME TO books");

            // nazivi indeksa MORAJU biti: index_<tabela>_<kolona>
            db.execSQL("CREATE INDEX IF NOT EXISTS index_books_author_last_name ON books(author_last_name)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_books_in_wishlist ON books(in_wishlist)");
        }
    };

    /*
        v3 -> v4 : dodaje "read" zastavicu, postojece knjige su "not read"(DEFAULT 0), tako da nista nije izgubljeno
        Mjenjamo tabelu, tj dodajemo jos jednu kolonu
     */
    static final Migration MIGRATION_3_4 = new Migration(3,4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE books ADD COLUMN is_read INTEGER NOT NULL DEFAULT 0");
        }
    };

    // Earlier versions stored genre names in Serbian (Latin and Cyrillic). Same order as GenreConstants.ALL_GENRES.
    private static final String[] OLD_LATIN_GENRES = {
        "Roman", "Naučna fantastika", "Fantazija", "Misterija / Triler",
        "Historija", "Biografija", "Naučna literatura", "Tehnika / IT",
        "Filozofija", "Poezija", "Putopis", "Dječija književnost"
    };
    private static final String[] OLD_CYRILLIC_GENRES = {
        "Роман", "Научна фантастика", "Фантазија", "Мистерија / Трилер",
        "Хисторија", "Биографија", "Научна литература", "Техника / ИТ",
        "Филозофија", "Поезија", "Путопис", "Дјечија књижевност"
    };

    /** Converts old Serbian genre names to the current English ones on every open (harmless to repeat). */
    private static void migrateGenres(SupportSQLiteDatabase db) {
        List<String> current = GenreConstants.ALL_GENRES;
        for (int i = 0; i < current.size(); i++) {
            if (i < OLD_LATIN_GENRES.length)
                db.execSQL("UPDATE books SET genre = ? WHERE genre = ?", new Object[]{current.get(i), OLD_LATIN_GENRES[i]});
            if (i < OLD_CYRILLIC_GENRES.length)
                db.execSQL("UPDATE books SET genre = ? WHERE genre = ?", new Object[]{current.get(i), OLD_CYRILLIC_GENRES[i]});
        }
    }

    public static AppDatabase getInstance(Context ctx) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            ctx.getApplicationContext(),
                            AppDatabase.class,
                            "personal_library.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3,MIGRATION_3_4)
                            .addCallback(new RoomDatabase.Callback() {
                                @Override public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    migrateGenres(db);
                                }
                            })
                            .build();
                }
            }
        }
        return instance;
    }
}
