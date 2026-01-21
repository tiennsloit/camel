# Camel Modular IO Platform

A minimal Apache Camel (Spring Boot) runtime that loads pluggable IO components providing a uniform `getFolderInfo` capability (list root folders or child folders). Components (e.g., Google Drive, OneDrive, Dropbox) implement a shared SPI, can be built as independent JARs, and are discovered on the classpath or from a plugins directory for hot drop-in on Azure.

## Layout
- `io-component-spi`: Shared interfaces/DTOs (`FolderInfoProvider`, `FolderInfoRequest`, `FolderInfoResponse`, `FolderInfoItem`).
- `io-component-gdrive`, `io-component-onedrive`: Sample stub providers; replace with real API calls.
- `camel-app`: Spring Boot Camel runtime exposing REST endpoints and loading providers (classpath + `./plugins`).

## Build
```bash
mvn clean package -DskipTests
```
Artifacts:
- Runtime: `camel-app/target/camel-app-0.1.0-SNAPSHOT.jar`
- Built-in providers: packaged transitively; external providers can be built separately as JARs.

## Run locally
```bash
java -jar camel-app/target/camel-app-0.1.0-SNAPSHOT.jar
```
Endpoints (platform-http, JSON):
- `GET /iocomponent/providers` → list provider ids.
- `GET /iocomponent/{provider}/getFolderInfo?folderId=<optional>` → folder listing (`folderId` omitted = roots). Additional query params prefixed with `option.` are passed through to providers.

## Adding a new IO component
1) Depend on `io-component-spi`:
```xml
<dependency>
  <groupId>com.example.camel</groupId>
  <artifactId>io-component-spi</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```
2) Implement `FolderInfoProvider` and return the shared `FolderInfoResponse` shape.
3) Register via `META-INF/services/com.example.camel.io.spi.FolderInfoProvider` containing the implementation class name.
4) Build a JAR (e.g., `mvn clean package`).

## Deploying / updating components on Azure
- **Runtime**: deploy `camel-app` JAR to Azure App Service, Container Apps, or AKS. Use Java 17 stack. Expose port `8080` (configurable via `server.port`).
- **External components**: upload provider JARs to a mounted path (e.g., Azure Files/Blob mounted to `/home/site/plugins` for App Service). Configure the runtime with:
```bash
JAVA_OPTS="... -Dapp.plugin.plugins-dir=/home/site/plugins"
```
Drop new JARs into that directory and restart the app (or recycle the pod) to load.

## Example requests
- Providers: `curl http://localhost:8080/iocomponent/providers`
- Root folders: `curl http://localhost:8080/iocomponent/onedrive/getFolderInfo`
- Child folders: `curl "http://localhost:8080/iocomponent/gdrive/getFolderInfo?folderId=abc123"`

## Notes
- The sample providers return stub data; replace with real Google Drive / OneDrive / Dropbox SDK calls while keeping the shared response shape.
- CORS is enabled on the REST configuration; lock down as needed for production.
