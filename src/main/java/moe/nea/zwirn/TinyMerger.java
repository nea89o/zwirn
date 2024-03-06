package moe.nea.zwirn;

import net.fabricmc.stitch.commands.tinyv2.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

class TinyMerger {
    private final @NotNull TinyFile base;
    private final @NotNull TinyFile overlay;
    private final @NotNull String sharedNamespace;
    private final int baseSharedIndex;
    private final int overlaySharedIndex;
    private final @NotNull Map<@NotNull String, @NotNull TinyClass> overlayLUT;
    private final @NotNull Map<@NotNull String, @NotNull TinyClass> baseLUT;
    private final Integer[] baseToOverlayIndexMap;
    private final @NotNull List<@NotNull TinyClass> entries = new ArrayList<>();
    private final SimpleRemapper remapperBase;
    private final SimpleRemapper remapperOverlay;

    public TinyMerger(TinyFile base, TinyFile overlay, String sharedNamespace) {
        this.base = base;
        this.overlay = overlay;
        this.sharedNamespace = sharedNamespace;
        this.baseSharedIndex = base.getHeader().getNamespaces().indexOf(sharedNamespace);
        this.overlaySharedIndex = overlay.getHeader().getNamespaces().indexOf(sharedNamespace);
        this.overlayLUT = overlay.getClassEntries().stream()
                .collect(Collectors.toMap(it -> it.getClassNames().get(this.overlaySharedIndex), Function.identity()));
        this.baseLUT = base.getClassEntries().stream()
                .collect(Collectors.toMap(it -> it.getClassNames().get(this.baseSharedIndex), Function.identity()));
        this.baseToOverlayIndexMap = base.getHeader().getNamespaces()
                .stream().map(it -> overlay.getHeader().getNamespaces().indexOf(it))
                .map(it -> it < 0 ? null : it)
                .toArray(Integer[]::new);
        var classMerged = classOnlyMerge();
        remapperBase = new SimpleRemapper(classMerged, "__old_base", base.getHeader().getNamespaces().get(0));
        remapperOverlay = new SimpleRemapper(classMerged, "__old_shared", base.getHeader().getNamespaces().get(0));
    }

    public TinyFile classOnlyMerge() {
        List<String> namespaces = new ArrayList<>();
        namespaces.add("__old_base");
        namespaces.add("__old_shared");
        namespaces.addAll(base.getHeader().getNamespaces());
        return new TinyFile(
                new TinyHeader(namespaces, 2, 0, new HashMap<>()),
                mergeChildren(
                        base,
                        overlay,
                        it -> it.getClassNames().get(baseSharedIndex),
                        it -> it.getClassNames().get(overlaySharedIndex),
                        TinyFile::getClassEntries,
                        (tinyClass, tinyClass2) -> {
                            List<String> mergedNames = new ArrayList<>();
                            if (tinyClass != null) {
                                mergedNames.add(tinyClass.getClassNames().get(baseSharedIndex));
                            } else {
                                mergedNames.add(tinyClass2.getClassNames().get(overlaySharedIndex));
                            }
                            if (tinyClass2 != null) {
                                mergedNames.add(tinyClass2.getClassNames().get(overlaySharedIndex));
                            } else {
                                mergedNames.add(tinyClass.getClassNames().get(baseSharedIndex));
                            }
                            mergedNames.addAll(mergeNames(tinyClass, tinyClass2, TinyClass::getClassNames));
                            return new TinyClass(mergedNames);
                        }
                )
        );
    }

    public @NotNull TinyFile merge() {
        mergeClasses();
        var baseHeader = base.getHeader();
        return new TinyFile(
                new TinyHeader(baseHeader.getNamespaces(), baseHeader.getMajorVersion(), baseHeader.getMinorVersion(), baseHeader.getProperties()),
                entries
        );
    }


    private @NotNull List<@NotNull String> mergeNames(
            @NotNull List<@NotNull String> baseNames,
            @NotNull List<@NotNull String> overlayNames) {
        List<String> mergedNames = new ArrayList<>(baseNames);
        String sharedName = baseNames.get(baseSharedIndex);
        for (int i = 0; i < mergedNames.size(); i++) {
            Integer overlayIndex = this.baseToOverlayIndexMap[i];
            if (overlayIndex == null) continue;
            String overlayName = overlayNames.get(overlayIndex);
            if (overlayName.isEmpty() || sharedName.equals(overlayName))
                continue;
            mergedNames.set(i, overlayName);
        }
        return mergedNames;
    }

