package moe.nea.zwirn;

import net.fabricmc.stitch.commands.tinyv2.TinyClass;
import net.fabricmc.stitch.commands.tinyv2.TinyFile;
import net.fabricmc.stitch.commands.tinyv2.TinyMethod;
import net.fabricmc.stitch.commands.tinyv2.TinyMethodParameter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ConstructorEnrichmentTask {
    private final TinyFile searge;
    private final List<McpConstructor> mcpConstructors;
    private final Map<String, List<McpConstructor>> indexedConstructors;
    private final SimpleRemapper remapper;

    public ConstructorEnrichmentTask(@NotNull TinyFile searge, Path joinedExc) {
        this.searge = searge;
        this.mcpConstructors = readExc(joinedExc);
        this.indexedConstructors = this.mcpConstructors.stream().collect(Collectors.groupingBy(McpConstructor::owningClass, Collectors.toList()));
        this.remapper = new SimpleRemapper(searge, "right", "left");
    }

    public TinyFile enrich() {
        return new TinyFile(
                searge.getHeader(),
                searge.getClassEntries()
                        .stream().map(this::enrichClass).collect(Collectors.toList())
        );
    }

    private TinyClass enrichClass(TinyClass tinyClass) {
        List<TinyMethod> methods = new ArrayList<>(tinyClass.getMethods());
        List<McpConstructor> constructors = indexedConstructors.getOrDefault(tinyClass.getClassNames().get(1).replace('/', '.'), Collections.emptyList());
        for (McpConstructor constructor : constructors) {
            methods.add(new TinyMethod(
                    remapper.remapMethodDescriptor(constructor.methodDescriptorInSrg()),
                    Arrays.asList("<init>", "<init>"),
                    constructor.getTinyParameters(),
                    Arrays.asList(),
                    Arrays.asList()
            ));
        }
        return new TinyClass(
                tinyClass.getClassNames(),
                methods,
                tinyClass.getFields(),
                tinyClass.getComments()
        );
    }

    record McpConstructor(
            String owningClass,
            String methodDescriptorInSrg,
            List<String> parameterNames
    ) {
        public Collection<TinyMethodParameter> getTinyParameters() {
            List<TinyMethodParameter> params = new ArrayList<>();
            int i = 0;
            for (String parameterName : parameterNames) {
                int lvIndex = ++i;
                params.add(new TinyMethodParameter(lvIndex, Arrays.asList("p" + lvIndex, parameterName), Arrays.asList()));
            }
            return params;
        }
    }

    private static List<McpConstructor> readExc(Path joinedExc) {
        var prop = new Properties();
        try (var reader = Files.newBufferedReader(joinedExc)) {
            prop.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<McpConstructor> cons = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : prop.entrySet()) {
            var target = (String) entry.getKey();
            var signatureInfo = (String) entry.getValue();
            if (!signatureInfo.contains("|")) continue;
            var targetParts = target.split("\\.");
            assert targetParts.length == 2;
            var className = targetParts[0].replace('/', '.');
            var constructorDescriptor = "(" + targetParts[1].split("\\(")[1];
            if (!targetParts[1].startsWith("<init>"))
                continue;
            int pipeIndex = signatureInfo.indexOf('|');
            var exceptions = signatureInfo.substring(0, pipeIndex);
            var parameterNameString = signatureInfo.substring(pipeIndex + 1);
            var parameterNames = parameterNameString.isBlank()
                    ? Arrays.<String>asList()
                    : Arrays.asList(parameterNameString.split(","));
            cons.add(new McpConstructor(className, constructorDescriptor, parameterNames));
        }
        return cons;
    }
}
