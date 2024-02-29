package moe.nea.zwirn;

import net.fabricmc.stitch.commands.tinyv2.TinyFile;
import org.jetbrains.annotations.NotNull;

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
}
