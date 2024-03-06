package moe.nea.zwirn.plugin;

import moe.nea.zwirn.Zwirn;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class MergeTinyFilesTask extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getBaseTinyFile();

    @InputFile
    public abstract RegularFileProperty getOverlayTinyFile();

    @Input
    public abstract Property<String> getSharedNamespace();

    @OutputFile
    public abstract RegularFileProperty getOutputTinyFile();

    @TaskAction
    public void mergeTinyFiles() {
        var merged = Zwirn.mergeTinyFile(
                ZwirnPluginUtils.readTiny(getBaseTinyFile()),
                ZwirnPluginUtils.readTiny(getOverlayTinyFile()),
                getSharedNamespace().get()
        );
        ZwirnPluginUtils.writeTiny(merged, getOutputTinyFile());
    }
}
