package com.personallibrary.utils;

import java.util.Arrays;
import java.util.List;

public class GenreConstants {
    // New genres must be appended at the END of the list (the genre migration in AppDatabase maps by position).
    public static final List<String> ALL_GENRES = Arrays.asList(
        "Novel", "Science fiction", "Fantasy", "Mystery / Thriller",
        "History", "Biography", "Non-fiction", "Technology / IT",
        "Philosophy", "Poetry", "Travel", "Children's"
    );
}
