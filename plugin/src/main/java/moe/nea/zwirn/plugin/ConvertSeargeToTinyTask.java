package moe.nea.zwirn.plugin;

import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.util.zip.ZipFile;

public abstract class ConvertSeargeToTinyTask extends DefaultTask {
    @InputFiles
    FileCollection srgArchive;

    public void setSrgArchive(FileCollection srgArchive) {
        this.srgArchive = srgArchive;
    }

    public FileCollection getSrgArchive() {
        return srgArchive;
    }

    @OutputFile
    public abstract RegularFileProperty getSrgTinyFile();

    @TaskAction
    public void convert() throws IOException {
        var zipFile = new ZipFile(getSrgArchive().getSingleFile());
        var srgFile = INamedMappingFile.load(zipFile.getInputStream(zipFile.getEntry("joined.srg")));
        zipFile.close();
        srgFile.write(ZwirnPluginUtils.getPath(getSrgTinyFile()), IMappingFile.Format.TINY);
    }
}
