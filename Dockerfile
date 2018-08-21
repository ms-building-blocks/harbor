FROM openjdk:jdk-slim as builder

RUN mkdir -p /build/
WORKDIR /build/

COPY . /build/

# Should fix StackOverflow errors if you occur in one of them
# ARG MAVEN_OPTS="-Xms256m -Xmx1024m -Xss1024k"

# Installs maven
RUN apt-get update &&  \
    apt-get install -y maven git

# Installs https://github.com/kubernetes-client/java
RUN path="$(pwd)" && \
    cd .. && \
    git clone --recursive https://github.com/kubernetes-client/java && \
    cd java && \
    git checkout 8d6ab536f565ee951141b14bfd170629399d8c67 && \
    mvn install -DskipTests && \
    cd $path

# Builds Harbor
RUN mvn package && \
    cd target/ && \
    mkdir -p bundle && \
    cp ../api_sample.json bundle/ && \
    cp $(ls | grep dependencies.jar) bundle/ && \
    cd bundle && \
    mv *.jar harbor.jar


FROM openjdk:8-jre-alpine

LABEL maintainer="poloniodavide@gmail.com"
LABEL license="GPLv3+"
LABEL description="Harbor Docker image"

# Available environment variables:
# -HARBOR_PORT: custom port in which harbor will run (the default is 80)
# -HARBOR_API_CONFIG: path to your API configuration json
# -HARBOR_KUBERNETES_URL: url to your Kubernetes API endpoint
# -HARBOR_YAML_STORAGE_PATH: path to an empty folder where Harbor will create it's home for YAML storage
# -KUBECTL_CONFIG_PATH: path to the kubectl config command. Useful only when Harbor uses kubectl to talk with k8s
ENV HARBOR_API_CONFIG=api_sample.json

# Installing kubectl
ADD https://storage.googleapis.com/kubernetes-release/release/v1.11.0/bin/linux/amd64/kubectl /usr/local/bin/rkubectl
COPY docker/scripts/kubectl.sh /usr/local/bin/kubectl
RUN chmod +x /usr/local/bin/rkubectl && \
    chmod +x /usr/local/bin/kubectl

RUN mkdir -p /config/
VOLUME /config/
WORKDIR /srv/

COPY --from=builder /build/target/bundle/harbor.jar /srv/
COPY --from=builder /build/target/bundle/api_sample.json /srv/

ENTRYPOINT ["java", "-jar", "harbor.jar"]

