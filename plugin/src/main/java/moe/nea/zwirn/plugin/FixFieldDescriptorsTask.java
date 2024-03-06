package moe.nea.zwirn.plugin;

import moe.nea.zwirn.Zwirn;
import net.fabricmc.stitch.commands.tinyv2.TinyV2Reader;
import net.fabricmc.stitch.commands.tinyv2.TinyV2Writer;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.FileSystems;

public abstract class FixFieldDescriptorsTask extends DefaultTask {

    @InputFile
    public abstract RegularFileProperty getJarInFirstNamespace();

    @InputFile
    public abstract RegularFileProperty getInputTinyFile();

    @InputFile
    public abstract RegularFileProperty getOutputTinyFile();

    @TaskAction
    public void fixFields() throws IOException {
        var tinyIn = TinyV2Reader.read(ZwirnPluginUtils.getPath(getInputTinyFile()));
        var fs = FileSystems.newFileSystem(ZwirnPluginUtils.getPath(getJarInFirstNamespace()));
        var fixed = Zwirn.fixFieldDescriptorsFromJar(tinyIn, fs.getPath("/"));
        fs.close();
        TinyV2Writer.write(fixed, ZwirnPluginUtils.getPath(getOutputTinyFile()));
    }

}
