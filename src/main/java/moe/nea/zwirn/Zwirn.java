package moe.nea.zwirn;

import net.fabricmc.stitch.commands.tinyv2.TinyFile;
import org.jetbrains.annotations.NotNull;

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
}
