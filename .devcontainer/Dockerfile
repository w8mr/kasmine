FROM mcr.microsoft.com/devcontainers/universal:noble

# Install Google Chrome for JS browser tests
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | gpg --dearmour -o /usr/share/keyrings/google-chrome.gpg && \
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] http://dl.google.com/linux/chrome/deb/ stable main" | tee /etc/apt/sources.list.d/google-chrome.list && \
    apt-get update && apt-get install -y google-chrome-stable && \
    rm -rf /var/lib/apt/lists/*

# Install opencode CLI (as codespace user so it lands in the right home)
RUN mkdir -p /home/codespace/.opencode/bin && \
    curl -fsSL https://opencode.ai/install.sh | USER=codespace HOME=/home/codespace sh && \
    chown -R codespace:codespace /home/codespace/.opencode

# Set environment variables
ENV CHROME_BIN=/usr/bin/google-chrome-stable
ENV JAVA_HOME="/usr/local/sdkman/candidates/java/current"
ENV PATH="/home/codespace/.opencode/bin:${PATH}"
