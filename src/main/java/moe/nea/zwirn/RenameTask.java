package moe.nea.zwirn;

import net.fabricmc.stitch.commands.tinyv2.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RenameTask {
    private final TinyFile tinyFile;
    private final List<Zwirn.RenameCommand> newNamespaceOrder;
    private final int[] namespaceRemapOrder;

    public RenameTask(@NotNull TinyFile tinyFile, @NotNull List<Zwirn.RenameCommand> newNamespaceOrder) {
        this.tinyFile = tinyFile;
        this.newNamespaceOrder = newNamespaceOrder;
        namespaceRemapOrder = newNamespaceOrder.stream().mapToInt(
                it -> tinyFile.getHeader().getNamespaces().indexOf(it.oldNamespaceName())
        ).toArray();
    }


    private List<String> rename(List<String> strings) {
        List<String> newNames = new ArrayList<>(namespaceRemapOrder.length);
        for (int j : namespaceRemapOrder) {
            newNames.add(strings.get(j));
        }
        return newNames;
    }

    public TinyFile rename() {
        return new TinyFile(
                new TinyHeader(newNamespaceOrder.stream().map(it -> it.newNamespaceName()).collect(Collectors.toList()), 2, 0, tinyFile.getHeader().getProperties()),
                tinyFile.getClassEntries().stream().map(this::renameClass).collect(Collectors.toList())
        );
    }

    private TinyClass renameClass(TinyClass tinyClass) {
        return new TinyClass(
                rename(tinyClass.getClassNames()),
                tinyClass.getMethods().stream().map(this::renameMethod).collect(Collectors.toList()),
                tinyClass.getFields().stream().map(this::renameField).collect(Collectors.toList()),
                tinyClass.getComments()
        );
    }

    private TinyField renameField(TinyField tinyField) {
        var names = rename(tinyField.getFieldNames());
        return new TinyField(
                names.get(0),
                names,
                tinyField.getComments()
        );
    }

    private TinyMethod renameMethod(TinyMethod tinyMethod) {
        var names = rename(tinyMethod.getMethodNames());
        return new TinyMethod(
                names.get(0),
                names,
                tinyMethod.getParameters().stream().map(this::renameMethodParameter).collect(Collectors.toList()),
                tinyMethod.getLocalVariables().stream().map(this::renameVariable).collect(Collectors.toList()),
                tinyMethod.getComments()
        );
    }

    private TinyMethodParameter renameMethodParameter(TinyMethodParameter tinyMethodParameter) {
        return new TinyMethodParameter(
                tinyMethodParameter.getLvIndex(),
                rename(tinyMethodParameter.getParameterNames()),
                tinyMethodParameter.getComments()
        );
    }

    private TinyLocalVariable renameVariable(TinyLocalVariable tinyLocalVariable) {
        return new TinyLocalVariable(
                tinyLocalVariable.getLvIndex(), tinyLocalVariable.getLvStartOffset(),
                tinyLocalVariable.getLvTableIndex(),
                rename(tinyLocalVariable.getLocalVariableNames()),
                tinyLocalVariable.getComments()
        );
    }
}
