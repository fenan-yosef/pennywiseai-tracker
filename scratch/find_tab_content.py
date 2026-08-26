path = r"c:\Users\HPZBOOK\StudioProjects\pennywiseai-tracker\app\src\main\java\com\pennywiseai\tracker\ui\screens\analytics\tabs\TabContent.kt"

with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "fun " in line or "class " in line or "@Composable" in line:
        print(f"Line {i+1}: {line.strip()}")
