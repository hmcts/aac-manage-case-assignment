ARG APP_INSIGHTS_AGENT_VERSION=3.4.13
ARG PLATFORM=""
# Application image

FROM hmctspublic.azurecr.io/base/java${PLATFORM}:17-distroless
USER hmcts

COPY lib/applicationinsights.json /opt/app
COPY build/libs/manage-case-assignment.jar /opt/app/

EXPOSE 4454
CMD [ "manage-case-assignment.jar" ]