    private <T> List<String> mergeNames(@Nullable T baseObject, @Nullable T overlayObject, @NotNull Function<@NotNull T, @NotNull List<@NotNull String>> nameExtractor) {
        if (baseObject == null && overlayObject == null)
            throw new IllegalArgumentException("Either baseObject or overlayObject must not be null");
        if (overlayObject == null)
            return nameExtractor.apply(baseObject);
        if (baseObject == null) {
            if (base.getHeader().getNamespaces().size() != overlay.getHeader().getNamespaces().size())
                throw new IllegalStateException("Can only merge unnamed base objects if both files have the same namespaces");
            List<String> names = new ArrayList<>();
            for (int i = 0; i < base.getHeader().getNamespaces().size(); i++) {
                names.add("");
            }
            List<@NotNull String> mergedNames = mergeNames(names, nameExtractor.apply(overlayObject));
            for (var name : mergedNames) {
                if (name.isEmpty())
                    throw new RuntimeException("Invariant violated: unnamed overlay name");
            }
            return mergedNames;
        }
        return mergeNames(nameExtractor.apply(baseObject), nameExtractor.apply(overlayObject));
    }

    private <T> @NotNull List<@NotNull String> mergeComments(@Nullable T baseObject, @Nullable T overlayObject, @NotNull Function<@NotNull T, ? extends @NotNull Collection<@NotNull String>> commentExtractor) {
        List<String> comments = new ArrayList<>();
        if (baseObject != null)
            comments.addAll(commentExtractor.apply(baseObject));
        if (overlayObject != null)
            comments.addAll(commentExtractor.apply(overlayObject));
        return comments;
    }

    private TinyClass mergeClass(@Nullable TinyClass baseClass, @Nullable TinyClass overlayClass) {
        return new TinyClass(
                mergeNames(baseClass, overlayClass, TinyClass::getClassNames),
                mergeMethods(baseClass, overlayClass),
                mergeFields(baseClass, overlayClass),
                mergeComments(baseClass, overlayClass, TinyClass::getComments)
        );
    }

    private Collection<TinyField> mergeFields(@Nullable TinyClass baseClass, @Nullable TinyClass overlayClass) {
        return mergeChildren(baseClass, overlayClass,
                it -> it.getFieldNames().get(baseSharedIndex),
                it -> it.getFieldNames().get(overlaySharedIndex),
                TinyClass::getFields,
                this::mergeField);
    }

    private TinyField mergeField(@Nullable TinyField baseField, @Nullable TinyField overlayField) {
        var mergedNames = mergeNames(baseField, overlayField, TinyField::getFieldNames);
        return new TinyField(
                baseField != null ? remapperBase.remapFieldDescriptor(baseField.getFieldDescriptorInFirstNamespace())
                        : remapperOverlay.remapFieldDescriptor(overlayField.getFieldDescriptorInFirstNamespace()),
                mergedNames,
                mergeComments(baseField, overlayField, TinyField::getComments)
        );
    }

    private Collection<TinyMethod> mergeMethods(@Nullable TinyClass baseClass, @Nullable TinyClass overlayClass) {
        return mergeChildren(baseClass, overlayClass,
                it -> it.getMethodNames().get(baseSharedIndex) + remapperBase.remapMethodDescriptor(it.getMethodDescriptorInFirstNamespace()),
                it -> it.getMethodNames().get(overlaySharedIndex) + remapperOverlay.remapMethodDescriptor(it.getMethodDescriptorInFirstNamespace()),
                TinyClass::getMethods,
                this::mergeMethod);
    }

    private @NotNull TinyMethod mergeMethod(@Nullable TinyMethod baseMethod, @Nullable TinyMethod overlayMethod) {
        var mergedNames = mergeNames(baseMethod, overlayMethod, TinyMethod::getMethodNames);
        return new TinyMethod(
                (baseMethod != null ? remapperBase.remapMethodDescriptor(baseMethod.getMethodDescriptorInFirstNamespace())
                        : remapperOverlay.remapMethodDescriptor(overlayMethod.getMethodDescriptorInFirstNamespace())),
                mergedNames,
                mergeChildren(baseMethod, overlayMethod,
                        TinyMethodParameter::getLvIndex,
                        TinyMethodParameter::getLvIndex,
                        TinyMethod::getParameters,
                        this::mergeMethodParameter),
                mergeChildren(baseMethod, overlayMethod,
                        TinyLocalVariable::getLvIndex,
                        TinyLocalVariable::getLvIndex,
                        TinyMethod::getLocalVariables,
                        this::mergeLocalVariable),
                mergeComments(baseMethod, overlayMethod, TinyMethod::getComments)
        );
    }

