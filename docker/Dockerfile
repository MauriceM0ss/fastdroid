# Build image for FastDroid: JDK 17 + Gradle + Android SDK. No app code is
# baked in — the repo is bind-mounted at /workspace by scripts/gradle.sh.
FROM eclipse-temurin:17-jdk-jammy

ARG CMDLINE_TOOLS_VERSION=11076708
ARG GRADLE_VERSION=8.11.1
ARG ANDROID_PLATFORM=36
ARG BUILD_TOOLS=36.0.0

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    GRADLE_HOME=/opt/gradle \
    DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl unzip ca-certificates \
 && rm -rf /var/lib/apt/lists/*

RUN mkdir -p "$ANDROID_HOME/cmdline-tools" \
 && curl -fsSL -o /tmp/tools.zip \
      "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" \
 && unzip -q /tmp/tools.zip -d "$ANDROID_HOME/cmdline-tools" \
 && mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" \
 && rm /tmp/tools.zip

ENV PATH=/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:/opt/gradle/bin:$PATH

RUN yes | sdkmanager --licenses > /dev/null \
 && sdkmanager --install \
      "platform-tools" \
      "platforms;android-${ANDROID_PLATFORM}" \
      "build-tools;${BUILD_TOOLS}" > /dev/null

RUN curl -fsSL -o /tmp/gradle.zip \
      "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
 && unzip -q /tmp/gradle.zip -d /opt \
 && mv "/opt/gradle-${GRADLE_VERSION}" "$GRADLE_HOME" \
 && rm /tmp/gradle.zip

# The container runs as the host user (--user), so the SDK must be world-readable.
RUN chmod -R a+rX "$ANDROID_HOME" "$GRADLE_HOME"

WORKDIR /workspace
