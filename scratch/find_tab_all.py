import os

workspace = r"c:\Users\HPZBOOK\StudioProjects\pennywiseai-tracker"
found = []

for root, dirs, files in os.walk(workspace):
    if ".gradle" in root or ".idea" in root or "build" in root:
        continue
    for file in files:
        if file.endswith(".kt") or file.endswith(".java"):
            path = os.path.join(root, file)
            try:
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    if "OverviewTab" in content:
                        found.append(path)
            except Exception as e:
                pass

print("KT/Java files containing OverviewTab:")
for path in found:
    print(path)
