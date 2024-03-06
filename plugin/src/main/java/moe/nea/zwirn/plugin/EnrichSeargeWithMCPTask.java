package moe.nea.zwirn.plugin;

import moe.nea.zwirn.Zwirn;
import net.fabricmc.stitch.commands.tinyv2.TinyV2Reader;
import net.fabricmc.stitch.commands.tinyv2.TinyV2Writer;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.FileSystems;

public abstract class EnrichSeargeWithMCPTask extends DefaultTask {

    @InputFiles
    FileCollection mcpArchive;

    public FileCollection getMcpArchive() {
        return mcpArchive;
    }

    public void setMcpArchive(FileCollection mcpArchive) {
        this.mcpArchive = mcpArchive;
    }

    @InputFile
    public abstract RegularFileProperty getSrgTinyFile();

    @OutputFile
    public abstract RegularFileProperty getEnrichedTinyFile();

    @TaskAction
    public void enrich() throws IOException {
        var mcpFs = FileSystems.newFileSystem(getMcpArchive().getSingleFile().toPath());
        var enriched = Zwirn.enrichSeargeWithMCP(
                TinyV2Reader.read(ZwirnPluginUtils.getPath(getSrgTinyFile())),
                mcpFs.getPath("/")
        );
        mcpFs.close();
        TinyV2Writer.write(enriched, ZwirnPluginUtils.getPath(getEnrichedTinyFile()));
    }
}
