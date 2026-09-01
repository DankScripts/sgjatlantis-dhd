import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public final class CompileJavaSource {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException("outputDir classpath sourceFile...");
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("JDK compiler is unavailable");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null)) {
            List<File> sources = new ArrayList<>();
            for (int index = 2; index < args.length; index++) {
                sources.add(Path.of(args[index]).toFile());
            }

            List<String> options = List.of(
                    "--release", "17",
                    "-proc:none",
                    "-classpath", args[1],
                    "-d", args[0]);
            boolean success = compiler.getTask(null, files, diagnostics, options, null,
                    files.getJavaFileObjectsFromFiles(sources)).call();
            diagnostics.getDiagnostics().forEach(System.err::println);
            if (!success) {
                System.exit(1);
            }
        }
    }
}
