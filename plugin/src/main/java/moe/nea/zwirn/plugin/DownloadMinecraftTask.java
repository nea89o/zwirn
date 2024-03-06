package moe.nea.zwirn.plugin;

import com.google.gson.JsonObject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;

public abstract class DownloadMinecraftTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @OutputFile
    public abstract RegularFileProperty getMinecraftJar();


    private JsonObject selectVersion(JsonObject meta) {
        for (var version : meta.getAsJsonArray("versions")) {
            if (version.getAsJsonObject().get("id").getAsString().equals(getVersion().get())) {
                return version.getAsJsonObject();
            }
        }
        throw new RuntimeException("Could not find version " + getVersion().get());
    }

    @TaskAction
    public void downloadMinecraft() {
        var meta = ZwirnPluginUtils.readUrl("https://launchermeta.mojang.com/mc/game/version_manifest.json");
        var versionObject = selectVersion(meta);
        var versionManifestUrl = versionObject.get("url").getAsString();
        var versionManifest = ZwirnPluginUtils.readUrl(versionManifestUrl);
        var downloadUrl = versionManifest.getAsJsonObject("downloads")
                .getAsJsonObject("client").get("url").getAsString();
        try (
                var output = Files.newOutputStream(ZwirnPluginUtils.getPath(getMinecraftJar()));
                var remote = ZwirnPluginUtils.readUrlAsStream(downloadUrl)
        ) {
            ZwirnPluginUtils.copy(remote, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
