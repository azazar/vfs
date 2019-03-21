## Example

```java
public class InputStreamWithCloseHookTest {

    public static void readFromArchive() throws IOException {
        // reading file named "1" from zip archive "/tmp/1.zip"
        StructuredFile f = new StructuredFile(new File("/tmp/1.zip"), "1");
        
        try (InputStream i = f.open()) {
            System.out.println(IOUtils.toString(i, StandardCharsets.UTF_8));
        }
    }
    
    public static void scan() throws IOException {
        // print content of all files in zip archive "/tmp/textfiles.zip"
        StructuredFileScanner scanner = new StructuredFileScanner(file -> {
            try {
                System.out.println(file.getContentAsUTF8String());
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        scanner.scan(new File("/tmp/textfiles.zip"));
    }
    
}
```

## Maven

### Repository

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### Artefact

```xml
<dependency>
    <groupId>com.github.azazar</groupId>
    <artifactId>vfs</artifactId>
    <version>1.1.1</version>
</dependency>
```
