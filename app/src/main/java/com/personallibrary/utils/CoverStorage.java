package com.personallibrary.utils;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * [ISPRAVKA K4] NOVA KLASA.
 *
 * Uri koji vrati ActivityResultContracts.GetContent je PRIVREMENA dozvola vezana za Activity.
 * Ako se takav "content://" Uri sačuva u bazu, nakon ponovnog pokretanja aplikacije Glide
 * više ne smije da ga otvori i naslovnica nestaje. Zato sliku odmah kopiramo u privatni
 * direktorij aplikacije (filesDir/covers) i u bazu upisujemo trajnu putanju.
 */
public final class CoverStorage {

    private static final String DIR = "covers";

    private CoverStorage() { }

    /** Kopira sliku u privatni direktorij. Vraća apsolutnu putanju ili null ako kopiranje ne uspije. */
    public static String copyToInternal(Context ctx, Uri source) {
        File dir = new File(ctx.getFilesDir(), DIR);
        if (!dir.exists() && !dir.mkdirs()) return null;

        File out = new File(dir, UUID.randomUUID() + ".jpg");
        try (InputStream in = ctx.getContentResolver().openInputStream(source);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) return null;
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) os.write(buf, 0, n);
            return out.getAbsolutePath();
        } catch (IOException | SecurityException e) {
            //noinspection ResultOfMethodCallIgnored
            out.delete();
            return null;
        }
    }

    /** Briše sliku samo ako je naša lokalna kopija (ne dira http/content adrese). */
    public static void deleteIfLocal(Context ctx, String path) {
        if (path == null || path.isEmpty()) return;
        File file = new File(path);
        File dir = new File(ctx.getFilesDir(), DIR);
        if (file.exists() && dir.equals(file.getParentFile())) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
