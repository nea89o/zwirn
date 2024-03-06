package moe.nea.zwirn.plugin;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.stitch.commands.tinyv2.TinyFile;
import net.fabricmc.stitch.commands.tinyv2.TinyV2Reader;
import net.fabricmc.stitch.commands.tinyv2.TinyV2Writer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemLocationProperty;
import org.gradle.api.file.RegularFileProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;

class ZwirnPluginUtils {
    public static Gson gson = new Gson();

    public static JsonObject readUrl(String url) {
        try (var reader = new InputStreamReader(new URL(url).openStream())) {
            return gson.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getPath(FileSystemLocationProperty<?> property) {
        return property.get().getAsFile().toPath();
    }

    public static InputStream readUrlAsStream(String downloadUrl) {
        try {
            return new URL(downloadUrl).openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        while (true) {
            int read = input.read(buffer);
            if (read < 0) break;
            output.write(buffer, 0, read);
        }
    }

    public static Path getPath(FileCollection collection) {
        return collection.getSingleFile().toPath();
    }

    public static TinyFile readTiny(RegularFileProperty tinyFile) {
        try {
            return TinyV2Reader.read(getPath(tinyFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeTiny(TinyFile tiny, RegularFileProperty tinyFile) {
        try{
            TinyV2Writer.write(tiny, getPath(tinyFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
