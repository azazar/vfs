# VFS

VFS that allows accessing remote or enclosed files without need for replicating
containers. It's original purpose was to allow recursive scanning of archives.
But now it's a bit more than that.

## Supported containers and compressors

Format|Extensions|Supported by
-|-|-
Zip|.zip|Both
Tar|.tar/.tgz|VFS
Rar|.rar|Scanner
GZip|.gz|VFS
BZip2|.bz2|VFS
ZStd|.zst|VFS

## Example

```java
public class Example {

    public static void readFromArchive() throws IOException {
        // reading file named "enclosed.txt" from zip archive "archive.zip"
        try (InputStream i = new VfsFile(new File("archive.zip"), "enclosed.txt").open()) {
            System.out.println(IOUtils.toString(i, StandardCharsets.UTF_8));
        }
    }
    
    public static void scan() throws IOException {
        // print content of all files in zip archive "archive"
        new VfsFileScanner(file -> {
            try {
                System.out.println(file.getContentAsUTF8String());
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }).scan(new File("archive.zip"));
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

### Artifact

```xml
<dependency>
    <groupId>com.github.azazar</groupId>
    <artifactId>vfs</artifactId>
    <version>1.1.4</version>
</dependency>
```
