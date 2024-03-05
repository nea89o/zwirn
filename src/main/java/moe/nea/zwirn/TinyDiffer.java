package moe.nea.zwirn;

import net.fabricmc.stitch.commands.tinyv2.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TinyDiffer {
    private final TinyFile base;
    private final TinyFile overlay;
    private final List<String> retainedNamespaces;
    int[] retainedToNormalLookup;
    protected final int baseSharedIndex;
    protected final int overlaySharedIndex;

    public TinyDiffer(@NotNull TinyFile base, @NotNull TinyFile overlay, @NotNull List<String> retainedNamespaces) {
        this.base = base;
        this.overlay = overlay;
        this.retainedNamespaces = retainedNamespaces;
        this.baseSharedIndex = base.getHeader().getNamespaces().indexOf(retainedNamespaces.get(0));// TODO: shared namespace argument
        this.overlaySharedIndex = overlay.getHeader().getNamespaces().indexOf(retainedNamespaces.get(0));

        retainedToNormalLookup = retainedNamespaces.stream()
                .mapToInt(it -> base.getHeader().getNamespaces().indexOf(it))
                .toArray();
    }


    public TinyFile createDiff() {
        return new TinyFile(new TinyHeader(retainedNamespaces,
                overlay.getHeader().getMajorVersion(),
                overlay.getHeader().getMinorVersion(),
                overlay.getHeader().getProperties()),
                diffClasses());
    }

    private <B, C extends Mapping> Retained<List<C>> diffChildrenByMapping(
            @Nullable B base,
            @Nullable B overlay,
            @NotNull Function<@NotNull B, ? extends @NotNull Collection<@NotNull C>> childExtractor,
            @NotNull BiFunction<@Nullable C, @Nullable C, @NotNull Retained<@NotNull C>> childDiffer
    ) {
        return this.diffChildren(base, overlay,
                it -> it.getMapping().get(baseSharedIndex),
                it -> it.getMapping().get(overlaySharedIndex),
                childExtractor,
                childDiffer
        );
    }

    private <B, C, K> Retained<List<C>> diffChildren(
            @Nullable B base,
            @Nullable B overlay,
            @NotNull Function<@NotNull C, @NotNull K> sharedBaseName,
            @NotNull Function<@NotNull C, @NotNull K> sharedOverlayName,
            @NotNull Function<@NotNull B, ? extends @NotNull Collection<@NotNull C>> childExtractor,
            @NotNull BiFunction<@Nullable C, @Nullable C, @NotNull Retained<@NotNull C>> childDiffer
    ) {
        Map<K, C> baseIndex =
                base == null ? Collections.emptyMap() : childExtractor.apply(base).stream()
                        .collect(Collectors.toMap(sharedBaseName, Function.identity()));
        Map<K, C> overlayIndex =
                overlay == null ? Collections.emptyMap() : childExtractor.apply(overlay).stream()
                        .collect(Collectors.toMap(sharedOverlayName, Function.identity()));
        var results = new ArrayList<@NotNull C>();
        if (base != null)
            for (var baseObject : childExtractor.apply(base)) {
                var sharedName = sharedBaseName.apply(baseObject);
                var overlayObject = overlayIndex.get(sharedName);
                var mergedClass = childDiffer.apply(baseObject, overlayObject);
                if (mergedClass.isMeaningfullyDifferent)
                    results.add(mergedClass.diff);
            }
        if (overlay != null)
            for (var overlayObject : childExtractor.apply(overlay)) {
                var sharedName = sharedOverlayName.apply(overlayObject);
                if (baseIndex.containsKey(sharedName))
                    continue;
                results.add(overlayObject);
            }
        if (results.isEmpty())
            return Retained.empty(results);
        return Retained.strong(results);
    }

    private Collection<TinyClass> diffClasses() {
        return diffChildrenByMapping(base, overlay,
                TinyFile::getClassEntries,
                this::diffClass).diff;
    }

    private List<String> retainNames(List<String> names) {
        List<String> newNames = new ArrayList<>(retainedToNormalLookup.length);
        for (int j : retainedToNormalLookup) {
            newNames.add(names.get(j));
        }
        return newNames;
    }


    private <T extends Mapping> @NotNull Retained<List<String>> diffNamesWithMappings(@Nullable T base, @Nullable T overlay) {
        return diffNames(mapNull(base, Mapping::getMapping), mapNull(overlay, Mapping::getMapping));
    }

    private @NotNull Retained<List<String>> diffNames(@Nullable List<String> base, @Nullable List<String> overlay) {
        if (overlay == null) return Retained.empty(retainNames(base));
        if (base == null)
            return Retained.strong(retainNames(overlay));
        var b = retainNames(base);
        var o = retainNames(overlay);
        if (o.equals(b)) {
            // TODO: return only the shared name and censor others
            return Retained.empty(o);
        }
        return Retained.strong(o);
    }

    static class Retained<T> {
        private final T diff;
        private final boolean isMeaningfullyDifferent;

        public Retained(T diff, boolean isMeaningfullyDifferent) {
            this.diff = diff;
            this.isMeaningfullyDifferent = isMeaningfullyDifferent;
        }

        public static <T> Retained<T> empty(T base) {
            return new Retained<>(base, false);
        }

        public static <T> Retained<T> strong(T base) {
            return new Retained<>(base, true);
        }

        public static <T> Retained<T> keep(T base, Retained<?>... dependencies) {
            return new Retained<>(base, shouldRetainAny(Arrays.asList(dependencies)));
        }

        public static boolean shouldRetainAny(Iterable<Retained<?>> list) {
            for (Retained<?> retained : list) {
                if (retained.isMeaningfullyDifferent)
                    return true;
            }
            return false;
        }
    }

    private <T, K> @Nullable K mapNull(@Nullable T t, @NotNull Function<@NotNull T, @NotNull K> mapper) {
        return t == null ? null : mapper.apply(t);
    }

    private Retained<TinyClass> diffClass(
            @Nullable TinyClass baseClass,
            @Nullable TinyClass overlayClass) {
        var names = diffNamesWithMappings(baseClass, overlayClass);
        var fields = diffChildrenByMapping(baseClass, overlayClass, TinyClass::getFields, this::diffField);
        var methods = diffChildrenByMapping(baseClass, overlayClass, TinyClass::getMethods, this::diffMethod);
        var comments = diffComments(baseClass, overlayClass, TinyClass::getComments);
        return Retained.keep(
                new TinyClass(
                        names.diff,
                        methods.diff,
                        fields.diff,
                        comments.diff
                ),
                names,
                methods,
                fields,
                comments
        );
    }

    private <T> Retained<List<String>> diffComments(T baseObject, T overlayObject, Function<T, ? extends Collection<String>> commentExtractor) {
        if (baseObject == null)
            return Retained.strong(new ArrayList<>(commentExtractor.apply(overlayObject)));
        if (overlayObject == null)
            return Retained.empty(new ArrayList<>(commentExtractor.apply(baseObject)));
        var comments = new ArrayList<>(commentExtractor.apply(overlayObject));
        comments.removeAll(commentExtractor.apply(baseObject));
        if (comments.isEmpty())
            return Retained.empty(comments);
        return Retained.strong(comments);
    }

    private Retained<TinyMethod> diffMethod(@Nullable TinyMethod baseMethod, @Nullable TinyMethod overlayMethod) {
        var names = diffNamesWithMappings(baseMethod, overlayMethod);
        var params = diffChildren(baseMethod, overlayMethod, TinyMethodParameter::getLvIndex, TinyMethodParameter::getLvIndex, TinyMethod::getParameters, this::diffParam);
        var variables = diffChildren(baseMethod, overlayMethod, TinyLocalVariable::getLvIndex, TinyLocalVariable::getLvIndex, TinyMethod::getLocalVariables, this::diffVariable);
        var comments = diffComments(baseMethod, overlayMethod, TinyMethod::getComments);
        return Retained.keep(
                new TinyMethod(
                        names.diff.get(0),
                        names.diff,
                        params.diff,
                        variables.diff,
                        comments.diff
                ),
                names, params, variables, comments
        );
    }

    private Retained<TinyLocalVariable> diffVariable(TinyLocalVariable baseVariable, TinyLocalVariable overlayVariable) {
        if (overlayVariable == null && baseVariable == null)
            throw new NullPointerException();
        if (overlayVariable != null && baseVariable != null) {
            if (overlayVariable.getLvIndex() != baseVariable.getLvIndex())
                throw new IllegalArgumentException("lvIndex must match on local variables with the same shared name");
            if (overlayVariable.getLvStartOffset() != baseVariable.getLvStartOffset())
                throw new IllegalArgumentException("lvStartOffset must match on local variables with the same shared name");
            if (overlayVariable.getLvTableIndex() != baseVariable.getLvTableIndex())
                throw new IllegalArgumentException("lvTableIndex must match on local variables with the same shared name");
        }
        var names = diffNamesWithMappings(baseVariable, overlayVariable);
        var comments = diffComments(baseVariable, overlayVariable, TinyLocalVariable::getComments);
        return Retained.keep(
                new TinyLocalVariable(
                        overlayVariable == null ? baseVariable.getLvIndex() : overlayVariable.getLvIndex(),
                        overlayVariable == null ? baseVariable.getLvStartOffset() : overlayVariable.getLvStartOffset(),
                        overlayVariable == null ? baseVariable.getLvTableIndex() : overlayVariable.getLvTableIndex(),
                        names.diff,
                        comments.diff
                ),
                names,
                comments
        );
    }

    private Retained<TinyMethodParameter> diffParam(TinyMethodParameter baseParam, TinyMethodParameter overlayParam) {
        if (baseParam == null && overlayParam == null)
            throw new NullPointerException();
        if (overlayParam != null && baseParam != null && overlayParam.getLvIndex() != baseParam.getLvIndex())
            throw new IllegalArgumentException("lvIndex must match on method parameters with the same shared name");

        var names = diffNamesWithMappings(baseParam, overlayParam);
        var comments = diffComments(baseParam, overlayParam, TinyMethodParameter::getComments);
        return Retained.keep(
                new TinyMethodParameter(
                        baseParam != null ? baseParam.getLvIndex() : overlayParam.getLvIndex(),
                        names.diff,
                        comments.diff
                ),
                names,
                comments
        );
    }

    private Retained<TinyField> diffField(TinyField baseField, TinyField overlayField) {
        var names = diffNamesWithMappings(baseField, overlayField);
        var comments = diffComments(baseField, overlayField, TinyField::getComments);
        return Retained.keep(
                new TinyField(
                        names.diff.get(0),
                        names.diff,
                        comments.diff
                ),
                names,
                comments
        );
    }
}
