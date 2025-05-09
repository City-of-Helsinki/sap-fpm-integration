FROM registry.access.redhat.com/ubi8/python-311

USER 0
ADD robot/src robot-sources
RUN chown -R 1001:0 ./
USER 1001

RUN pip install robotframework
RUN pip install pyftpdlib