import os

app_dir = r"c:\Users\HPZBOOK\StudioProjects\pennywiseai-tracker\app"
found = []

for root, dirs, files in os.walk(app_dir):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            try:
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    if "OverviewTab" in content:
                        found.append((path, "OverviewTab" in content))
            except Exception as e:
                pass

print("Files containing OverviewTab:")
for path, _ in found:
    print(path)
