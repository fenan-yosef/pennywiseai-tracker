path = r"c:\Users\HPZBOOK\StudioProjects\pennywiseai-tracker\app\src\main\java\com\pennywiseai\tracker\ui\screens\analytics\AnalyticsScreen.kt"

with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "tab" in line.lower() or "snapshot" in line.lower() or "trends" in line.lower():
        print(f"Line {i+1}: {line.strip()}")
