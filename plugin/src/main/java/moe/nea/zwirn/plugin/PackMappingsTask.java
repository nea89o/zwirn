package moe.nea.zwirn.plugin;

import cuchaz.enigma.command.ConvertMappingsCommand;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

public abstract class PackMappingsTask extends DefaultTask {
    @InputDirectory
    public abstract DirectoryProperty getInputEnigmaDirectory();

    @OutputFile
    public abstract RegularFileProperty getOutputTinyFile();

    @Input
    public abstract Property<String> getObfuscatedNamespace();

    @Input
    public abstract Property<String> getReadableNamespace();

    @TaskAction
    public void packMappings() throws MappingParseException, IOException {
        new ConvertMappingsCommand().run(
                "enigma",
                ZwirnPluginUtils.getPath(getInputEnigmaDirectory()).toAbsolutePath().toString(),
                "tinyv2:" + getObfuscatedNamespace().get() + ":" + getReadableNamespace().get(),
                ZwirnPluginUtils.getPath(getOutputTinyFile()).toAbsolutePath().toString()
        );
    }

}
