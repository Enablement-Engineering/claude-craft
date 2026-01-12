---
name: deploy
description: Build and deploy the mod to Prism Launcher
---

Build the mod and copy to Prism Launcher instance.

```bash
JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ./gradlew build && \
cp build/libs/claudecraft-0.0.1.jar "/Users/dylanisaac/Library/Application Support/PrismLauncher/instances/1.21.1 neoforge/minecraft/mods/"
```

Report: "Deployed! Launch instance and press `\` to chat."
