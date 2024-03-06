package moe.nea.zwirn.plugin;

import moe.nea.zwirn.Zwirn;
import net.fabricmc.stitch.commands.tinyv2.TinyV2Reader;
import net.fabricmc.stitch.commands.tinyv2.TinyV2Writer;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

public abstract class ReorderNamespacesTask extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getInputTinyFile();

    @OutputFile
    public abstract RegularFileProperty getOutputTinyFile();

    @Input
    public abstract ListProperty<Zwirn.RenameCommand> getNamespaceOrder();

    public void appendNamespaceKeepName(String namespace) {
        appendNamespaceRenamed(namespace, namespace);
    }

    public void appendNamespaceRenamed(String oldName, String newName) {
        getNamespaceOrder().add(new Zwirn.RenameCommand(oldName, newName));
    }

    @TaskAction
    public void reorderNamespaces() throws IOException {
        var oldFile = TinyV2Reader.read(ZwirnPluginUtils.getPath(getInputTinyFile()));
        var newFile = Zwirn.renameNamespaces(oldFile, getNamespaceOrder().get());
        TinyV2Writer.write(newFile, ZwirnPluginUtils.getPath(getOutputTinyFile()));
    }

}