    private TinyLocalVariable mergeLocalVariable(
            @Nullable TinyLocalVariable baseVariable,
            @Nullable TinyLocalVariable overlayVariable) {
        if (baseVariable == null && overlayVariable == null)
            throw new NullPointerException();
        if (overlayVariable != null && baseVariable != null) {
            if (overlayVariable.getLvIndex() != baseVariable.getLvIndex())
                throw new IllegalArgumentException("lvIndex must match on local variables with the same shared name");
            if (overlayVariable.getLvStartOffset() != baseVariable.getLvStartOffset())
                throw new IllegalArgumentException("lvStartOffset must match on local variables with the same shared name");
            if (overlayVariable.getLvTableIndex() != baseVariable.getLvTableIndex())
                throw new IllegalArgumentException("lvTableIndex must match on local variables with the same shared name");
        }
        return new TinyLocalVariable(
                baseVariable == null ? overlayVariable.getLvIndex() : baseVariable.getLvIndex(),
                baseVariable == null ? overlayVariable.getLvStartOffset() : baseVariable.getLvStartOffset(),
                baseVariable == null ? overlayVariable.getLvTableIndex() : baseVariable.getLvTableIndex(),
                mergeNames(baseVariable, overlayVariable, TinyLocalVariable::getLocalVariableNames),
                mergeComments(baseVariable, overlayVariable, TinyLocalVariable::getComments)
        );
    }

    private TinyMethodParameter mergeMethodParameter(
            @Nullable TinyMethodParameter baseParam,
            @Nullable TinyMethodParameter overlayParam) {
        if (baseParam == null && overlayParam == null)
            throw new NullPointerException();
        if (overlayParam != null && baseParam != null && overlayParam.getLvIndex() != baseParam.getLvIndex())
            throw new IllegalArgumentException("lvIndex must match on method parameters with the same shared name");
        return new TinyMethodParameter(
                baseParam != null ? baseParam.getLvIndex() : overlayParam.getLvIndex(),
                mergeNames(baseParam, overlayParam, TinyMethodParameter::getParameterNames),
                mergeComments(baseParam, overlayParam, TinyMethodParameter::getComments)
        );
    }

    private <B, C, K> List<C> mergeChildren(
            @Nullable B base,
            @Nullable B overlay,
            @NotNull Function<@NotNull C, @NotNull K> sharedBaseName,
            @NotNull Function<@NotNull C, @NotNull K> sharedOverlayName,
            @NotNull Function<@NotNull B, ? extends @NotNull Collection<@NotNull C>> childExtractor,
            @NotNull BiFunction<@Nullable C, @Nullable C, @NotNull C> childMerger
    ) {
        Map<K, C> baseIndex =
                base == null ? Collections.emptyMap() : childExtractor.apply(base).stream()
                        .collect(Collectors.toMap(sharedBaseName, Function.identity()));
        Map<K, C> overlayIndex =
                overlay == null ? Collections.emptyMap() : childExtractor.apply(overlay).stream()
                        .collect(Collectors.toMap(sharedOverlayName, Function.identity()));
        var results = new ArrayList<C>();
        if (base != null)
            for (var baseObject : childExtractor.apply(base)) {
                var sharedName = sharedBaseName.apply(baseObject);
                var overlayObject = overlayIndex.get(sharedName);
                var mergedClass = childMerger.apply(baseObject, overlayObject);
                results.add(mergedClass);
            }
        if (overlay != null)
            for (var overlayObject : childExtractor.apply(overlay)) {
                var sharedName = sharedOverlayName.apply(overlayObject);
                if (baseIndex.containsKey(sharedName))
                    continue;
                results.add(overlayObject);
            }
        return results;
    }

    private void mergeClasses() {
        entries.addAll(mergeChildren(
                base,
                overlay,
                it -> it.getClassNames().get(baseSharedIndex),
                it -> it.getClassNames().get(overlaySharedIndex),
                TinyFile::getClassEntries,
                this::mergeClass
        ));
    }
}
