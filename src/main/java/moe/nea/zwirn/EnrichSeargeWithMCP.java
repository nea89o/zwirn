package moe.nea.zwirn;

import de.siegmar.fastcsv.reader.CsvReader;
import net.fabricmc.stitch.commands.tinyv2.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EnrichSeargeWithMCP {
    private final TinyFile searge;
    private final List<MCPField> fields;
    private final List<MCPMethod> methods;
    private final List<MCPParam> params;
    private final Map<String, MCPField> fieldMap;
    private final Map<Integer, List<MCPParam>> paramMap;
    private final Map<String, MCPMethod> methodMap;
    private final Map<String, MCPParam> constructorParamMap;

    // TODO: parse joined.exc for constructor indexes parameters
    public EnrichSeargeWithMCP(@NotNull TinyFile searge, Path fields, Path methods, Path params) throws IOException {
        this.searge = searge;
        this.fields = readFields(fields);
        this.methods = readMethods(methods);
        this.params = readParams(params);
        this.fieldMap = this.fields.stream().collect(Collectors.toMap(MCPField::searge, Function.identity()));
        this.methodMap = this.methods.stream().collect(Collectors.toMap(MCPMethod::searge, Function.identity()));
        this.paramMap = this.params.stream().filter(it -> it.methodId() != null).collect(Collectors.groupingBy(MCPParam::methodId, Collectors.toList()));
        this.constructorParamMap = this.params.stream().collect(Collectors.toMap(MCPParam::searge, Function.identity()));
    }

    record MCPParam(
            String searge,
            String name
    ) {
        public Integer methodId() {
            var matcher = SEARGE_PARAM_ID_PATTERN.matcher(searge);
            if (!matcher.matches())
                return -1;
            return Integer.parseInt(matcher.group(1));
        }

        public Integer lvIndexHeuristic() {
            var matcher = SEARGE_PARAM_ID_PATTERN.matcher(searge);
            if (!matcher.matches())
                return -1;
            return Integer.parseInt(matcher.group(2));
        }

        static final Pattern SEARGE_PARAM_ID_PATTERN = Pattern.compile("^p_([0-9]+)_([0-9]+)_$");

    }

    record MCPField(String searge, String name, String desc) {
    }

    record MCPMethod(String searge, String name, String desc) {
        public int methodId() {
            var matcher = SEARGE_METHOD_ID_PATTERN.matcher(searge);
            if (!matcher.matches())
                throw new IllegalStateException("Searge name does not contain method id");
            return Integer.parseInt(matcher.group(1));
        }

        static final Pattern SEARGE_METHOD_ID_PATTERN = Pattern.compile("^func_([0-9]+)_.+$");
    }


    private static List<MCPParam> readParams(Path params) throws IOException {
        try (var csvReader = CsvReader.builder().ofNamedCsvRecord(params)
                .stream()) {
            // Header: param,name,side
            return csvReader.map(
                    it -> new MCPParam(it.getField("param"), it.getField("name"))
            ).collect(Collectors.toList());
        }
    }

    private static List<MCPField> readFields(Path fields) throws IOException {
        try (var csvReader = CsvReader.builder().ofNamedCsvRecord(fields)
                .stream()) {
            // Header: searge,name,side,desc
            return csvReader.map(
                    it -> new MCPField(it.getField("searge"), it.getField("name"), it.getField("desc").replace("\\n", "\n"))
            ).collect(Collectors.toList());
        }
    }

    private static List<MCPMethod> readMethods(Path methods) throws IOException {
        try (var csvReader = CsvReader.builder().ofNamedCsvRecord(methods)
                .stream()) {
            // Header: searge,name,side,desc
            return csvReader.map(
                    it -> new MCPMethod(it.getField("searge"), it.getField("name"), it.getField("desc").replace("\\n", "\n"))
            ).collect(Collectors.toList());
        }
    }

    public TinyFile mergeTinyFile() {
        return new TinyFile(
                new TinyHeader(
                        Arrays.asList("notch", "searge", "mcp"),
                        2, 0, new HashMap<>()
                ),
                searge.getClassEntries()
                        .stream().map(this::mergeClass)
                        .collect(Collectors.toList())
        );
    }

    private TinyClass mergeClass(TinyClass tinyClass) {
        return new TinyClass(
                Arrays.asList(
                        tinyClass.getClassNames().get(0), tinyClass.getClassNames().get(1),
                        tinyClass.getClassNames().get(1) // MCP does not handle class names. those are done in searge
                ),
                tinyClass.getMethods().stream().map(this::mergeMethod).collect(Collectors.toList()),
                tinyClass.getFields().stream().map(this::mergeField).collect(Collectors.toList()),
                Arrays.asList() // Searge doesn't have comments
        );
    }

    private TinyField mergeField(TinyField tinyField) {
        var srg = tinyField.getFieldNames().get(1);
        var mcpField = fieldMap.get(srg);
        return new TinyField(
                tinyField.getFieldDescriptorInFirstNamespace(),
                Arrays.asList(tinyField.getFieldNames().get(0), srg, mcpField == null ? srg : mcpField.name()),
                mcpField == null ? Arrays.asList() : Arrays.asList(mcpField.desc)// TODO: handle empty comment
        );
    }

    private TinyMethod mergeMethod(TinyMethod tinyMethod) {
        var srg = tinyMethod.getMethodNames().get(1);
        var mcpMethod = methodMap.get(srg);
        Map<Integer, TinyMethodParameter> params = new HashMap<>();
        if (mcpMethod != null) {
            var mcpParams = paramMap.get(mcpMethod.methodId());
            if (mcpParams != null) for (var param : mcpParams) {
                params.put(param.lvIndexHeuristic(), new TinyMethodParameter(
                        param.lvIndexHeuristic(),
                        Arrays.asList("p" + param.lvIndexHeuristic(), param.searge(), param.name()),
                        Arrays.asList()
                ));
            }
        }
        for (TinyMethodParameter parameter : tinyMethod.getParameters()) {
            MCPParam mcpParam = constructorParamMap.get(parameter.getParameterNames().get(1));
            if (mcpParam != null)
                params.put(parameter.getLvIndex(), new TinyMethodParameter(
                        parameter.getLvIndex(),
                        Arrays.asList(parameter.getParameterNames().get(0), parameter.getParameterNames().get(1), mcpParam.name()),
                        parameter.getComments()
                ));
        }
        return new TinyMethod(
                tinyMethod.getMethodDescriptorInFirstNamespace(),
                Arrays.asList(tinyMethod.getMethodNames().get(0), srg, mcpMethod == null ? srg : mcpMethod.name()),
                params.values(),
                Arrays.asList(),
                mcpMethod == null ? Arrays.asList() : Arrays.asList(mcpMethod.desc) // TODO: handle empty comment
        );
    }
}
