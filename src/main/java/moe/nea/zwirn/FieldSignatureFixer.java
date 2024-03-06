package moe.nea.zwirn;

import net.fabricmc.stitch.commands.tinyv2.TinyClass;
import net.fabricmc.stitch.commands.tinyv2.TinyField;
import net.fabricmc.stitch.commands.tinyv2.TinyFile;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldSignatureFixer {
    private final TinyFile tinyFile;
    private final Path classRoot;

    public FieldSignatureFixer(@NotNull TinyFile tinyFile, @NotNull Path classRoot) {
        this.tinyFile = tinyFile;
        this.classRoot = classRoot;
    }

    public TinyFile fix() {
        return new TinyFile(tinyFile.getHeader(),
                tinyFile.getClassEntries().stream().map(this::fixClass).collect(Collectors.toList())
        );
    }

    private TinyClass fixClass(TinyClass tinyClass) {
        var path = tinyClass.getClassNames().get(0).replace('.', '/') + ".class";
        var classPath = classRoot.resolve(path);
        Map<String, String> fieldNameToDescriptorMap = new HashMap<>();
        if (Files.exists(classPath))
            try (var is = Files.newInputStream(classPath)) {
                var reader = new ClassReader(is);
                var visitor = new FieldSignatureVisitor();
                reader.accept(visitor, 0);
                fieldNameToDescriptorMap.putAll(visitor.fieldNameToDescriptorMap);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        return new TinyClass(
                tinyClass.getClassNames(),
                tinyClass.getMethods(),
                tinyClass.getFields().stream()
                        .map(it -> new TinyField(fieldNameToDescriptorMap.get(it.getFieldNames().get(0)), it.getFieldNames(), it.getComments()))
                        .collect(Collectors.toList()),
                tinyClass.getComments()
        );
    }

    static class FieldSignatureVisitor extends ClassVisitor {
        protected FieldSignatureVisitor() {
            super(Opcodes.ASM9);
        }

        public Map<String, String> fieldNameToDescriptorMap = new HashMap<>();

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            fieldNameToDescriptorMap.put(name, descriptor);
            return super.visitField(access, name, descriptor, signature, value);
        }
    }
}
