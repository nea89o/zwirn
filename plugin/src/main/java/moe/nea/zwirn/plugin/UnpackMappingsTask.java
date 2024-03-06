package moe.nea.zwirn.plugin;

import cuchaz.enigma.command.ConvertMappingsCommand;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

public abstract class UnpackMappingsTask extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getInputTinyFile();

    @OutputDirectory
    public abstract DirectoryProperty getOutputEnigmaDirectory();

    @TaskAction
    public void unpackMappings() throws MappingParseException, IOException {
        new ConvertMappingsCommand()
                .run(
                        "tinyv2",
                        ZwirnPluginUtils.getPath(getInputTinyFile()).toAbsolutePath().toString(),
                        "enigma",
                        ZwirnPluginUtils.getPath(getOutputEnigmaDirectory()).toAbsolutePath().toString()
                );
    }
}
