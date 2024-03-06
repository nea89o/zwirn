package moe.nea.zwirn.plugin;

import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.util.regex.Pattern;

public abstract class MapJarTask extends DefaultTask {
    @InputFiles
    FileCollection inputJar;

    public FileCollection getInputJar() {
        return inputJar;
    }

    public void setInputJar(FileCollection inputJar) {
        this.inputJar = inputJar;
    }

    @InputFile
    public abstract RegularFileProperty getMappingTinyFile();

    @Input
    public abstract Property<String> getInputNamespace();

    @Input
    public abstract Property<String> getOutputNamespace();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @TaskAction
    public void mapJar() throws IOException {
        var mappingProvider =
                TinyUtils.createTinyMappingProvider(
                        ZwirnPluginUtils.getPath(getMappingTinyFile()),
                        getInputNamespace().get(),
                        getOutputNamespace().get()
                );
        var remapper = TinyRemapper.newRemapper()
                .withMappings(mappingProvider)
                .renameInvalidLocals(true)
                .rebuildSourceFilenames(true)
                .invalidLvNamePattern(Pattern.compile("\\$\\$\\d+"))
                .inferNameFromSameLvIndex(true)
                .build();
        try (var outputConsumer = new OutputConsumerPath.Builder(ZwirnPluginUtils.getPath(getOutputJar())).build()) {
            remapper.readInputsAsync(ZwirnPluginUtils.getPath(getInputJar()));
            remapper.apply(outputConsumer);
        } finally {
            remapper.finish();
        }
    }
}
