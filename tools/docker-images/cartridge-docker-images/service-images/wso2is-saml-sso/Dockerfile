FROM chamilad/base-image:4.1.0-beta
MAINTAINER dev@stratos.apache.org

ENV DEBIAN_FRONTEND noninteractive
ENV WSO2_IS_VERSION 5.0.0

# ----------------------
# Install prerequisites
# ----------------------
WORKDIR /opt
ADD packs/jdk-7u67-linux-x64.tar.gz /mnt/jdk-7u67-linux-x64.tar.gz
RUN mv /mnt/jdk-7u67-linux-x64.tar.gz/jdk1.7.0_67 /opt/jdk1.7.0_67

ENV JAVA_HOME /opt/jdk1.7.0_67

# -----------------------------
# Install WSO2 Identity Server
# -----------------------------
ADD packs/wso2is-${WSO2_IS_VERSION}-patched.zip /opt/wso2is-${WSO2_IS_VERSION}-patched.zip
RUN unzip /opt/wso2is-${WSO2_IS_VERSION}-patched.zip -d /opt/
ENV CARBON_HOME /opt/wso2is-${WSO2_IS_VERSION}

ADD files/env /tmp/env
RUN cat /tmp/env >> /etc/environment

ADD files/carbon.xml ${CARBON_HOME}/repository/conf/carbon.xml

#------------------------
# Copy Tomcat related PCA plugins
#-----------------------
ADD packs/plugins /mnt/plugins


EXPOSE 9443

# -----------------------
# Define entry point
# -----------------------
ENTRYPOINT /usr/local/bin/run | /usr/sbin/sshd -D
