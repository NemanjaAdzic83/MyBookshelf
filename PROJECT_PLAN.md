# PROJECT_PLAN.md – MyBookshelf

Plan razvoja po fazama. Faze 1–3 su urađene (rekonstruisano iz Git istorije).
Faze 4 i dalje su **prijedlog** – redoslijed i obim treba potvrditi prije početka.

## Faza 1 – Osnova aplikacije ✅

1. Room baza, `Book` entitet, DAO, repository, MVVM ekrani
2. Library i Wishlist, prebacivanje iz wishliste u biblioteku (i nazad)
3. Skeniranje ISBN-a, pretraga Google Books → Open Library
4. Ručni unos i izmjena knjige, naslovnica iz galerije
5. Pretraga bez dijakritika, filter po žanru, grupisanje po autoru
6. Formatiranje naslova i imena (veliko početno slovo)

## Faza 2 – Funkcije za korisnika ✅

1. Oznaka "pročitano" + filter read/unread + broj pročitanih na Home
2. Backup export/import (ZIP sa JSON-om i naslovnicama)
3. Generisane naslovnice za knjige bez slike
4. Izdavač, serija, datum kupovine i datum čitanja (baza v5)

## Faza 3 – Redizajn interfejsa ✅

1. Home dashboard sa karticama i gridom nedavno dodanih
2. Plutajuća pill navigacija umjesto bottom navigation
3. Search bar sa ikonom barkoda u Library i Wishlist
4. Sortiranje biblioteke (autor, naslov, najnovije, najstarije)
5. Novi ekran detalja knjige (hero naslovnica, tabela podataka, "more by author", donja traka akcija)

## Faza 4 – Priprema za GitHub (u toku)

| # | Zadatak | Kako provjeriti |
|---|---|---|
| 4.1 | Tamne ikonice statusne i navigacione trake | Sat/baterija i dugmad vidljivi na svim ekranima; skener i dalje bijelo na crnom |
| 4.2 | Demo podaci (`demo-backup.zip`, 20 knjiga) i screenshotovi u `docs/screenshots/` | Import prolazi, sve naslovnice se učitaju |
| 4.3 | Ažurirati `README.md`: screenshotovi, nove funkcije (backup, pročitano, sortiranje, izdavač/serija), izvor podataka i naslovnica (Open Library, Google Books), ukloniti zastarjelu stavku "No backup/export" | README na GitHubu prikazuje slike i tačan opis |
| 4.4 | Dodati Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) za Gradle 8.2 | `./gradlew assembleDebug` radi iz terminala na čistom klonu |

## Faza 5 – Testovi (prijedlog)

| # | Zadatak | Kako provjeriti |
|---|---|---|
| 5.1 | Unit testovi za čistu logiku (formatiranje imena, pretraga bez dijakritika, duplikati, `GenreConstants`) | `./gradlew test` zelen |
| 5.2 | Instrumented testovi za `BookDao` (in-memory Room baza) | `./gradlew connectedAndroidTest` zelen |
| 5.3 | Test migracija baze 1→5 (`MigrationTestHelper`, zahtijeva `exportSchema = true`) | Stara baza se otvara bez greške i bez gubitka podataka |
| 5.4 | Test backup round-trip (export pa import daje iste knjige) | Broj i podaci knjiga isti |

## Faza 6 – Poznata ograničenja iz README-a (prijedlog)

| # | Zadatak | Kako provjeriti |
|---|---|---|
| 6.1 | Čuvati ISBN uz knjigu (baza v6 + migracija + backup) | ISBN vidljiv na detaljima, preživi export/import |
| 6.2 | Upozorenje na duplikat pri skeniranju po ISBN-u | Skeniranje iste knjige dvaput javlja da već postoji |
| 6.3 | Pretraga koja povezuje ćirilicu i latinicu (`Андрић` ↔ `Andrić`) | Obje varijante nalaze istu knjigu |

## Faza 7 – Moguće dalje učenje (otvoreno)

Ideje za kasnije, kad budeš htio da učiš nove tehnologije na postojećem projektu:
postepeni prelazak na Kotlin, Coroutines/Flow umjesto `ExecutorService` + `Callback`,
Retrofit umjesto ručnog OkHttp parsiranja, Hilt za dependency injection, tamna tema.
