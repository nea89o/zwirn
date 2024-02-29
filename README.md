# Zwirn

> Merging, diffing and converting of tiny/yarn and other mapping formats

```
    mappings {
        searge()
        mapping(rootProject.files("yarn.zip"), "yarn") {
            outputs("yarn", true) { listOf("searge") }
            mapNamespace("named", "yarn")
            mapNamespace("intermediary", "searge")
            sourceNamespace("searge")
            renest()
        }
    }
```





