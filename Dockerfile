# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.9
ARG PLATFORM=""
# Application image

FROM hmctsprod.azurecr.io/base/java${PLATFORM}:25-distroless
USER hmcts

COPY lib/applicationinsights.json /opt/app
COPY build/libs/manage-case-assignment.jar /opt/app/

EXPOSE 4454
CMD [ "manage-case-assignment.jar" ]
