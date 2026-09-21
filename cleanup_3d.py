import pathlib

# 1. Fix page routing - redirect / to /live2d
path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\control\SekaiFormPageControl.java')
content = path.read_text('utf-8')
content = content.replace('@GetMapping("/") public String viewer() { return "index"; }', 
                          '@GetMapping("/") public String home() { return "redirect:/live2d"; }')
# Remove viewer/{id} route
content = content.replace('@GetMapping("/viewer/{id}") public String viewerDetail(@PathVariable Long id, Model model) { model.addAttribute("charId", id); return "viewer"; }\n\n', '')
# Remove unused import
content = content.replace('import org.springframework.web.bind.annotation.PathVariable;\n', '')
path.write_text(content, 'utf-8')
print('PageControl updated: / redirects to /live2d')

# 2. Simplify SekaiFormModelControl - remove 3D routes, keep 2D
path2 = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\control\SekaiFormModelControl.java')
content2 = path2.read_text('utf-8')

# Remove the 3D list method
old_3d_list = '''    @GetMapping("/list") public Result<List<String>> listModels() {
        try {
            List<String> names = new ArrayList<>();
            Path dir3d = MODEL_BASE.resolve("3d");
            if (Files.isDirectory(dir3d)) {
                try (var stream = Files.list(dir3d)) {
                    stream.filter(Files::isDirectory).map(p -> p.getFileName().toString()).forEach(names::add);
                }
            }
            return Result.ok(names);
        } catch (IOException e) { return Result.fail(e.getMessage()); }
    }

    '''
content2 = content2.replace(old_3d_list, '')

# Remove the delete method with 3D default
old_delete = '''@DeleteMapping("/delete") public Result<Void> delete(@RequestParam String fileName, @RequestParam(defaultValue="3d") String type) {
        try {
            Path path = MODEL_BASE.resolve(type).resolve(fileName);
            if (Files.isDirectory(path)) {
                try (var s = Files.walk(path)) { s.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete); }
            } else { Files.deleteIfExists(path); }
            return Result.ok("删除成功", null);
        } catch (IOException e) { return Result.fail(e.getMessage()); }
    }'''
new_delete = '''@DeleteMapping("/delete") public Result<Void> delete(@RequestParam String fileName) {
        try {
            Path path = MODEL_BASE.resolve("2d").resolve(fileName);
            if (Files.isDirectory(path)) {
                try (var s = Files.walk(path)) { s.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete); }
            } else { Files.deleteIfExists(path); }
            return Result.ok("删除成功", null);
        } catch (IOException e) { return Result.fail(e.getMessage()); }
    }'''
content2 = content2.replace(old_delete, new_delete)

# Remove the 3D upload part from the upload method
old_3d_upload = '''} else {
                targetDir = MODEL_BASE.resolve("3d").resolve(name);
                Files.createDirectories(targetDir.getParent());
                try (InputStream is = file.getInputStream()) {
                    Files.copy(is, targetDir, StandardCopyOption.REPLACE_EXISTING);
                }
            }'''
content2 = content2.replace(old_3d_upload, '}')

# Remove the unused import for Comparator
content2 = content2.replace('import java.util.Comparator;\n', '')
content2 = content2.replace('import java.util.zip.ZipEntry;\nimport java.util.zip.ZipInputStream;\n', 'import java.util.zip.ZipEntry;\nimport java.util.zip.ZipInputStream;\n')

path2.write_text(content2, 'utf-8')
print('ModelControl simplified: 3D routes removed')
