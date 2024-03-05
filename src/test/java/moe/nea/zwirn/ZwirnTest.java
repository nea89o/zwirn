package moe.nea.zwirn;

import net.fabricmc.stitch.commands.tinyv2.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

class ZwirnTest {

    @Test
    void diffTinyFile() throws Exception {
        var base = getBaseFile();
        var overlay = getOverlayFile();
        var merged = Zwirn.mergeTinyFile(base, overlay, "official");
        var unmerged = Zwirn.createOverlayTinyFile(base, merged, Arrays.asList("official", "named"), "official");
        TinyV2Writer.write(overlay, Path.of("overlay.tiny"));
        TinyV2Writer.write(unmerged, Path.of("unmerged.tiny"));
    }


    TinyFile getBaseFile() {
        return new TinyFile(
                new TinyHeader(
                        Arrays.asList("official", "intermediary", "named"),
                        2, 0, new HashMap<>()
                ),
                Arrays.asList(
                        new TinyClass(
                                Arrays.asList("A", "ClassA", "SomeClass"),
                                Arrays.asList(
                                        new TinyMethod(
                                                "a",
                                                Arrays.asList("a", "method_a", "doSomething"),
                                                Arrays.asList(
                                                        new TinyMethodParameter(
                                                                0,
                                                                Arrays.asList("a", "param_a_a", "somethingToOperateOn"),
                                                                Arrays.asList()
                                                        )
                                                ),
                                                Arrays.asList(),
                                                Arrays.asList("method comment")
                                        )
                                ),
                                Arrays.asList(new TinyField("a",
                                        Arrays.asList("a", "field_a", "myField"), Arrays.asList("Field comment"))),
                                Arrays.asList("some comment")
                        ),
                        new TinyClass(
                                Arrays.asList("C", "ClassC", "SomeOtherClass"),
                                Arrays.asList(),
                                Arrays.asList(),
                                Arrays.asList()
                        )
                )
        );
    }

    @Test
    void mergeTinyFile() throws IOException {
        var base = getBaseFile();
        var overlay = getOverlayFile();
        var merged = Zwirn.mergeTinyFile(base, overlay, "official");
        TinyV2Writer.write(merged, Path.of("out.tiny"));
    }

    TinyFile getOverlayFile() {
        return new TinyFile(
                new TinyHeader(
                        Arrays.asList("official", "named"),
                        2, 0, new HashMap<>()
                ),
                Arrays.asList(
                        new TinyClass(
                                Arrays.asList("A", "SomeClassButNamedBetter"),
                                Arrays.asList(),
                                Arrays.asList(new TinyField("a",
                                        Arrays.asList("a", "myFieldButNamedCool"), Arrays.asList("Better comment"))),
                                Arrays.asList()
                        ),
                        new TinyClass(
                                Arrays.asList("B", "OtherClass")
                        )
                )
        );
    }
}