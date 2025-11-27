# Spring Boot 開發環境 - OpenJDK 21 + Maven
FROM eclipse-temurin:21-jdk-jammy

# 安裝基礎工具
RUN apt-get update && apt-get install -y \
    git \
    curl \
    wget \
    vim \
    nano \
    maven \
    && rm -rf /var/lib/apt/lists/*

# 安裝 Node.js 24 (Claude Code 需要)
RUN curl -fsSL https://deb.nodesource.com/setup_24.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# 安裝 Claude Code
RUN npm install -g @anthropic-ai/claude-code

# 建立非 root 使用者
ARG USERNAME=vscode
ARG USER_UID=1000
ARG USER_GID=$USER_UID

RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME \
    && apt-get update \
    && apt-get install -y sudo \
    && echo $USERNAME ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME \
    && rm -rf /var/lib/apt/lists/*

# 建立 Maven 目錄 (home 目錄會被掛載覆蓋，但 .m2 用 named volume)
RUN mkdir -p /home/$USERNAME/.m2/repository \
    && chown -R $USERNAME:$USERNAME /home/$USERNAME \
    && chmod -R 755 /home/$USERNAME/.m2

# 建立 bash history 持久化目錄
RUN mkdir -p /commandhistory \
    && touch /commandhistory/.bash_history \
    && chown -R $USERNAME:$USERNAME /commandhistory

# 設定 bash 歷史記錄和 readline 配置
RUN echo '' >> /home/$USERNAME/.bashrc \
    && echo '# Bash history configuration' >> /home/$USERNAME/.bashrc \
    && echo 'export HISTFILE=/commandhistory/.bash_history' >> /home/$USERNAME/.bashrc \
    && echo 'export HISTSIZE=10000' >> /home/$USERNAME/.bashrc \
    && echo 'export HISTFILESIZE=20000' >> /home/$USERNAME/.bashrc \
    && echo 'export HISTCONTROL=ignoredups:ignorespace' >> /home/$USERNAME/.bashrc \
    && echo 'shopt -s histappend' >> /home/$USERNAME/.bashrc \
    && echo 'PROMPT_COMMAND="history -a; $PROMPT_COMMAND"' >> /home/$USERNAME/.bashrc

# 設定全域 bash 環境變數 (對 non-interactive shell 也生效)
RUN echo '# Global bash history configuration' >> /etc/bash.bashrc \
    && echo 'export HISTFILE=/commandhistory/.bash_history' >> /etc/bash.bashrc \
    && echo 'export HISTSIZE=10000' >> /etc/bash.bashrc \
    && echo 'export HISTFILESIZE=20000' >> /etc/bash.bashrc \
    && echo 'export HISTCONTROL=ignoredups:ignorespace' >> /etc/bash.bashrc \
    && echo 'shopt -s histappend 2>/dev/null || true' >> /etc/bash.bashrc \
    && echo 'set -o emacs 2>/dev/null || true' >> /etc/bash.bashrc

# 設定 inputrc 以支援上下箭頭歷史記錄
RUN echo '# Enable arrow keys for history navigation' > /home/$USERNAME/.inputrc \
    && echo '"\e[A": previous-history' >> /home/$USERNAME/.inputrc \
    && echo '"\e[B": next-history' >> /home/$USERNAME/.inputrc \
    && echo '"\eOA": previous-history' >> /home/$USERNAME/.inputrc \
    && echo '"\eOB": next-history' >> /home/$USERNAME/.inputrc \
    && echo '' >> /home/$USERNAME/.inputrc \
    && echo '# Enable Ctrl+arrow for word movement' >> /home/$USERNAME/.inputrc \
    && echo '"\e[1;5C": forward-word' >> /home/$USERNAME/.inputrc \
    && echo '"\e[1;5D": backward-word' >> /home/$USERNAME/.inputrc \
    && echo '' >> /home/$USERNAME/.inputrc \
    && echo '# Case-insensitive tab completion' >> /home/$USERNAME/.inputrc \
    && echo 'set completion-ignore-case on' >> /home/$USERNAME/.inputrc \
    && echo 'set show-all-if-ambiguous on' >> /home/$USERNAME/.inputrc \
    && echo '' >> /home/$USERNAME/.inputrc \
    && echo '# History configuration' >> /home/$USERNAME/.inputrc \
    && echo 'set history-size 10000' >> /home/$USERNAME/.inputrc \
    && echo 'set history-preserve-point on' >> /home/$USERNAME/.inputrc \
    && echo '' >> /home/$USERNAME/.inputrc \
    && echo '# Enable editing mode' >> /home/$USERNAME/.inputrc \
    && echo 'set editing-mode emacs' >> /home/$USERNAME/.inputrc \
    && chown $USERNAME:$USERNAME /home/$USERNAME/.inputrc \
    && chmod 644 /home/$USERNAME/.inputrc

# 設定全域 inputrc (對所有使用者和 non-interactive shell 生效)
RUN echo '# Global inputrc configuration' > /etc/inputrc \
    && echo '"\e[A": previous-history' >> /etc/inputrc \
    && echo '"\e[B": next-history' >> /etc/inputrc \
    && echo '"\eOA": previous-history' >> /etc/inputrc \
    && echo '"\eOB": next-history' >> /etc/inputrc \
    && echo '"\e[1;5C": forward-word' >> /etc/inputrc \
    && echo '"\e[1;5D": backward-word' >> /etc/inputrc \
    && echo 'set completion-ignore-case on' >> /etc/inputrc \
    && echo 'set show-all-if-ambiguous on' >> /etc/inputrc \
    && echo 'set history-size 10000' >> /etc/inputrc \
    && echo 'set history-preserve-point on' >> /etc/inputrc \
    && echo 'set editing-mode emacs' >> /etc/inputrc

# 建立工作目錄
WORKDIR /workspace

# 設定環境變數
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV MAVEN_HOME=/usr/share/maven
ENV MAVEN_CONFIG=/home/vscode/.m2

# 驗證安裝
RUN java -version && mvn -version

CMD ["sleep", "infinity"]
