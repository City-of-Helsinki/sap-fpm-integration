FROM registry.access.redhat.com/ubi8/python-311 AS build

USER 0
ADD robot/src robot-sources
RUN chown -R 1001:0 ./
USER 1001

RUN pip install robotframework
RUN pip install pyftpdlib

EXPOSE 2121 2122 2123 6000 6001 6002

ENTRYPOINT sleep infinity
#ENTRYPOINT robot .