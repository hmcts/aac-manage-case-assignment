ARG APP_INSIGHTS_AGENT_VERSION=2.5.1
ARG PLATFORM=""
# Application image

FROM hmctspublic.azurecr.io/base/java${PLATFORM}:11-distroless

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/manage-case-assignment.jar /opt/app/

EXPOSE 4454
CMD [ "manage-case-assignment.jar" ]

