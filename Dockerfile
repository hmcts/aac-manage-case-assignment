ARG APP_INSIGHTS_AGENT_VERSION=2.5.1

# Application image

FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.2
USER hmcts

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/manage-case-assignment.jar /opt/app/

EXPOSE 4454
CMD [ "manage-case-assignment.jar" ]

