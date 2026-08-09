package com.sekai.sekai_form.control;

import com.sekai.sekai_form.model.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/api/model")
public class SekaiFormModelControl {
    private static final Path MODEL_BASE;
    static {
        Path cwd = Paths.get("").toAbsolutePath();
        Path candidate = cwd.resolve("models");
        if (!Files.isDirectory(candidate)) candidate = cwd.resolve("SekaiForm").resolve("models");
        MODEL_BASE = candidate.normalize();
    }

@GetMapping("/2d/list") public Result<List<String>> list2DModels() {
        try {
            List<String> names = new ArrayList<>();
            Path dir2d = MODEL_BASE.resolve("2d");
            if (Files.isDirectory(dir2d)) {
                try (var stream = Files.list(dir2d)) {
                    stream.filter(Files::isDirectory).map(p -> p.getFileName().toString()).forEach(names::add);
                }
            }
            return Result.ok(names);
        } catch (IOException e) { return Result.fail(e.getMessage()); }
    }

    
    @PostMapping("/upload") public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.fail("文件为空");
        try {
            String name = file.getOriginalFilename();
            if (name == null) return Result.fail("文件名无效");
            Path targetDir = null;
            if (name.endsWith(".zip")) {
                String baseName = name.replace(".zip", "");
                targetDir = MODEL_BASE.resolve("2d").resolve(baseName);
                // Extract to temp dir first
                Path tempDir = MODEL_BASE.resolve("2d").resolve(baseName + "_tmp_" + System.currentTimeMillis());
                Files.createDirectories(tempDir);
                try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        Path out = tempDir.resolve(entry.getName()).normalize();
                        if (!out.startsWith(tempDir)) continue;
                        if (entry.isDirectory()) { Files.createDirectories(out); continue; }
                        Files.createDirectories(out.getParent());
                        try (OutputStream os = Files.newOutputStream(out)) { zis.transferTo(os); }
                    }
                }
                // Find model3.json recursively and reorganize
                File modelJson = findModelJson(tempDir.toFile());
                if (modelJson != null) {
                    File srcDir = modelJson.getParentFile();
                    String expectedName = baseName + ".model3.json";
                    File renamed = new File(srcDir, expectedName);
                    if (!modelJson.getName().equals(expectedName)) {
                        modelJson.renameTo(renamed);
                    }
                    Files.createDirectories(targetDir);
                    moveContents(srcDir, targetDir.toFile());
                } else {
                    // No model3.json found, move all
                    Files.createDirectories(targetDir);
                    moveContents(tempDir.toFile(), targetDir.toFile());
                }
                // Clean up temp dir
                deleteDir(tempDir.toFile());
            } else {
                return Result.fail("仅支持zip格式上传");
            }
            Map<String, String> result = new HashMap<>();
            result.put("fileName", targetDir.getFileName().toString());
            return Result.ok("上传成功", result);
        } catch (IOException e) { return Result.fail("上传失败: " + e.getMessage()); }
    }

    private File findModelJson(File dir) {
        if (!dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File found = findModelJson(f);
                if (found != null) return found;
            } else if (f.getName().toLowerCase().endsWith(".model3.json")) {
                return f;
            }
        }
        return null;
    }

    private void moveContents(File src, File dest) throws IOException {
        if (!src.isDirectory()) return;
        Files.createDirectories(dest.toPath());
        File[] files = src.listFiles();
        if (files == null) return;
        for (File f : files) {
            Path target = dest.toPath().resolve(f.getName());
            if (f.isDirectory()) {
                moveContents(f, target.toFile());
            } else {
                Files.move(f.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) { if (f.isDirectory()) deleteDir(f); else f.delete(); }
        dir.delete();
    }
@DeleteMapping("/delete") public Result<Void> delete(@RequestParam String fileName) {
        try {
            Path path = MODEL_BASE.resolve("2d").resolve(fileName);
            if (Files.isDirectory(path)) {
                try (var s = Files.walk(path)) { s.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete); }
            } else { Files.deleteIfExists(path); }
            return Result.ok("删除成功", null);
        } catch (IOException e) { return Result.fail(e.getMessage()); }
    }
}