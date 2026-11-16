# CLAUDE.md – MyBookshelf

Pravila i tehničke informacije o projektu. Opšta pravila rada (jezik, mentorski pristup,
odobrenje prije izmjena, Git) su u globalnom `~/.claude/CLAUDE.md` i važe i ovdje.

## O aplikaciji

Lična Android aplikacija za vođenje biblioteke: knjige koje imam (Library) i koje želim (Wishlist).
Knjige se dodaju skeniranjem ISBN barkoda (Google Books, pa Open Library kao rezerva) ili ručno.
Sve se čuva lokalno (offline-first), internet treba samo za pretragu po ISBN-u i naslovnice sa weba.

## Tehnologije

- **Jezik:** Java, nivo jezika 17 (`compileOptions` u `app/build.gradle`), bez Kotlina.
  Dostupne klase/metode određuje `minSdk 24`, ne verzija Jave (npr. `List.of()` nije dozvoljen).
- **Build:** Gradle 8.2, AGP 8.2.2 (Groovy `build.gradle`), `compileSdk 34`, `targetSdk 34`, `minSdk 24`.
  Gradle se pokreće na JDK 21 iz Android Studija (`D:\Android Studio\jbr`).
  AGP 8.2.0 ne radi sa JDK 21 + Java 17 (greška u `jlink`) – ne vraćati na 8.2.0.
- **Mreža na razvojnom računaru:** Avast HTTPS scanning mora biti isključen, inače Gradle ne može
  preuzeti biblioteke (`PKIX path building failed`).
- **UI:** XML layouti + ViewBinding, ConstraintLayout, Material Components (`Theme.MaterialComponents.Light`)
- **Arhitektura:** MVVM – ViewModel + LiveData
- **Baza:** Room (SQLite), trenutno **verzija 5**, `exportSchema = false`
- **Mreža:** OkHttp (Google Books API, Open Library)
- **Slike:** Glide (preko `utils/CoverLoader`)
- **Skeniranje:** ZXing (`zxing-android-embedded`)
- **Nema:** Hilt/DI, Retrofit, Coroutines, Compose, Navigation Component

## Struktura paketa (`com.personallibrary`)

| Paket | Odgovornost |
|---|---|
| `activities` | `MainActivity` (drži fragmente + pill navigaciju), `AddBookActivity`, `BookDetailActivity`, `SearchActivity`, `BarcodeScannerActivity` |
| `fragments` | `HomeFragment` (dashboard + backup meni), `LibraryFragment`, `WishlistFragment` |
| `viewmodel` | Po jedan ViewModel za svaki ekran |
| `repository` | `BookRepository` – jedini ulaz u bazu za ViewModele; pozadinski rad na single-thread `ExecutorService`, rezultat kroz `Callback<T>` |
| `database` | `AppDatabase` (singleton + migracije 1→5), `BookDao` |
| `models` | `Book` (Room entitet, tabela `books`), `BookInfo` (rezultat ISBN pretrage) |
| `adapters` | RecyclerView adapteri (grid, lista, grupisano po autoru, "more by author") |
| `utils` | `BackupManager`, `CoverLoader`, `CoverDrawable`, `CoverStorage`, `GenreConstants`, `GoogleBooksApiClient`, `BookDates`, `ScanResultDialog` |
| `views` | `PillNavigationView` – plutajuća donja navigacija |

Tok podataka: **Activity/Fragment → ViewModel → BookRepository → BookDao → Room**.
Navigacija između glavnih ekrana: `MainActivity.load()` radi `replace` fragmenta u `fragment_container`.

## Posebna pravila i konvencije

- **Tekst za korisnika** je na engleskom i ide u `res/values/strings.xml`. Nazivi žanrova su u `GenreConstants.ALL_GENRES`.
- **Žanr** u bazi mora biti tačno jedna vrijednost iz `GenreConstants.ALL_GENRES` (stari srpski nazivi se mapiraju u migraciji).
- **Duplikat knjige** = isti naslov + isto prezime autora, bez obzira na velika/mala slova (ručni unos i import).
- **Knjiga u wishlisti ne može biti pročitana** (`is_read = 0`, `read_date = 0`).
- **Promjena šeme baze:** povećati `version` u `AppDatabase`, napisati novu `Migration` i dodati je u `addMigrations(...)`.
  Nikad `fallbackToDestructiveMigration` – korisnik bi izgubio knjige. Ako se mijenja `Book`, ažurirati i `BackupManager` (export + import).
- **Backup format:** ZIP sa `books.json` (`format: "MyBookshelfBackup"`, `version: 2`) i `covers/N.jpg`.
  Import samo dodaje knjige, nikad ne briše ni ne prepisuje. Imena fajlova iz ZIP-a se ne koriste kao putanje (zip slip).
- **Naslovnice:** uvijek učitavati preko `CoverLoader` (otkazuje stare Glide zahtjeve u recikliranim redovima).
  Web naslovnice moraju biti `https` – `usesCleartextTraffic="false"`.
- **Google Books API ključ** je u `local.properties` (`GOOGLE_BOOKS_API_KEY`), nikad u Git-u.
- **Tema:** svijetla pozadina sistemskih traka → `windowLightStatusBar` / `windowLightNavigationBar` = `true`.
  Ekran skenera ima posebnu tamnu temu `Theme.PersonalLibrary.Scanner`.
- **Commit poruke** na engleskom, jedna logička cjelina po commitu.

## Pokretanje

Otvoriti projekat u Android Studiju, sačekati Gradle Sync, pokrenuti na uređaju/emulatoru API 24+.
Iz terminala: `./gradlew assembleDebug` (na Windowsu `.\gradlew.bat assembleDebug`, uz `JAVA_HOME=D:\Android Studio\jbr`).

## Testovi

Trenutno ne postoje (`app/src/test` i `app/src/androidTest` nisu napravljeni). Vidi `PROJECT_PLAN.md`.
