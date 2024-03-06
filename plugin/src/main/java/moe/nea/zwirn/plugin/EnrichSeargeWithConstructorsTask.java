package moe.nea.zwirn.plugin;

import moe.nea.zwirn.Zwirn;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.FileSystems;

public abstract class EnrichSeargeWithConstructorsTask extends DefaultTask {
    @InputFiles
    FileCollection srgArchive;

    public void setSrgArchive(FileCollection srgArchive) {
        this.srgArchive = srgArchive;
    }

    public FileCollection getSrgArchive() {
        return srgArchive;
    }

    @InputFile
    public abstract RegularFileProperty getSrgTinyFile();

    @OutputFile
    public abstract RegularFileProperty getEnrichedTinyFile();

    @TaskAction
    public void enrich() throws IOException {
        var srgFs = FileSystems.newFileSystem(ZwirnPluginUtils.getPath(getSrgArchive()));
        var enriched = Zwirn.enrichSeargeWithConstructors(
                ZwirnPluginUtils.readTiny(getSrgTinyFile()),
                srgFs.getPath("/")
        );
        srgFs.close();
        ZwirnPluginUtils.writeTiny(enriched, getEnrichedTinyFile());
    }
}
