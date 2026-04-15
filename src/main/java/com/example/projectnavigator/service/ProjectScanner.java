package com.example.projectnavigator.service;

import com.example.projectnavigator.config.AppProperties;
import com.example.projectnavigator.dto.ConfigSignal;
import com.example.projectnavigator.dto.EntryPointInfo;
import com.example.projectnavigator.dto.FileSummary;
import com.example.projectnavigator.dto.ModuleCard;
import com.example.projectnavigator.dto.ProjectIndex;
import com.example.projectnavigator.dto.RouteInfo;
import com.example.projectnavigator.dto.ScheduledJobInfo;
import com.example.projectnavigator.dto.SymbolInfo;
import com.example.projectnavigator.util.PathRules;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ProjectScanner {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^package\\s+([\\w.]+);", Pattern.MULTILINE);
    private static final Pattern CLASS_PATTERN = Pattern.compile("(class|interface|record)\\s+(\\w+)");
    private static final Pattern REQUEST_MAPPING_PATTERN = Pattern.compile("@RequestMapping\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern METHOD_MAPPING_PATTERN = Pattern.compile("@(Get|Post|Put|Delete|Patch)Mapping\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern SCHEDULE_PATTERN = Pattern.compile("@Scheduled\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern QUOTED_PATTERN = Pattern.compile("\"([^\"]+)\"");
    private static final int MAX_EXCLUSION_PREVIEW = 60;

    private final AppProperties properties;
    private final PathRules pathRules;

    public ProjectScanner(AppProperties properties, PathRules pathRules) {
        this.properties = properties;
        this.pathRules = pathRules;
    }

    public ProjectIndex scan(String projectId, Path root) throws IOException {
        Map<String, ModuleAccumulator> modules = new LinkedHashMap<>();
        List<EntryPointInfo> entryPoints = new ArrayList<>();
        List<RouteInfo> routes = new ArrayList<>();
        List<ScheduledJobInfo> jobs = new ArrayList<>();
        List<ConfigSignal> configSignals = new ArrayList<>();
        List<SymbolInfo> symbols = new ArrayList<>();
        List<FileSummary> summaries = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> stackSignals = new LinkedHashSet<>();
        ExclusionTracker exclusionTracker = new ExclusionTracker();

        IndexedFileVisitor visitor = new IndexedFileVisitor(
                root,
                modules,
                entryPoints,
                routes,
                jobs,
                configSignals,
                symbols,
                summaries,
                warnings,
                stackSignals,
                exclusionTracker);
        Files.walkFileTree(root, visitor);

        if (visitor.indexedFileLimitReached()) {
            warnings.add("Indexed file limit reached; some files were skipped.");
        }

        String stack = detectStack(root, stackSignals);
        List<ModuleCard> moduleCards = modules.values().stream()
                .map(ModuleAccumulator::toModuleCard)
                .sorted(Comparator.comparing(ModuleCard::name))
                .toList();

        return new ProjectIndex(
                projectId,
                stack,
                moduleCards,
                entryPoints,
                routes,
                jobs,
                configSignals,
                symbols,
                summaries,
                Instant.now(),
                1,
                exclusionTracker.snapshot(),
                warnings);
    }

    private void detectConfigs(String path, String content, List<ConfigSignal> configSignals, Set<String> stackSignals) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith("pom.xml")) {
            stackSignals.add("java");
            if (content.contains("spring-boot")) {
                stackSignals.add("spring");
            }
            configSignals.add(new ConfigSignal("build", path, "Maven build descriptor"));
        }
        if (lower.endsWith("build.gradle") || lower.endsWith("build.gradle.kts")) {
            stackSignals.add("java");
            configSignals.add(new ConfigSignal("build", path, "Gradle build descriptor"));
        }
        if (lower.endsWith("application.properties") || lower.endsWith("application.yml") || lower.endsWith("application.yaml")) {
            stackSignals.add("spring");
            configSignals.add(new ConfigSignal("application-config", path, firstMeaningfulLine(content)));
        }
        if (lower.endsWith("package.json")) {
            stackSignals.add("node");
            configSignals.add(new ConfigSignal("frontend-manifest", path, firstMeaningfulLine(content)));
        }
        if (lower.endsWith("pyproject.toml") || lower.endsWith("requirements.txt")) {
            stackSignals.add("python");
            configSignals.add(new ConfigSignal("python-manifest", path, firstMeaningfulLine(content)));
        }
        if (lower.endsWith("dockerfile")) {
            configSignals.add(new ConfigSignal("container", path, "Docker build instructions"));
        }
    }

    private void detectSummary(String path, String extension, String content, List<FileSummary> summaries) {
        List<String> tags = new ArrayList<>();
        if ("java".equals(extension)) {
            tags.add("java");
        } else if ("xml".equals(extension)) {
            tags.add("xml");
        } else if ("yml".equals(extension) || "yaml".equals(extension) || "properties".equals(extension)) {
            tags.add("config");
        } else if ("js".equals(extension) || "ts".equals(extension) || "tsx".equals(extension) || "jsx".equals(extension)) {
            tags.add("frontend");
        }

        summaries.add(new FileSummary(path, extension.isBlank() ? "text" : extension, summarizeContent(content), tags));
    }

    private void detectJavaSignals(
            String path,
            String content,
            Map<String, ModuleAccumulator> modules,
            List<EntryPointInfo> entryPoints,
            List<RouteInfo> routes,
            List<ScheduledJobInfo> jobs,
            List<SymbolInfo> symbols,
            Set<String> stackSignals) {
        if (!path.endsWith(".java")) {
            return;
        }

        stackSignals.add("java");
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(content);
        String packageName = packageMatcher.find() ? packageMatcher.group(1) : inferPackageFromPath(path);
        String moduleKey = moduleKey(packageName, path);
        ModuleAccumulator module = modules.computeIfAbsent(moduleKey, ignored -> new ModuleAccumulator(moduleKey));
        module.paths.add(path);
        module.technologies.add("Java");

        boolean restController = content.contains("@RestController");
        boolean controller = restController || content.contains("@Controller");
        boolean service = content.contains("@Service");
        boolean repository = content.contains("@Repository");
        boolean scheduled = content.contains("@Scheduled");
        boolean entryPoint = content.contains("@SpringBootApplication") || content.contains("public static void main");

        if (restController || controller) {
            module.kind = "controller";
            module.descriptionHints.add("Exposes HTTP endpoints");
            stackSignals.add("spring");
        } else if (service) {
            module.kind = "service";
            module.descriptionHints.add("Implements business logic");
            stackSignals.add("spring");
        } else if (repository) {
            module.kind = "repository";
            module.descriptionHints.add("Handles data access");
            stackSignals.add("spring");
        } else if (scheduled) {
            module.kind = "job";
            module.descriptionHints.add("Contains scheduled execution");
            stackSignals.add("spring");
        }

        Matcher classMatcher = CLASS_PATTERN.matcher(content);
        String lastClassName = null;
        while (classMatcher.find()) {
            lastClassName = classMatcher.group(2);
            symbols.add(new SymbolInfo(lastClassName, classMatcher.group(1), path, packageName));
        }

        if (entryPoint && lastClassName != null) {
            module.descriptionHints.add("Owns an application entry point");
            module.technologies.add("Spring Boot");
            entryPoints.add(new EntryPointInfo(lastClassName, path, "application", "Spring Boot bootstrap class"));
        }

        if (controller && lastClassName != null) {
            String prefix = extractMappingPrefix(content);
            Matcher methodMatcher = METHOD_MAPPING_PATTERN.matcher(content);
            while (methodMatcher.find()) {
                String verb = methodMatcher.group(1).toUpperCase(Locale.ROOT);
                String route = joinRoute(prefix, extractQuoted(methodMatcher.group(2)));
                routes.add(new RouteInfo(verb, route, lastClassName, path, "Detected from Spring mapping annotation"));
            }
        }

        if (scheduled && lastClassName != null) {
            Matcher scheduleMatcher = SCHEDULE_PATTERN.matcher(content);
            while (scheduleMatcher.find()) {
                String cron = extractQuoted(scheduleMatcher.group(1));
                jobs.add(new ScheduledJobInfo(lastClassName, cron == null ? "n/a" : cron, path, "Detected @Scheduled task"));
            }
        }
    }

    private String detectStack(Path root, Set<String> signals) {
        if (signals.contains("java") && signals.contains("spring")) {
            return "Java / Spring Boot";
        }
        if (signals.contains("java")) {
            return "Java";
        }
        if (signals.contains("node")) {
            return "Node.js";
        }
        if (signals.contains("python")) {
            return "Python";
        }
        if (Files.exists(root.resolve("pom.xml")) || Files.exists(root.resolve("build.gradle"))) {
            return "Java";
        }
        return "General";
    }

    private String summarizeContent(String content) {
        String line = firstMeaningfulLine(content);
        if (line.isBlank()) {
            return "No concise summary extracted";
        }
        return line.length() > 120 ? line.substring(0, 120) + "..." : line;
    }

    private String firstMeaningfulLine(String content) {
        return content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank() && !line.startsWith("//") && !line.startsWith("*") && !line.startsWith("/*"))
                .findFirst()
                .orElse("");
    }

    private String extensionOf(String path) {
        int index = path.lastIndexOf('.');
        return index >= 0 ? path.substring(index + 1).toLowerCase(Locale.ROOT) : "";
    }

    private String moduleKey(String packageName, String path) {
        if (packageName != null && packageName.split("\\.").length >= 3) {
            String[] parts = packageName.split("\\.");
            return String.join(".", parts[0], parts[1], parts[2]);
        }
        String[] parts = path.split("/");
        if (parts.length >= 4) {
            return String.join("/", parts[0], parts[1], parts[2]);
        }
        return packageName == null ? "root" : packageName;
    }

    private String inferPackageFromPath(String path) {
        String cleaned = path.replace('/', '.');
        if (cleaned.endsWith(".java")) {
            cleaned = cleaned.substring(0, cleaned.length() - 5);
        }
        int index = cleaned.indexOf("java.");
        return index >= 0 ? cleaned.substring(index + 5) : cleaned;
    }

    private String extractMappingPrefix(String content) {
        Matcher matcher = REQUEST_MAPPING_PATTERN.matcher(content);
        return matcher.find() ? extractQuoted(matcher.group(1)) : null;
    }

    private String extractQuoted(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = QUOTED_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String joinRoute(String prefix, String route) {
        String left = prefix == null ? "" : prefix;
        String right = route == null ? "" : route;
        String joined = ("/" + left + "/" + right).replaceAll("/+", "/");
        return joined.length() > 1 && joined.endsWith("/") ? joined.substring(0, joined.length() - 1) : joined;
    }

    private final class IndexedFileVisitor extends SimpleFileVisitor<Path> {

        private final Path root;
        private final Map<String, ModuleAccumulator> modules;
        private final List<EntryPointInfo> entryPoints;
        private final List<RouteInfo> routes;
        private final List<ScheduledJobInfo> jobs;
        private final List<ConfigSignal> configSignals;
        private final List<SymbolInfo> symbols;
        private final List<FileSummary> summaries;
        private final List<String> warnings;
        private final Set<String> stackSignals;
        private final ExclusionTracker exclusionTracker;
        private int indexedFiles;
        private boolean indexedFileLimitReached;

        private IndexedFileVisitor(
                Path root,
                Map<String, ModuleAccumulator> modules,
                List<EntryPointInfo> entryPoints,
                List<RouteInfo> routes,
                List<ScheduledJobInfo> jobs,
                List<ConfigSignal> configSignals,
                List<SymbolInfo> symbols,
                List<FileSummary> summaries,
                List<String> warnings,
                Set<String> stackSignals,
                ExclusionTracker exclusionTracker) {
            this.root = root;
            this.modules = modules;
            this.entryPoints = entryPoints;
            this.routes = routes;
            this.jobs = jobs;
            this.configSignals = configSignals;
            this.symbols = symbols;
            this.summaries = summaries;
            this.warnings = warnings;
            this.stackSignals = stackSignals;
            this.exclusionTracker = exclusionTracker;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (dir.equals(root)) {
                return FileVisitResult.CONTINUE;
            }

            Path relative = root.relativize(dir);
            if (pathRules.isExcluded(relative)) {
                exclusionTracker.record(normalize(relative) + "/ (excluded directory)");
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            Path relative = root.relativize(file);
            String path = normalize(relative);

            if (pathRules.isExcluded(relative)) {
                exclusionTracker.record(path + " (excluded)");
                return FileVisitResult.CONTINUE;
            }

            if (pathRules.isSensitive(relative)) {
                exclusionTracker.record(path + " (sensitive)");
                return FileVisitResult.CONTINUE;
            }

            if (attrs.size() > properties.getMaxFileSizeBytes()) {
                exclusionTracker.record(path + " (too large)");
                return FileVisitResult.CONTINUE;
            }

            if (++indexedFiles > properties.getMaxIndexedFiles()) {
                indexedFileLimitReached = true;
                return FileVisitResult.TERMINATE;
            }

            try {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                String extension = extensionOf(path);

                detectConfigs(path, content, configSignals, stackSignals);
                detectSummary(path, extension, content, summaries);
                detectJavaSignals(path, content, modules, entryPoints, routes, jobs, symbols, stackSignals);
            } catch (Exception ex) {
                warnings.add("Skipped unreadable file: " + path);
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            Path relative = root.relativize(file);
            warnings.add("Skipped unreadable file: " + normalize(relative));
            return FileVisitResult.CONTINUE;
        }

        private boolean indexedFileLimitReached() {
            return indexedFileLimitReached;
        }
    }

    private String normalize(Path relative) {
        return relative.toString().replace('\\', '/');
    }

    private static final class ExclusionTracker {
        private final LinkedHashSet<String> entries = new LinkedHashSet<>();
        private int additionalCount;

        private void record(String value) {
            if (entries.size() < MAX_EXCLUSION_PREVIEW) {
                entries.add(value);
                return;
            }
            additionalCount++;
        }

        private List<String> snapshot() {
            List<String> result = entries.stream().sorted().toList();
            if (additionalCount > 0) {
                List<String> withSummary = new ArrayList<>(result);
                withSummary.add("... and " + additionalCount + " more excluded paths");
                return withSummary;
            }
            return result;
        }
    }

    private static final class ModuleAccumulator {
        private final String name;
        private String kind = "module";
        private final Set<String> descriptionHints = new LinkedHashSet<>();
        private final Set<String> technologies = new LinkedHashSet<>();
        private final Set<String> paths = new LinkedHashSet<>();

        private ModuleAccumulator(String name) {
            this.name = name;
        }

        private ModuleCard toModuleCard() {
            List<String> sortedPaths = paths.stream().sorted().limit(5).toList();
            String description = descriptionHints.isEmpty()
                    ? "Contains source files grouped by package or folder"
                    : String.join("; ", descriptionHints);
            return new ModuleCard(
                    name,
                    kind,
                    description,
                    sortedPaths,
                    new ArrayList<>(technologies),
                    Math.min(1.0, 0.4 + (paths.size() * 0.05)));
        }
    }
}
