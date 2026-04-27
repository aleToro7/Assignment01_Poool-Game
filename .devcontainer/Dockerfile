FROM mcr.microsoft.com/vscode/devcontainers/java:11

# Install utils to clone JPF and build it (git, ant) and dos2unix to fix line endings on Windows
RUN apt-get update && apt-get install -y git ant dos2unix

# 1. Install JPF on /opt/jpf
WORKDIR /opt/jpf
RUN git clone https://github.com/javapathfinder/jpf-core.git

WORKDIR /opt/jpf/jpf-core
# Force LF on gradlew to avoid errors on Windows
RUN dos2unix gradlew && chmod +x gradlew
# Estendiamo il tempo di attesa per il download a 120 secondi (120000ms)
ENV GRADLE_OPTS="-Dorg.gradle.internal.http.connectionTimeout=120000 -Dorg.gradle.internal.http.socketTimeout=120000"

# Ora eseguiamo il comando di build con il nuovo limite di tempo
RUN ./gradlew clean build

# 2. Configure site.properties (The "Map" of JPF)
RUN mkdir -p /home/vscode/.jpf && \
    echo "jpf.home = /opt/jpf" > /home/vscode/.jpf/site.properties && \
    echo "jpf-core = /opt/jpf/jpf-core" >> /home/vscode/.jpf/site.properties && \
    echo "extensions = \${jpf-core}" >> /home/vscode/.jpf/site.properties && \
    chown -R vscode:vscode /home/vscode/.jpf

# 3. Prepare the working directory required by the professor
RUN mkdir -p /pcd-jpf && chown -R vscode:vscode /pcd-jpf

# 4. Alias 'jpf' for convenience in the terminal
RUN echo "alias jpf='java -jar /opt/jpf/jpf-core/build/RunJPF.jar'" >> /home/vscode/.bashrc

# Set the working directory to /pcd-jpf for compatibility with the professor's instructions
WORKDIR /pcd-jpf