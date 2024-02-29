package moe.nea.zwirn;

import net.fabricmc.stitch.commands.tinyv2.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

class ZwirnTest {

    @Test
    void mergeTinyFile() throws IOException {
        var base = new TinyFile(
                new TinyHeader(
                        Arrays.asList("official", "intermediary", "named"),
                        2, 0, new HashMap<>()
                ),
                Arrays.asList(
                        new TinyClass(
                                Arrays.asList("A", "ClassA", "SomeClass"),
                                Arrays.asList(),
                                Arrays.asList(new TinyField("a",
                                        Arrays.asList("a", "field_a", "myField"), Arrays.asList("Field comment"))),
                                Arrays.asList("some comment")
                        )
                )
        );
        var overlay = new TinyFile(
                new TinyHeader(
                        Arrays.asList("official", "intermediary", "named"),
                        2, 0, new HashMap<>()
                ),
                Arrays.asList(
                        new TinyClass(
                                Arrays.asList("A", "A", "SomeClassButNamedBetter"),
                                Arrays.asList(),
                                Arrays.asList(new TinyField("a",
                                        Arrays.asList("a", "a", "myFieldButNamedCool"), Arrays.asList("Better comment"))),
                                Arrays.asList()
                        ),
                        new TinyClass(
                                Arrays.asList("B", "B", "OtherClass")
                        )
                )
        );
        var merged = Zwirn.mergeTinyFile(base, overlay, "official");
        TinyV2Writer.write(merged, Path.of("out.tiny"));
    }
}