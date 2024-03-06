package moe.nea.zwirn;

import net.fabricmc.stitch.commands.tinyv2.TinyFile;

import java.util.HashMap;
import java.util.Map;

public class SimpleRemapper {
    private final Map<String, String> lookup = new HashMap<>();

    public SimpleRemapper(TinyFile file, String sourceNs, String targetNs) {
        int sourceIndex = file.getHeader().getNamespaces().indexOf(sourceNs);
        int targetIndex = file.getHeader().getNamespaces().indexOf(targetNs);
        for (var classEntry : file.getClassEntries()) {
            lookup.put(classEntry.getClassNames().get(sourceIndex).replace('/', '.'),
                    classEntry.getClassNames().get(targetIndex).replace('/', '.'));
        }
    }

    public String remapClass(String dottedName) {
        return lookup.getOrDefault(dottedName, dottedName);
    }

    public String remapFieldDescriptor(String field) {
        if ("null".equals(field)) return "null";
        return Descriptor.Field.readField(new GoodStringReader(field)).mapClassName(this).toJvmDescriptor();
    }

    public String remapMethodDescriptor(String method) {
        if ("null".equals(method)) return "null";
        return Descriptor.Method.readMethod(new GoodStringReader(method)).mapClassName(this).toJvmDescriptor();
    }
}
