FROM stratos/base-image:4.1.0-beta
MAINTAINER dev@stratos.apache.org

# ----------------------
# Install prerequisites
# ----------------------
RUN apt-get update && \
    apt-get install -yq --no-install-recommends openjdk-7-jre wget ca-certificates && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV CATALINA_HOME /opt/tomcat

# ----------------------
# Install Tomcat
# ----------------------
RUN cd /opt/ && \
    wget -q https://archive.apache.org/dist/tomcat/tomcat-7/v7.0.55/bin/apache-tomcat-7.0.55.tar.gz && \
    wget -qO- https://archive.apache.org/dist/tomcat/tomcat-7/v7.0.55/bin/apache-tomcat-7.0.55.tar.gz.md5 | md5sum -c - && \
    tar zxf apache-tomcat-7.0.55.tar.gz && \
    rm apache-tomcat-7.0.55.tar.gz && \
    mv apache-tomcat-7.0.55 tomcat

# -----------------------
# Add shell scripts
# -----------------------
ADD files/create-admin-user.sh /opt/create-admin-user.sh
ADD files/run.sh /opt/run.sh
RUN chmod +x /opt/create-admin-user.sh /opt/run.sh && \
    bash /opt/create-admin-user.sh

EXPOSE 8080

# -----------------------
# Define entry point
# -----------------------
ENTRYPOINT /usr/local/bin/run | /opt/run.sh | /usr/sbin/sshd -D
