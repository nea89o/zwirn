package moe.nea.zwirn;

import net.fabricmc.stitch.commands.tinyv2.TinyClass;
import net.fabricmc.stitch.commands.tinyv2.TinyFile;
import net.fabricmc.stitch.commands.tinyv2.TinyHeader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Zwirn {
    public static @NotNull TinyFile mergeTinyFile(
            @NotNull TinyFile base, @NotNull TinyFile overlay,
            @NotNull String sharedNamespace) {
        var namespaces = base.getHeader().getNamespaces();
        if (!namespaces.containsAll(overlay.getHeader().getNamespaces()))
            throw new IllegalArgumentException("When merging a tiny file, overlay may not introduce a new namespace.");
        if (!overlay.getHeader().getNamespaces().contains(sharedNamespace))
            throw new IllegalArgumentException("When merging a tiny file, overlay must contain the shared namespace");
        return new TinyMerger(base, overlay, sharedNamespace).merge();
    }


    public record RenameCommand(
            String oldNamespaceName,
            String newNamespaceName
    ) {
    }

    public static @NotNull TinyFile renameNamespaces(
            @NotNull TinyFile tinyFile,
            @NotNull List<@NotNull RenameCommand> newNamespaceOrder
    ) {
        for (var renameCommand : newNamespaceOrder) {
            if (!tinyFile.getHeader().getNamespaces().contains(renameCommand.oldNamespaceName()))
                throw new IllegalArgumentException("Old namespace " + renameCommand.oldNamespaceName() + " not found");
        }
        return new RenameTask(tinyFile, newNamespaceOrder).rename();
    }

    public static @NotNull TinyFile enrichSeargeWithMCP(@NotNull TinyFile searge, @NotNull Path mcpArchiveRoot) throws IOException {
        if (!searge.getHeader().getNamespaces().equals(Arrays.asList("left", "right")))
            throw new IllegalArgumentException("Searge namespaces need to be left and right");
        var fields = mcpArchiveRoot.resolve("fields.csv");
        var methods = mcpArchiveRoot.resolve("methods.csv");
        var params = mcpArchiveRoot.resolve("params.csv");
        if (!Files.exists(fields))
            throw new IllegalArgumentException("Missing fields.csv");
        if (!Files.exists(methods))
            throw new IllegalArgumentException("Missing methods.csv");
        if (!Files.exists(params))
            throw new IllegalArgumentException("Missing params.csv");
        return new EnrichSeargeWithMCP(searge, fields, methods, params).mergeTinyFile();
    }

    public static @NotNull TinyFile createOverlayTinyFile(
            @NotNull TinyFile base, @NotNull TinyFile overlay,
            @NotNull List<@NotNull String> retainedNamespaces,
            @NotNull String sharedNamespace
    ) {
        if (!base.getHeader().getNamespaces().equals(overlay.getHeader().getNamespaces()))
            throw new IllegalArgumentException("Namespaces in input must be equal");
        if (!base.getHeader().getNamespaces().containsAll(retainedNamespaces))
            throw new IllegalArgumentException("Retained namespaces must be present in input files");
        if (!retainedNamespaces.contains(sharedNamespace))
            throw new IllegalArgumentException("Shared namespace must be retained");
        return new TinyDiffer(base, overlay, retainedNamespaces).createDiff();
    }

    public static @NotNull TinyFile fixFieldDescriptorsFromJar(@NotNull TinyFile tinyFile, @NotNull Path classRoot) {
        return new FieldSignatureFixer(tinyFile, classRoot).fix();
    }

}
