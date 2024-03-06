package moe.nea.zwirn.plugin;

import moe.nea.zwirn.Zwirn;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class DiffTinyFilesTask extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getMergedTinyFile();

    @InputFile
    public abstract RegularFileProperty getBaseTinyFile();

    @OutputFile
    public abstract RegularFileProperty getOutputTinyFile();

    @Input
    public abstract Property<String> getSharedNamespace();

    @Input
    public abstract ListProperty<String> getRetainedNamespaces();

    @TaskAction
    public void diffTiny() {
        var merged = ZwirnPluginUtils.readTiny(getMergedTinyFile());
        var base = ZwirnPluginUtils.readTiny(getBaseTinyFile());
        var remerged = Zwirn.mergeTinyFile(base, merged, getSharedNamespace().get());
        var overlay = Zwirn.createOverlayTinyFile(base, remerged, getRetainedNamespaces().get(), getSharedNamespace().get());
        ZwirnPluginUtils.writeTiny(overlay, getOutputTinyFile());
    }


}
