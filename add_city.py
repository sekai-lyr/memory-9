import pathlib

path = pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\service\impl\Live2DServiceImpl.java')
content = path.read_text('utf-8')

# Add extractCity method before the last closing brace of the class
# Find the final } that closes the class (last line)
insert_point = content.rfind('\n}')
city_method = '''
    /** Extract city name from message - 7.24.pm pattern */
    private String extractCity(String text) {
        if (text == null) return "\u5317\u4eac";
        String[] cities = {"\u676d\u5dde", "\u5317\u4eac", "\u4e0a\u6d77", "\u5e7f\u5dde", "\u6df1\u5733", 
                          "\u6210\u90fd", "\u91cd\u5e86", "\u6b66\u6c49", "\u5357\u4eac", "\u897f\u5b89",
                          "\u957f\u6c99", "\u90d1\u5dde", "\u5929\u6d25", "\u82cf\u5dde", "\u6c88\u9633",
                          "\u9752\u5c9b", "\u5927\u8fde", "\u53a6\u95e8", "\u6d4e\u5357", "\u6606\u660e"};
        for (String city : cities) {
            if (text.contains(city)) return city;
        }
        // Try to extract anything that looks like a city name (2-3 chars before weather-related words)
        String clean = text.replaceAll("[\u5929\u6c14\u6e29\u5ea6\u600e\u4e48\u6837\u5982\u4f55\u4eca\u5929\u660e\u5929\u67e5\u8be2\u5462\u5417\u5440\u561e\u5566\u54e6\u561e]", "");
        clean = clean.replaceAll("[?\\uff1f!\\uff01.。,，\\s]+", "").trim();
        if (clean.length() >= 2 && clean.length() <= 4) return clean;
        return "\u5317\u4eac";
    }'''

content = content[:insert_point] + city_method + content[insert_point:]
path.write_text(content, 'utf-8')
print('ADDED: extractCity method')
