# Notice

Copyright (C) 2025 European Union

Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions
of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.

You may obtain a copy of the Licence at:

https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12

Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
"AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for the specific
language governing permissions and limitations under the Licence.

This product depends on software developed by third parties as listed in the following sections.

## Third-party libraries used per module and licences

The current section includes all third party libraries used as binary distributions in the GITB software's executable
build. These libraries are used as-is, with no modifications to source code.

Library licences are organised in three sections:

- The **gitb-srv** component (test engine)
- The **gitb-ui** component (user interface)
- The **gitb-ui frontend** component (user interface frontend)

### Component gitb-srv (test engine)

| Licence type | Name | Dependency | Version | Link |
| :----------- | :--- | :--------- | :------ | :--- |
| Eclipse Public License - v1.0, GNU Lesser General Public License (LGPL), Version 2.1 | Logback Classic Module | ch.qos.logback:logback-classic | 1.5.18 | http://logback.qos.ch/logback-classic |
| Eclipse Public License - v1.0, GNU Lesser General Public License (LGPL), Version 2.1 | Logback Core Module | ch.qos.logback:logback-core | 1.5.18 | http://logback.qos.ch/logback-core |
| Apache License, Version 2.0 | Titanium JSON-LD 1.1 (JRE11) | com.apicatalog:titanium-json-ld | 1.4.1 | https://github.com/filip26/titanium-json-ld |
| Apache License, Version 2.0 | Internet Time Utility | com.ethlo.time:itu | 1.10.3 | https://github.com/ethlo/itu |
| Apache License, Version 2.0 | ClassMate | com.fasterxml:classmate | 1.7.0 | https://github.com/FasterXML/java-classmate |
| Apache License, Version 2.0 | Jackson-annotations | com.fasterxml.jackson.core:jackson-annotations | 2.19.0 | https://github.com/FasterXML/jackson |
| Apache License, Version 2.0 | Jackson-core | com.fasterxml.jackson.core:jackson-core | 2.19.0 | https://github.com/FasterXML/jackson-core |
| Apache License, Version 2.0 | jackson-databind | com.fasterxml.jackson.core:jackson-databind | 2.19.0 | https://github.com/FasterXML/jackson |
| Apache License, Version 2.0 | Jackson-dataformat-YAML | com.fasterxml.jackson.dataformat:jackson-dataformat-yaml | 2.19.0 | https://github.com/FasterXML/jackson-dataformats-text |
| Apache License, Version 2.0 | Jackson datatype: jdk8 | com.fasterxml.jackson.datatype:jackson-datatype-jdk8 | 2.19.0 | https://github.com/FasterXML/jackson-modules-java8/jackson-datatype-jdk8 |
| Apache License, Version 2.0 | Jackson datatype: JSR310 | com.fasterxml.jackson.datatype:jackson-datatype-jsr310 | 2.19.0 | https://github.com/FasterXML/jackson-modules-java8/jackson-datatype-jsr310 |
| Apache License, Version 2.0 | Jackson-module-parameter-names | com.fasterxml.jackson.module:jackson-module-parameter-names | 2.19.0 | https://github.com/FasterXML/jackson-modules-java8/jackson-module-parameter-names |
| Apache License, Version 2.0 | Woodstox | com.fasterxml.woodstox:woodstox-core | 7.1.0 | https://github.com/FasterXML/woodstox |
| MIT License | dexx | com.github.andrewoma.dexx:collection | 0.7 | https://github.com/andrewoma/dexx |
| Apache License, Version 2.0 | Caffeine cache | com.github.ben-manes.caffeine:caffeine | 3.2.0 | https://github.com/ben-manes/caffeine |
| Apache License, Version 2.0 | RgxGen | com.github.curious-odd-man:rgxgen | 1.4 | https://github.com/curious-odd-man/RgxGen |
| Apache License, Version 2.0 | FindBugs-jsr305 | com.google.code.findbugs:jsr305 | 3.0.2 | http://findbugs.sourceforge.net/ |
| Apache License, Version 2.0 | Gson | com.google.code.gson:gson | 2.13.1 | https://github.com/google/gson |
| Apache License, Version 2.0 | error-prone annotations | com.google.errorprone:error_prone_annotations | 2.38.0 | https://errorprone.info/error_prone_annotations |
| The 3-Clause BSD License | Protocol Buffers [Core] | com.google.protobuf:protobuf-java | 4.28.2 | https://developers.google.com/protocol-buffers/protobuf-java/ |
| Apache License, Version 2.0 | ph-commons | com.helger.commons:ph-commons | 11.1.6 | https://github.com/phax/ph-commons/ph-commons |
| Apache License, Version 2.0 | ph-jaxb | com.helger.commons:ph-jaxb | 11.1.6 | https://github.com/phax/ph-commons/ph-jaxb |
| Apache License, Version 2.0 | ph-xml | com.helger.commons:ph-xml | 11.1.6 | https://github.com/phax/ph-commons/ph-xml |
| Apache License, Version 2.0 | ph-schematron-api | com.helger.schematron:ph-schematron-api | 8.0.6 | https://github.com/phax/ph-schematron/ph-schematron-api |
| Apache License, Version 2.0 | ph-schematron-pure | com.helger.schematron:ph-schematron-pure | 8.0.6 | https://github.com/phax/ph-schematron/ph-schematron-pure |
| Apache License, Version 2.0 | ph-schematron-xslt | com.helger.schematron:ph-schematron-xslt | 8.0.6 | https://github.com/phax/ph-schematron/ph-schematron-xslt |
| Apache License, Version 2.0 | ph-xsds-xml | com.helger.xsd:ph-xsds-xml | 3.0.0 | https://github.com/phax/ph-xsds/ph-xsds-xml |
| Apache License, Version 2.0 | JsonSchemaValidator | com.networknt:json-schema-validator | 1.5.7 | https://github.com/networknt/json-schema-validator |
| GNU Lesser General Public License (LGPL), Version 2.1 | Openhtmltopdf Core Renderer | com.openhtmltopdf:openhtmltopdf-core | 1.0.10 | https://github.com/danfickle/openhtmltopdf/openhtmltopdf-core |
| GNU Lesser General Public License (LGPL), Version 2.1 | Openhtmltopdf PDF Rendering (Apache PDF-BOX 2) | com.openhtmltopdf:openhtmltopdf-pdfbox | 1.0.10 | https://github.com/danfickle/openhtmltopdf/openhtmltopdf-pdfbox |
| GNU Lesser General Public License (LGPL), Version 2.1 | Openhtmltopdf slf4j Support | com.openhtmltopdf:openhtmltopdf-slf4j | 1.0.10 | https://github.com/danfickle/openhtmltopdf/openhtmltopdf-slf4j |
| GNU Lesser General Public License (LGPL), Version 2.1 | Openhtmltopdf SVG Support | com.openhtmltopdf:openhtmltopdf-svg-support | 1.0.10 | https://github.com/danfickle/openhtmltopdf/openhtmltopdf-svg-support |
| Eclipse Distribution License - v1.0 | istack common utility code runtime | com.sun.istack:istack-commons-runtime | 4.1.2 | https://projects.eclipse.org/projects/ee4j/istack-commons/istack-commons-runtime |
| Eclipse Distribution License - v1.0 | Old JAXB Core | com.sun.xml.bind:jaxb-core | 4.0.5 | https://eclipse-ee4j.github.io/jaxb-ri/ |
| Eclipse Distribution License - v1.0 | Old JAXB Runtime | com.sun.xml.bind:jaxb-impl | 4.0.4 | https://eclipse-ee4j.github.io/jaxb-ri/ |
| Eclipse Distribution License - v1.0 | Jakarta SOAP Implementation | com.sun.xml.messaging.saaj:saaj-impl | 3.0.4 | https://projects.eclipse.org/projects/ee4j/metro-saaj/saaj-impl |
| Apache License, Version 2.0 | config | com.typesafe:config | 1.4.3 | https://github.com/lightbend/config |
| Apache License, Version 2.0 | Apache Commons Codec | commons-codec:commons-codec | 1.17.1 | https://commons.apache.org/proper/commons-codec/ |
| Apache License, Version 2.0 | Apache Commons FileUpload | commons-fileupload:commons-fileupload | 1.5 | https://commons.apache.org/proper/commons-fileupload/ |
| Apache License, Version 2.0 | Apache Commons IO | commons-io:commons-io | 2.17.0 | https://commons.apache.org/proper/commons-io/ |
| Apache License, Version 2.0 | Apache Commons Logging | commons-logging:commons-logging | 1.2 | http://commons.apache.org/proper/commons-logging/ |
| Apache License, Version 2.0 | Apache Commons Logging | commons-logging:commons-logging | 1.3.2 | https://commons.apache.org/proper/commons-logging/ |
| Apache License, Version 2.0 | PDFBox-Graphics2d | de.rototor.pdfbox:graphics2d | 0.32 | https://github.com/rototor/pdfbox-graphics2d/graphics2d |
| The 3-Clause BSD License | dnsjava | dnsjava:dnsjava | 3.6.0 | https://github.com/dnsjava/dnsjava |
| Apache License, Version 2.0 | micrometer-commons | io.micrometer:micrometer-commons | 1.15.0 | https://github.com/micrometer-metrics/micrometer |
| Apache License, Version 2.0 | micrometer-observation | io.micrometer:micrometer-observation | 1.15.0 | https://github.com/micrometer-metrics/micrometer |
| Eclipse Distribution License - v1.0 | Jakarta Activation API | jakarta.activation:jakarta.activation-api | 2.1.2 | https://github.com/jakartaee/jaf-api |
| Eclipse Public License - v2.0, GNU General Public License (GPL), Version 2 (with Classpath Exception) | Jakarta Annotations API | jakarta.annotation:jakarta.annotation-api | 2.1.1 | https://projects.eclipse.org/projects/ee4j.ca |
| Eclipse Distribution License - v1.0 | Jakarta Web Services Metadata API | jakarta.jws:jakarta.jws-api | 3.0.0 | https://github.com/eclipse-ee4j/jws-api |
| Eclipse Distribution License - v1.0, Eclipse Public License - v2.0, GNU General Public License (GPL), Version 2 (with Classpath Exception) | Jakarta Mail API | jakarta.mail:jakarta.mail-api | 2.1.3 | https://projects.eclipse.org/projects/ee4j/jakarta.mail-api |
| Apache License, Version 2.0 | Jakarta Bean Validation API | jakarta.validation:jakarta.validation-api | 3.0.2 | https://beanvalidation.org |
| Eclipse Distribution License - v1.0 | Jakarta XML Binding API | jakarta.xml.bind:jakarta.xml.bind-api | 4.0.2 | https://github.com/jakartaee/jaxb-api/jakarta.xml.bind-api |
| Eclipse Distribution License - v1.0 | Jakarta SOAP with Attachments API | jakarta.xml.soap:jakarta.xml.soap-api | 3.0.2 | https://github.com/jakartaee/saaj-api |
| Eclipse Distribution License - v1.0 | Jakarta XML Web Services API | jakarta.xml.ws:jakarta.xml.ws-api | 4.0.2 | https://github.com/jakartaee/jax-ws-api |
| Mozilla Public License Version 2.0 | Saxon-HE | net.sf.saxon:Saxon-HE | 12.4 | http://www.saxonica.com/ |
| Apache License, Version 2.0 | Apache Commons Collections | org.apache.commons:commons-collections4 | 4.4 | https://commons.apache.org/proper/commons-collections/ |
| Apache License, Version 2.0 | Apache Commons Compress | org.apache.commons:commons-compress | 1.27.1 | https://commons.apache.org/proper/commons-compress/ |
| Apache License, Version 2.0 | Apache Commons Configuration | org.apache.commons:commons-configuration2 | 2.11.0 | https://commons.apache.org/proper/commons-configuration/ |
| Apache License, Version 2.0 | Apache Commons CSV | org.apache.commons:commons-csv | 1.12.0 | https://commons.apache.org/proper/commons-csv/ |
| Apache License, Version 2.0 | Apache Commons Lang | org.apache.commons:commons-lang3 | 3.17.0 | https://commons.apache.org/proper/commons-lang/ |
| Apache License, Version 2.0 | Apache Commons Text | org.apache.commons:commons-text | 1.13.1 | https://commons.apache.org/proper/commons-text |
| Apache License, Version 2.0 | Apache CXF Core | org.apache.cxf:cxf-core | 4.1.1 | https://cxf.apache.org |
| Apache License, Version 2.0 | Apache CXF Runtime SOAP Binding | org.apache.cxf:cxf-rt-bindings-soap | 4.1.1 | https://cxf.apache.org |
| Apache License, Version 2.0 | Apache CXF Runtime XML Binding | org.apache.cxf:cxf-rt-bindings-xml | 4.1.1 | https://cxf.apache.org |
| Apache License, Version 2.0 | Apache CXF Runtime JAXB DataBinding | org.apache.cxf:cxf-rt-databinding-jaxb | 4.1.1 | https://cxf.apache.org |
| Apache License, Version 2.0 | Apache CXF Metrics Feature | org.apache.cxf:cxf-rt-features-metrics | 4.1.1 | https://cxf.apache.org/cxf-rt-features-metrics |
| Apache License, Version 2.0 | Apache CXF Runtime JAX-WS Frontend | org.apache.cxf:cxf-rt-frontend-jaxws | 4.1.1 | https://cxf.apache.org |
| Apache License, Version 2.0 | Apache CXF Runtime Simple Frontend | org.apache.cxf:cxf-rt-frontend-simple | 4.1.1 | https://cxf.apache.org |
| Apache License, Version 2.0 | Apache CXF Runtime HTTP Transport | org.apache.cxf:cxf-rt-transports-http | 4.1.1 | https://cxf.apache.org |
| Apache License, Version 2.0 | Apache CXF Runtime WS Addressing | org.apache.cxf:cxf-rt-ws-addr | 4.1.1 | https://cxf.apache.org |
| Apache License, Version 2.0 | Apache CXF Runtime WS Policy | org.apache.cxf:cxf-rt-ws-policy | 4.1.1 | https://cxf.apache.org |
| Apache License, Version 2.0 | Apache CXF Runtime Core for WSDL | org.apache.cxf:cxf-rt-wsdl | 4.1.1 | https://cxf.apache.org |
| Apache License, Version 2.0 | Apache CXF Spring Boot Autoconfigure | org.apache.cxf:cxf-spring-boot-autoconfigure | 4.1.1 | https://cxf.apache.org |
| Apache License, Version 2.0 | Apache CXF Spring Boot Starter JAX-WS | org.apache.cxf:cxf-spring-boot-starter-jaxws | 4.1.1 | https://cxf.apache.org |
| Apache License, Version 2.0 | Apache HttpClient | org.apache.httpcomponents:httpclient | 4.5.13 | http://hc.apache.org/httpcomponents-client |
| Apache License, Version 2.0 | Apache HttpCore | org.apache.httpcomponents:httpcore | 4.4.16 | http://hc.apache.org/httpcomponents-core-ga |
| Apache License, Version 2.0 | Apache HttpClient Mime | org.apache.httpcomponents:httpmime | 4.5.13 | http://hc.apache.org/httpcomponents-client |
| Apache License, Version 2.0 | Apache HttpClient | org.apache.httpcomponents.client5:httpclient5 | 5.4.4 | https://hc.apache.org/httpcomponents-client-5.4.x/5.4.4/httpclient5/ |
| Apache License, Version 2.0 | Apache HttpComponents Core HTTP/1.1 | org.apache.httpcomponents.core5:httpcore5 | 5.3.4 | https://hc.apache.org/httpcomponents-core-5.3.x/5.3.4/httpcore5/ |
| Apache License, Version 2.0 | Apache HttpComponents Core HTTP/2 | org.apache.httpcomponents.core5:httpcore5-h2 | 5.3.4 | https://hc.apache.org/httpcomponents-core-5.3.x/5.3.4/httpcore5-h2/ |
| Apache License, Version 2.0 | Apache Jena - ARQ | org.apache.jena:jena-arq | 5.2.0 | https://jena.apache.org/jena-arq/ |
| Apache License, Version 2.0 | Apache Jena - Base | org.apache.jena:jena-base | 5.2.0 | https://jena.apache.org/jena-base/ |
| Apache License, Version 2.0 | Apache Jena - Core | org.apache.jena:jena-core | 5.2.0 | https://jena.apache.org/jena-core/ |
| Apache License, Version 2.0 | Apache Jena - IRI | org.apache.jena:jena-iri | 5.2.0 | https://jena.apache.org/jena-iri/ |
| Apache License, Version 2.0 | Apache Log4j API | org.apache.logging.log4j:log4j-api | 2.24.3 | https://logging.apache.org/log4j/2.x/log4j/log4j-api/ |
| Apache License, Version 2.0 | Log4j API to SLF4J Adapter | org.apache.logging.log4j:log4j-to-slf4j | 2.24.3 | https://logging.apache.org/log4j/2.x/log4j/log4j-to-slf4j/ |
| Apache License, Version 2.0 | Apache Neethi | org.apache.neethi:neethi | 3.2.1 | https://ws.apache.org/neethi/ |
| Apache License, Version 2.0 | Apache FontBox | org.apache.pdfbox:fontbox | 2.0.31 | http://pdfbox.apache.org/ |
| Apache License, Version 2.0 | Apache PDFBox | org.apache.pdfbox:pdfbox | 2.0.31 | https://www.apache.org/pdfbox-parent/pdfbox/ |
| Apache License, Version 2.0 | Apache XmpBox | org.apache.pdfbox:xmpbox | 2.0.24 | https://www.apache.org/pdfbox-parent/xmpbox/ |
| Apache License, Version 2.0 | Apache Pekko Actor | org.apache.pekko:pekko-actor_3 | 1.1.3 | https://pekko.apache.org/ |
| Apache License, Version 2.0 | Apache Thrift | org.apache.thrift:libthrift | 0.21.0 | http://thrift.apache.org |
| Apache License, Version 2.0 | Apache Tika core | org.apache.tika:tika-core | 3.1.0 | https://tika.apache.org/ |
| Apache License, Version 2.0 | tomcat-embed-core | org.apache.tomcat.embed:tomcat-embed-core | 10.1.41 | https://tomcat.apache.org/ |
| Apache License, Version 2.0 | tomcat-embed-el | org.apache.tomcat.embed:tomcat-embed-el | 10.1.41 | https://tomcat.apache.org/ |
| Apache License, Version 2.0 | tomcat-embed-websocket | org.apache.tomcat.embed:tomcat-embed-websocket | 10.1.41 | https://tomcat.apache.org/ |
| Apache License, Version 2.0 | XmlSchema Core | org.apache.ws.xmlschema:xmlschema-core | 2.3.1 | https://ws.apache.org/commons/xmlschema20/xmlschema-core/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-anim | org.apache.xmlgraphics:batik-anim | 1.17 | http://xmlgraphics.apache.org/batik/batik-anim/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-awt-util | org.apache.xmlgraphics:batik-awt-util | 1.17 | http://xmlgraphics.apache.org/batik/batik-awt-util/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-bridge | org.apache.xmlgraphics:batik-bridge | 1.17 | http://xmlgraphics.apache.org/batik/batik-bridge/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-codec | org.apache.xmlgraphics:batik-codec | 1.17 | http://xmlgraphics.apache.org/batik/batik-codec/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-constants | org.apache.xmlgraphics:batik-constants | 1.17 | http://xmlgraphics.apache.org/batik/batik-constants/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-css | org.apache.xmlgraphics:batik-css | 1.17 | http://xmlgraphics.apache.org/batik/batik-css/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-dom | org.apache.xmlgraphics:batik-dom | 1.17 | http://xmlgraphics.apache.org/batik/batik-dom/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-ext | org.apache.xmlgraphics:batik-ext | 1.17 | http://xmlgraphics.apache.org/batik/batik-ext/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-gvt | org.apache.xmlgraphics:batik-gvt | 1.17 | http://xmlgraphics.apache.org/batik/batik-gvt/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-i18n | org.apache.xmlgraphics:batik-i18n | 1.17 | http://xmlgraphics.apache.org/batik/batik-i18n/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-parser | org.apache.xmlgraphics:batik-parser | 1.17 | http://xmlgraphics.apache.org/batik/batik-parser/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-script | org.apache.xmlgraphics:batik-script | 1.17 | http://xmlgraphics.apache.org/batik/batik-script/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-shared-resources | org.apache.xmlgraphics:batik-shared-resources | 1.17 | http://xmlgraphics.apache.org/batik/batik-shared-resources/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-svg-dom | org.apache.xmlgraphics:batik-svg-dom | 1.17 | http://xmlgraphics.apache.org/batik/batik-svg-dom/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-svggen | org.apache.xmlgraphics:batik-svggen | 1.17 | http://xmlgraphics.apache.org/batik/batik-svggen/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-transcoder | org.apache.xmlgraphics:batik-transcoder | 1.17 | http://xmlgraphics.apache.org/batik/batik-transcoder/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-util | org.apache.xmlgraphics:batik-util | 1.17 | http://xmlgraphics.apache.org/batik/batik-util/ |
| Apache License, Version 2.0 | org.apache.xmlgraphics:batik-xml | org.apache.xmlgraphics:batik-xml | 1.17 | http://xmlgraphics.apache.org/batik/batik-xml/ |
| Apache License, Version 2.0 | Apache XML Graphics Commons | org.apache.xmlgraphics:xmlgraphics-commons | 2.6 | http://xmlgraphics.apache.org/commons/ |
| The 2-Clause BSD License | Stax2 API | org.codehaus.woodstox:stax2-api | 4.2.2 | http://github.com/FasterXML/stax2-api |
| Eclipse Distribution License - v1.0 | Angus Activation Registries | org.eclipse.angus:angus-activation | 2.0.2 | https://github.com/eclipse-ee4j/angus-activation/angus-activation |
| Eclipse Distribution License - v1.0, Eclipse Public License - v2.0, GNU General Public License (GPL), Version 2 (with Classpath Exception) | Angus Mail Provider | org.eclipse.angus:angus-mail | 2.0.3 | http://eclipse-ee4j.github.io/angus-mail/angus-mail |
| Apache License, Version 2.0 | Apache FreeMarker | org.freemarker:freemarker | 2.3.34 | https://freemarker.apache.org/ |
| Eclipse Public License - v2.0, GNU General Public License (GPL), Version 2 (with Classpath Exception) | JSON-P Default Provider | org.glassfish:jakarta.json | 2.0.1 | https://github.com/eclipse-ee4j/jsonp |
| Eclipse Distribution License - v1.0 | JAXB Core | org.glassfish.jaxb:jaxb-core | 4.0.5 | https://eclipse-ee4j.github.io/jaxb-ri/ |
| Eclipse Distribution License - v1.0 | JAXB Runtime | org.glassfish.jaxb:jaxb-runtime | 4.0.5 | https://eclipse-ee4j.github.io/jaxb-ri/ |
| Eclipse Distribution License - v1.0 | TXW2 Runtime | org.glassfish.jaxb:txw2 | 4.0.5 | https://eclipse-ee4j.github.io/jaxb-ri/ |
| Apache License, Version 2.0 | Hibernate Validator Engine | org.hibernate.validator:hibernate-validator | 8.0.2.Final | http://hibernate.org/validator/hibernate-validator |
| Apache License, Version 2.0, GNU Lesser General Public License (LGPL), Version 2.1, Mozilla Public License Version 1.1 | Javassist | org.javassist:javassist | 3.28.0-GA | http://www.javassist.org/ |
| Apache License, Version 2.0 | JBoss Logging 3 | org.jboss.logging:jboss-logging | 3.6.1.Final | http://www.jboss.org |
| MIT License | jsoup Java HTML Parser | org.jsoup:jsoup | 1.15.4 | https://jsoup.org/ |
| Apache License, Version 2.0 | JSpecify annotations | org.jspecify:jspecify | 1.0.0 | http://jspecify.org/ |
| Eclipse Distribution License - v1.0 | Extended StAX API | org.jvnet.staxex:stax-ex | 2.1.0 | https://projects.eclipse.org/projects/ee4j/stax-ex |
| The 3-Clause BSD License | asm | org.ow2.asm:asm | 9.7.1 | http://asm.ow2.io/ |
| Apache License, Version 2.0, WTFPL | Reflections | org.reflections:reflections | 0.10.2 | http://github.com/ronmamo/reflections |
| Apache License, Version 2.0 | org.roaringbitmap:RoaringBitmap | org.roaringbitmap:RoaringBitmap | 1.3.0 | https://github.com/RoaringBitmap/RoaringBitmap |
| Apache License, Version 2.0 | Scala Library | org.scala-lang:scala-library | 2.13.14 | https://www.scala-lang.org/ |
| Apache License, Version 2.0 | scala3-library-bootstrapped | org.scala-lang:scala3-library_3 | 3.3.4 | https://github.com/scala/scala3 |
| Apache License, Version 2.0 | JCL 1.2 implemented over SLF4J | org.slf4j:jcl-over-slf4j | 2.0.17 | http://www.slf4j.org |
| MIT License | JUL to SLF4J bridge | org.slf4j:jul-to-slf4j | 2.0.17 | http://www.slf4j.org |
| MIT License | SLF4J API Module | org.slf4j:slf4j-api | 2.0.17 | http://www.slf4j.org |
| Apache License, Version 2.0 | Spring AOP | org.springframework:spring-aop | 6.2.7 | https://github.com/spring-projects/spring-framework |
| Apache License, Version 2.0 | Spring Beans | org.springframework:spring-beans | 6.2.7 | https://github.com/spring-projects/spring-framework |
| Apache License, Version 2.0 | Spring Context | org.springframework:spring-context | 6.2.7 | https://github.com/spring-projects/spring-framework |
| Apache License, Version 2.0 | Spring Core | org.springframework:spring-core | 6.2.7 | https://github.com/spring-projects/spring-framework |
| Apache License, Version 2.0 | Spring Expression Language (SpEL) | org.springframework:spring-expression | 6.2.7 | https://github.com/spring-projects/spring-framework |
| Apache License, Version 2.0 | Spring Commons Logging Bridge | org.springframework:spring-jcl | 6.2.7 | https://github.com/spring-projects/spring-framework |
| Apache License, Version 2.0 | Spring Web | org.springframework:spring-web | 6.2.7 | https://github.com/spring-projects/spring-framework |
| Apache License, Version 2.0 | Spring Web MVC | org.springframework:spring-webmvc | 6.2.7 | https://github.com/spring-projects/spring-framework |
| Apache License, Version 2.0 | spring-boot | org.springframework.boot:spring-boot | 3.5.0 | https://spring.io/projects/spring-boot |
| Apache License, Version 2.0 | spring-boot-autoconfigure | org.springframework.boot:spring-boot-autoconfigure | 3.5.0 | https://spring.io/projects/spring-boot |
| Apache License, Version 2.0 | spring-boot-starter | org.springframework.boot:spring-boot-starter | 3.4.5 | https://spring.io/projects/spring-boot |
| Apache License, Version 2.0 | spring-boot-starter-json | org.springframework.boot:spring-boot-starter-json | 3.5.0 | https://spring.io/projects/spring-boot |
| Apache License, Version 2.0 | spring-boot-starter-logging | org.springframework.boot:spring-boot-starter-logging | 3.4.5 | https://spring.io/projects/spring-boot |
| Apache License, Version 2.0 | spring-boot-starter-tomcat | org.springframework.boot:spring-boot-starter-tomcat | 3.5.0 | https://spring.io/projects/spring-boot |
| Apache License, Version 2.0 | spring-boot-starter-validation | org.springframework.boot:spring-boot-starter-validation | 3.4.5 | https://spring.io/projects/spring-boot |
| Apache License, Version 2.0 | spring-boot-starter-web | org.springframework.boot:spring-boot-starter-web | 3.5.0 | https://spring.io/projects/spring-boot |
| Apache License, Version 2.0 | TopBraid SHACL API | org.topbraid:shacl | 1.4.4 | http://topbraid.org/shacl/api |
| Apache License, Version 2.0 | XML Resolver | org.xmlresolver:xmlresolver | 5.2.2 | https://github.com/xmlresolver/xmlresolver |
| Apache License, Version 2.0 | org.xmlunit:xmlunit-core | org.xmlunit:xmlunit-core | 2.10.0 | https://www.xmlunit.org/ |
| Apache License, Version 2.0 | SnakeYAML | org.yaml:snakeyaml | 2.4 | https://bitbucket.org/snakeyaml/snakeyaml |
| Common Public License 1.0 | WSDL4J | wsdl4j:wsdl4j | 1.6.3 | http://sf.net/projects/wsdl4j |
| Apache License, Version 2.0 | Xerces2-j | xerces:xercesImpl | 2.12.2 | https://xerces.apache.org/xerces2-j/ |
| Apache License, Version 2.0, The SAX License, The W3C License | XML Commons External Components XML APIs | xml-apis:xml-apis | 1.4.01 | http://xml.apache.org/commons/components/external/ |
| Apache License, Version 2.0 | XML Commons External Components XML APIs Extensions | xml-apis:xml-apis-ext | 1.3.04 | http://xml.apache.org/commons/components/external/ |
| Apache License, Version 2.0 | XML Commons Resolver Component | xml-resolver:xml-resolver | 1.2 | http://xml.apache.org/commons/components/resolver/ |

### Component gitb-ui (user interface)

| Category | License | Dependency | Notes |
| --- | --- | --- | --- |
| Apache | [ASF 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | cglib # cglib # 3.3.0 | <notextile></notextile> |
| Apache | [Apache]() | [commons-collections # commons-collections # 3.2.2](http://commons.apache.org/collections/) | <notextile></notextile> |
| Apache | [Apache]() | [commons-digester # commons-digester # 2.1](http://commons.apache.org/digester/) | <notextile></notextile> |
| Apache | [Apache]() | [org.apache.commons # commons-exec # 1.3](http://commons.apache.org/proper/commons-exec/) | <notextile></notextile> |
| Apache | [Apache]() | org.apereo.cas.client # cas-client-core # 4.0.4 | <notextile></notextile> |
| Apache | [Apache]() | org.apereo.cas.client # cas-client-support-saml # 4.0.4 | <notextile></notextile> |
| Apache | [Apache]() | [xml-resolver # xml-resolver # 1.2](http://xml.apache.org/commons/components/resolver/) | <notextile></notextile> |
| Apache | [Apache 2](http://www.apache.org/licenses/LICENSE-2.0.txt) | io.fluentlenium # fluentlenium-core # 6.0.0 | <notextile></notextile> |
| Apache | [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [com.google.auto.service # auto-service-annotations # 1.1.1](https://github.com/google/auto/tree/main/service) | <notextile></notextile> |
| Apache | [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | com.google.errorprone # error_prone_annotations # 2.36.0 | <notextile></notextile> |
| Apache | [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html) | [net.debasishg # redisclient_2.13 # 3.42](https://github.com/debasishg/scala-redis) | <notextile></notextile> |
| Apache | [Apache License version 2.0](https://www.apache.org/licenses/LICENSE-2.0) | [org.xmlresolver # xmlresolver # 5.2.2](https://github.com/xmlresolver/xmlresolver) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [com.github.stephenc.jcip # jcip-annotations # 1.0-1](http://stephenc.github.com/jcip-annotations) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | com.google.guava # failureaccess # 1.0.3 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [com.google.guava # guava # 33.4.6-jre](https://github.com/google/guava) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [com.google.j2objc # j2objc-annotations # 3.0.0](https://github.com/google/j2objc/) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | com.googlecode.owasp-java-html-sanitizer # java10-shim # 20240325.1 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | com.googlecode.owasp-java-html-sanitizer # java8-shim # 20240325.1 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | com.googlecode.owasp-java-html-sanitizer # owasp-java-html-sanitizer # 20240325.1 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [com.password4j # password4j # 1.8.2](https://password4j.com/) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [com.shapesecurity # salvation2 # 3.0.1](http://cspvalidator.org) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [commons-beanutils # commons-beanutils # 1.9.4](https://commons.apache.org/proper/commons-beanutils/) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [commons-net # commons-net # 3.9.0](https://commons.apache.org/proper/commons-net/) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [commons-validator # commons-validator # 1.7](http://commons.apache.org/proper/commons-validator/) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | de.rototor.pdfbox # graphics2d # 0.32 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://apache.org/licenses/LICENSE-2.0) | dev.failsafe # failsafe # 3.3.2 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [io.appium # java-client # 8.2.1](http://appium.io) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) | io.jsonwebtoken # jjwt-api # 0.11.5 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) | io.jsonwebtoken # jjwt-impl # 0.11.5 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) | io.jsonwebtoken # jjwt-jackson # 0.11.5 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [javax.cache # cache-api # 1.1.1](https://github.com/jsr107/jsr107spec) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | net.bytebuddy # byte-buddy # 1.14.5 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [net.lingala.zip4j # zip4j # 2.11.5](https://github.com/srikanth-lingala/zip4j) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [net.sourceforge.htmlunit # htmlunit # 2.70.0](http://htmlunit.sourceforge.net) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [net.sourceforge.htmlunit # htmlunit-cssparser # 1.14.0](https://github.com/HtmlUnit/htmlunit-cssparser) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [net.sourceforge.htmlunit # htmlunit-xpath # 2.70.0](http://htmlunit.sourceforge.net) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [net.sourceforge.htmlunit # neko-htmlunit # 2.70.0](http://htmlunit.sourceforge.net) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.commons # commons-pool2 # 2.8.0](https://commons.apache.org/proper/commons-pool/) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.httpcomponents # httpclient # 4.5.14](http://hc.apache.org/httpcomponents-client-ga) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.httpcomponents # httpcore # 4.4.16](http://hc.apache.org/httpcomponents-core-ga) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.httpcomponents # httpmime # 4.5.14](http://hc.apache.org/httpcomponents-client-ga) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.httpcomponents.client5 # httpclient5 # 5.1.3 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.httpcomponents.core5 # httpcore5 # 5.1.3 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.httpcomponents.core5 # httpcore5-h2 # 5.1.3 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.pdfbox # fontbox # 2.0.31](http://pdfbox.apache.org/) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.pdfbox # pdfbox # 2.0.31 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.pdfbox # xmpbox # 2.0.24 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.ws.xmlschema # xmlschema-core # 2.3.1 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://localhost) | org.atteo.classindex # classindex # 3.13 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.codehaus.plexus # plexus-utils # 3.5.1 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://flywaydb.org/licenses/flyway-oss) | org.flywaydb # flyway-core # 11.7.2 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://flywaydb.org/licenses/flyway-oss) | org.flywaydb # flyway-mysql # 11.7.2 | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](https://github.com/flyway/flyway-play/blob/master/LICENSE.txt) | [org.flywaydb # flyway-play_2.13 # 9.1.0](https://github.com/flyway/flyway-play) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.freemarker # freemarker # 2.3.34](https://freemarker.apache.org/) | <notextile></notextile> |
| Apache | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0) | [org.webjars # bootstrap # 5.3.5](http://webjars.org) | <notextile></notextile> |
| Apache | [Apache Software License - Version 2.0](http://www.apache.org/licenses/LICENSE-2.0) | org.eclipse.jetty # jetty-client # 9.4.50.v20221201 | <notextile></notextile> |
| Apache | [Apache Software License - Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) | org.eclipse.jetty # jetty-http # 12.0.16 | <notextile></notextile> |
| Apache | [Apache Software License - Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) | org.eclipse.jetty # jetty-io # 12.0.16 | <notextile></notextile> |
| Apache | [Apache Software License - Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) | org.eclipse.jetty # jetty-security # 12.0.16 | <notextile></notextile> |
| Apache | [Apache Software License - Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) | org.eclipse.jetty # jetty-server # 12.0.16 | <notextile></notextile> |
| Apache | [Apache Software License - Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) | org.eclipse.jetty # jetty-session # 12.0.16 | <notextile></notextile> |
| Apache | [Apache Software License - Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) | org.eclipse.jetty # jetty-util # 12.0.16 | <notextile></notextile> |
| Apache | [Apache Software License - Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) | org.eclipse.jetty.ee10 # jetty-ee10-servlet # 12.0.16 | <notextile></notextile> |
| Apache | [Apache Software License - Version 2.0](http://www.apache.org/licenses/LICENSE-2.0) | org.eclipse.jetty.websocket # websocket-api # 9.4.50.v20221201 | <notextile></notextile> |
| Apache | [Apache Software License - Version 2.0](http://www.apache.org/licenses/LICENSE-2.0) | org.eclipse.jetty.websocket # websocket-client # 9.4.50.v20221201 | <notextile></notextile> |
| Apache | [Apache Software License - Version 2.0](http://www.apache.org/licenses/LICENSE-2.0) | org.eclipse.jetty.websocket # websocket-common # 9.4.50.v20221201 | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | com.google.code.gson # gson # 2.10 | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | [com.typesafe # config # 1.4.3](https://github.com/lightbend/config) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [com.typesafe # ssl-config-core_2.13 # 0.6.1](https://github.com/lightbend/ssl-config) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [commons-codec # commons-codec # 1.17.1](https://commons.apache.org/proper/commons-codec/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [commons-logging # commons-logging # 1.3.2](https://commons.apache.org/proper/commons-logging/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [net.java.dev.jna # jna # 5.14.0](https://github.com/java-native-access/jna) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.commons # commons-configuration2 # 2.11.0](https://commons.apache.org/proper/commons-configuration/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.commons # commons-lang3 # 3.17.0](https://commons.apache.org/proper/commons-lang/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.commons # commons-text # 1.13.1](https://commons.apache.org/proper/commons-text) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.cxf # cxf-core # 4.1.1](https://cxf.apache.org) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.cxf # cxf-rt-bindings-soap # 4.1.1](https://cxf.apache.org) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.cxf # cxf-rt-bindings-xml # 4.1.1](https://cxf.apache.org) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.cxf # cxf-rt-databinding-jaxb # 4.1.1](https://cxf.apache.org) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.cxf # cxf-rt-frontend-jaxws # 4.1.1](https://cxf.apache.org) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.cxf # cxf-rt-frontend-simple # 4.1.1](https://cxf.apache.org) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.cxf # cxf-rt-transports-http # 4.1.1](https://cxf.apache.org) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.cxf # cxf-rt-transports-http-jetty # 4.1.1](https://cxf.apache.org) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.cxf # cxf-rt-ws-addr # 4.1.1](https://cxf.apache.org) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.cxf # cxf-rt-ws-policy # 4.1.1](https://cxf.apache.org) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.cxf # cxf-rt-wsdl # 4.1.1](https://cxf.apache.org) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.maven # maven-model # 3.9.1 | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.neethi # neethi # 3.2.1](https://ws.apache.org/neethi/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | [org.apache.pekko # pekko-actor-typed_2.13 # 1.1.3](https://pekko.apache.org/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | [org.apache.pekko # pekko-actor_2.13 # 1.1.3](https://pekko.apache.org/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | org.apache.pekko # pekko-http-core_2.13 # 1.0.1 | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | org.apache.pekko # pekko-parsing_2.13 # 1.0.1 | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | [org.apache.pekko # pekko-pki_2.13 # 1.1.3](https://pekko.apache.org/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | [org.apache.pekko # pekko-protobuf-v3_2.13 # 1.1.3](https://pekko.apache.org/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | [org.apache.pekko # pekko-remote_2.13 # 1.1.3](https://pekko.apache.org/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | [org.apache.pekko # pekko-serialization-jackson_2.13 # 1.1.3](https://pekko.apache.org/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | [org.apache.pekko # pekko-slf4j_2.13 # 1.1.3](https://pekko.apache.org/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | [org.apache.pekko # pekko-stream_2.13 # 1.1.3](https://pekko.apache.org/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.tika # tika-core # 3.1.0](https://tika.apache.org/) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.parboiled # parboiled_2.13 # 2.5.0](http://parboiled.org) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # cachecontrol_2.13 # 3.0.1](https://github.com/playframework/cachecontrol) | <notextile></notextile> |
| Apache | [Apache-2.0](http://opensource.org/licenses/Apache-2.0) | [org.playframework # play-ahc-ws-standalone_2.13 # 3.0.7](https://github.com/playframework/play-ws/) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-ahc-ws_2.13 # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-build-link # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-cache_2.13 # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-configuration_2.13 # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-ehcache_2.13 # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-exceptions # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-filters-helpers_2.13 # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-functional_2.13 # 3.0.4](https://github.com/playframework/play-json) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-guice_2.13 # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-jdbc-api_2.13 # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-json_2.13 # 3.0.4](https://github.com/playframework/play-json) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-logback_2.13 # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-pekko-http-server_2.13 # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-server_2.13 # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-slick_2.13 # 6.2.0](https://github.com/playframework/play-slick) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-streams_2.13 # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-test_2.13 # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](http://opensource.org/licenses/Apache-2.0) | [org.playframework # play-ws-standalone-json_2.13 # 3.0.7](https://github.com/playframework/play-ws/) | <notextile></notextile> |
| Apache | [Apache-2.0](http://opensource.org/licenses/Apache-2.0) | [org.playframework # play-ws-standalone-xml_2.13 # 3.0.7](https://github.com/playframework/play-ws/) | <notextile></notextile> |
| Apache | [Apache-2.0](http://opensource.org/licenses/Apache-2.0) | [org.playframework # play-ws-standalone_2.13 # 3.0.7](https://github.com/playframework/play-ws/) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play-ws_2.13 # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework # play_2.13 # 3.0.7](https://github.com/playframework/playframework) | <notextile></notextile> |
| Apache | [Apache-2.0](http://opensource.org/licenses/Apache-2.0) | [org.playframework # shaded-asynchttpclient # 3.0.7](https://github.com/playframework/play-ws/) | <notextile></notextile> |
| Apache | [Apache-2.0](http://opensource.org/licenses/Apache-2.0) | [org.playframework # shaded-oauth # 3.0.7](https://github.com/playframework/play-ws/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.html) | [org.playframework.twirl # twirl-api_2.13 # 2.0.8](https://github.com/playframework/twirl) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | [org.scala-lang # scala-compiler # 2.13.14](https://www.scala-lang.org/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | [org.scala-lang # scala-library # 2.13.14](https://www.scala-lang.org/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | [org.scala-lang # scala-reflect # 2.13.14](https://www.scala-lang.org/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | [org.scala-lang.modules # scala-parser-combinators_2.13 # 1.1.2](http://www.scala-lang.org/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | [org.scala-lang.modules # scala-xml_2.13 # 2.2.0](http://www.scala-lang.org/) | <notextile></notextile> |
| Apache | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.slf4j # jcl-over-slf4j # 2.0.17](http://www.slf4j.org) | <notextile></notextile> |
| Apache | [Apache-2.0](http://localhost) | [org.webjars # swagger-ui # 5.21.0](https://www.webjars.org) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [com.fasterxml.woodstox # woodstox-core # 7.1.0](https://github.com/FasterXML/woodstox) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) | [com.hierynomus # asn-one # 0.6.0](https://github.com/hierynomus/asn-one) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [io.opentelemetry # opentelemetry-api # 1.28.0](https://github.com/open-telemetry/opentelemetry-java) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [io.opentelemetry # opentelemetry-api-events # 1.28.0-alpha](https://github.com/open-telemetry/opentelemetry-java) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [io.opentelemetry # opentelemetry-context # 1.28.0](https://github.com/open-telemetry/opentelemetry-java) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [io.opentelemetry # opentelemetry-exporter-logging # 1.28.0](https://github.com/open-telemetry/opentelemetry-java) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [io.opentelemetry # opentelemetry-extension-incubator # 1.28.0-alpha](https://github.com/open-telemetry/opentelemetry-java) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [io.opentelemetry # opentelemetry-sdk # 1.28.0](https://github.com/open-telemetry/opentelemetry-java) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [io.opentelemetry # opentelemetry-sdk-common # 1.28.0](https://github.com/open-telemetry/opentelemetry-java) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [io.opentelemetry # opentelemetry-sdk-extension-autoconfigure # 1.28.0](https://github.com/open-telemetry/opentelemetry-java) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [io.opentelemetry # opentelemetry-sdk-extension-autoconfigure-spi # 1.28.0](https://github.com/open-telemetry/opentelemetry-java) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [io.opentelemetry # opentelemetry-sdk-logs # 1.28.0](https://github.com/open-telemetry/opentelemetry-java) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [io.opentelemetry # opentelemetry-sdk-metrics # 1.28.0](https://github.com/open-telemetry/opentelemetry-java) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [io.opentelemetry # opentelemetry-sdk-trace # 1.28.0](https://github.com/open-telemetry/opentelemetry-java) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [io.opentelemetry # opentelemetry-semconv # 1.28.0-alpha](https://github.com/open-telemetry/opentelemetry-java) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [org.agrona # agrona # 1.22.0](https://github.com/real-logic/agrona) | <notextile></notextile> |
| Apache | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.jspecify # jspecify # 1.0.0](http://jspecify.org/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [com.fasterxml.jackson.core # jackson-annotations # 2.18.3](https://github.com/FasterXML/jackson) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [com.fasterxml.jackson.core # jackson-core # 2.18.3](https://github.com/FasterXML/jackson-core) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [com.fasterxml.jackson.core # jackson-databind # 2.18.3](https://github.com/FasterXML/jackson) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [com.fasterxml.jackson.dataformat # jackson-dataformat-cbor # 2.17.3](https://github.com/FasterXML/jackson-dataformats-binary) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [com.fasterxml.jackson.dataformat # jackson-dataformat-toml # 2.15.2](https://github.com/FasterXML/jackson-dataformats-text) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | com.fasterxml.jackson.datatype # jackson-datatype-jdk8 # 2.17.3 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | com.fasterxml.jackson.datatype # jackson-datatype-jsr310 # 2.18.3 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [com.fasterxml.jackson.module # jackson-module-jakarta-xmlbind-annotations # 2.18.3](https://github.com/FasterXML/jackson-modules-base) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | com.fasterxml.jackson.module # jackson-module-parameter-names # 2.17.3 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [com.fasterxml.jackson.module # jackson-module-scala_2.13 # 2.18.3](https://github.com/FasterXML/jackson-module-scala) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [com.google.code.findbugs # jsr305 # 3.0.2](http://findbugs.sourceforge.net/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | com.google.guava # listenablefuture # 9999.0-empty-to-avoid-conflict-with-guava | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | com.google.inject # guice # 6.0.0 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | com.google.inject.extensions # guice-assistedinject # 6.0.0 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [com.nimbusds # nimbus-jose-jwt # 9.37.3](https://bitbucket.org/connect2id/nimbus-jose-jwt) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) | [com.zaxxer # HikariCP # 6.2.1](https://github.com/brettwooldridge/HikariCP) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | io.github.java-diff-utils # java-diff-utils # 4.12 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [jakarta.inject # jakarta.inject-api # 2.0.1](https://github.com/eclipse-ee4j/injection-api) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [javax.inject # javax.inject # 1](http://code.google.com/p/atinject/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](src/assemble/EHCACHE-CORE-LICENSE.txt) | [net.sf.ehcache # ehcache # 2.10.9.2](http://ehcache.org) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-anim # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-awt-util # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-bridge # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-codec # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-constants # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-css # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-dom # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-ext # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-gvt # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-i18n # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-parser # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-script # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-shared-resources # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-svg-dom # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-svggen # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-transcoder # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-util # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.apache.xmlgraphics # batik-xml # 1.17 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.apache.xmlgraphics # xmlgraphics-commons # 2.9](http://xmlgraphics.apache.org/commons/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.ehcache # jcache # 1.0.1 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.jasypt # jasypt # 1.9.3](http://www.jasypt.org) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.lz4 # lz4-java # 1.8.0](https://github.com/lz4/lz4-java) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.pac4j # pac4j-cas # 6.1.2 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.pac4j # pac4j-core # 6.1.2 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | org.pac4j # play-pac4j_2.13 # 12.0.2-PLAY3.0 | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.reflections # reflections # 0.10.2](http://github.com/ronmamo/reflections) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.seleniumhq.selenium # htmlunit-driver # 4.13.0](https://github.com/SeleniumHQ/htmlunit-driver) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.seleniumhq.selenium # selenium-api # 4.14.1](https://selenium.dev/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.seleniumhq.selenium # selenium-devtools-v85 # 4.14.1](https://selenium.dev/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.seleniumhq.selenium # selenium-firefox-driver # 4.14.1](https://selenium.dev/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.seleniumhq.selenium # selenium-http # 4.14.1](https://selenium.dev/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.seleniumhq.selenium # selenium-json # 4.14.1](https://selenium.dev/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.seleniumhq.selenium # selenium-manager # 4.14.1](https://selenium.dev/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.seleniumhq.selenium # selenium-os # 4.14.1](https://selenium.dev/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.seleniumhq.selenium # selenium-remote-driver # 4.14.1](https://selenium.dev/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [org.seleniumhq.selenium # selenium-support # 4.14.1](https://selenium.dev/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [xerces # xercesImpl # 2.12.2](https://xerces.apache.org/xerces2-j/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [xml-apis # xml-apis # 1.4.01](http://xml.apache.org/commons/components/external/) | <notextile></notextile> |
| Apache | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [xml-apis # xml-apis-ext # 1.3.04](http://xml.apache.org/commons/components/external/) | <notextile></notextile> |
| BSD | [BSD](LICENSE.txt) | com.thoughtworks.paranamer # paranamer # 2.8 | <notextile></notextile> |
| BSD | [BSD-3-Clause](https://asm.ow2.io/license.html) | [org.ow2.asm # asm # 9.7.1](http://asm.ow2.io/) | <notextile></notextile> |
| BSD | [EDL 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | com.sun.activation # jakarta.activation # 2.0.1 | <notextile></notextile> |
| BSD | [EDL 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | com.sun.mail # jakarta.mail # 2.0.1 | <notextile></notextile> |
| BSD | [EDL 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | [jakarta.activation # jakarta.activation-api # 2.1.3](https://github.com/jakartaee/jaf-api) | <notextile></notextile> |
| BSD | [EDL 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | jakarta.mail # jakarta.mail-api # 2.1.3 | <notextile></notextile> |
| BSD | [EDL 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | org.eclipse.angus # angus-activation # 2.0.2 | <notextile></notextile> |
| BSD | [EDL 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | org.eclipse.angus # angus-mail # 2.0.3 | <notextile></notextile> |
| BSD | [Eclipse Distribution License - v 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | com.sun.istack # istack-commons-runtime # 4.1.2 | <notextile></notextile> |
| BSD | [Eclipse Distribution License - v 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | [com.sun.xml.bind # jaxb-core # 4.0.5](https://eclipse-ee4j.github.io/jaxb-ri/) | <notextile></notextile> |
| BSD | [Eclipse Distribution License - v 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | [com.sun.xml.bind # jaxb-impl # 4.0.5](https://eclipse-ee4j.github.io/jaxb-ri/) | <notextile></notextile> |
| BSD | [Eclipse Distribution License - v 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | com.sun.xml.messaging.saaj # saaj-impl # 3.0.4 | <notextile></notextile> |
| BSD | [Eclipse Distribution License - v 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | [jakarta.jws # jakarta.jws-api # 3.0.0](https://github.com/eclipse-ee4j/jws-api) | <notextile></notextile> |
| BSD | [Eclipse Distribution License - v 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | jakarta.xml.bind # jakarta.xml.bind-api # 4.0.2 | <notextile></notextile> |
| BSD | [Eclipse Distribution License - v 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | [jakarta.xml.soap # jakarta.xml.soap-api # 3.0.2](https://github.com/jakartaee/saaj-api) | <notextile></notextile> |
| BSD | [Eclipse Distribution License - v 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | [jakarta.xml.ws # jakarta.xml.ws-api # 4.0.2](https://github.com/jakartaee/jax-ws-api) | <notextile></notextile> |
| BSD | [Eclipse Distribution License - v 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | [org.glassfish.jaxb # jaxb-core # 4.0.5](https://eclipse-ee4j.github.io/jaxb-ri/) | <notextile></notextile> |
| BSD | [Eclipse Distribution License - v 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | [org.glassfish.jaxb # jaxb-runtime # 4.0.5](https://eclipse-ee4j.github.io/jaxb-ri/) | <notextile></notextile> |
| BSD | [Eclipse Distribution License - v 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | [org.glassfish.jaxb # txw2 # 4.0.5](https://eclipse-ee4j.github.io/jaxb-ri/) | <notextile></notextile> |
| BSD | [Eclipse Distribution License - v 1.0](http://www.eclipse.org/org/documents/edl-v10.php) | org.jvnet.staxex # stax-ex # 2.1.0 | <notextile></notextile> |
| BSD | [The BSD 2-Clause License](http://www.opensource.org/licenses/bsd-license.php) | [org.codehaus.woodstox # stax2-api # 4.2.2](http://github.com/FasterXML/stax2-api) | <notextile></notextile> |
| BSD | [Two-clause BSD-style license](https://github.com/slick/slick/blob/main/LICENSE.txt) | [com.typesafe.slick # slick-hikaricp_2.13 # 3.6.0](https://scala-slick.org) | <notextile></notextile> |
| BSD | [Two-clause BSD-style license](https://github.com/slick/slick/blob/main/LICENSE.txt) | [com.typesafe.slick # slick_2.13 # 3.6.0](https://scala-slick.org) | <notextile></notextile> |
| Bouncy Castle License | [Bouncy Castle Licence](https://www.bouncycastle.org/licence.html) | [org.bouncycastle # bcmail-jdk18on # 1.80](https://www.bouncycastle.org/download/bouncy-castle-java/) | <notextile></notextile> |
| Bouncy Castle License | [Bouncy Castle Licence](https://www.bouncycastle.org/licence.html) | [org.bouncycastle # bcpkix-jdk18on # 1.80](https://www.bouncycastle.org/download/bouncy-castle-java/) | <notextile></notextile> |
| Bouncy Castle License | [Bouncy Castle Licence](https://www.bouncycastle.org/licence.html) | [org.bouncycastle # bcprov-jdk18on # 1.80](https://www.bouncycastle.org/download/bouncy-castle-java/) | <notextile></notextile> |
| Bouncy Castle License | [Bouncy Castle Licence](https://www.bouncycastle.org/licence.html) | [org.bouncycastle # bcutil-jdk18on # 1.80](https://www.bouncycastle.org/download/bouncy-castle-java/) | <notextile></notextile> |
| Common Public License | [CPL](http://www.opensource.org/licenses/cpl1.0.txt) | [wsdl4j # wsdl4j # 1.6.3](http://sf.net/projects/wsdl4j) | <notextile>Used transitively by CXF, see (https://www.apache.org/legal/resolved.html#category-b).</notextile> |
| EPL | [Eclipse Public License 1.0](http://www.eclipse.org/legal/epl-v10.html) | [junit # junit # 4.13.2](http://junit.org) | <notextile></notextile> |
| GPL | [The GNU General Public License, v2 with Universal FOSS Exception, v1.0](http://localhost) | [com.mysql # mysql-connector-j # 9.2.0](http://dev.mysql.com/doc/connector-j/en/) | <notextile>The Universal FOSS Exception allows its usage as it is used unchanged.</notextile> |
| GPL with Classpath Extension | [GPL with Classpath Extension]() | [jakarta.annotation # jakarta.annotation-api # 2.1.1](https://projects.eclipse.org/projects/ee4j.ca) | <notextile></notextile> |
| GPL with Classpath Extension | [GPL with Classpath Extension]() | [jakarta.servlet # jakarta.servlet-api # 6.0.0](https://projects.eclipse.org/projects/ee4j.servlet) | <notextile></notextile> |
| LGPL | [GNU Lesser General Public License](http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html) | ch.qos.logback # logback-classic # 1.5.17 | <notextile></notextile> |
| LGPL | [GNU Lesser General Public License](http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html) | ch.qos.logback # logback-core # 1.5.17 | <notextile></notextile> |
| LGPL | [GNU Lesser General Public License (LGPL), version 2.1 or later](http://www.gnu.org/licenses/lgpl.html) | com.openhtmltopdf # openhtmltopdf-core # 1.0.10 | <notextile></notextile> |
| LGPL | [GNU Lesser General Public License (LGPL), version 2.1 or later](http://www.gnu.org/licenses/lgpl.html) | com.openhtmltopdf # openhtmltopdf-pdfbox # 1.0.10 | <notextile></notextile> |
| LGPL | [GNU Lesser General Public License (LGPL), version 2.1 or later](http://www.gnu.org/licenses/lgpl.html) | com.openhtmltopdf # openhtmltopdf-slf4j # 1.0.10 | <notextile></notextile> |
| LGPL | [GNU Lesser General Public License (LGPL), version 2.1 or later](http://www.gnu.org/licenses/lgpl.html) | com.openhtmltopdf # openhtmltopdf-svg-support # 1.0.10 | <notextile></notextile> |
| MIT | [MIT](https://opensource.org/license/mit) | [org.slf4j # jul-to-slf4j # 2.0.17](http://www.slf4j.org) | <notextile></notextile> |
| MIT | [MIT](https://opensource.org/license/mit) | [org.slf4j # slf4j-api # 2.0.17](http://www.slf4j.org) | <notextile></notextile> |
| MIT | [MIT License](http://www.opensource.org/licenses/mit-license.php) | org.brotli # dec # 0.1.2 | <notextile></notextile> |
| MIT | [MIT License](https://github.com/jquery/jquery/blob/master/MIT-LICENSE.txt) | [org.webjars # jquery # 3.7.1](http://webjars.org) | <notextile></notextile> |
| MIT | [MIT-0](https://spdx.org/licenses/MIT-0.html) | [org.reactivestreams # reactive-streams # 1.0.4](http://www.reactive-streams.org/) | <notextile></notextile> |
| MIT | [The MIT License](https://jsoup.org/license) | [org.jsoup # jsoup # 1.15.4](https://jsoup.org/) | <notextile></notextile> |
| MIT | [The MIT License](https://projectlombok.org/LICENSE) | [org.projectlombok # lombok # 1.18.36](https://projectlombok.org) | <notextile></notextile> |
| Mozilla | [MPL 1.1](http://www.mozilla.org/MPL/MPL-1.1.html) | [org.javassist # javassist # 3.28.0-GA](http://www.javassist.org/) | <notextile></notextile> |
| Mozilla | [Mozilla Public License Version 2.0](http://www.mozilla.org/MPL/2.0/) | [net.sf.saxon # Saxon-HE # 12.4](http://www.saxonica.com/) | <notextile></notextile> |
| Mozilla | [Mozilla Public License, Version 2.0](http://www.mozilla.org/MPL/2.0/index.txt) | [net.sourceforge.htmlunit # htmlunit-core-js # 2.70.0](http://htmlunit.sourceforge.net) | <notextile></notextile> |
| Public Domain | [Public Domain](http://localhost) | [aopalliance # aopalliance # 1.0](http://aopalliance.sourceforge.net) | <notextile></notextile> |

### Component gitb-ui frontend (user interface frontend)

| License type                    | Name                              | Installed version | Link                                                        |
| :------------------------------ | :-------------------------------- | :---------------- | :---------------------------------------------------------- |
| MIT                             | @angular/animations               | 19.2.13           | git+https://github.com/angular/angular.git                  |
| MIT                             | @angular/cdk                      | 19.2.17           | git+https://github.com/angular/components.git               |
| MIT                             | @angular/common                   | 19.2.13           | git+https://github.com/angular/angular.git                  |
| MIT                             | @angular/compiler                 | 19.2.13           | git+https://github.com/angular/angular.git                  |
| MIT                             | @angular/core                     | 19.2.13           | git+https://github.com/angular/angular.git                  |
| MIT                             | @angular/forms                    | 19.2.13           | git+https://github.com/angular/angular.git                  |
| MIT                             | @angular/platform-browser         | 19.2.13           | git+https://github.com/angular/angular.git                  |
| MIT                             | @angular/platform-browser-dynamic | 19.2.13           | git+https://github.com/angular/angular.git                  |
| MIT                             | @angular/router                   | 19.2.13           | git+https://github.com/angular/angular.git                  |
| MIT                             | @ctrl/ngx-codemirror              | 7.0.0             | git+https://github.com/scttcper/ngx-codemirror.git          |
| (CC-BY-4.0 AND OFL-1.1 AND MIT) | @fortawesome/fontawesome-free     | 6.7.2             | git+https://github.com/FortAwesome/Font-Awesome.git         |
| MIT                             | @tinymce/tinymce-angular          | 8.0.1             | git+https://github.com/tinymce/tinymce-angular.git          |
| MIT                             | angular2-notifications            | 16.0.1            | git+https://github.com/flauc/angular2-notifications.git     |
| MIT                             | bootstrap                         | 5.3.3             | git+https://github.com/twbs/bootstrap.git                   |
| MIT                             | codemirror                        | 5.65.18           | git+https://github.com/codemirror/CodeMirror.git            |
| MIT                             | file-saver                        | 2.0.5             | git+https://github.com/eligrey/FileSaver.js.git             |
| MIT                             | lodash                            | 4.17.21           | git+https://github.com/lodash/lodash.git                    |
| MIT                             | marked                            | 15.0.12           | git://github.com/markedjs/marked.git                        |
| MIT                             | ngx-bootstrap                     | 19.0.2            | git+ssh://git@github.com/valor-software/ngx-bootstrap.git   |
| MIT                             | ngx-color-picker                  | 17.0.0            | git+https://github.com/zefoy/ngx-color-picker.git           |
| MIT                             | ngx-cookie-service                | 19.1.0            | git+https://github.com/stevermeister/ngx-cookie-service.git |
| MIT                             | ngx-markdown                      | 19.1.1            | git+https://github.com/jfcere/ngx-markdown.git              |
| Apache-2.0                      | rxjs                              | 7.8.1             | git+https://github.com/reactivex/rxjs.git                   |
| MIT                             | tinymce                           | 6.8.4             | git+https://github.com/tinymce/tinymce.git                  |
| MIT                             | zone.js                           | 0.15.0            | git://github.com/angular/angular.git                        |

## Third-party library licences

This section lists the complete licence texts per licence type that, unless otherwise stated, are not specific to a
given library. Library copyright attributions are listed in the following section.

The mapping of licences listed here to libraries is provided by the previous mapping tables.

### Apache License, Version 2.0

```
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

### Bouncy Castle License

```
Copyright (c) 2000 - 2024 The Legion of the Bouncy Castle Inc. (https://www.bouncycastle.org)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

### Common Public License Version 1.0

```
Common Public License Version 1.0

THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS COMMON PUBLIC LICENSE (“AGREEMENT”). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM CONSTITUTES RECIPIENT’S ACCEPTANCE OF THIS AGREEMENT.

1. DEFINITIONS “Contribution” means:

a) in the case of the initial Contributor, the initial code and documentation distributed under this Agreement, and

b) in the case of each subsequent Contributor: i) changes to the Program, and ii) additions to the Program; where such changes and/or additions to the Program originate from and are distributed by that particular Contributor. A Contribution ‘originates’ from a Contributor if it was added to the Program by such Contributor itself or anyone acting on such Contributor’s behalf. Contributions do not include additions to the Program which: (i) are separate modules of software distributed in conjunction with the Program under their own license agreement, and (ii) are not derivative works of the Program. “Contributor” means any person or entity that distributes the Program. “Licensed Patents ” mean patent claims licensable by a Contributor which are necessarily infringed by the use or sale of its Contribution alone or when combined with the Program. “Program” means the Contributions distributed in accordance with this Agreement. “Recipient” means anyone who receives the Program under this Agreement, including all Contributors.

2. GRANT OF RIGHTS

a) Subject to the terms of this Agreement, each Contributor hereby grants Recipient a non-exclusive, worldwide, royalty-free copyright license to reproduce, prepare derivative works of, publicly display, publicly perform, distribute and sublicense the Contribution of such Contributor, if any, and such derivative works, in source code and object code form.

b) Subject to the terms of this Agreement, each Contributor hereby grants Recipient a non-exclusive, worldwide, royalty-free patent license under Licensed Patents to make, use, sell, offer to sell, import and otherwise transfer the Contribution of such Contributor, if any, in source code and object code form. This patent license shall apply to the combination of the Contribution and the Program if, at the time the Contribution is added by the Contributor, such addition of the Contribution causes such combination to be covered by the Licensed Patents. The patent license shall not apply to any other combinations which include the Contribution. No hardware per se is licensed hereunder.

c) Recipient understands that although each Contributor grants the licenses to its Contributions set forth herein, no assurances are provided by any Contributor that the Program does not infringe the patent or other intellectual property rights of any other entity. Each Contributor disclaims any liability to Recipient for claims brought by any other entity based on infringement of intellectual property rights or otherwise. As a condition to exercising the rights and licenses granted hereunder, each Recipient hereby assumes sole responsibility to secure any other intellectual property rights needed, if any. For example, if a third party patent license is required to allow Recipient to distribute the Program, it is Recipient’s responsibility to acquire that license before distributing the Program.

d) Each Contributor represents that to its knowledge it has sufficient copyright rights in its Contribution, if any, to grant the copyright license set forth in this Agreement.

3. REQUIREMENTS A Contributor may choose to distribute the Program in object code form under its own license agreement, provided that:

a) it complies with the terms and conditions of this Agreement; and

b) its license agreement:

i) effectively disclaims on behalf of all Contributors all warranties and conditions, express and implied, including warranties or conditions of title and non-infringement, and implied warranties or conditions of merchantability and fitness for a particular purpose;

ii) effectively excludes on behalf of all Contributors all liability for damages, including direct, indirect, special, incidental and consequential damages, such as lost profits;

iii) states that any provisions which differ from this Agreement are offered by that Contributor alone and not by any other party; and

iv) states that source code for the Program is available from such Contributor, and informs licensees how to obtain it in a reasonable manner on or through a medium customarily used for software exchange. When the Program is made available in source code form: a) it must be made available under this Agreement; and b) a copy of this Agreement must be included with each copy of the Program. Contributors may not remove or alter any copyright notices contained within the Program. Each Contributor must identify itself as the originator of its Contribution, if any, in a manner that reasonably allows subsequent Recipients to identify the originator of the Contribution.

4. COMMERCIAL DISTRIBUTION Commercial distributors of software may accept certain responsibilities with respect to end users, business partners and the like. While this license is intended to facilitate the commercial use of the Program, the Contributor who includes the Program in a commercial product offering should do so in a manner which does not create potential liability for other Contributors. Therefore, if a Contributor includes the Program in a commercial product offering, such Contributor (“Commercial Contributor”) hereby agrees to defend and indemnify every other Contributor (“Indemnified Contributor”) against any losses, damages and costs (collectively “Losses”) arising from claims, lawsuits and other legal actions brought by a third party against the Indemnified Contributor to the extent caused by the acts or omissions of such Commercial Contributor in connection with its distribution of the Program in a commercial product offering. The obligations in this section do not apply to any claims or Losses relating to any actual or alleged intellectual property infringement. In order to qualify, an Indemnified Contributor must:

a) promptly notify the Commercial Contributor in writing of such claim, and

b) allow the Commercial Contributor to control, and cooperate with the Commercial Contributor in, the defense and any related settlement negotiations. The Indemnified Contributor may participate in any such claim at its own expense. For example, a Contributor might include the Program in a commercial product offering, Product X. That Contributor is then a Commercial Contributor. If that Commercial Contributor then makes performance claims, or offers warranties related to Product X, those performance claims and warranties are such Commercial Contributor’s responsibility alone. Under this section, the Commercial Contributor would have to defend claims against the other Contributors related to those performance claims and warranties, and if a court requires any other Contributor to pay any damages as a result, the Commercial Contributor must pay those damages.

5. NO WARRANTY EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, THE PROGRAM IS PROVIDED ON AN “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED INCLUDING, WITHOUT LIMITATION, ANY WARRANTIES OR CONDITIONS OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.

Each Recipient is solely responsible for determining the appropriateness of using and distributing the Program and assumes all risks associated with its exercise of rights under this Agreement, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and unavailability or interruption of operations.

6. DISCLAIMER OF LIABILITY EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, NEITHER RECIPIENT NOR ANY CONTRIBUTORS SHALL HAVE ANY LIABILITY FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING WITHOUT LIMITATION LOST PROFITS), HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OR DISTRIBUTION OF THE PROGRAM OR THE EXERCISE OF ANY RIGHTS GRANTED HEREUNDER, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

7. GENERAL If any provision of this Agreement is invalid or unenforceable under applicable law, it shall not affect the validity or enforceability of the remainder of the terms of this Agreement, and without further action by the parties hereto, such provision shall be reformed to the minimum extent necessary to make such provision valid and enforceable. If Recipient institutes patent litigation against a Contributor with respect to a patent applicable to software (including a cross-claim or counterclaim in a lawsuit), then any patent licenses granted by that Contributor to such Recipient under this Agreement shall terminate as of the date such litigation is filed. In addition, if Recipient institutes patent litigation against any entity (including a cross-claim or counterclaim in a lawsuit) alleging that the Program itself (excluding combinations of the Program with other software or hardware) infringes such Recipient’s patent(s), then such Recipient’s rights granted under Section 2(b) shall terminate as of the date such litigation is filed. All Recipient’s rights under this Agreement shall terminate if it fails to comply with any of the material terms or conditions of this Agreement and does not cure such failure in a reasonable period of time after becoming aware of such noncompliance. If all Recipient’s rights under this Agreement terminate, Recipient agrees to cease use and distribution of the Program as soon as reasonably practicable. However, Recipient’s obligations under this Agreement and any licenses granted by Recipient relating to the Program shall continue and survive. Everyone is permitted to copy and distribute copies of this Agreement, but in order to avoid inconsistency the Agreement is copyrighted and may only be modified in the following manner. The Agreement Steward reserves the right to publish new versions (including revisions) of this Agreement from time to time. No one other than the Agreement Steward has the right to modify this Agreement. IBM is the initial Agreement Steward. IBM may assign the responsibility to serve as the Agreement Steward to a suitable separate entity. Each new version of the Agreement will be given a distinguishing version number. The Program (including Contributions) may always be distributed subject to the version of the Agreement under which it was received. In addition, after a new version of the Agreement is published, Contributor may elect to distribute the Program (including its Contributions) under the new version. Except as expressly stated in Sections 2(a) and 2(b) above, Recipient receives no rights or licenses to the intellectual property of any Contributor under this Agreement, whether expressly, by implication, estoppel or otherwise. All rights in the Program not expressly granted under this Agreement are reserved. This Agreement is governed by the laws of the State of New York and the intellectual property laws of the United States of America. No party to this Agreement will bring a legal action under this Agreement more than one year after the cause of action arose. Each party waives its rights to a jury trial in any resulting litigation.
```

### Eclipse Distribution License - v1.0

```
Copyright (c) 2007, Eclipse Foundation, Inc. and its licensors.

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
Neither the name of the Eclipse Foundation, Inc. nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

### Eclipse Public License - v 1.0

```
Eclipse Public License - v 1.0

THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.

1. DEFINITIONS

"Contribution" means:

a) in the case of the initial Contributor, the initial code and documentation distributed under this Agreement, and

b) in the case of each subsequent Contributor:

i) changes to the Program, and

ii) additions to the Program;

where such changes and/or additions to the Program originate from and are distributed by that particular Contributor. A Contribution 'originates' from a Contributor if it was added to the Program by such Contributor itself or anyone acting on such Contributor's behalf. Contributions do not include additions to the Program which: (i) are separate modules of software distributed in conjunction with the Program under their own license agreement, and (ii) are not derivative works of the Program.

"Contributor" means any person or entity that distributes the Program.

"Licensed Patents" mean patent claims licensable by a Contributor which are necessarily infringed by the use or sale of its Contribution alone or when combined with the Program.

"Program" means the Contributions distributed in accordance with this Agreement.

"Recipient" means anyone who receives the Program under this Agreement, including all Contributors.

2. GRANT OF RIGHTS

a) Subject to the terms of this Agreement, each Contributor hereby grants Recipient a non-exclusive, worldwide, royalty-free copyright license to reproduce, prepare derivative works of, publicly display, publicly perform, distribute and sublicense the Contribution of such Contributor, if any, and such derivative works, in source code and object code form.

b) Subject to the terms of this Agreement, each Contributor hereby grants Recipient a non-exclusive, worldwide, royalty-free patent license under Licensed Patents to make, use, sell, offer to sell, import and otherwise transfer the Contribution of such Contributor, if any, in source code and object code form. This patent license shall apply to the combination of the Contribution and the Program if, at the time the Contribution is added by the Contributor, such addition of the Contribution causes such combination to be covered by the Licensed Patents. The patent license shall not apply to any other combinations which include the Contribution. No hardware per se is licensed hereunder.

c) Recipient understands that although each Contributor grants the licenses to its Contributions set forth herein, no assurances are provided by any Contributor that the Program does not infringe the patent or other intellectual property rights of any other entity. Each Contributor disclaims any liability to Recipient for claims brought by any other entity based on infringement of intellectual property rights or otherwise. As a condition to exercising the rights and licenses granted hereunder, each Recipient hereby assumes sole responsibility to secure any other intellectual property rights needed, if any. For example, if a third party patent license is required to allow Recipient to distribute the Program, it is Recipient's responsibility to acquire that license before distributing the Program.

d) Each Contributor represents that to its knowledge it has sufficient copyright rights in its Contribution, if any, to grant the copyright license set forth in this Agreement.

3. REQUIREMENTS

A Contributor may choose to distribute the Program in object code form under its own license agreement, provided that:

a) it complies with the terms and conditions of this Agreement; and

b) its license agreement:

i) effectively disclaims on behalf of all Contributors all warranties and conditions, express and implied, including warranties or conditions of title and non-infringement, and implied warranties or conditions of merchantability and fitness for a particular purpose;

ii) effectively excludes on behalf of all Contributors all liability for damages, including direct, indirect, special, incidental and consequential damages, such as lost profits;

iii) states that any provisions which differ from this Agreement are offered by that Contributor alone and not by any other party; and

iv) states that source code for the Program is available from such Contributor, and informs licensees how to obtain it in a reasonable manner on or through a medium customarily used for software exchange.

When the Program is made available in source code form:

a) it must be made available under this Agreement; and

b) a copy of this Agreement must be included with each copy of the Program.

Contributors may not remove or alter any copyright notices contained within the Program.

Each Contributor must identify itself as the originator of its Contribution, if any, in a manner that reasonably allows subsequent Recipients to identify the originator of the Contribution.

4. COMMERCIAL DISTRIBUTION

Commercial distributors of software may accept certain responsibilities with respect to end users, business partners and the like. While this license is intended to facilitate the commercial use of the Program, the Contributor who includes the Program in a commercial product offering should do so in a manner which does not create potential liability for other Contributors. Therefore, if a Contributor includes the Program in a commercial product offering, such Contributor ("Commercial Contributor") hereby agrees to defend and indemnify every other Contributor ("Indemnified Contributor") against any losses, damages and costs (collectively "Losses") arising from claims, lawsuits and other legal actions brought by a third party against the Indemnified Contributor to the extent caused by the acts or omissions of such Commercial Contributor in connection with its distribution of the Program in a commercial product offering. The obligations in this section do not apply to any claims or Losses relating to any actual or alleged intellectual property infringement. In order to qualify, an Indemnified Contributor must: a) promptly notify the Commercial Contributor in writing of such claim, and b) allow the Commercial Contributor to control, and cooperate with the Commercial Contributor in, the defense and any related settlement negotiations. The Indemnified Contributor may participate in any such claim at its own expense.

For example, a Contributor might include the Program in a commercial product offering, Product X. That Contributor is then a Commercial Contributor. If that Commercial Contributor then makes performance claims, or offers warranties related to Product X, those performance claims and warranties are such Commercial Contributor's responsibility alone. Under this section, the Commercial Contributor would have to defend claims against the other Contributors related to those performance claims and warranties, and if a court requires any other Contributor to pay any damages as a result, the Commercial Contributor must pay those damages.

5. NO WARRANTY

EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, THE PROGRAM IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED INCLUDING, WITHOUT LIMITATION, ANY WARRANTIES OR CONDITIONS OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Each Recipient is solely responsible for determining the appropriateness of using and distributing the Program and assumes all risks associated with its exercise of rights under this Agreement , including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and unavailability or interruption of operations.

6. DISCLAIMER OF LIABILITY

EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, NEITHER RECIPIENT NOR ANY CONTRIBUTORS SHALL HAVE ANY LIABILITY FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING WITHOUT LIMITATION LOST PROFITS), HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OR DISTRIBUTION OF THE PROGRAM OR THE EXERCISE OF ANY RIGHTS GRANTED HEREUNDER, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

7. GENERAL

If any provision of this Agreement is invalid or unenforceable under applicable law, it shall not affect the validity or enforceability of the remainder of the terms of this Agreement, and without further action by the parties hereto, such provision shall be reformed to the minimum extent necessary to make such provision valid and enforceable.

If Recipient institutes patent litigation against any entity (including a cross-claim or counterclaim in a lawsuit) alleging that the Program itself (excluding combinations of the Program with other software or hardware) infringes such Recipient's patent(s), then such Recipient's rights granted under Section 2(b) shall terminate as of the date such litigation is filed.

All Recipient's rights under this Agreement shall terminate if it fails to comply with any of the material terms or conditions of this Agreement and does not cure such failure in a reasonable period of time after becoming aware of such noncompliance. If all Recipient's rights under this Agreement terminate, Recipient agrees to cease use and distribution of the Program as soon as reasonably practicable. However, Recipient's obligations under this Agreement and any licenses granted by Recipient relating to the Program shall continue and survive.

Everyone is permitted to copy and distribute copies of this Agreement, but in order to avoid inconsistency the Agreement is copyrighted and may only be modified in the following manner. The Agreement Steward reserves the right to publish new versions (including revisions) of this Agreement from time to time. No one other than the Agreement Steward has the right to modify this Agreement. The Eclipse Foundation is the initial Agreement Steward. The Eclipse Foundation may assign the responsibility to serve as the Agreement Steward to a suitable separate entity. Each new version of the Agreement will be given a distinguishing version number. The Program (including Contributions) may always be distributed subject to the version of the Agreement under which it was received. In addition, after a new version of the Agreement is published, Contributor may elect to distribute the Program (including its Contributions) under the new version. Except as expressly stated in Sections 2(a) and 2(b) above, Recipient receives no rights or licenses to the intellectual property of any Contributor under this Agreement, whether expressly, by implication, estoppel or otherwise. All rights in the Program not expressly granted under this Agreement are reserved.

This Agreement is governed by the laws of the State of New York and the intellectual property laws of the United States of America. No party to this Agreement will bring a legal action under this Agreement more than one year after the cause of action arose. Each party waives its rights to a jury trial in any resulting litigation.
```

### Eclipse Public License - v 2.0

```
Eclipse Public License - v 2.0
THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE PUBLIC LICENSE (“AGREEMENT”). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.

1. DEFINITIONS
“Contribution” means:

a) in the case of the initial Contributor, the initial content Distributed under this Agreement, and
b) in the case of each subsequent Contributor:
i) changes to the Program, and
ii) additions to the Program;
where such changes and/or additions to the Program originate from and are Distributed by that particular Contributor. A Contribution “originates” from a Contributor if it was added to the Program by such Contributor itself or anyone acting on such Contributor's behalf. Contributions do not include changes or additions to the Program that are not Modified Works.
“Contributor” means any person or entity that Distributes the Program.

“Licensed Patents” mean patent claims licensable by a Contributor which are necessarily infringed by the use or sale of its Contribution alone or when combined with the Program.

“Program” means the Contributions Distributed in accordance with this Agreement.

“Recipient” means anyone who receives the Program under this Agreement or any Secondary License (as applicable), including Contributors.

“Derivative Works” shall mean any work, whether in Source Code or other form, that is based on (or derived from) the Program and for which the editorial revisions, annotations, elaborations, or other modifications represent, as a whole, an original work of authorship.

“Modified Works” shall mean any work in Source Code or other form that results from an addition to, deletion from, or modification of the contents of the Program, including, for purposes of clarity any new file in Source Code form that contains any contents of the Program. Modified Works shall not include works that contain only declarations, interfaces, types, classes, structures, or files of the Program solely in each case in order to link to, bind by name, or subclass the Program or Modified Works thereof.

“Distribute” means the acts of a) distributing or b) making available in any manner that enables the transfer of a copy.

“Source Code” means the form of a Program preferred for making modifications, including but not limited to software source code, documentation source, and configuration files.

“Secondary License” means either the GNU General Public License, Version 2.0, or any later versions of that license, including any exceptions or additional permissions as identified by the initial Contributor.

2. GRANT OF RIGHTS
a) Subject to the terms of this Agreement, each Contributor hereby grants Recipient a non-exclusive, worldwide, royalty-free copyright license to reproduce, prepare Derivative Works of, publicly display, publicly perform, Distribute and sublicense the Contribution of such Contributor, if any, and such Derivative Works.
b) Subject to the terms of this Agreement, each Contributor hereby grants Recipient a non-exclusive, worldwide, royalty-free patent license under Licensed Patents to make, use, sell, offer to sell, import and otherwise transfer the Contribution of such Contributor, if any, in Source Code or other form. This patent license shall apply to the combination of the Contribution and the Program if, at the time the Contribution is added by the Contributor, such addition of the Contribution causes such combination to be covered by the Licensed Patents. The patent license shall not apply to any other combinations which include the Contribution. No hardware per se is licensed hereunder.
c) Recipient understands that although each Contributor grants the licenses to its Contributions set forth herein, no assurances are provided by any Contributor that the Program does not infringe the patent or other intellectual property rights of any other entity. Each Contributor disclaims any liability to Recipient for claims brought by any other entity based on infringement of intellectual property rights or otherwise. As a condition to exercising the rights and licenses granted hereunder, each Recipient hereby assumes sole responsibility to secure any other intellectual property rights needed, if any. For example, if a third party patent license is required to allow Recipient to Distribute the Program, it is Recipient's responsibility to acquire that license before distributing the Program.
d) Each Contributor represents that to its knowledge it has sufficient copyright rights in its Contribution, if any, to grant the copyright license set forth in this Agreement.
e) Notwithstanding the terms of any Secondary License, no Contributor makes additional grants to any Recipient (other than those set forth in this Agreement) as a result of such Recipient's receipt of the Program under the terms of a Secondary License (if permitted under the terms of Section 3).
3. REQUIREMENTS
3.1 If a Contributor Distributes the Program in any form, then:

a) the Program must also be made available as Source Code, in accordance with section 3.2, and the Contributor must accompany the Program with a statement that the Source Code for the Program is available under this Agreement, and informs Recipients how to obtain it in a reasonable manner on or through a medium customarily used for software exchange; and
b) the Contributor may Distribute the Program under a license different than this Agreement, provided that such license:
i) effectively disclaims on behalf of all other Contributors all warranties and conditions, express and implied, including warranties or conditions of title and non-infringement, and implied warranties or conditions of merchantability and fitness for a particular purpose;
ii) effectively excludes on behalf of all other Contributors all liability for damages, including direct, indirect, special, incidental and consequential damages, such as lost profits;
iii) does not attempt to limit or alter the recipients' rights in the Source Code under section 3.2; and
iv) requires any subsequent distribution of the Program by any party to be under a license that satisfies the requirements of this section 3.
3.2 When the Program is Distributed as Source Code:

a) it must be made available under this Agreement, or if the Program (i) is combined with other material in a separate file or files made available under a Secondary License, and (ii) the initial Contributor attached to the Source Code the notice described in Exhibit A of this Agreement, then the Program may be made available under the terms of such Secondary Licenses, and
b) a copy of this Agreement must be included with each copy of the Program.
3.3 Contributors may not remove or alter any copyright, patent, trademark, attribution notices, disclaimers of warranty, or limitations of liability (‘notices’) contained within the Program from any copy of the Program which they Distribute, provided that Contributors may add their own appropriate notices.

4. COMMERCIAL DISTRIBUTION
Commercial distributors of software may accept certain responsibilities with respect to end users, business partners and the like. While this license is intended to facilitate the commercial use of the Program, the Contributor who includes the Program in a commercial product offering should do so in a manner which does not create potential liability for other Contributors. Therefore, if a Contributor includes the Program in a commercial product offering, such Contributor (“Commercial Contributor”) hereby agrees to defend and indemnify every other Contributor (“Indemnified Contributor”) against any losses, damages and costs (collectively “Losses”) arising from claims, lawsuits and other legal actions brought by a third party against the Indemnified Contributor to the extent caused by the acts or omissions of such Commercial Contributor in connection with its distribution of the Program in a commercial product offering. The obligations in this section do not apply to any claims or Losses relating to any actual or alleged intellectual property infringement. In order to qualify, an Indemnified Contributor must: a) promptly notify the Commercial Contributor in writing of such claim, and b) allow the Commercial Contributor to control, and cooperate with the Commercial Contributor in, the defense and any related settlement negotiations. The Indemnified Contributor may participate in any such claim at its own expense.

For example, a Contributor might include the Program in a commercial product offering, Product X. That Contributor is then a Commercial Contributor. If that Commercial Contributor then makes performance claims, or offers warranties related to Product X, those performance claims and warranties are such Commercial Contributor's responsibility alone. Under this section, the Commercial Contributor would have to defend claims against the other Contributors related to those performance claims and warranties, and if a court requires any other Contributor to pay any damages as a result, the Commercial Contributor must pay those damages.

5. NO WARRANTY
EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, AND TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE PROGRAM IS PROVIDED ON AN “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED INCLUDING, WITHOUT LIMITATION, ANY WARRANTIES OR CONDITIONS OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Each Recipient is solely responsible for determining the appropriateness of using and distributing the Program and assumes all risks associated with its exercise of rights under this Agreement, including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and unavailability or interruption of operations.

6. DISCLAIMER OF LIABILITY
EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, AND TO THE EXTENT PERMITTED BY APPLICABLE LAW, NEITHER RECIPIENT NOR ANY CONTRIBUTORS SHALL HAVE ANY LIABILITY FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING WITHOUT LIMITATION LOST PROFITS), HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OR DISTRIBUTION OF THE PROGRAM OR THE EXERCISE OF ANY RIGHTS GRANTED HEREUNDER, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

7. GENERAL
If any provision of this Agreement is invalid or unenforceable under applicable law, it shall not affect the validity or enforceability of the remainder of the terms of this Agreement, and without further action by the parties hereto, such provision shall be reformed to the minimum extent necessary to make such provision valid and enforceable.

If Recipient institutes patent litigation against any entity (including a cross-claim or counterclaim in a lawsuit) alleging that the Program itself (excluding combinations of the Program with other software or hardware) infringes such Recipient's patent(s), then such Recipient's rights granted under Section 2(b) shall terminate as of the date such litigation is filed.

All Recipient's rights under this Agreement shall terminate if it fails to comply with any of the material terms or conditions of this Agreement and does not cure such failure in a reasonable period of time after becoming aware of such noncompliance. If all Recipient's rights under this Agreement terminate, Recipient agrees to cease use and distribution of the Program as soon as reasonably practicable. However, Recipient's obligations under this Agreement and any licenses granted by Recipient relating to the Program shall continue and survive.

Everyone is permitted to copy and distribute copies of this Agreement, but in order to avoid inconsistency the Agreement is copyrighted and may only be modified in the following manner. The Agreement Steward reserves the right to publish new versions (including revisions) of this Agreement from time to time. No one other than the Agreement Steward has the right to modify this Agreement. The Eclipse Foundation is the initial Agreement Steward. The Eclipse Foundation may assign the responsibility to serve as the Agreement Steward to a suitable separate entity. Each new version of the Agreement will be given a distinguishing version number. The Program (including Contributions) may always be Distributed subject to the version of the Agreement under which it was received. In addition, after a new version of the Agreement is published, Contributor may elect to Distribute the Program (including its Contributions) under the new version.

Except as expressly stated in Sections 2(a) and 2(b) above, Recipient receives no rights or licenses to the intellectual property of any Contributor under this Agreement, whether expressly, by implication, estoppel or otherwise. All rights in the Program not expressly granted under this Agreement are reserved. Nothing in this Agreement is intended to be enforceable by any entity that is not a Contributor or Recipient. No third-party beneficiary rights are created under this Agreement.

Exhibit A – Form of Secondary Licenses Notice
“This Source Code may also be made available under the following Secondary Licenses when the conditions for such availability set forth in the Eclipse Public License, v. 2.0 are satisfied: {name license(s), version(s), and exceptions or additional permissions here}.”

Simply including a copy of this Agreement, including this Exhibit A is not sufficient to license the Source Code under Secondary Licenses.

If it is not possible or desirable to put the notice in a particular file, then You may include the notice in a location (such as a LICENSE file in a relevant directory) where a recipient would be likely to look for such a notice.

You may add additional accurate notices of copyright ownership.
```

### GNU General Public License, version 2

```
Copyright (C) 1989, 1991 Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA

Everyone is permitted to copy and distribute verbatim copies
of this license document, but changing it is not allowed.

Preamble

The licenses for most software are designed to take away your freedom to share and change it. By contrast, the GNU General Public License is intended to guarantee your freedom to share and change free software–to make sure the software is free for all its users. This General Public License applies to most of the Free Software Foundation’s software and to any other program whose authors commit to using it. (Some other Free Software Foundation software is covered by the GNU Library General Public License instead.) You can apply it to your programs, too.

When we speak of free software, we are referring to freedom, not price. Our General Public Licenses are designed to make sure that you have the freedom to distribute copies of free software (and charge for this service if you wish), that you receive source code or can get it if you want it, that you can change the software or use pieces of it in new free programs; and that you know you can do these things.

To protect your rights, we need to make restrictions that forbid anyone to deny you these rights or to ask you to surrender the rights. These restrictions translate to certain responsibilities for you if you distribute copies of the software, or if you modify it.

For example, if you distribute copies of such a program, whether gratis or for a fee, you must give the recipients all the rights that you have. You must make sure that they, too, receive or can get the source code. And you must show them these terms so they know their rights.

We protect your rights with two steps: (1) copyright the software, and (2) offer you this license which gives you legal permission to copy, distribute and/or modify the software.

Also, for each author’s protection and ours, we want to make certain that everyone understands that there is no warranty for this free software. If the software is modified by someone else and passed on, we want its recipients to know that what they have is not the original, so that any problems introduced by others will not reflect on the original authors’ reputations.

Finally, any free program is threatened constantly by software patents. We wish to avoid the danger that redistributors of a free program will individually obtain patent licenses, in effect making the program proprietary. To prevent this, we have made it clear that any patent must be licensed for everyone’s free use or not licensed at all.

The precise terms and conditions for copying, distribution and modification follow.

TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

0. This License applies to any program or other work which contains a notice placed by the copyright holder saying it may be distributed under the terms of this General Public License. The “Program”, below, refers to any such program or work, and a “work based on the Program” means either the Program or any derivative work under copyright law: that is to say, a work containing the Program or a portion of it, either verbatim or with modifications and/or translated into another language. (Hereinafter, translation is included without limitation in the term “modification”.) Each licensee is addressed as “you”.

Activities other than copying, distribution and modification are not covered by this License; they are outside its scope. The act of running the Program is not restricted, and the output from the Program is covered only if its contents constitute a work based on the Program (independent of having been made by running the Program). Whether that is true depends on what the Program does.

1. You may copy and distribute verbatim copies of the Program’s source code as you receive it, in any medium, provided that you conspicuously and appropriately publish on each copy an appropriate copyright notice and disclaimer of warranty; keep intact all the notices that refer to this License and to the absence of any warranty; and give any other recipients of the Program a copy of this License along with the Program.

You may charge a fee for the physical act of transferring a copy, and you may at your option offer warranty protection in exchange for a fee.

2. You may modify your copy or copies of the Program or any portion of it, thus forming a work based on the Program, and copy and distribute such modifications or work under the terms of Section 1 above, provided that you also meet all of these conditions:

a) You must cause the modified files to carry prominent notices stating that you changed the files and the date of any change.

b) You must cause any work that you distribute or publish, that in whole or in part contains or is derived from the Program or any part thereof, to be licensed as a whole at no charge to all third parties under the terms of this License.

c) If the modified program normally reads commands interactively when run, you must cause it, when started running for such interactive use in the most ordinary way, to print or display an announcement including an appropriate copyright notice and a notice that there is no warranty (or else, saying that you provide a warranty) and that users may redistribute the program under these conditions, and telling the user how to view a copy of this License. (Exception: if the Program itself is interactive but does not normally print such an announcement, your work based on the Program is not required to print an announcement.)

These requirements apply to the modified work as a whole. If identifiable sections of that work are not derived from the Program, and can be reasonably considered independent and separate works in themselves, then this License, and its terms, do not apply to those sections when you distribute them as separate works. But when you distribute the same sections as part of a whole which is a work based on the Program, the distribution of the whole must be on the terms of this License, whose permissions for other licensees extend to the entire whole, and thus to each and every part regardless of who wrote it.

Thus, it is not the intent of this section to claim rights or contest your rights to work written entirely by you; rather, the intent is to exercise the right to control the distribution of derivative or collective works based on the Program.

In addition, mere aggregation of another work not based on the Program with the Program (or with a work based on the Program) on a volume of a storage or distribution medium does not bring the other work under the scope of this License.

3. You may copy and distribute the Program (or a work based on it, under Section 2) in object code or executable form under the terms of Sections 1 and 2 above provided that you also do one of the following:

a) Accompany it with the complete corresponding machine-readable source code, which must be distributed under the terms of Sections 1 and 2 above on a medium customarily used for software interchange; or,

b) Accompany it with a written offer, valid for at least three years, to give any third party, for a charge no more than your cost of physically performing source distribution, a complete machine-readable copy of the corresponding source code, to be distributed under the terms of Sections 1 and 2 above on a medium customarily used for software interchange; or,

c) Accompany it with the information you received as to the offer to distribute corresponding source code. (This alternative is allowed only for noncommercial distribution and only if you received the program in object code or executable form with such an offer, in accord with Subsection b above.)

The source code for a work means the preferred form of the work for making modifications to it. For an executable work, complete source code means all the source code for all modules it contains, plus any associated interface definition files, plus the scripts used to control compilation and installation of the executable. However, as a special exception, the source code distributed need not include anything that is normally distributed (in either source or binary form) with the major components (compiler, kernel, and so on) of the operating system on which the executable runs, unless that component itself accompanies the executable.

If distribution of executable or object code is made by offering access to copy from a designated place, then offering equivalent access to copy the source code from the same place counts as distribution of the source code, even though third parties are not compelled to copy the source along with the object code.

4. You may not copy, modify, sublicense, or distribute the Program except as expressly provided under this License. Any attempt otherwise to copy, modify, sublicense or distribute the Program is void, and will automatically terminate your rights under this License. However, parties who have received copies, or rights, from you under this License will not have their licenses terminated so long as such parties remain in full compliance.

5. You are not required to accept this License, since you have not signed it. However, nothing else grants you permission to modify or distribute the Program or its derivative works. These actions are prohibited by law if you do not accept this License. Therefore, by modifying or distributing the Program (or any work based on the Program), you indicate your acceptance of this License to do so, and all its terms and conditions for copying, distributing or modifying the Program or works based on it.

6. Each time you redistribute the Program (or any work based on the Program), the recipient automatically receives a license from the original licensor to copy, distribute or modify the Program subject to these terms and conditions. You may not impose any further restrictions on the recipients’ exercise of the rights granted herein. You are not responsible for enforcing compliance by third parties to this License.

7. If, as a consequence of a court judgment or allegation of patent infringement or for any other reason (not limited to patent issues), conditions are imposed on you (whether by court order, agreement or otherwise) that contradict the conditions of this License, they do not excuse you from the conditions of this License. If you cannot distribute so as to satisfy simultaneously your obligations under this License and any other pertinent obligations, then as a consequence you may not distribute the Program at all. For example, if a patent license would not permit royalty-free redistribution of the Program by all those who receive copies directly or indirectly through you, then the only way you could satisfy both it and this License would be to refrain entirely from distribution of the Program.

If any portion of this section is held invalid or unenforceable under any particular circumstance, the balance of the section is intended to apply and the section as a whole is intended to apply in other circumstances.

It is not the purpose of this section to induce you to infringe any patents or other property right claims or to contest validity of any such claims; this section has the sole purpose of protecting the integrity of the free software distribution system, which is implemented by public license practices. Many people have made generous contributions to the wide range of software distributed through that system in reliance on consistent application of that system; it is up to the author/donor to decide if he or she is willing to distribute software through any other system and a licensee cannot impose that choice.

This section is intended to make thoroughly clear what is believed to be a consequence of the rest of this License.

8. If the distribution and/or use of the Program is restricted in certain countries either by patents or by copyrighted interfaces, the original copyright holder who places the Program under this License may add an explicit geographical distribution limitation excluding those countries, so that distribution is permitted only in or among countries not thus excluded. In such case, this License incorporates the limitation as if written in the body of this License.

9. The Free Software Foundation may publish revised and/or new versions of the General Public License from time to time. Such new versions will be similar in spirit to the present version, but may differ in detail to address new problems or concerns.

Each version is given a distinguishing version number. If the Program specifies a version number of this License which applies to it and “any later version”, you have the option of following the terms and conditions either of that version or of any later version published by the Free Software Foundation. If the Program does not specify a version number of this License, you may choose any version ever published by the Free Software Foundation.

10. If you wish to incorporate parts of the Program into other free programs whose distribution conditions are different, write to the author to ask for permission. For software which is copyrighted by the Free Software Foundation, write to the Free Software Foundation; we sometimes make exceptions for this. Our decision will be guided by the two goals of preserving the free status of all derivatives of our free software and of promoting the sharing and reuse of software generally.

NO WARRANTY

11. BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.

12. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY AND/OR REDISTRIBUTE THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

END OF TERMS AND CONDITIONS


How to Apply These Terms to Your New Programs

If you develop a new program, and you want it to be of the greatest possible use to the public, the best way to achieve this is to make it free software which everyone can redistribute and change under these terms.

To do so, attach the following notices to the program. It is safest to attach them to the start of each source file to most effectively convey the exclusion of warranty; and each file should have at least the “copyright” line and a pointer to where the full notice is found.

One line to give the program’s name and a brief idea of what it does.
Copyright (C) <year> <name of author>

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

Also add information on how to contact you by electronic and paper mail.

If the program is interactive, make it output a short notice like this when it starts in an interactive mode:

Gnomovision version 69, Copyright (C) year name of author Gnomovision comes with ABSOLUTELY NO WARRANTY; for details type `show w’. This is free software, and you are welcome to redistribute it under certain conditions; type `show c’ for details.

The hypothetical commands `show w’ and `show c’ should show the appropriate parts of the General Public License. Of course, the commands you use may be called something other than `show w’ and `show c’; they could even be mouse-clicks or menu items–whatever suits your program.

You should also get your employer (if you work as a programmer) or your school, if any, to sign a “copyright disclaimer” for the program, if necessary. Here is a sample; alter the names:

Yoyodyne, Inc., hereby disclaims all copyright interest in the program `Gnomovision’ (which makes passes at compilers) written by James Hacker.

signature of Ty Coon, 1 April 1989
Ty Coon, President of Vice

This General Public License does not permit incorporating your program into proprietary programs. If your program is a subroutine library, you may consider it more useful to permit linking proprietary applications with the library. If this is what you want to do, use the GNU Library General Public License instead of this License.
```

### GNU General Public License, version 2, with the Classpath Exception

```
The GNU General Public License (GPL)

Version 2, June 1991

Copyright (C) 1989, 1991 Free Software Foundation, Inc.
59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

Everyone is permitted to copy and distribute verbatim copies of this license
document, but changing it is not allowed.

Preamble

The licenses for most software are designed to take away your freedom to share
and change it.  By contrast, the GNU General Public License is intended to
guarantee your freedom to share and change free software--to make sure the
software is free for all its users.  This General Public License applies to
most of the Free Software Foundation's software and to any other program whose
authors commit to using it.  (Some other Free Software Foundation software is
covered by the GNU Library General Public License instead.) You can apply it to
your programs, too.

When we speak of free software, we are referring to freedom, not price.  Our
General Public Licenses are designed to make sure that you have the freedom to
distribute copies of free software (and charge for this service if you wish),
that you receive source code or can get it if you want it, that you can change
the software or use pieces of it in new free programs; and that you know you
can do these things.

To protect your rights, we need to make restrictions that forbid anyone to deny
you these rights or to ask you to surrender the rights.  These restrictions
translate to certain responsibilities for you if you distribute copies of the
software, or if you modify it.

For example, if you distribute copies of such a program, whether gratis or for
a fee, you must give the recipients all the rights that you have.  You must
make sure that they, too, receive or can get the source code.  And you must
show them these terms so they know their rights.

We protect your rights with two steps: (1) copyright the software, and (2)
offer you this license which gives you legal permission to copy, distribute
and/or modify the software.

Also, for each author's protection and ours, we want to make certain that
everyone understands that there is no warranty for this free software.  If the
software is modified by someone else and passed on, we want its recipients to
know that what they have is not the original, so that any problems introduced
by others will not reflect on the original authors' reputations.

Finally, any free program is threatened constantly by software patents.  We
wish to avoid the danger that redistributors of a free program will
individually obtain patent licenses, in effect making the program proprietary.
To prevent this, we have made it clear that any patent must be licensed for
everyone's free use or not licensed at all.

The precise terms and conditions for copying, distribution and modification
follow.

TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

0. This License applies to any program or other work which contains a notice
placed by the copyright holder saying it may be distributed under the terms of
this General Public License.  The "Program", below, refers to any such program
or work, and a "work based on the Program" means either the Program or any
derivative work under copyright law: that is to say, a work containing the
Program or a portion of it, either verbatim or with modifications and/or
translated into another language.  (Hereinafter, translation is included
without limitation in the term "modification".) Each licensee is addressed as
"you".

Activities other than copying, distribution and modification are not covered by
this License; they are outside its scope.  The act of running the Program is
not restricted, and the output from the Program is covered only if its contents
constitute a work based on the Program (independent of having been made by
running the Program).  Whether that is true depends on what the Program does.

1. You may copy and distribute verbatim copies of the Program's source code as
you receive it, in any medium, provided that you conspicuously and
appropriately publish on each copy an appropriate copyright notice and
disclaimer of warranty; keep intact all the notices that refer to this License
and to the absence of any warranty; and give any other recipients of the
Program a copy of this License along with the Program.

You may charge a fee for the physical act of transferring a copy, and you may
at your option offer warranty protection in exchange for a fee.

2. You may modify your copy or copies of the Program or any portion of it, thus
forming a work based on the Program, and copy and distribute such modifications
or work under the terms of Section 1 above, provided that you also meet all of
these conditions:

    a) You must cause the modified files to carry prominent notices stating
    that you changed the files and the date of any change.

    b) You must cause any work that you distribute or publish, that in whole or
    in part contains or is derived from the Program or any part thereof, to be
    licensed as a whole at no charge to all third parties under the terms of
    this License.

    c) If the modified program normally reads commands interactively when run,
    you must cause it, when started running for such interactive use in the
    most ordinary way, to print or display an announcement including an
    appropriate copyright notice and a notice that there is no warranty (or
    else, saying that you provide a warranty) and that users may redistribute
    the program under these conditions, and telling the user how to view a copy
    of this License.  (Exception: if the Program itself is interactive but does
    not normally print such an announcement, your work based on the Program is
    not required to print an announcement.)

These requirements apply to the modified work as a whole.  If identifiable
sections of that work are not derived from the Program, and can be reasonably
considered independent and separate works in themselves, then this License, and
its terms, do not apply to those sections when you distribute them as separate
works.  But when you distribute the same sections as part of a whole which is a
work based on the Program, the distribution of the whole must be on the terms
of this License, whose permissions for other licensees extend to the entire
whole, and thus to each and every part regardless of who wrote it.

Thus, it is not the intent of this section to claim rights or contest your
rights to work written entirely by you; rather, the intent is to exercise the
right to control the distribution of derivative or collective works based on
the Program.

In addition, mere aggregation of another work not based on the Program with the
Program (or with a work based on the Program) on a volume of a storage or
distribution medium does not bring the other work under the scope of this
License.

3. You may copy and distribute the Program (or a work based on it, under
Section 2) in object code or executable form under the terms of Sections 1 and
2 above provided that you also do one of the following:

    a) Accompany it with the complete corresponding machine-readable source
    code, which must be distributed under the terms of Sections 1 and 2 above
    on a medium customarily used for software interchange; or,

    b) Accompany it with a written offer, valid for at least three years, to
    give any third party, for a charge no more than your cost of physically
    performing source distribution, a complete machine-readable copy of the
    corresponding source code, to be distributed under the terms of Sections 1
    and 2 above on a medium customarily used for software interchange; or,

    c) Accompany it with the information you received as to the offer to
    distribute corresponding source code.  (This alternative is allowed only
    for noncommercial distribution and only if you received the program in
    object code or executable form with such an offer, in accord with
    Subsection b above.)

The source code for a work means the preferred form of the work for making
modifications to it.  For an executable work, complete source code means all
the source code for all modules it contains, plus any associated interface
definition files, plus the scripts used to control compilation and installation
of the executable.  However, as a special exception, the source code
distributed need not include anything that is normally distributed (in either
source or binary form) with the major components (compiler, kernel, and so on)
of the operating system on which the executable runs, unless that component
itself accompanies the executable.

If distribution of executable or object code is made by offering access to copy
from a designated place, then offering equivalent access to copy the source
code from the same place counts as distribution of the source code, even though
third parties are not compelled to copy the source along with the object code.

4. You may not copy, modify, sublicense, or distribute the Program except as
expressly provided under this License.  Any attempt otherwise to copy, modify,
sublicense or distribute the Program is void, and will automatically terminate
your rights under this License.  However, parties who have received copies, or
rights, from you under this License will not have their licenses terminated so
long as such parties remain in full compliance.

5. You are not required to accept this License, since you have not signed it.
However, nothing else grants you permission to modify or distribute the Program
or its derivative works.  These actions are prohibited by law if you do not
accept this License.  Therefore, by modifying or distributing the Program (or
any work based on the Program), you indicate your acceptance of this License to
do so, and all its terms and conditions for copying, distributing or modifying
the Program or works based on it.

6. Each time you redistribute the Program (or any work based on the Program),
the recipient automatically receives a license from the original licensor to
copy, distribute or modify the Program subject to these terms and conditions.
You may not impose any further restrictions on the recipients' exercise of the
rights granted herein.  You are not responsible for enforcing compliance by
third parties to this License.

7. If, as a consequence of a court judgment or allegation of patent
infringement or for any other reason (not limited to patent issues), conditions
are imposed on you (whether by court order, agreement or otherwise) that
contradict the conditions of this License, they do not excuse you from the
conditions of this License.  If you cannot distribute so as to satisfy
simultaneously your obligations under this License and any other pertinent
obligations, then as a consequence you may not distribute the Program at all.
For example, if a patent license would not permit royalty-free redistribution
of the Program by all those who receive copies directly or indirectly through
you, then the only way you could satisfy both it and this License would be to
refrain entirely from distribution of the Program.

If any portion of this section is held invalid or unenforceable under any
particular circumstance, the balance of the section is intended to apply and
the section as a whole is intended to apply in other circumstances.

It is not the purpose of this section to induce you to infringe any patents or
other property right claims or to contest validity of any such claims; this
section has the sole purpose of protecting the integrity of the free software
distribution system, which is implemented by public license practices.  Many
people have made generous contributions to the wide range of software
distributed through that system in reliance on consistent application of that
system; it is up to the author/donor to decide if he or she is willing to
distribute software through any other system and a licensee cannot impose that
choice.

This section is intended to make thoroughly clear what is believed to be a
consequence of the rest of this License.

8. If the distribution and/or use of the Program is restricted in certain
countries either by patents or by copyrighted interfaces, the original
copyright holder who places the Program under this License may add an explicit
geographical distribution limitation excluding those countries, so that
distribution is permitted only in or among countries not thus excluded.  In
such case, this License incorporates the limitation as if written in the body
of this License.

9. The Free Software Foundation may publish revised and/or new versions of the
General Public License from time to time.  Such new versions will be similar in
spirit to the present version, but may differ in detail to address new problems
or concerns.

Each version is given a distinguishing version number.  If the Program
specifies a version number of this License which applies to it and "any later
version", you have the option of following the terms and conditions either of
that version or of any later version published by the Free Software Foundation.
If the Program does not specify a version number of this License, you may
choose any version ever published by the Free Software Foundation.

10. If you wish to incorporate parts of the Program into other free programs
whose distribution conditions are different, write to the author to ask for
permission.  For software which is copyrighted by the Free Software Foundation,
write to the Free Software Foundation; we sometimes make exceptions for this.
Our decision will be guided by the two goals of preserving the free status of
all derivatives of our free software and of promoting the sharing and reuse of
software generally.

NO WARRANTY

11. BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY FOR
THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW.  EXCEPT WHEN OTHERWISE
STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE
PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE.  THE ENTIRE RISK AS TO THE QUALITY AND
PERFORMANCE OF THE PROGRAM IS WITH YOU.  SHOULD THE PROGRAM PROVE DEFECTIVE,
YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.

12. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL
ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY AND/OR REDISTRIBUTE THE
PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY
GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR
INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA
BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A
FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER
OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

END OF TERMS AND CONDITIONS

How to Apply These Terms to Your New Programs

If you develop a new program, and you want it to be of the greatest possible
use to the public, the best way to achieve this is to make it free software
which everyone can redistribute and change under these terms.

To do so, attach the following notices to the program.  It is safest to attach
them to the start of each source file to most effectively convey the exclusion
of warranty; and each file should have at least the "copyright" line and a
pointer to where the full notice is found.

    One line to give the program's name and a brief idea of what it does.

    Copyright (C) <year> <name of author>

    This program is free software; you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by the Free
    Software Foundation; either version 2 of the License, or (at your option)
    any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
    more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc., 59
    Temple Place, Suite 330, Boston, MA 02111-1307 USA

Also add information on how to contact you by electronic and paper mail.

If the program is interactive, make it output a short notice like this when it
starts in an interactive mode:

    Gnomovision version 69, Copyright (C) year name of author Gnomovision comes
    with ABSOLUTELY NO WARRANTY; for details type 'show w'.  This is free
    software, and you are welcome to redistribute it under certain conditions;
    type 'show c' for details.

The hypothetical commands 'show w' and 'show c' should show the appropriate
parts of the General Public License.  Of course, the commands you use may be
called something other than 'show w' and 'show c'; they could even be
mouse-clicks or menu items--whatever suits your program.

You should also get your employer (if you work as a programmer) or your school,
if any, to sign a "copyright disclaimer" for the program, if necessary.  Here
is a sample; alter the names:

    Yoyodyne, Inc., hereby disclaims all copyright interest in the program
    'Gnomovision' (which makes passes at compilers) written by James Hacker.

    signature of Ty Coon, 1 April 1989

    Ty Coon, President of Vice

This General Public License does not permit incorporating your program into
proprietary programs.  If your program is a subroutine library, you may
consider it more useful to permit linking proprietary applications with the
library.  If this is what you want to do, use the GNU Library General Public
License instead of this License.


"CLASSPATH" EXCEPTION TO THE GPL

Certain source files distributed by Oracle America and/or its affiliates are
subject to the following clarification and special exception to the GPL, but
only where Oracle has expressly included in the particular source file's header
the words "Oracle designates this particular file as subject to the "Classpath"
exception as provided by Oracle in the LICENSE file that accompanied this code."

    Linking this library statically or dynamically with other modules is making
    a combined work based on this library.  Thus, the terms and conditions of
    the GNU General Public License cover the whole combination.

    As a special exception, the copyright holders of this library give you
    permission to link this library with independent modules to produce an
    executable, regardless of the license terms of these independent modules,
    and to copy and distribute the resulting executable under terms of your
    choice, provided that you also meet, for each linked independent module,
    the terms and conditions of the license of that module.  An independent
    module is a module which is not derived from or based on this library.  If
    you modify this library, you may extend this exception to your version of
    the library, but you are not obligated to do so.  If you do not wish to do
    so, delete this exception statement from your version.

ADDITIONAL INFORMATION ABOUT LICENSING

Certain files distributed by Oracle America, Inc. and/or its affiliates are
subject to the following clarification and special exception to the GPLv2,
based on the GNU Project exception for its Classpath libraries, known as the
GNU Classpath Exception.

Note that Oracle includes multiple, independent programs in this software
package.  Some of those programs are provided under licenses deemed
incompatible with the GPLv2 by the Free Software Foundation and others.
For example, the package includes programs licensed under the Apache
License, Version 2.0 and may include FreeType. Such programs are licensed
to you under their original licenses.

Oracle facilitates your further distribution of this package by adding the
Classpath Exception to the necessary parts of its GPLv2 code, which permits
you to use that code in combination with other independent modules not
licensed under the GPLv2. However, note that this would not permit you to
commingle code under an incompatible license with Oracle's GPLv2 licensed
code by, for example, cutting and pasting such code into a file also
containing Oracle's GPLv2 licensed code and then distributing the result.

Additionally, if you were to remove the Classpath Exception from any of the
files to which it applies and distribute the result, you would likely be
required to license some or all of the other code in that distribution under
the GPLv2 as well, and since the GPLv2 is incompatible with the license terms
of some items included in the distribution by Oracle, removing the Classpath
Exception could therefore effectively compromise your ability to further
distribute the package.

Failing to distribute notices associated with some files may also create
unexpected legal consequences.

Proceed with caution and we recommend that you obtain the advice of a lawyer
skilled in open source matters before removing the Classpath Exception or
making modifications to this package which may subsequently be redistributed
and/or involve the use of third party software.
```

### GNU Lesser General Public License (LGPL), Version 2.1

```
		  GNU LESSER GENERAL PUBLIC LICENSE
		       Version 2.1, February 1999

 Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.

[This is the first released version of the Lesser GPL.  It also counts
 as the successor of the GNU Library Public License, version 2, hence
 the version number 2.1.]

			    Preamble

  The licenses for most software are designed to take away your
freedom to share and change it.  By contrast, the GNU General Public
Licenses are intended to guarantee your freedom to share and change
free software--to make sure the software is free for all its users.

  This license, the Lesser General Public License, applies to some
specially designated software packages--typically libraries--of the
Free Software Foundation and other authors who decide to use it.  You
can use it too, but we suggest you first think carefully about whether
this license or the ordinary General Public License is the better
strategy to use in any particular case, based on the explanations below.

  When we speak of free software, we are referring to freedom of use,
not price.  Our General Public Licenses are designed to make sure that
you have the freedom to distribute copies of free software (and charge
for this service if you wish); that you receive source code or can get
it if you want it; that you can change the software and use pieces of
it in new free programs; and that you are informed that you can do
these things.

  To protect your rights, we need to make restrictions that forbid
distributors to deny you these rights or to ask you to surrender these
rights.  These restrictions translate to certain responsibilities for
you if you distribute copies of the library or if you modify it.

  For example, if you distribute copies of the library, whether gratis
or for a fee, you must give the recipients all the rights that we gave
you.  You must make sure that they, too, receive or can get the source
code.  If you link other code with the library, you must provide
complete object files to the recipients, so that they can relink them
with the library after making changes to the library and recompiling
it.  And you must show them these terms so they know their rights.

  We protect your rights with a two-step method: (1) we copyright the
library, and (2) we offer you this license, which gives you legal
permission to copy, distribute and/or modify the library.

  To protect each distributor, we want to make it very clear that
there is no warranty for the free library.  Also, if the library is
modified by someone else and passed on, the recipients should know
that what they have is not the original version, so that the original
author's reputation will not be affected by problems that might be
introduced by others.

  Finally, software patents pose a constant threat to the existence of
any free program.  We wish to make sure that a company cannot
effectively restrict the users of a free program by obtaining a
restrictive license from a patent holder.  Therefore, we insist that
any patent license obtained for a version of the library must be
consistent with the full freedom of use specified in this license.

  Most GNU software, including some libraries, is covered by the
ordinary GNU General Public License.  This license, the GNU Lesser
General Public License, applies to certain designated libraries, and
is quite different from the ordinary General Public License.  We use
this license for certain libraries in order to permit linking those
libraries into non-free programs.

  When a program is linked with a library, whether statically or using
a shared library, the combination of the two is legally speaking a
combined work, a derivative of the original library.  The ordinary
General Public License therefore permits such linking only if the
entire combination fits its criteria of freedom.  The Lesser General
Public License permits more lax criteria for linking other code with
the library.

  We call this license the "Lesser" General Public License because it
does Less to protect the user's freedom than the ordinary General
Public License.  It also provides other free software developers Less
of an advantage over competing non-free programs.  These disadvantages
are the reason we use the ordinary General Public License for many
libraries.  However, the Lesser license provides advantages in certain
special circumstances.

  For example, on rare occasions, there may be a special need to
encourage the widest possible use of a certain library, so that it becomes
a de-facto standard.  To achieve this, non-free programs must be
allowed to use the library.  A more frequent case is that a free
library does the same job as widely used non-free libraries.  In this
case, there is little to gain by limiting the free library to free
software only, so we use the Lesser General Public License.

  In other cases, permission to use a particular library in non-free
programs enables a greater number of people to use a large body of
free software.  For example, permission to use the GNU C Library in
non-free programs enables many more people to use the whole GNU
operating system, as well as its variant, the GNU/Linux operating
system.

  Although the Lesser General Public License is Less protective of the
users' freedom, it does ensure that the user of a program that is
linked with the Library has the freedom and the wherewithal to run
that program using a modified version of the Library.

  The precise terms and conditions for copying, distribution and
modification follow.  Pay close attention to the difference between a
"work based on the library" and a "work that uses the library".  The
former contains code derived from the library, whereas the latter must
be combined with the library in order to run.

		  GNU LESSER GENERAL PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

  0. This License Agreement applies to any software library or other
program which contains a notice placed by the copyright holder or
other authorized party saying it may be distributed under the terms of
this Lesser General Public License (also called "this License").
Each licensee is addressed as "you".

  A "library" means a collection of software functions and/or data
prepared so as to be conveniently linked with application programs
(which use some of those functions and data) to form executables.

  The "Library", below, refers to any such software library or work
which has been distributed under these terms.  A "work based on the
Library" means either the Library or any derivative work under
copyright law: that is to say, a work containing the Library or a
portion of it, either verbatim or with modifications and/or translated
straightforwardly into another language.  (Hereinafter, translation is
included without limitation in the term "modification".)

  "Source code" for a work means the preferred form of the work for
making modifications to it.  For a library, complete source code means
all the source code for all modules it contains, plus any associated
interface definition files, plus the scripts used to control compilation
and installation of the library.

  Activities other than copying, distribution and modification are not
covered by this License; they are outside its scope.  The act of
running a program using the Library is not restricted, and output from
such a program is covered only if its contents constitute a work based
on the Library (independent of the use of the Library in a tool for
writing it).  Whether that is true depends on what the Library does
and what the program that uses the Library does.

  1. You may copy and distribute verbatim copies of the Library's
complete source code as you receive it, in any medium, provided that
you conspicuously and appropriately publish on each copy an
appropriate copyright notice and disclaimer of warranty; keep intact
all the notices that refer to this License and to the absence of any
warranty; and distribute a copy of this License along with the
Library.

  You may charge a fee for the physical act of transferring a copy,
and you may at your option offer warranty protection in exchange for a
fee.

  2. You may modify your copy or copies of the Library or any portion
of it, thus forming a work based on the Library, and copy and
distribute such modifications or work under the terms of Section 1
above, provided that you also meet all of these conditions:

    a) The modified work must itself be a software library.

    b) You must cause the files modified to carry prominent notices
    stating that you changed the files and the date of any change.

    c) You must cause the whole of the work to be licensed at no
    charge to all third parties under the terms of this License.

    d) If a facility in the modified Library refers to a function or a
    table of data to be supplied by an application program that uses
    the facility, other than as an argument passed when the facility
    is invoked, then you must make a good faith effort to ensure that,
    in the event an application does not supply such function or
    table, the facility still operates, and performs whatever part of
    its purpose remains meaningful.

    (For example, a function in a library to compute square roots has
    a purpose that is entirely well-defined independent of the
    application.  Therefore, Subsection 2d requires that any
    application-supplied function or table used by this function must
    be optional: if the application does not supply it, the square
    root function must still compute square roots.)

These requirements apply to the modified work as a whole.  If
identifiable sections of that work are not derived from the Library,
and can be reasonably considered independent and separate works in
themselves, then this License, and its terms, do not apply to those
sections when you distribute them as separate works.  But when you
distribute the same sections as part of a whole which is a work based
on the Library, the distribution of the whole must be on the terms of
this License, whose permissions for other licensees extend to the
entire whole, and thus to each and every part regardless of who wrote
it.

Thus, it is not the intent of this section to claim rights or contest
your rights to work written entirely by you; rather, the intent is to
exercise the right to control the distribution of derivative or
collective works based on the Library.

In addition, mere aggregation of another work not based on the Library
with the Library (or with a work based on the Library) on a volume of
a storage or distribution medium does not bring the other work under
the scope of this License.

  3. You may opt to apply the terms of the ordinary GNU General Public
License instead of this License to a given copy of the Library.  To do
this, you must alter all the notices that refer to this License, so
that they refer to the ordinary GNU General Public License, version 2,
instead of to this License.  (If a newer version than version 2 of the
ordinary GNU General Public License has appeared, then you can specify
that version instead if you wish.)  Do not make any other change in
these notices.

  Once this change is made in a given copy, it is irreversible for
that copy, so the ordinary GNU General Public License applies to all
subsequent copies and derivative works made from that copy.

  This option is useful when you wish to copy part of the code of
the Library into a program that is not a library.

  4. You may copy and distribute the Library (or a portion or
derivative of it, under Section 2) in object code or executable form
under the terms of Sections 1 and 2 above provided that you accompany
it with the complete corresponding machine-readable source code, which
must be distributed under the terms of Sections 1 and 2 above on a
medium customarily used for software interchange.

  If distribution of object code is made by offering access to copy
from a designated place, then offering equivalent access to copy the
source code from the same place satisfies the requirement to
distribute the source code, even though third parties are not
compelled to copy the source along with the object code.

  5. A program that contains no derivative of any portion of the
Library, but is designed to work with the Library by being compiled or
linked with it, is called a "work that uses the Library".  Such a
work, in isolation, is not a derivative work of the Library, and
therefore falls outside the scope of this License.

  However, linking a "work that uses the Library" with the Library
creates an executable that is a derivative of the Library (because it
contains portions of the Library), rather than a "work that uses the
library".  The executable is therefore covered by this License.
Section 6 states terms for distribution of such executables.

  When a "work that uses the Library" uses material from a header file
that is part of the Library, the object code for the work may be a
derivative work of the Library even though the source code is not.
Whether this is true is especially significant if the work can be
linked without the Library, or if the work is itself a library.  The
threshold for this to be true is not precisely defined by law.

  If such an object file uses only numerical parameters, data
structure layouts and accessors, and small macros and small inline
functions (ten lines or less in length), then the use of the object
file is unrestricted, regardless of whether it is legally a derivative
work.  (Executables containing this object code plus portions of the
Library will still fall under Section 6.)

  Otherwise, if the work is a derivative of the Library, you may
distribute the object code for the work under the terms of Section 6.
Any executables containing that work also fall under Section 6,
whether or not they are linked directly with the Library itself.

  6. As an exception to the Sections above, you may also combine or
link a "work that uses the Library" with the Library to produce a
work containing portions of the Library, and distribute that work
under terms of your choice, provided that the terms permit
modification of the work for the customer's own use and reverse
engineering for debugging such modifications.

  You must give prominent notice with each copy of the work that the
Library is used in it and that the Library and its use are covered by
this License.  You must supply a copy of this License.  If the work
during execution displays copyright notices, you must include the
copyright notice for the Library among them, as well as a reference
directing the user to the copy of this License.  Also, you must do one
of these things:

    a) Accompany the work with the complete corresponding
    machine-readable source code for the Library including whatever
    changes were used in the work (which must be distributed under
    Sections 1 and 2 above); and, if the work is an executable linked
    with the Library, with the complete machine-readable "work that
    uses the Library", as object code and/or source code, so that the
    user can modify the Library and then relink to produce a modified
    executable containing the modified Library.  (It is understood
    that the user who changes the contents of definitions files in the
    Library will not necessarily be able to recompile the application
    to use the modified definitions.)

    b) Use a suitable shared library mechanism for linking with the
    Library.  A suitable mechanism is one that (1) uses at run time a
    copy of the library already present on the user's computer system,
    rather than copying library functions into the executable, and (2)
    will operate properly with a modified version of the library, if
    the user installs one, as long as the modified version is
    interface-compatible with the version that the work was made with.

    c) Accompany the work with a written offer, valid for at
    least three years, to give the same user the materials
    specified in Subsection 6a, above, for a charge no more
    than the cost of performing this distribution.

    d) If distribution of the work is made by offering access to copy
    from a designated place, offer equivalent access to copy the above
    specified materials from the same place.

    e) Verify that the user has already received a copy of these
    materials or that you have already sent this user a copy.

  For an executable, the required form of the "work that uses the
Library" must include any data and utility programs needed for
reproducing the executable from it.  However, as a special exception,
the materials to be distributed need not include anything that is
normally distributed (in either source or binary form) with the major
components (compiler, kernel, and so on) of the operating system on
which the executable runs, unless that component itself accompanies
the executable.

  It may happen that this requirement contradicts the license
restrictions of other proprietary libraries that do not normally
accompany the operating system.  Such a contradiction means you cannot
use both them and the Library together in an executable that you
distribute.

  7. You may place library facilities that are a work based on the
Library side-by-side in a single library together with other library
facilities not covered by this License, and distribute such a combined
library, provided that the separate distribution of the work based on
the Library and of the other library facilities is otherwise
permitted, and provided that you do these two things:

    a) Accompany the combined library with a copy of the same work
    based on the Library, uncombined with any other library
    facilities.  This must be distributed under the terms of the
    Sections above.

    b) Give prominent notice with the combined library of the fact
    that part of it is a work based on the Library, and explaining
    where to find the accompanying uncombined form of the same work.

  8. You may not copy, modify, sublicense, link with, or distribute
the Library except as expressly provided under this License.  Any
attempt otherwise to copy, modify, sublicense, link with, or
distribute the Library is void, and will automatically terminate your
rights under this License.  However, parties who have received copies,
or rights, from you under this License will not have their licenses
terminated so long as such parties remain in full compliance.

  9. You are not required to accept this License, since you have not
signed it.  However, nothing else grants you permission to modify or
distribute the Library or its derivative works.  These actions are
prohibited by law if you do not accept this License.  Therefore, by
modifying or distributing the Library (or any work based on the
Library), you indicate your acceptance of this License to do so, and
all its terms and conditions for copying, distributing or modifying
the Library or works based on it.

  10. Each time you redistribute the Library (or any work based on the
Library), the recipient automatically receives a license from the
original licensor to copy, distribute, link with or modify the Library
subject to these terms and conditions.  You may not impose any further
restrictions on the recipients' exercise of the rights granted herein.
You are not responsible for enforcing compliance by third parties with
this License.

  11. If, as a consequence of a court judgment or allegation of patent
infringement or for any other reason (not limited to patent issues),
conditions are imposed on you (whether by court order, agreement or
otherwise) that contradict the conditions of this License, they do not
excuse you from the conditions of this License.  If you cannot
distribute so as to satisfy simultaneously your obligations under this
License and any other pertinent obligations, then as a consequence you
may not distribute the Library at all.  For example, if a patent
license would not permit royalty-free redistribution of the Library by
all those who receive copies directly or indirectly through you, then
the only way you could satisfy both it and this License would be to
refrain entirely from distribution of the Library.

If any portion of this section is held invalid or unenforceable under any
particular circumstance, the balance of the section is intended to apply,
and the section as a whole is intended to apply in other circumstances.

It is not the purpose of this section to induce you to infringe any
patents or other property right claims or to contest validity of any
such claims; this section has the sole purpose of protecting the
integrity of the free software distribution system which is
implemented by public license practices.  Many people have made
generous contributions to the wide range of software distributed
through that system in reliance on consistent application of that
system; it is up to the author/donor to decide if he or she is willing
to distribute software through any other system and a licensee cannot
impose that choice.

This section is intended to make thoroughly clear what is believed to
be a consequence of the rest of this License.

  12. If the distribution and/or use of the Library is restricted in
certain countries either by patents or by copyrighted interfaces, the
original copyright holder who places the Library under this License may add
an explicit geographical distribution limitation excluding those countries,
so that distribution is permitted only in or among countries not thus
excluded.  In such case, this License incorporates the limitation as if
written in the body of this License.

  13. The Free Software Foundation may publish revised and/or new
versions of the Lesser General Public License from time to time.
Such new versions will be similar in spirit to the present version,
but may differ in detail to address new problems or concerns.

Each version is given a distinguishing version number.  If the Library
specifies a version number of this License which applies to it and
"any later version", you have the option of following the terms and
conditions either of that version or of any later version published by
the Free Software Foundation.  If the Library does not specify a
license version number, you may choose any version ever published by
the Free Software Foundation.

  14. If you wish to incorporate parts of the Library into other free
programs whose distribution conditions are incompatible with these,
write to the author to ask for permission.  For software which is
copyrighted by the Free Software Foundation, write to the Free
Software Foundation; we sometimes make exceptions for this.  Our
decision will be guided by the two goals of preserving the free status
of all derivatives of our free software and of promoting the sharing
and reuse of software generally.

			    NO WARRANTY

  15. BECAUSE THE LIBRARY IS LICENSED FREE OF CHARGE, THERE IS NO
WARRANTY FOR THE LIBRARY, TO THE EXTENT PERMITTED BY APPLICABLE LAW.
EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
OTHER PARTIES PROVIDE THE LIBRARY "AS IS" WITHOUT WARRANTY OF ANY
KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE.  THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE
LIBRARY IS WITH YOU.  SHOULD THE LIBRARY PROVE DEFECTIVE, YOU ASSUME
THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.

  16. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN
WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY
AND/OR REDISTRIBUTE THE LIBRARY AS PERMITTED ABOVE, BE LIABLE TO YOU
FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR
CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE
LIBRARY (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING
RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A
FAILURE OF THE LIBRARY TO OPERATE WITH ANY OTHER SOFTWARE), EVEN IF
SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH
DAMAGES.

		     END OF TERMS AND CONDITIONS

           How to Apply These Terms to Your New Libraries

  If you develop a new library, and you want it to be of the greatest
possible use to the public, we recommend making it free software that
everyone can redistribute and change.  You can do so by permitting
redistribution under these terms (or, alternatively, under the terms of the
ordinary General Public License).

  To apply these terms, attach the following notices to the library.  It is
safest to attach them to the start of each source file to most effectively
convey the exclusion of warranty; and each file should have at least the
"copyright" line and a pointer to where the full notice is found.

    <one line to give the library's name and a brief idea of what it does.>
    Copyright (C) <year>  <name of author>

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

Also add information on how to contact you by electronic and paper mail.

You should also get your employer (if you work as a programmer) or your
school, if any, to sign a "copyright disclaimer" for the library, if
necessary.  Here is a sample; alter the names:

  Yoyodyne, Inc., hereby disclaims all copyright interest in the
  library `Frob' (a library for tweaking knobs) written by James Random Hacker.

  <signature of Ty Coon>, 1 April 1990
  Ty Coon, President of Vice

That's all there is to it!
```

### MIT Licence

```
MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

### Mozilla Public License Version 2.0

```
Mozilla Public License Version 2.0
==================================

1. Definitions
--------------

1.1. "Contributor"
    means each individual or legal entity that creates, contributes to
    the creation of, or owns Covered Software.

1.2. "Contributor Version"
    means the combination of the Contributions of others (if any) used
    by a Contributor and that particular Contributor's Contribution.

1.3. "Contribution"
    means Covered Software of a particular Contributor.

1.4. "Covered Software"
    means Source Code Form to which the initial Contributor has attached
    the notice in Exhibit A, the Executable Form of such Source Code
    Form, and Modifications of such Source Code Form, in each case
    including portions thereof.

1.5. "Incompatible With Secondary Licenses"
    means

    (a) that the initial Contributor has attached the notice described
        in Exhibit B to the Covered Software; or

    (b) that the Covered Software was made available under the terms of
        version 1.1 or earlier of the License, but not also under the
        terms of a Secondary License.

1.6. "Executable Form"
    means any form of the work other than Source Code Form.

1.7. "Larger Work"
    means a work that combines Covered Software with other material, in
    a separate file or files, that is not Covered Software.

1.8. "License"
    means this document.

1.9. "Licensable"
    means having the right to grant, to the maximum extent possible,
    whether at the time of the initial grant or subsequently, any and
    all of the rights conveyed by this License.

1.10. "Modifications"
    means any of the following:

    (a) any file in Source Code Form that results from an addition to,
        deletion from, or modification of the contents of Covered
        Software; or

    (b) any new file in Source Code Form that contains any Covered
        Software.

1.11. "Patent Claims" of a Contributor
    means any patent claim(s), including without limitation, method,
    process, and apparatus claims, in any patent Licensable by such
    Contributor that would be infringed, but for the grant of the
    License, by the making, using, selling, offering for sale, having
    made, import, or transfer of either its Contributions or its
    Contributor Version.

1.12. "Secondary License"
    means either the GNU General Public License, Version 2.0, the GNU
    Lesser General Public License, Version 2.1, the GNU Affero General
    Public License, Version 3.0, or any later versions of those
    licenses.

1.13. "Source Code Form"
    means the form of the work preferred for making modifications.

1.14. "You" (or "Your")
    means an individual or a legal entity exercising rights under this
    License. For legal entities, "You" includes any entity that
    controls, is controlled by, or is under common control with You. For
    purposes of this definition, "control" means (a) the power, direct
    or indirect, to cause the direction or management of such entity,
    whether by contract or otherwise, or (b) ownership of more than
    fifty percent (50%) of the outstanding shares or beneficial
    ownership of such entity.

2. License Grants and Conditions
--------------------------------

2.1. Grants

Each Contributor hereby grants You a world-wide, royalty-free,
non-exclusive license:

(a) under intellectual property rights (other than patent or trademark)
    Licensable by such Contributor to use, reproduce, make available,
    modify, display, perform, distribute, and otherwise exploit its
    Contributions, either on an unmodified basis, with Modifications, or
    as part of a Larger Work; and

(b) under Patent Claims of such Contributor to make, use, sell, offer
    for sale, have made, import, and otherwise transfer either its
    Contributions or its Contributor Version.

2.2. Effective Date

The licenses granted in Section 2.1 with respect to any Contribution
become effective for each Contribution on the date the Contributor first
distributes such Contribution.

2.3. Limitations on Grant Scope

The licenses granted in this Section 2 are the only rights granted under
this License. No additional rights or licenses will be implied from the
distribution or licensing of Covered Software under this License.
Notwithstanding Section 2.1(b) above, no patent license is granted by a
Contributor:

(a) for any code that a Contributor has removed from Covered Software;
    or

(b) for infringements caused by: (i) Your and any other third party's
    modifications of Covered Software, or (ii) the combination of its
    Contributions with other software (except as part of its Contributor
    Version); or

(c) under Patent Claims infringed by Covered Software in the absence of
    its Contributions.

This License does not grant any rights in the trademarks, service marks,
or logos of any Contributor (except as may be necessary to comply with
the notice requirements in Section 3.4).

2.4. Subsequent Licenses

No Contributor makes additional grants as a result of Your choice to
distribute the Covered Software under a subsequent version of this
License (see Section 10.2) or under the terms of a Secondary License (if
permitted under the terms of Section 3.3).

2.5. Representation

Each Contributor represents that the Contributor believes its
Contributions are its original creation(s) or it has sufficient rights
to grant the rights to its Contributions conveyed by this License.

2.6. Fair Use

This License is not intended to limit any rights You have under
applicable copyright doctrines of fair use, fair dealing, or other
equivalents.

2.7. Conditions

Sections 3.1, 3.2, 3.3, and 3.4 are conditions of the licenses granted
in Section 2.1.

3. Responsibilities
-------------------

3.1. Distribution of Source Form

All distribution of Covered Software in Source Code Form, including any
Modifications that You create or to which You contribute, must be under
the terms of this License. You must inform recipients that the Source
Code Form of the Covered Software is governed by the terms of this
License, and how they can obtain a copy of this License. You may not
attempt to alter or restrict the recipients' rights in the Source Code
Form.

3.2. Distribution of Executable Form

If You distribute Covered Software in Executable Form then:

(a) such Covered Software must also be made available in Source Code
    Form, as described in Section 3.1, and You must inform recipients of
    the Executable Form how they can obtain a copy of such Source Code
    Form by reasonable means in a timely manner, at a charge no more
    than the cost of distribution to the recipient; and

(b) You may distribute such Executable Form under the terms of this
    License, or sublicense it under different terms, provided that the
    license for the Executable Form does not attempt to limit or alter
    the recipients' rights in the Source Code Form under this License.

3.3. Distribution of a Larger Work

You may create and distribute a Larger Work under terms of Your choice,
provided that You also comply with the requirements of this License for
the Covered Software. If the Larger Work is a combination of Covered
Software with a work governed by one or more Secondary Licenses, and the
Covered Software is not Incompatible With Secondary Licenses, this
License permits You to additionally distribute such Covered Software
under the terms of such Secondary License(s), so that the recipient of
the Larger Work may, at their option, further distribute the Covered
Software under the terms of either this License or such Secondary
License(s).

3.4. Notices

You may not remove or alter the substance of any license notices
(including copyright notices, patent notices, disclaimers of warranty,
or limitations of liability) contained within the Source Code Form of
the Covered Software, except that You may alter any license notices to
the extent required to remedy known factual inaccuracies.

3.5. Application of Additional Terms

You may choose to offer, and to charge a fee for, warranty, support,
indemnity or liability obligations to one or more recipients of Covered
Software. However, You may do so only on Your own behalf, and not on
behalf of any Contributor. You must make it absolutely clear that any
such warranty, support, indemnity, or liability obligation is offered by
You alone, and You hereby agree to indemnify every Contributor for any
liability incurred by such Contributor as a result of warranty, support,
indemnity or liability terms You offer. You may include additional
disclaimers of warranty and limitations of liability specific to any
jurisdiction.

4. Inability to Comply Due to Statute or Regulation
---------------------------------------------------

If it is impossible for You to comply with any of the terms of this
License with respect to some or all of the Covered Software due to
statute, judicial order, or regulation then You must: (a) comply with
the terms of this License to the maximum extent possible; and (b)
describe the limitations and the code they affect. Such description must
be placed in a text file included with all distributions of the Covered
Software under this License. Except to the extent prohibited by statute
or regulation, such description must be sufficiently detailed for a
recipient of ordinary skill to be able to understand it.

5. Termination
--------------

5.1. The rights granted under this License will terminate automatically
if You fail to comply with any of its terms. However, if You become
compliant, then the rights granted under this License from a particular
Contributor are reinstated (a) provisionally, unless and until such
Contributor explicitly and finally terminates Your grants, and (b) on an
ongoing basis, if such Contributor fails to notify You of the
non-compliance by some reasonable means prior to 60 days after You have
come back into compliance. Moreover, Your grants from a particular
Contributor are reinstated on an ongoing basis if such Contributor
notifies You of the non-compliance by some reasonable means, this is the
first time You have received notice of non-compliance with this License
from such Contributor, and You become compliant prior to 30 days after
Your receipt of the notice.

5.2. If You initiate litigation against any entity by asserting a patent
infringement claim (excluding declaratory judgment actions,
counter-claims, and cross-claims) alleging that a Contributor Version
directly or indirectly infringes any patent, then the rights granted to
You by any and all Contributors for the Covered Software under Section
2.1 of this License shall terminate.

5.3. In the event of termination under Sections 5.1 or 5.2 above, all
end user license agreements (excluding distributors and resellers) which
have been validly granted by You or Your distributors under this License
prior to termination shall survive termination.

************************************************************************
*                                                                      *
*  6. Disclaimer of Warranty                                           *
*  -------------------------                                           *
*                                                                      *
*  Covered Software is provided under this License on an "as is"       *
*  basis, without warranty of any kind, either expressed, implied, or  *
*  statutory, including, without limitation, warranties that the       *
*  Covered Software is free of defects, merchantable, fit for a        *
*  particular purpose or non-infringing. The entire risk as to the     *
*  quality and performance of the Covered Software is with You.        *
*  Should any Covered Software prove defective in any respect, You     *
*  (not any Contributor) assume the cost of any necessary servicing,   *
*  repair, or correction. This disclaimer of warranty constitutes an   *
*  essential part of this License. No use of any Covered Software is   *
*  authorized under this License except under this disclaimer.         *
*                                                                      *
************************************************************************

************************************************************************
*                                                                      *
*  7. Limitation of Liability                                          *
*  --------------------------                                          *
*                                                                      *
*  Under no circumstances and under no legal theory, whether tort      *
*  (including negligence), contract, or otherwise, shall any           *
*  Contributor, or anyone who distributes Covered Software as          *
*  permitted above, be liable to You for any direct, indirect,         *
*  special, incidental, or consequential damages of any character      *
*  including, without limitation, damages for lost profits, loss of    *
*  goodwill, work stoppage, computer failure or malfunction, or any    *
*  and all other commercial damages or losses, even if such party      *
*  shall have been informed of the possibility of such damages. This   *
*  limitation of liability shall not apply to liability for death or   *
*  personal injury resulting from such party's negligence to the       *
*  extent applicable law prohibits such limitation. Some               *
*  jurisdictions do not allow the exclusion or limitation of           *
*  incidental or consequential damages, so this exclusion and          *
*  limitation may not apply to You.                                    *
*                                                                      *
************************************************************************

8. Litigation
-------------

Any litigation relating to this License may be brought only in the
courts of a jurisdiction where the defendant maintains its principal
place of business and such litigation shall be governed by laws of that
jurisdiction, without reference to its conflict-of-law provisions.
Nothing in this Section shall prevent a party's ability to bring
cross-claims or counter-claims.

9. Miscellaneous
----------------

This License represents the complete agreement concerning the subject
matter hereof. If any provision of this License is held to be
unenforceable, such provision shall be reformed only to the extent
necessary to make it enforceable. Any law or regulation which provides
that the language of a contract shall be construed against the drafter
shall not be used to construe this License against a Contributor.

10. Versions of the License
---------------------------

10.1. New Versions

Mozilla Foundation is the license steward. Except as provided in Section
10.3, no one other than the license steward has the right to modify or
publish new versions of this License. Each version will be given a
distinguishing version number.

10.2. Effect of New Versions

You may distribute the Covered Software under the terms of the version
of the License under which You originally received the Covered Software,
or under the terms of any subsequent version published by the license
steward.

10.3. Modified Versions

If you create software not governed by this License, and you want to
create a new license for such software, you may create and use a
modified version of this License if you rename the license and remove
any references to the name of the license steward (except to note that
such modified license differs from this License).

10.4. Distributing Source Code Form that is Incompatible With Secondary
Licenses

If You choose to distribute Source Code Form that is Incompatible With
Secondary Licenses under the terms of this version of the License, the
notice described in Exhibit B of this License must be attached.

Exhibit A - Source Code Form License Notice
-------------------------------------------

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at https://mozilla.org/MPL/2.0/.

If it is not possible or desirable to put the notice in a particular
file, then You may include the notice in a location (such as a LICENSE
file in a relevant directory) where a recipient would be likely to look
for such a notice.

You may add additional accurate notices of copyright ownership.

Exhibit B - "Incompatible With Secondary Licenses" Notice
---------------------------------------------------------

  This Source Code Form is "Incompatible With Secondary Licenses", as
  defined by the Mozilla Public License, v. 2.0.
```

### Mozilla Public License Version 1.1

```
                          MOZILLA PUBLIC LICENSE
                                Version 1.1

                              ---------------

1. Definitions.

     1.0.1. "Commercial Use" means distribution or otherwise making the
     Covered Code available to a third party.

     1.1. "Contributor" means each entity that creates or contributes to
     the creation of Modifications.

     1.2. "Contributor Version" means the combination of the Original
     Code, prior Modifications used by a Contributor, and the Modifications
     made by that particular Contributor.

     1.3. "Covered Code" means the Original Code or Modifications or the
     combination of the Original Code and Modifications, in each case
     including portions thereof.

     1.4. "Electronic Distribution Mechanism" means a mechanism generally
     accepted in the software development community for the electronic
     transfer of data.

     1.5. "Executable" means Covered Code in any form other than Source
     Code.

     1.6. "Initial Developer" means the individual or entity identified
     as the Initial Developer in the Source Code notice required by Exhibit
     A.

     1.7. "Larger Work" means a work which combines Covered Code or
     portions thereof with code not governed by the terms of this License.

     1.8. "License" means this document.

     1.8.1. "Licensable" means having the right to grant, to the maximum
     extent possible, whether at the time of the initial grant or
     subsequently acquired, any and all of the rights conveyed herein.

     1.9. "Modifications" means any addition to or deletion from the
     substance or structure of either the Original Code or any previous
     Modifications. When Covered Code is released as a series of files, a
     Modification is:
          A. Any addition to or deletion from the contents of a file
          containing Original Code or previous Modifications.

          B. Any new file that contains any part of the Original Code or
          previous Modifications.

     1.10. "Original Code" means Source Code of computer software code
     which is described in the Source Code notice required by Exhibit A as
     Original Code, and which, at the time of its release under this
     License is not already Covered Code governed by this License.

     1.10.1. "Patent Claims" means any patent claim(s), now owned or
     hereafter acquired, including without limitation,  method, process,
     and apparatus claims, in any patent Licensable by grantor.

     1.11. "Source Code" means the preferred form of the Covered Code for
     making modifications to it, including all modules it contains, plus
     any associated interface definition files, scripts used to control
     compilation and installation of an Executable, or source code
     differential comparisons against either the Original Code or another
     well known, available Covered Code of the Contributor's choice. The
     Source Code can be in a compressed or archival form, provided the
     appropriate decompression or de-archiving software is widely available
     for no charge.

     1.12. "You" (or "Your")  means an individual or a legal entity
     exercising rights under, and complying with all of the terms of, this
     License or a future version of this License issued under Section 6.1.
     For legal entities, "You" includes any entity which controls, is
     controlled by, or is under common control with You. For purposes of
     this definition, "control" means (a) the power, direct or indirect,
     to cause the direction or management of such entity, whether by
     contract or otherwise, or (b) ownership of more than fifty percent
     (50%) of the outstanding shares or beneficial ownership of such
     entity.

2. Source Code License.

     2.1. The Initial Developer Grant.
     The Initial Developer hereby grants You a world-wide, royalty-free,
     non-exclusive license, subject to third party intellectual property
     claims:
          (a)  under intellectual property rights (other than patent or
          trademark) Licensable by Initial Developer to use, reproduce,
          modify, display, perform, sublicense and distribute the Original
          Code (or portions thereof) with or without Modifications, and/or
          as part of a Larger Work; and

          (b) under Patents Claims infringed by the making, using or
          selling of Original Code, to make, have made, use, practice,
          sell, and offer for sale, and/or otherwise dispose of the
          Original Code (or portions thereof).

          (c) the licenses granted in this Section 2.1(a) and (b) are
          effective on the date Initial Developer first distributes
          Original Code under the terms of this License.

          (d) Notwithstanding Section 2.1(b) above, no patent license is
          granted: 1) for code that You delete from the Original Code; 2)
          separate from the Original Code;  or 3) for infringements caused
          by: i) the modification of the Original Code or ii) the
          combination of the Original Code with other software or devices.

     2.2. Contributor Grant.
     Subject to third party intellectual property claims, each Contributor
     hereby grants You a world-wide, royalty-free, non-exclusive license

          (a)  under intellectual property rights (other than patent or
          trademark) Licensable by Contributor, to use, reproduce, modify,
          display, perform, sublicense and distribute the Modifications
          created by such Contributor (or portions thereof) either on an
          unmodified basis, with other Modifications, as Covered Code
          and/or as part of a Larger Work; and

          (b) under Patent Claims infringed by the making, using, or
          selling of  Modifications made by that Contributor either alone
          and/or in combination with its Contributor Version (or portions
          of such combination), to make, use, sell, offer for sale, have
          made, and/or otherwise dispose of: 1) Modifications made by that
          Contributor (or portions thereof); and 2) the combination of
          Modifications made by that Contributor with its Contributor
          Version (or portions of such combination).

          (c) the licenses granted in Sections 2.2(a) and 2.2(b) are
          effective on the date Contributor first makes Commercial Use of
          the Covered Code.

          (d)    Notwithstanding Section 2.2(b) above, no patent license is
          granted: 1) for any code that Contributor has deleted from the
          Contributor Version; 2)  separate from the Contributor Version;
          3)  for infringements caused by: i) third party modifications of
          Contributor Version or ii)  the combination of Modifications made
          by that Contributor with other software  (except as part of the
          Contributor Version) or other devices; or 4) under Patent Claims
          infringed by Covered Code in the absence of Modifications made by
          that Contributor.

3. Distribution Obligations.

     3.1. Application of License.
     The Modifications which You create or to which You contribute are
     governed by the terms of this License, including without limitation
     Section 2.2. The Source Code version of Covered Code may be
     distributed only under the terms of this License or a future version
     of this License released under Section 6.1, and You must include a
     copy of this License with every copy of the Source Code You
     distribute. You may not offer or impose any terms on any Source Code
     version that alters or restricts the applicable version of this
     License or the recipients' rights hereunder. However, You may include
     an additional document offering the additional rights described in
     Section 3.5.

     3.2. Availability of Source Code.
     Any Modification which You create or to which You contribute must be
     made available in Source Code form under the terms of this License
     either on the same media as an Executable version or via an accepted
     Electronic Distribution Mechanism to anyone to whom you made an
     Executable version available; and if made available via Electronic
     Distribution Mechanism, must remain available for at least twelve (12)
     months after the date it initially became available, or at least six
     (6) months after a subsequent version of that particular Modification
     has been made available to such recipients. You are responsible for
     ensuring that the Source Code version remains available even if the
     Electronic Distribution Mechanism is maintained by a third party.

     3.3. Description of Modifications.
     You must cause all Covered Code to which You contribute to contain a
     file documenting the changes You made to create that Covered Code and
     the date of any change. You must include a prominent statement that
     the Modification is derived, directly or indirectly, from Original
     Code provided by the Initial Developer and including the name of the
     Initial Developer in (a) the Source Code, and (b) in any notice in an
     Executable version or related documentation in which You describe the
     origin or ownership of the Covered Code.

     3.4. Intellectual Property Matters
          (a) Third Party Claims.
          If Contributor has knowledge that a license under a third party's
          intellectual property rights is required to exercise the rights
          granted by such Contributor under Sections 2.1 or 2.2,
          Contributor must include a text file with the Source Code
          distribution titled "LEGAL" which describes the claim and the
          party making the claim in sufficient detail that a recipient will
          know whom to contact. If Contributor obtains such knowledge after
          the Modification is made available as described in Section 3.2,
          Contributor shall promptly modify the LEGAL file in all copies
          Contributor makes available thereafter and shall take other steps
          (such as notifying appropriate mailing lists or newsgroups)
          reasonably calculated to inform those who received the Covered
          Code that new knowledge has been obtained.

          (b) Contributor APIs.
          If Contributor's Modifications include an application programming
          interface and Contributor has knowledge of patent licenses which
          are reasonably necessary to implement that API, Contributor must
          also include this information in the LEGAL file.

               (c)    Representations.
          Contributor represents that, except as disclosed pursuant to
          Section 3.4(a) above, Contributor believes that Contributor's
          Modifications are Contributor's original creation(s) and/or
          Contributor has sufficient rights to grant the rights conveyed by
          this License.

     3.5. Required Notices.
     You must duplicate the notice in Exhibit A in each file of the Source
     Code.  If it is not possible to put such notice in a particular Source
     Code file due to its structure, then You must include such notice in a
     location (such as a relevant directory) where a user would be likely
     to look for such a notice.  If You created one or more Modification(s)
     You may add your name as a Contributor to the notice described in
     Exhibit A.  You must also duplicate this License in any documentation
     for the Source Code where You describe recipients' rights or ownership
     rights relating to Covered Code.  You may choose to offer, and to
     charge a fee for, warranty, support, indemnity or liability
     obligations to one or more recipients of Covered Code. However, You
     may do so only on Your own behalf, and not on behalf of the Initial
     Developer or any Contributor. You must make it absolutely clear than
     any such warranty, support, indemnity or liability obligation is
     offered by You alone, and You hereby agree to indemnify the Initial
     Developer and every Contributor for any liability incurred by the
     Initial Developer or such Contributor as a result of warranty,
     support, indemnity or liability terms You offer.

     3.6. Distribution of Executable Versions.
     You may distribute Covered Code in Executable form only if the
     requirements of Section 3.1-3.5 have been met for that Covered Code,
     and if You include a notice stating that the Source Code version of
     the Covered Code is available under the terms of this License,
     including a description of how and where You have fulfilled the
     obligations of Section 3.2. The notice must be conspicuously included
     in any notice in an Executable version, related documentation or
     collateral in which You describe recipients' rights relating to the
     Covered Code. You may distribute the Executable version of Covered
     Code or ownership rights under a license of Your choice, which may
     contain terms different from this License, provided that You are in
     compliance with the terms of this License and that the license for the
     Executable version does not attempt to limit or alter the recipient's
     rights in the Source Code version from the rights set forth in this
     License. If You distribute the Executable version under a different
     license You must make it absolutely clear that any terms which differ
     from this License are offered by You alone, not by the Initial
     Developer or any Contributor. You hereby agree to indemnify the
     Initial Developer and every Contributor for any liability incurred by
     the Initial Developer or such Contributor as a result of any such
     terms You offer.

     3.7. Larger Works.
     You may create a Larger Work by combining Covered Code with other code
     not governed by the terms of this License and distribute the Larger
     Work as a single product. In such a case, You must make sure the
     requirements of this License are fulfilled for the Covered Code.

4. Inability to Comply Due to Statute or Regulation.

     If it is impossible for You to comply with any of the terms of this
     License with respect to some or all of the Covered Code due to
     statute, judicial order, or regulation then You must: (a) comply with
     the terms of this License to the maximum extent possible; and (b)
     describe the limitations and the code they affect. Such description
     must be included in the LEGAL file described in Section 3.4 and must
     be included with all distributions of the Source Code. Except to the
     extent prohibited by statute or regulation, such description must be
     sufficiently detailed for a recipient of ordinary skill to be able to
     understand it.

5. Application of this License.

     This License applies to code to which the Initial Developer has
     attached the notice in Exhibit A and to related Covered Code.

6. Versions of the License.

     6.1. New Versions.
     Netscape Communications Corporation ("Netscape") may publish revised
     and/or new versions of the License from time to time. Each version
     will be given a distinguishing version number.

     6.2. Effect of New Versions.
     Once Covered Code has been published under a particular version of the
     License, You may always continue to use it under the terms of that
     version. You may also choose to use such Covered Code under the terms
     of any subsequent version of the License published by Netscape. No one
     other than Netscape has the right to modify the terms applicable to
     Covered Code created under this License.

     6.3. Derivative Works.
     If You create or use a modified version of this License (which you may
     only do in order to apply it to code which is not already Covered Code
     governed by this License), You must (a) rename Your license so that
     the phrases "Mozilla", "MOZILLAPL", "MOZPL", "Netscape",
     "MPL", "NPL" or any confusingly similar phrase do not appear in your
     license (except to note that your license differs from this License)
     and (b) otherwise make it clear that Your version of the license
     contains terms which differ from the Mozilla Public License and
     Netscape Public License. (Filling in the name of the Initial
     Developer, Original Code or Contributor in the notice described in
     Exhibit A shall not of themselves be deemed to be modifications of
     this License.)

7. DISCLAIMER OF WARRANTY.

     COVERED CODE IS PROVIDED UNDER THIS LICENSE ON AN "AS IS" BASIS,
     WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING,
     WITHOUT LIMITATION, WARRANTIES THAT THE COVERED CODE IS FREE OF
     DEFECTS, MERCHANTABLE, FIT FOR A PARTICULAR PURPOSE OR NON-INFRINGING.
     THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE COVERED CODE
     IS WITH YOU. SHOULD ANY COVERED CODE PROVE DEFECTIVE IN ANY RESPECT,
     YOU (NOT THE INITIAL DEVELOPER OR ANY OTHER CONTRIBUTOR) ASSUME THE
     COST OF ANY NECESSARY SERVICING, REPAIR OR CORRECTION. THIS DISCLAIMER
     OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE. NO USE OF
     ANY COVERED CODE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.

8. TERMINATION.

     8.1.  This License and the rights granted hereunder will terminate
     automatically if You fail to comply with terms herein and fail to cure
     such breach within 30 days of becoming aware of the breach. All
     sublicenses to the Covered Code which are properly granted shall
     survive any termination of this License. Provisions which, by their
     nature, must remain in effect beyond the termination of this License
     shall survive.

     8.2.  If You initiate litigation by asserting a patent infringement
     claim (excluding declatory judgment actions) against Initial Developer
     or a Contributor (the Initial Developer or Contributor against whom
     You file such action is referred to as "Participant")  alleging that:

     (a)  such Participant's Contributor Version directly or indirectly
     infringes any patent, then any and all rights granted by such
     Participant to You under Sections 2.1 and/or 2.2 of this License
     shall, upon 60 days notice from Participant terminate prospectively,
     unless if within 60 days after receipt of notice You either: (i)
     agree in writing to pay Participant a mutually agreeable reasonable
     royalty for Your past and future use of Modifications made by such
     Participant, or (ii) withdraw Your litigation claim with respect to
     the Contributor Version against such Participant.  If within 60 days
     of notice, a reasonable royalty and payment arrangement are not
     mutually agreed upon in writing by the parties or the litigation claim
     is not withdrawn, the rights granted by Participant to You under
     Sections 2.1 and/or 2.2 automatically terminate at the expiration of
     the 60 day notice period specified above.

     (b)  any software, hardware, or device, other than such Participant's
     Contributor Version, directly or indirectly infringes any patent, then
     any rights granted to You by such Participant under Sections 2.1(b)
     and 2.2(b) are revoked effective as of the date You first made, used,
     sold, distributed, or had made, Modifications made by that
     Participant.

     8.3.  If You assert a patent infringement claim against Participant
     alleging that such Participant's Contributor Version directly or
     indirectly infringes any patent where such claim is resolved (such as
     by license or settlement) prior to the initiation of patent
     infringement litigation, then the reasonable value of the licenses
     granted by such Participant under Sections 2.1 or 2.2 shall be taken
     into account in determining the amount or value of any payment or
     license.

     8.4.  In the event of termination under Sections 8.1 or 8.2 above,
     all end user license agreements (excluding distributors and resellers)
     which have been validly granted by You or any distributor hereunder
     prior to termination shall survive termination.

9. LIMITATION OF LIABILITY.

     UNDER NO CIRCUMSTANCES AND UNDER NO LEGAL THEORY, WHETHER TORT
     (INCLUDING NEGLIGENCE), CONTRACT, OR OTHERWISE, SHALL YOU, THE INITIAL
     DEVELOPER, ANY OTHER CONTRIBUTOR, OR ANY DISTRIBUTOR OF COVERED CODE,
     OR ANY SUPPLIER OF ANY OF SUCH PARTIES, BE LIABLE TO ANY PERSON FOR
     ANY INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES OF ANY
     CHARACTER INCLUDING, WITHOUT LIMITATION, DAMAGES FOR LOSS OF GOODWILL,
     WORK STOPPAGE, COMPUTER FAILURE OR MALFUNCTION, OR ANY AND ALL OTHER
     COMMERCIAL DAMAGES OR LOSSES, EVEN IF SUCH PARTY SHALL HAVE BEEN
     INFORMED OF THE POSSIBILITY OF SUCH DAMAGES. THIS LIMITATION OF
     LIABILITY SHALL NOT APPLY TO LIABILITY FOR DEATH OR PERSONAL INJURY
     RESULTING FROM SUCH PARTY'S NEGLIGENCE TO THE EXTENT APPLICABLE LAW
     PROHIBITS SUCH LIMITATION. SOME JURISDICTIONS DO NOT ALLOW THE
     EXCLUSION OR LIMITATION OF INCIDENTAL OR CONSEQUENTIAL DAMAGES, SO
     THIS EXCLUSION AND LIMITATION MAY NOT APPLY TO YOU.

10. U.S. GOVERNMENT END USERS.

     The Covered Code is a "commercial item," as that term is defined in
     48 C.F.R. 2.101 (Oct. 1995), consisting of "commercial computer
     software" and "commercial computer software documentation," as such
     terms are used in 48 C.F.R. 12.212 (Sept. 1995). Consistent with 48
     C.F.R. 12.212 and 48 C.F.R. 227.7202-1 through 227.7202-4 (June 1995),
     all U.S. Government End Users acquire Covered Code with only those
     rights set forth herein.

11. MISCELLANEOUS.

     This License represents the complete agreement concerning subject
     matter hereof. If any provision of this License is held to be
     unenforceable, such provision shall be reformed only to the extent
     necessary to make it enforceable. This License shall be governed by
     California law provisions (except to the extent applicable law, if
     any, provides otherwise), excluding its conflict-of-law provisions.
     With respect to disputes in which at least one party is a citizen of,
     or an entity chartered or registered to do business in the United
     States of America, any litigation relating to this License shall be
     subject to the jurisdiction of the Federal Courts of the Northern
     District of California, with venue lying in Santa Clara County,
     California, with the losing party responsible for costs, including
     without limitation, court costs and reasonable attorneys' fees and
     expenses. The application of the United Nations Convention on
     Contracts for the International Sale of Goods is expressly excluded.
     Any law or regulation which provides that the language of a contract
     shall be construed against the drafter shall not apply to this
     License.

12. RESPONSIBILITY FOR CLAIMS.

     As between Initial Developer and the Contributors, each party is
     responsible for claims and damages arising, directly or indirectly,
     out of its utilization of rights under this License and You agree to
     work with Initial Developer and Contributors to distribute such
     responsibility on an equitable basis. Nothing herein is intended or
     shall be deemed to constitute any admission of liability.

13. MULTIPLE-LICENSED CODE.

     Initial Developer may designate portions of the Covered Code as
     "Multiple-Licensed".  "Multiple-Licensed" means that the Initial
     Developer permits you to utilize portions of the Covered Code under
     Your choice of the MPL or the alternative licenses, if any, specified
     by the Initial Developer in the file described in Exhibit A.

EXHIBIT A -Mozilla Public License.

     ``The contents of this file are subject to the Mozilla Public License
     Version 1.1 (the "License"); you may not use this file except in
     compliance with the License. You may obtain a copy of the License at
     https://www.mozilla.org/MPL/

     Software distributed under the License is distributed on an "AS IS"
     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
     License for the specific language governing rights and limitations
     under the License.

     The Original Code is ______________________________________.

     The Initial Developer of the Original Code is ________________________.
     Portions created by ______________________ are Copyright (C) ______
     _______________________. All Rights Reserved.

     Contributor(s): ______________________________________.

     Alternatively, the contents of this file may be used under the terms
     of the _____ license (the  "[___] License"), in which case the
     provisions of [______] License are applicable instead of those
     above.  If you wish to allow use of your version of this file only
     under the terms of the [____] License and not to allow others to use
     your version of this file under the MPL, indicate your decision by
     deleting  the provisions above and replace  them with the notice and
     other provisions required by the [___] License.  If you do not delete
     the provisions above, a recipient may use your version of this file
     under either the MPL or the [___] License."

     [NOTE: The text of this Exhibit A may differ slightly from the text of
     the notices in the Source Code files of the Original Code. You should
     use the text of this Exhibit A rather than the text found in the
     Original Code Source Code for Your Modifications.]
```

### mysql-connector-j

```
Licensing Information User Manual

MySQL Connector/J 8.0
     __________________________________________________________________

Introduction

   This License Information User Manual contains Oracle's product license
   and other licensing information, including licensing information for
   third-party software which may be included in this distribution of
   MySQL Connector/J 8.0.

   Last updated: June 2020

Licensing Information

   This is a release of MySQL Connector/J 8.0, brought to you by the MySQL
   team at Oracle. This software is released under version 2 of the GNU
   General Public License (GPLv2), as set forth below, with the following
   additional permissions:

   This distribution of MySQL Connector/J 8.0 is distributed with certain
   software that is licensed under separate terms, as designated in a
   particular file or component or in the license documentation. Without
   limiting your rights under the GPLv2, the authors of MySQL hereby grant
   you an additional permission to link the program and your derivative
   works with the separately licensed software that they have included
   with the program.

   Without limiting the foregoing grant of rights under the GPLv2 and
   additional permission as to separately licensed software, this
   Connector is also subject to the Universal FOSS Exception, version 1.0,
   a copy of which is reproduced below and can also be found along with
   its FAQ at http://oss.oracle.com/licenses/universal-foss-exception.

   Copyright (c) 2017, 2020, Oracle and/or its affiliates.

Election of GPLv2

   For the avoidance of doubt, except that if any license choice other
   than GPL or LGPL is available it will apply instead, Oracle elects to
   use only the General Public License version 2 (GPLv2) at this time for
   any software where a choice of GPL license versions is made available
   with the language indicating that GPLv2 or any later version may be
   used, or where a choice of which version of the GPL is applied is
   otherwise unspecified.

GNU General Public License Version 2.0, June 1991

The following applies to all products licensed under the GNU General
Public License, Version 2.0: You may not use the identified files
except in compliance with the GNU General Public License, Version
2.0 (the "License.") You may obtain a copy of the License at
http://www.gnu.org/licenses/gpl-2.0.txt. A copy of the license is
also reproduced below. Unless required by applicable law or agreed
to in writing, software distributed under the License is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied. See the License for the specific language
governing permissions and limitations under the License.

GNU GENERAL PUBLIC LICENSE
Version 2, June 1991

Copyright (C) 1989, 1991 Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
Everyone is permitted to copy and distribute verbatim
copies of this license document, but changing it is not
allowed.

                     Preamble

  The licenses for most software are designed to take away your
freedom to share and change it.  By contrast, the GNU General Public
License is intended to guarantee your freedom to share and change free
software--to make sure the software is free for all its users.  This
General Public License applies to most of the Free Software
Foundation's software and to any other program whose authors commit to
using it.  (Some other Free Software Foundation software is covered by
the GNU Lesser General Public License instead.)  You can apply it to
your programs, too.

  When we speak of free software, we are referring to freedom, not
price.  Our General Public Licenses are designed to make sure that you
have the freedom to distribute copies of free software (and charge for
this service if you wish), that you receive source code or can get it
if you want it, that you can change the software or use pieces of it
in new free programs; and that you know you can do these things.

  To protect your rights, we need to make restrictions that forbid
anyone to deny you these rights or to ask you to surrender the rights.
These restrictions translate to certain responsibilities for you if you
distribute copies of the software, or if you modify it.

  For example, if you distribute copies of such a program, whether
gratis or for a fee, you must give the recipients all the rights that
you have.  You must make sure that they, too, receive or can get the
source code.  And you must show them these terms so they know their
rights.

  We protect your rights with two steps: (1) copyright the software,
and (2) offer you this license which gives you legal permission to
copy, distribute and/or modify the software.

  Also, for each author's protection and ours, we want to make certain
that everyone understands that there is no warranty for this free
software.  If the software is modified by someone else and passed on,
we want its recipients to know that what they have is not the original,
so that any problems introduced by others will not reflect on the
original authors' reputations.

  Finally, any free program is threatened constantly by software
patents.  We wish to avoid the danger that redistributors of a free
program will individually obtain patent licenses, in effect making the
program proprietary.  To prevent this, we have made it clear that any
patent must be licensed for everyone's free use or not licensed at all.

  The precise terms and conditions for copying, distribution and
modification follow.

                    GNU GENERAL PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

  0. This License applies to any program or other work which contains
a notice placed by the copyright holder saying it may be distributed
under the terms of this General Public License.  The "Program", below,
refers to any such program or work, and a "work based on the Program"
means either the Program or any derivative work under copyright law:
that is to say, a work containing the Program or a portion of it,
either verbatim or with modifications and/or translated into another
language.  (Hereinafter, translation is included without limitation in
the term "modification".)  Each licensee is addressed as "you".

Activities other than copying, distribution and modification are not
covered by this License; they are outside its scope.  The act of
running the Program is not restricted, and the output from the Program
is covered only if its contents constitute a work based on the
Program (independent of having been made by running the Program).
Whether that is true depends on what the Program does.


  1. You may copy and distribute verbatim copies of the Program's
source code as you receive it, in any medium, provided that you
conspicuously and appropriately publish on each copy an appropriate
copyright notice and disclaimer of warranty; keep intact all the
notices that refer to this License and to the absence of any warranty;
and give any other recipients of the Program a copy of this License
along with the Program.

You may charge a fee for the physical act of transferring a copy, and
you may at your option offer warranty protection in exchange for a fee.


  2. You may modify your copy or copies of the Program or any portion
of it, thus forming a work based on the Program, and copy and
distribute such modifications or work under the terms of Section 1
above, provided that you also meet all of these conditions:

    a) You must cause the modified files to carry prominent notices
    stating that you changed the files and the date of any change.

    b) You must cause any work that you distribute or publish, that in
    whole or in part contains or is derived from the Program or any
    part thereof, to be licensed as a whole at no charge to all third
    parties under the terms of this License.

    c) If the modified program normally reads commands interactively
    when run, you must cause it, when started running for such
    interactive use in the most ordinary way, to print or display an
    announcement including an appropriate copyright notice and a
    notice that there is no warranty (or else, saying that you provide
    a warranty) and that users may redistribute the program under
    these conditions, and telling the user how to view a copy of this
    License.  (Exception: if the Program itself is interactive but
    does not normally print such an announcement, your work based on
    the Program is not required to print an announcement.)

These requirements apply to the modified work as a whole.  If
identifiable sections of that work are not derived from the Program,
and can be reasonably considered independent and separate works in
themselves, then this License, and its terms, do not apply to those
sections when you distribute them as separate works.  But when you
distribute the same sections as part of a whole which is a work based
on the Program, the distribution of the whole must be on the terms of
this License, whose permissions for other licensees extend to the
entire whole, and thus to each and every part regardless of who wrote it.

Thus, it is not the intent of this section to claim rights or contest
your rights to work written entirely by you; rather, the intent is to
exercise the right to control the distribution of derivative or
collective works based on the Program.

In addition, mere aggregation of another work not based on the Program
with the Program (or with a work based on the Program) on a volume of
a storage or distribution medium does not bring the other work under
the scope of this License.


  3. You may copy and distribute the Program (or a work based on it,
under Section 2) in object code or executable form under the terms of
Sections 1 and 2 above provided that you also do one of the following:

    a) Accompany it with the complete corresponding machine-readable
    source code, which must be distributed under the terms of Sections
    1 and 2 above on a medium customarily used for software
    interchange; or,

    b) Accompany it with a written offer, valid for at least three
    years, to give any third party, for a charge no more than your
    cost of physically performing source distribution, a complete
    machine-readable copy of the corresponding source code, to be
    distributed under the terms of Sections 1 and 2 above on a medium
    customarily used for software interchange; or,

    c) Accompany it with the information you received as to the offer
    to distribute corresponding source code.  (This alternative is
    allowed only for noncommercial distribution and only if you
    received the program in object code or executable form with such
    an offer, in accord with Subsection b above.)

The source code for a work means the preferred form of the work for
making modifications to it.  For an executable work, complete source
code means all the source code for all modules it contains, plus any
associated interface definition files, plus the scripts used to
control compilation and installation of the executable.  However, as
a special exception, the source code distributed need not include
anything that is normally distributed (in either source or binary
form) with the major components (compiler, kernel, and so on) of the
operating system on which the executable runs, unless that component
itself accompanies the executable.

If distribution of executable or object code is made by offering
access to copy from a designated place, then offering equivalent
access to copy the source code from the same place counts as
distribution of the source code, even though third parties are not
compelled to copy the source along with the object code.


  4. You may not copy, modify, sublicense, or distribute the Program
except as expressly provided under this License.  Any attempt
otherwise to copy, modify, sublicense or distribute the Program is
void, and will automatically terminate your rights under this License.
However, parties who have received copies, or rights, from you under
this License will not have their licenses terminated so long as such
parties remain in full compliance.


  5. You are not required to accept this License, since you have not
signed it.  However, nothing else grants you permission to modify or
distribute the Program or its derivative works.  These actions are
prohibited by law if you do not accept this License.  Therefore, by
modifying or distributing the Program (or any work based on the
Program), you indicate your acceptance of this License to do so, and
all its terms and conditions for copying, distributing or modifying
the Program or works based on it.


  6. Each time you redistribute the Program (or any work based on the
Program), the recipient automatically receives a license from the
original licensor to copy, distribute or modify the Program subject to
these terms and conditions.  You may not impose any further
restrictions on the recipients' exercise of the rights granted herein.
You are not responsible for enforcing compliance by third parties to
this License.


  7. If, as a consequence of a court judgment or allegation of patent
infringement or for any other reason (not limited to patent issues),
conditions are imposed on you (whether by court order, agreement or
otherwise) that contradict the conditions of this License, they do not
excuse you from the conditions of this License.  If you cannot
distribute so as to satisfy simultaneously your obligations under this
License and any other pertinent obligations, then as a consequence you
may not distribute the Program at all.  For example, if a patent
license would not permit royalty-free redistribution of the Program by
all those who receive copies directly or indirectly through you, then
the only way you could satisfy both it and this License would be to
refrain entirely from distribution of the Program.

If any portion of this section is held invalid or unenforceable under
any particular circumstance, the balance of the section is intended to
apply and the section as a whole is intended to apply in other
circumstances.

It is not the purpose of this section to induce you to infringe any
patents or other property right claims or to contest validity of any
such claims; this section has the sole purpose of protecting the
integrity of the free software distribution system, which is
implemented by public license practices.  Many people have made
generous contributions to the wide range of software distributed
through that system in reliance on consistent application of that
system; it is up to the author/donor to decide if he or she is willing
to distribute software through any other system and a licensee cannot
impose that choice.

This section is intended to make thoroughly clear what is believed to
be a consequence of the rest of this License.


  8. If the distribution and/or use of the Program is restricted in
certain countries either by patents or by copyrighted interfaces, the
original copyright holder who places the Program under this License
may add an explicit geographical distribution limitation excluding
those countries, so that distribution is permitted only in or among
countries not thus excluded.  In such case, this License incorporates
the limitation as if written in the body of this License.


  9. The Free Software Foundation may publish revised and/or new
versions of the General Public License from time to time.  Such new
versions will be similar in spirit to the present version, but may
differ in detail to address new problems or concerns.

Each version is given a distinguishing version number.  If the Program
specifies a version number of this License which applies to it and
"any later version", you have the option of following the terms and
conditions either of that version or of any later version published by
the Free Software Foundation.  If the Program does not specify a
version number of this License, you may choose any version ever
published by the Free Software Foundation.

  10. If you wish to incorporate parts of the Program into other free
programs whose distribution conditions are different, write to the
author to ask for permission.  For software which is copyrighted by the
Free Software Foundation, write to the Free Software Foundation; we
sometimes make exceptions for this.  Our decision will be guided by the
two goals of preserving the free status of all derivatives of our free
software and of promoting the sharing and reuse of software generally.

                            NO WARRANTY

  11. BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO
WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW.
EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND,
EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS
WITH YOU.  SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF
ALL NECESSARY SERVICING, REPAIR OR CORRECTION.

  12. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN
WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY
AND/OR REDISTRIBUTE THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU
FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR
CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE
PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING
RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A
FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF
SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH
DAMAGES.

                     END OF TERMS AND CONDITIONS

            How to Apply These Terms to Your New Programs

  If you develop a new program, and you want it to be of the greatest
possible use to the public, the best way to achieve this is to make it
free software which everyone can redistribute and change under these terms.

  To do so, attach the following notices to the program.  It is safest
to attach them to the start of each source file to most effectively
convey the exclusion of warranty; and each file should have at least
the "copyright" line and a pointer to where the full notice is found.

    <one line to give the program's name and a brief idea of what it does.>
    Copyright (C) <year>  <name of author>

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License as
    published by the Free Software Foundation; either version 2 of

    the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
    02110-1301 USA.

Also add information on how to contact you by electronic and paper mail.

If the program is interactive, make it output a short notice like this
when it starts in an interactive mode:

    Gnomovision version 69, Copyright (C) year name of author
    Gnomovision comes with ABSOLUTELY NO WARRANTY; for details
    type 'show w'. This is free software, and you are welcome
    to redistribute it under certain conditions; type 'show c'
    for details.

The hypothetical commands 'show w' and 'show c' should show the
appropriate parts of the General Public License.  Of course, the
commands you use may be called something other than 'show w' and
'show c'; they could even be mouse-clicks or menu items--whatever
suits your program.

You should also get your employer (if you work as a programmer) or your
school, if any, to sign a "copyright disclaimer" for the program, if
necessary.  Here is a sample; alter the names:

  Yoyodyne, Inc., hereby disclaims all copyright interest in the
  program 'Gnomovision' (which makes passes at compilers) written
  by James Hacker.

  <signature of Ty Coon>, 1 April 1989
  Ty Coon, President of Vice

This General Public License does not permit incorporating your program
into proprietary programs.  If your program is a subroutine library,
you may consider it more useful to permit linking proprietary
applications with the library.  If this is what you want to do, use
the GNU Lesser General Public License instead of this License.

The Universal FOSS Exception, Version 1.0

   In addition to the rights set forth in the other license(s) included in
   the distribution for this software, data, and/or documentation
   (collectively the "Software", and such licenses collectively with this
   additional permission the "Software License"), the copyright holders
   wish to facilitate interoperability with other software, data, and/or
   documentation distributed with complete corresponding source under a
   license that is OSI-approved and/or categorized by the FSF as free
   (collectively "Other FOSS"). We therefore hereby grant the following
   additional permission with respect to the use and distribution of the
   Software with Other FOSS, and the constants, function signatures, data
   structures and other invocation methods used to run or interact with
   each of them (as to each, such software's "Interfaces"):
    i. The Software's Interfaces may, to the extent permitted by the
       license of the Other FOSS, be copied into, used and distributed in
       the Other FOSS in order to enable interoperability, without
       requiring a change to the license of the Other FOSS other than as
       to any Interfaces of the Software embedded therein. The Software's
       Interfaces remain at all times under the Software License,
       including without limitation as used in the Other FOSS (which upon
       any such use also then contains a portion of the Software under the
       Software License).
   ii. The Other FOSS's Interfaces may, to the extent permitted by the
       license of the Other FOSS, be copied into, used and distributed in
       the Software in order to enable interoperability, without requiring
       that such Interfaces be licensed under the terms of the Software
       License or otherwise altering their original terms, if this does
       not require any portion of the Software other than such Interfaces
       to be licensed under the terms other than the Software License.
   iii. If only Interfaces and no other code is copied between the
       Software and the Other FOSS in either direction, the use and/or
       distribution of the Software with the Other FOSS shall not be
       deemed to require that the Other FOSS be licensed under the license
       of the Software, other than as to any Interfaces of the Software
       copied into the Other FOSS. This includes, by way of example and
       without limitation, statically or dynamically linking the Software
       together with Other FOSS after enabling interoperability using the
       Interfaces of one or both, and distributing the resulting
       combination under different licenses for the respective portions
       thereof. For avoidance of doubt, a license which is OSI-approved or
       categorized by the FSF as free, includes, for the purpose of this
       permission, such licenses with additional permissions, and any
       license that has previously been so approved or categorized as
       free, even if now deprecated or otherwise no longer recognized as
       approved or free. Nothing in this additional permission grants any
       right to distribute any portion of the Software on terms other than
       those of the Software License or grants any additional permission
       of any kind for use or distribution of the Software in conjunction
       with software other than Other FOSS.

Licenses for Third-Party Components

   The following sections contain licensing information for libraries that
   we have included with the MySQL Connector/J 8.0 source and components
   used to test MySQL Connector/J 8.0. Commonly used licenses referenced
   herein can be found in Commonly Used Licenses. We are thankful to all
   individuals that have created these.

Ant-Contrib

   The following software may be included in this product:
Ant-Contrib
Copyright (c) 2001-2003 Ant-Contrib project. All rights reserved.
Licensed under the Apache 1.1 License Agreement, a copy of which is reproduced b
elow.

The Apache Software License, Version 1.1

Copyright (c) 2001-2003 Ant-Contrib project.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:


 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.


 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in
    the documentation and/or other materials provided with the
    distribution.


 3. The end-user documentation included with the redistribution, if
    any, must include the following acknowlegement:
       "This product includes software developed by the
        Ant-Contrib project (http://sourceforge.net/projects/ant-contrib)."
    Alternately, this acknowlegement may appear in the software itself,
    if and wherever such third-party acknowlegements normally appear.


 4. The name Ant-Contrib must not be used to endorse or promote
    products derived from this software without prior written
    permission. For written permission, please contact
    ant-contrib-developers@lists.sourceforge.net.


 5. Products derived from this software may not be called "Ant-Contrib"
    nor may "Ant-Contrib" appear in their names without prior written
    permission of the Ant-Contrib project.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED.  IN NO EVENT SHALL THE ANT-CONTRIB PROJECT OR ITS
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

c3p0 JDBC Library

   The MySQL Connector/J implements interfaces that are included in c3p0,
   although no part of c3p0 is included or distributed with MySQL.
Copyright (C) 2019 Machinery For Change, Inc.


 * This library is free software; you can redistribute it and/or modify

 * it under the terms of EITHER:
 *

 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as

 *        published by the Free Software Foundation
 *

 * OR
 *

 *     2) The Eclipse Public License (EPL), version 1.0

 * You may choose which license to accept if you wish to redistribute

 * or modify this work. You may offer derivatives of this work

 * under the license you have chosen, or you may provide the same

 * choice of license which you have been offered here.
 *

 * This software is distributed in the hope that it will be useful,

 * but WITHOUT ANY WARRANTY; without even the implied warranty of

 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *

 * You should have received copies of both LGPL v2.1 and EPL v1.0

 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.

 * If not, the text of these licenses are currently available at
 *

 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html

 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php

Eclipse Public License - v 1.0

THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE PUBLIC
LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM
CONSTITUTES RECIPIENT?S ACCEPTANCE OF THIS AGREEMENT.

1. DEFINITIONS

"Contribution" means:

a) in the case of the initial Contributor, the initial code and documentation

   distributed under this Agreement, and

b) in the case of each subsequent Contributor:

    i) changes to the Program, and

   ii) additions to the Program;

where such changes and/or additions to the Program originate from and are
distributed by that particular Contributor. A Contribution 'originates' from
a Contributor if it was added to the Program by such Contributor itself or
anyone acting on such Contributor?s behalf. Contributions do not include
additionsto the Program which: (i) are separate modules of software
distributed in conjunction with the Program under their own license agreement, a
nd (ii)
are not derivative works of the Program.

"Contributor" means any person or entity that distributes the Program.

"Licensed Patents " mean patent claims licensable by a Contributor which are
necessarily infringed by the use or sale of its Contribution alone or when
combined with the Program.

"Program" means the Contributions distributed in accordance with this
Agreement.

"Recipient" means anyone who receives the Program under this Agreement,
including all Contributors.

2. GRANT OF RIGHTS

a) Subject to the terms of this Agreement, each Contributor hereby grants
Recipient a non-exclusive, worldwide, royalty-free copyright license to
reproduce, prepare derivative works of, publicly display, publicly perform,
distribute and sublicense the Contribution of such Contributor, if any, and
such derivative works, in source code and object code form.

b) Subject to the terms of this Agreement, each Contributor hereby grants
Recipient a non-exclusive, worldwide, royalty-free patent license under
Licensed Patents to make, use, sell, offer to sell, import and otherwise
transfer the Contribution of such Contributor, if any, in source code and
object code form. This patent license shall apply to the combination of the
Contribution and the Program if, at the time the Contribution is added by the
Contributor, such addition of the Contribution causes such combination to be
covered by the Licensed Patents. The patent license shall not apply to any
other combinations which include the Contribution. No hardware per se is
licensed hereunder.

c) Recipient understands that although each Contributor grants the licenses
to its Contributions set forth herein, no assurances are provided by any
Contributor that the Program does not infringe the patent or other
intellectual property rights of any other entity. Each Contributor disclaims any
 liability
to Recipient for claims brought by any other entity based on infringement of
intellectual property rights or otherwise. As a condition to exercising the
rights and licenses granted hereunder, each Recipient hereby assumes sole
responsibility to secure any other intellectual property rights needed, if
any. For example, if a third party patent license is required to allow Recipient
to distribute the Program, it is Recipient?s responsibility to acquire that
license before distributing the Program.

d) Each Contributor represents that to its knowledge it has sufficient
copyright rights in its Contribution, if any, to grant the copyright license
set forth in this Agreement.

3. REQUIREMENTS

A Contributor may choose to distribute the Program in object code form under
its own license agreement, provided that:

a) it complies with the terms and conditions of this Agreement; and

b) its license agreement:

     i) effectively disclaims on behalf of all Contributors all warranties
        and conditions, express and implied, including warranties or conditions
        of title and non-infringement, and implied warranties or conditions of
        merchantability and fitness for a particular purpose;

    ii) effectively excludes on behalf of all Contributors all liability for
        damages, including direct, indirect, special, incidental and
        consequential damages, such as lost profits;

   iii) states that any provisions which differ from this Agreement are
        offered by that Contributor alone and not by any other party; and

    iv) states that source code for the Program is available from such
        Contributor, and informs licensees how to obtain it in a reasonable
        manner on or through a medium customarily used for software exchange.

When the Program is made available in source code form:

a) it must be made available under this Agreement; and

b) a copy of this Agreement must be included with each copy of the Program.

Contributors may not remove or alter any copyright notices contained within
the Program.

Each Contributor must identify itself as the originator of its Contribution,
if any, in a manner that reasonably allows subsequent Recipients to identify
the originator of the Contribution.

4. COMMERCIAL DISTRIBUTION

Commercial distributors of software may accept certain responsibilities with
respect to end users, business partners and the like. While this license is
intended to facilitate the commercial use of the Program, the Contributor who
includes the Program in a commercial product offering should do so in a
manner which does not create potential liability for other Contributors. Therefo
re,
if a Contributor includes the Program in a commercial product offering, such
Contributor ("Commercial Contributor") hereby agrees to defend and indemnify
every other Contributor ("Indemnified Contributor") against any losses,
damages and costs (collectively "Losses") arising from claims, lawsuits and othe
r
legal actions brought by a third party against the Indemnified Contributor to th
e
extent caused by the acts or omissions of such Commercial Contributor in
connection with its distribution of the Program in a commercial product
offering. The obligations in this section do not apply to any claims or
Losses relating to any actual or alleged intellectual property infringement. In
order to qualify, an Indemnified Contributor must: a) promptly notify the
Commercial Contributor in writing of such claim, and b) allow the Commercial Con
tributor
to control, and cooperate with the Commercial Contributor in, the defense and
any related settlement negotiations. The Indemnified Contributor may
participate in any such claim at its own expense.

For example, a Contributor might include the Program in a commercial product
offering, Product X. That Contributor is then a Commercial Contributor. If
that Commercial Contributor then makes performance claims, or offers warranties
related to Product X, those performance claims and warranties are such
Commercial Contributor?s responsibility alone. Under this section, the
Commercial Contributor would have to defend claims against the other
Contributors related to those performance claims and warranties, and if a
court requires any other Contributor to pay any damages as a result, the Commerc
ial
Contributor must pay those damages.

5. NO WARRANTY

EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, THE PROGRAM IS PROVIDED ON
AN "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS
OR IMPLIED INCLUDING, WITHOUT LIMITATION, ANY WARRANTIES OR CONDITIONS OF TITLE,
NON-INFRINGEMENT, MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Each
Recipient is solely responsible for determining the appropriateness of using
and distributing the Program and assumes all risks associated with its
exercise of rights under this Agreement , including but not limited to the
risks and costs of program errors, compliance with applicable laws, damage to
or loss of data, programs or equipment, and unavailability or interruption of
operations.

6. DISCLAIMER OF LIABILITY

EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, NEITHER RECIPIENT NOR ANY
CONTRIBUTORS SHALL HAVE ANY LIABILITY FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING WITHOUT LIMITATION
LOST PROFITS), HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRAC
T,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
WAY OUT OF THE USE OR DISTRIBUTION OF THE PROGRAM OR THE EXERCISE OF ANY
RIGHTS GRANTED HEREUNDER, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

7. GENERAL

If any provision of this Agreement is invalid or unenforceable under
applicable law, it shall not affect the validity or enforceability of the remain
der of
the terms of this Agreement, and without further action by the parties hereto,
such provision shall be reformed to the minimum extent necessary to make such
provision valid and enforceable.

If Recipient institutes patent litigation against any entity (including a
cross-claim or counterclaim in a lawsuit) alleging that the Program itself
(excluding combinations of the Program with other software or hardware)
infringes such Recipient?s patent(s), then such Recipient?s rights granted
under Section 2(b) shall terminate as of the date such litigation is filed.

All Recipient?s rights under this Agreement shall terminate if it fails to
comply with any of the material terms or conditions of this Agreement and
does not cure such failure in a reasonable period of time after becoming aware o
f
such noncompliance. If all Recipient?s rights under this Agreement terminate,
Recipient agrees to cease use and distribution of the Program as soon as
reasonably practicable. However, Recipient?s obligations under this Agreement
and any licenses granted by Recipient relating to the Program shall continue
and survive.

Everyone is permitted to copy and distribute copies of this Agreement, but in
order to avoid inconsistency the Agreement is copyrighted and may only be
modified in the following manner. The Agreement Steward reserves the right to
publish new versions (including revisions) of this Agreement from time to
time. No one other than the Agreement Steward has the right to modify this
Agreement. The Eclipse Foundation is the initial Agreement Steward. The Eclipse
Foundation may assign the responsibility to serve as the Agreement Steward to a
suitable
separate entity. Each new version of the Agreement will be given a
distinguishing version number. The Program (including Contributions) may
always be distributed subject to the version of the Agreement under which it was
received. In addition, after a new version of the Agreement is published,
Contributor may elect to distribute the Program (including its Contributions)
under the new version. Except as expressly stated in Sections 2(a) and 2(b)
above, Recipient receives no rights or licenses to the intellectual property
of any Contributor under this Agreement, whether expressly, by implication,
estoppel or otherwise. All rights in the Program not expressly granted under
this Agreement are reserved.

This Agreement is governed by the laws of the State of New York and the
intellectual property laws of the United States of America. No party to this
Agreement will bring a legal action under this Agreement more than one year
after the cause of action arose. Each party waives its rights to a jury trial
in any resulting litigation.

   The LGPL v2.1 can be found in GNU Lesser General Public License Version
   2.1, February 1999.

Google Protocol Buffers

   The following software may be included in this product:
Copyright 2008 Google Inc.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:


    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.

    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Code generated by the Protocol Buffer compiler is owned by the owner
of the input file used when generating it.  This code is not
standalone and requires a support library to be linked with it.  This
support library is itself covered by the above license.

Java Hamcrest

   The following software may be included in this product:
Copyright (c) 2000-2015 www.hamcrest.org
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer. Redistributions in binary form
must
reproduce the above copyright notice, this list of conditions and the following
disclaimer in the documentation and/or other materials provided with the distrib
ution.

Neither the name of Hamcrest nor the names of its contributors may be used to
endorse or promote products derived from this software without specific prior wr
itten
permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIE
D
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIME
D. IN NO
EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRE
CT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY
, WHETHER
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSI
BILITY OF
SUCH DAMAGE.

jboss-common-jdbc-wrapper.jar

   This product may include a copy of jboss-common-jdbc-wrapper.jar in
   both source and object code in the following
   /src/lib/jboss-common-jdbc-wrapper.jar. The terms of the Oracle license
   do NOT apply to jboss-common-jdbc-wrapper.jar; it is licensed under the
   following license, separately from the Oracle programs you receive. If
   you do not wish to install this library, you may remove the file
   /src/lib/jboss-common-jdbc-wrapper.jar, but the Oracle program might
   not operate properly or at all without the library.

   This component is licensed under GNU Lesser General Public License
   Version 2.1, February 1999.

JUnit 5

   The following software may be included in this product:
COPYRIGHT: Copyright 2015-2020 the original author or authors.
LICENSE: Eclipse Public License - v 2.0

THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE PUBLIC
LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM
CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.

1. DEFINITIONS

"Contribution" means:

    a) in the case of the initial Contributor, the initial content Distributed
       under this Agreement, and
    b) in the case of each subsequent Contributor:
        i) changes to the Program, and
        ii) additions to the Program;
    where such changes and/or additions to the Program originate from and are
    Distributed by that particular Contributor. A Contribution "originates" from
    a Contributor if it was added to the Program by such Contributor itself or
    anyone acting on such Contributor's behalf. Contributions do not include
    changes  or additions to the Program that are not Modified Works.

"Contributor" means any person or entity that Distributes the Program.

"Licensed Patents" mean patent claims licensable by a Contributor which are
necessarily infringed by the use  or sale of its Contribution alone or when
combined with the Program.

"Program" means the Contributions Distributed in accordance with this Agreement.

"Recipient" means anyone who receives the Program under this Agreement or any
Secondary License (as applicable),  including Contributors.

"Derivative Works" shall mean any work, whether in Source Code or other form,
that is based on (or derived from)  the Program and for which the editorial
revisions, annotations, elaborations, or other modifications represent,  as a
whole, an original work of authorship.

"Modified Works" shall mean any work in Source Code or other form that results
from an addition to, deletion  from, or modification of the contents of the
Program, including, for purposes of clarity any new file in Source Code  form
that contains any contents of the Program. Modified Works shall not include
works that contain only declarations,  interfaces, types, classes, structures,
or files of the Program solely in each case in order to link to, bind by  name,
or subclass the Program or Modified Works thereof.

"Distribute" means the acts of a) distributing or b) making available in any
"manner that enables the transfer of a copy.

"Source Code" means the form of a Program preferred for making modifications,
including but not limited to software  source code, documentation source, and
configuration files.

"Secondary License" means either the GNU General Public License, Version 2.0, or
any later versions of that  license, including any exceptions or additional
permissions as identified by the initial Contributor.

2. GRANT OF RIGHTS

    a) Subject to the terms of this Agreement, each Contributor hereby grants
    Recipient a non-exclusive, worldwide,  royalty-free copyright license to
    reproduce, prepare Derivative Works of, publicly display, publicly perform,
    Distribute and sublicense the Contribution of such Contributor, if any, and
    such Derivative Works.

    b) Subject to the terms of this Agreement, each Contributor hereby grants
    Recipient a non-exclusive, worldwide,  royalty-free patent license under
    Licensed Patents to make, use, sell, offer to sell, import and otherwise
    transfer the Contribution of such Contributor, if any, in Source Code or
    other form. This patent license  shall apply to the combination of the
    Contribution and the Program if, at the time the Contribution is added  by
    the Contributor, such addition of the Contribution causes such combination
    to be covered by the Licensed  Patents. The patent license shall not apply
    to any other combinations which include the Contribution. No  hardware per
    se is licensed hereunder.

    c) Recipient understands that although each Contributor grants the licenses
    to its Contributions set forth herein,  no assurances are provided by any
    Contributor that the Program does not infringe the patent or other
    intellectual  property rights of any other entity. Each Contributor
    disclaims any liability to Recipient for claims brought  by any other entity
    based on infringement of intellectual property rights or otherwise. As a
    condition to  exercising the rights and licenses granted hereunder, each
    Recipient hereby assumes sole responsibility to secure  any other
    intellectual property rights needed, if any. For example, if a third party
    patent license is required  to allow Recipient to Distribute the Program, it
    is Recipient's responsibility to acquire that license before  distributing
    the Program.

    d) Each Contributor represents that to its knowledge it has sufficient
    copyright rights in its Contribution, if any,  to grant the copyright
    license set forth in this Agreement.

    e) Notwithstanding the terms of any Secondary License, no Contributor makes
    additional grants to any Recipient  (other than those set forth in this
    Agreement) as a result of such Recipient's receipt of the Program under the
    terms of a Secondary License (if permitted under the terms of Section 3).

3. REQUIREMENTS

3.1 If a Contributor Distributes the Program in any form, then:

    a) the Program must also be made available as Source Code, in accordance
    with section 3.2, and the Contributor  must accompany the Program with a
    statement that the Source Code for the Program is available under this
    Agreement, and informs Recipients how to obtain it in a reasonable manner on
    or through a medium customarily  used for software exchange; and

    b) the Contributor may Distribute the Program under a license different than
    this Agreement, provided that  such license:

          i) effectively disclaims on behalf of all other Contributors all
          warranties and conditions, express and implied,  including warranties
          or conditions of title and non-infringement, and implied warranties or
          conditions of merchantability and fitness for a particular purpose;

          ii) effectively excludes on behalf of all other Contributors all
          liability for damages, including direct, indirect,  special, incidenta
l
          and consequential damages, such as lost profits;

          iii) does not attempt to limit or alter the recipients' rights in the
          Source Code under section 3.2; and

          iv) requires any subsequent distribution of the Program by any party t
o
          be under a license that satisfies the  requirements of this section 3.

3.2 When the Program is Distributed as Source Code:

    a) it must be made available under this Agreement, or if the Program (i) is
    combined with other material in a separate  file or files made available
    under a Secondary License, and (ii) the initial Contributor attached to the
    Source Code  the notice described in Exhibit A of this Agreement, then the
    Program may be made available under the terms of such  Secondary Licenses,
    and
    b) a copy of this Agreement must be included with each copy of the Program.

3.3 Contributors may not remove or alter any copyright, patent, trademark,
attribution notices, disclaimers of warranty, or  limitations of liability
(`notices') contained within the Program from any copy of the Program which they
Distribute,  provided that Contributors may add their own appropriate notices.

4. COMMERCIAL DISTRIBUTION

Commercial distributors of software may accept certain responsibilities with
respect to end users, business partners and  the like. While this license is
intended to facilitate the commercial use of the Program, the Contributor who
includes the  Program in a commercial product offering should do so in a manner
which does not create potential liability for other Contributors.  Therefore, if
a Contributor includes the Program in a commercial product offering, such
Contributor ("Commercial Contributor")  hereby agrees to defend and indemnify
every other Contributor ("Indemnified Contributor") against any losses, damages
and costs  (collectively "Losses") arising from claims, lawsuits and other legal
actions brought by a third party against the Indemnified  Contributor to the
extent caused by the acts or omissions of such Commercial Contributor in
connection with its distribution  of the Program in a commercial product
offering. The obligations in this section do not apply to any claims or Losses
relating  to any actual or alleged intellectual property infringement. In order
to qualify, an Indemnified Contributor must: a) promptly  notify the Commercial
Contributor in writing of such claim, and b) allow the Commercial Contributor to
control, and cooperate  with the Commercial Contributor in, the defense and any
related settlement negotiations. The Indemnified Contributor may  participate in
any such claim at its own expense.

For example, a Contributor might include the Program in a commercial product
offering, Product X. That Contributor is then a  Commercial Contributor. If that
Commercial Contributor then makes performance claims, or offers warranties
related to Product X,  those performance claims and warranties are such
Commercial Contributor's responsibility alone. Under this section, the
Commercial Contributor would have to defend claims against the other
Contributors related to those performance claims and  warranties, and if a court
requires any other Contributor to pay any damages as a result, the Commercial
Contributor must  pay those damages.

5. NO WARRANTY

EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, AND TO THE EXTENT PERMITTED BY
APPLICABLE LAW, THE PROGRAM IS PROVIDED  ON AN "AS IS" BASIS, WITHOUT WARRANTIES
OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED INCLUDING, WITHOUT
LIMITATION,  ANY WARRANTIES OR CONDITIONS OF TITLE, NON-INFRINGEMENT,
MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Each Recipient  is solely
responsible for determining the appropriateness of using and distributing the
Program and assumes all risks  associated with its exercise of rights under this
Agreement, including but not limited to the risks and costs of program  errors,
compliance with applicable laws, damage to or loss of data, programs or
equipment, and unavailability or interruption  of operations.

6. DISCLAIMER OF LIABILITY

EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, AND TO THE EXTENT PERMITTED BY
APPLICABLE LAW, NEITHER RECIPIENT NOR ANY  CONTRIBUTORS SHALL HAVE ANY LIABILITY
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES  (INCLUDING WITHOUT LIMITATION LOST PROFITS), HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT  LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OR DISTRIBUTION OF
THE PROGRAM  OR THE EXERCISE OF ANY RIGHTS GRANTED HEREUNDER, EVEN IF ADVISED OF
THE POSSIBILITY OF SUCH DAMAGES.

7. GENERAL

If any provision of this Agreement is invalid or unenforceable under applicable
law, it shall not affect the validity or  enforceability of the remainder of the
terms of this Agreement, and without further action by the parties hereto, such
provision shall be reformed to the minimum extent necessary to make such
provision valid and enforceable.

If Recipient institutes patent litigation against any entity (including a
cross-claim or counterclaim in a lawsuit)  alleging that the Program itself
(excluding combinations of the Program with other software or hardware)
infringes  such Recipient's patent(s), then such Recipient's rights granted
under Section 2(b) shall terminate as of the date  such litigation is filed.

All Recipient's rights under this Agreement shall terminate if it fails to
comply with any of the material terms  or conditions of this Agreement and does
not cure such failure in a reasonable period of time after becoming aware  of
such noncompliance. If all Recipient's rights under this Agreement terminate,
Recipient agrees to cease use and  distribution of the Program as soon as
reasonably practicable. However, Recipient's obligations under this Agreement
and any licenses granted by Recipient relating to the Program shall continue and
survive.

Everyone is permitted to copy and distribute copies of this Agreement, but in
order to avoid inconsistency the  Agreement is copyrighted and may only be
modified in the following manner. The Agreement Steward reserves the right  to
publish new versions (including revisions) of this Agreement from time to time.
No one other than the Agreement  Steward has the right to modify this Agreement.
The Eclipse Foundation is the initial Agreement Steward. The Eclipse  Foundation
may assign the responsibility to serve as the Agreement Steward to a suitable
separate entity. Each new  version of the Agreement will be given a
distinguishing version number. The Program (including Contributions) may  always
be Distributed subject to the version of the Agreement under which it was
received. In addition, after a  new version of the Agreement is published,
Contributor may elect to Distribute the Program (including its  Contributions)
under the new version.

Except as expressly stated in Sections 2(a) and 2(b) above, Recipient receives
no rights or licenses to the  intellectual property of any Contributor under
this Agreement, whether expressly, by implication, estoppel or  otherwise. All
rights in the Program not expressly granted under this Agreement are reserved.
Nothing in this  Agreement is intended to be enforceable by any entity that is
not a Contributor or Recipient. No third-party  beneficiary rights are created
under this Agreement. Exhibit A - Form of Secondary Licenses Notice

"This Source Code may also be made available under the following Secondary
Licenses when the conditions for such  availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: {name license(s), version(s), and
exceptions or additional permissions here}."

    Simply including a copy of this Agreement, including this Exhibit A is not
    sufficient to license the Source  Code under Secondary Licenses.

    If it is not possible or desirable to put the notice in a particular file,
    then You may include the notice  in a location (such as a LICENSE file in a
    relevant directory) where a recipient would be likely to look  for such a
    notice.

    You may add additional accurate notices of copyright ownership.


***************************
4P Dependencies:
========
POM FILE:
org.apiguardian -- apiguardian-api 1.1.0

LICENSE: Apache 2.0 http://www.apache.org/licenses/LICENSE-2.0.txt
COPYRIGHT: Copyright 2002-2019 the original author or authors.


The above component is licensed under Apache License Version 2.0, January 2004.


org.junit.platform -- junit-platform-commons 1.6.0
LICENSE: Eclipse Public License - v2.0 http://www.eclipse.org/legal/epl-v20.html
COPYRIGHT: Copyright 2015-2019 the original author or authors.

Open Source Licenses
====================
This product may include a number of subcomponents with separate
copyright notices and license terms. Your use of the source code for
these subcomponents is subject to the terms and conditions of the
subcomponent's license, as noted in the LICENSE-<subcomponent>.md
files.

Additional licenses
====================
junit-jupiter-params & junit-platform-console directories include an Apache
license file

Additional external dependencies
================================
https://github.com/apiguardian-team/apiguardian/archive/r1.1.0.zip
/*
* Copyright 2002-2017 the original author or authors.
*


The above component is licensed under
Apache License Version 2.0, January 2004.


https://github.com/ota4j-team/opentest4j/archive/r1.2.0.zip
/*
* Copyright 2015-2018 the original author or authors.
*


The above component is licensed under
Apache License Version 2.0, January 2004.

NanoXML

   The following software may be included in this product:

   NanoXML

 * Copyright (C) 2000-2002 Marc De Scheemaecker, All Rights Reserved.
 *

 * This software is provided 'as-is', without any express or implied warranty.

 * In no event will the authors be held liable for any damages arising from the

 * use of this software.
 *

 * Permission is granted to anyone to use this software for any purpose,

 * including commercial applications, and to alter it and redistribute it

 * freely, subject to the following restrictions:
 *

 *  1. The origin of this software must not be misrepresented; you must not

 *     claim that you wrote the original software. If you use this software in

 *     a product, an acknowledgment in the product documentation would be

 *     appreciated but is not required.
 *

 *  2. Altered source versions must be plainly marked as such, and must not be

 *     misrepresented as being the original software.
 *

 *  3. This notice may not be removed or altered from any source distribution.
 *

rox.jar

   The following software may be included in this product:

   rox.jar
Copyright (c) 2006, James Greenfield
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:


    * Redistributions of source code must retain the above copyright notice, thi
s
      list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.

    * Neither the name of the <ORGANIZATION> nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIE
D
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVI
CES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Simple Logging Facade for Java (SLF4J)

   The following software may be included in this product:
Simple Logging Facade for Java (SLF4J)

Copyright (c) 2004-2011 QOS.ch
All rights reserved.

Permission is hereby granted, free of charge,
to any person obtaining a copy of this software
and associated documentation files (the "Software"),
to deal in the Software without restriction, including
without limitation the rights to use, copy, modify,
merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom
the Software is furnished to do so, subject to the
following conditions:

The above copyright notice and this permission notice
shall be included in all copies or substantial portions
of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY
OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
OR OTHER DEALINGS IN THE SOFTWARE.

Unicode Data Files

   The following software may be included in this product:

   Unicode Data Files
COPYRIGHT AND PERMISSION NOTICE

Copyright (c) 1991-2014 Unicode, Inc. All rights reserved. Distributed under
the Terms of Use in http://www.unicode.org/copyright.html.

Permission is hereby granted, free of charge, to any person obtaining a copy
of the Unicode data files and any associated documentation (the "Data Files")
or Unicode software and any associated documentation (the "Software") to deal
in the Data Files or Software without restriction, including without
limitation the rights to use, copy, modify, merge, publish, distribute,
and/or sell copies of the Data Files or Software, and to permit persons to
whom the Data Files or Software are furnished to do so, provided that (a) the
above copyright notice(s) and this permission notice appear with all copies
of the Data Files or Software, (b) both the above copyright notice(s) and
this permission notice appear in associated documentation, and (c) there is
clear notice in each modified Data File or in the Software as well as in the
documentation associated with the Data File(s) or Software that the data or
software has been modified.

THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF
THIRD PARTY RIGHTS. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS
INCLUDED IN THIS NOTICE BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR
CONSEQUENTIAL DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE,
DATA OR
PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THE
DATA FILES OR SOFTWARE.

Except as contained in this notice, the name of a copyright holder shall not
be used in advertising or otherwise to promote the sale, use or other
dealings in these Data Files or Software without prior written authorization
of the copyright holder.

Commonly Used Licenses

Apache License Version 2.0, January 2004

The following applies to all products licensed under the Apache 2.0
License: You may not use the identified files except in compliance
with the Apache License, Version 2.0 (the "License.") You may obtain a
copy of the License at http://www.apache.org/licenses/LICENSE-2.0. A
copy of the license is also reproduced below. Unless required by
applicable law or agreed to in writing, software distributed under the
License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for
the specific language governing permissions and limitations under the
License.

Apache License Version 2.0, January 2004 http://www.apache.org/licenses/

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

1. Definitions.

"License" shall mean the terms and conditions for use, reproduction,
and distribution as defined by Sections 1 through 9 of this document.

"Licensor" shall mean the copyright owner or entity authorized by the
copyright owner that is granting the License.

"Legal Entity" shall mean the union of the acting entity and all other
entities that control, are controlled by, or are under common control
with that entity. For the purposes of this definition, "control" means
(i) the power, direct or indirect, to cause the direction or
management of such entity, whether by contract or otherwise, or (ii)
ownership of fifty percent (50%) or more of the outstanding shares, or
(iii) beneficial ownership of such entity.

"You" (or "Your") shall mean an individual or Legal Entity exercising
permissions granted by this License.

"Source" form shall mean the preferred form for making modifications,
including but not limited to software source code, documentation
source, and configuration files.

"Object" form shall mean any form resulting from mechanical
transformation or translation of a Source form, including but not
limited to compiled object code, generated documentation, and
conversions to other media types.

"Work" shall mean the work of authorship, whether in Source or Object
form, made available under the License, as indicated by a copyright
notice that is included in or attached to the work (an example is
provided in the Appendix below).

"Derivative Works" shall mean any work, whether in Source or Object
form, that is based on (or derived from) the Work and for which the
editorial revisions, annotations, elaborations, or other modifications
represent, as a whole, an original work of authorship. For the
purposes of this License, Derivative Works shall not include works
that remain separable from, or merely link (or bind by name) to the
interfaces of, the Work and Derivative Works thereof.

"Contribution" shall mean any work of authorship, including the
original version of the Work and any modifications or additions to
that Work or Derivative Works thereof, that is intentionally submitted
to Licensor for inclusion in the Work by the copyright owner or by an
individual or Legal Entity authorized to submit on behalf of the
copyright owner. For the purposes of this definition, "submitted"
means any form of electronic, verbal, or written communication sent to
the Licensor or its representatives, including but not limited to
communication on electronic mailing lists, source code control
systems, and issue tracking systems that are managed by, or on behalf
of, the Licensor for the purpose of discussing and improving the Work,
but excluding communication that is conspicuously marked or otherwise
designated in writing by the copyright owner as "Not a Contribution."

"Contributor" shall mean Licensor and any individual or Legal Entity
on behalf of whom a Contribution has been received by Licensor and
subsequently incorporated within the Work.

2. Grant of Copyright License. Subject to the terms and conditions of
this License, each Contributor hereby grants to You a perpetual,
worldwide, non-exclusive, no-charge, royalty-free, irrevocable
copyright license to reproduce, prepare Derivative Works of, publicly
display, publicly perform, sublicense, and distribute the Work and
such Derivative Works in Source or Object form.

3. Grant of Patent License. Subject to the terms and conditions of
this License, each Contributor hereby grants to You a perpetual,
worldwide, non-exclusive, no-charge, royalty-free, irrevocable (except
as stated in this section) patent license to make, have made, use,
offer to sell, sell, import, and otherwise transfer the Work, where
such license applies only to those patent claims licensable by such
Contributor that are necessarily infringed by their Contribution(s)
alone or by combination of their Contribution(s) with the Work to
which such Contribution(s) was submitted. If You institute patent
litigation against any entity (including a cross-claim or counterclaim
in a lawsuit) alleging that the Work or a Contribution incorporated
within the Work constitutes direct or contributory patent
infringement, then any patent licenses granted to You under this
License for that Work shall terminate as of the date such litigation
is filed.

4. Redistribution. You may reproduce and distribute copies of the Work
or Derivative Works thereof in any medium, with or without
modifications, and in Source or Object form, provided that You meet
the following conditions:

(a) You must give any other recipients of the Work or Derivative Works
a copy of this License; and

(b) You must cause any modified files to carry prominent notices
stating that You changed the files; and

(c) You must retain, in the Source form of any Derivative Works that
You distribute, all copyright, patent, trademark, and attribution
notices from the Source form of the Work, excluding those notices that
do not pertain to any part of the Derivative Works; and

(d) If the Work includes a "NOTICE" text file as part of its
distribution, then any Derivative Works that You distribute must
include a readable copy of the attribution notices contained

within such NOTICE file, excluding those notices that do not pertain
to any part of the Derivative Works, in at least one of the following
places: within a NOTICE text file distributed as part of the
Derivative Works; within the Source form or documentation, if provided
along with the Derivative Works; or, within a display generated by the
Derivative Works, if and wherever such third-party notices normally
appear. The contents of the NOTICE file are for informational purposes
only and do not modify the License. You may add Your own attribution
notices within Derivative Works that You distribute, alongside or as
an addendum to the NOTICE text from the Work, provided that such
additional attribution notices cannot be construed as modifying the
License.

You may add Your own copyright statement to Your modifications and may
provide additional or different license terms and conditions for use,
reproduction, or distribution of Your modifications, or for any such
Derivative Works as a whole, provided Your use, reproduction, and
distribution of the Work otherwise complies with the conditions stated
in this License.

5. Submission of Contributions. Unless You explicitly state otherwise,
any Contribution intentionally submitted for inclusion in the Work by
You to the Licensor shall be under the terms and conditions of this
License, without any additional terms or conditions.  Notwithstanding
the above, nothing herein shall supersede or modify the terms of any
separate license agreement you may have executed with Licensor
regarding such Contributions.

6. Trademarks. This License does not grant permission to use the trade
names, trademarks, service marks, or product names of the Licensor,
except as required for reasonable and customary use in describing the
origin of the Work and reproducing the content of the NOTICE file.

7. Disclaimer of Warranty. Unless required by applicable law or agreed
to in writing, Licensor provides the Work (and each Contributor
provides its Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied, including, without
limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT,
MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE. You are solely
responsible for determining the appropriateness of using or
redistributing the Work and assume any risks associated with Your
exercise of permissions under this License.

8. Limitation of Liability. In no event and under no legal theory,
whether in tort (including negligence), contract, or otherwise, unless
required by applicable law (such as deliberate and grossly negligent
acts) or agreed to in writing, shall any Contributor be liable to You
for damages, including any direct, indirect, special, incidental, or
consequential damages of any character arising as a result of this
License or out of the use or inability to use the Work (including but
not limited to damages for loss of goodwill, work stoppage, computer
failure or malfunction, or any and all other commercial damages or
losses), even if such Contributor has been advised of the possibility
of such damages.

9. Accepting Warranty or Additional Liability. While redistributing
the Work or Derivative Works thereof, You may choose to offer, and
charge a fee for, acceptance of support, warranty, indemnity, or other
liability obligations and/or rights consistent with this
License. However, in accepting such obligations, You may act only on
Your own behalf and on Your sole responsibility, not on behalf of any
other Contributor, and only if You agree to indemnify, defend, and
hold each Contributor harmless for any liability incurred by, or
claims asserted against, such Contributor by reason of your accepting
any such warranty or additional liability.

END OF TERMS AND CONDITIONS

APPENDIX: How to apply the Apache License to your work

To apply the Apache License to your work, attach the following boilerplate
notice, with the fields enclosed by brackets "[]" replaced with your own
identifying information. (Don't include the brackets!) The text should be
enclosed in the appropriate comment syntax for the file format. We also
recommend that a file or class name and description of purpose be included
on the same "printed page" as the copyright notice for easier identification
within third-party archives.

Copyright [yyyy] [name of copyright owner]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing permissions
and limitations under the License.

Artistic License (Perl) 1.0

The "Artistic License"

Preamble

The intent of this document is to state the conditions under which a
Package may be copied, such that the Copyright Holder maintains some
semblance of artistic control over the development of the package,
while giving the users of the package the right to use and distribute
the Package in a more-or-less customary fashion, plus the right to make
reasonable modifications.

Definitions:

        "Package" refers to the collection of files distributed by the
        Copyright Holder, and derivatives of that collection of files
        created through textual modification.

        "Standard Version" refers to such a Package if it has not been
        modified, or has been modified in accordance with the wishes
        of the Copyright Holder as specified below.

        "Copyright Holder" is whoever is named in the copyright or
        copyrights for the package.

        "You" is you, if you're thinking about copying or distributing
        this Package.

        "Reasonable copying fee" is whatever you can justify on the
        basis of media cost, duplication charges, time of people involved,
        and so on.  (You will not be required to justify it to the
        Copyright Holder, but only to the computing community at large
        as a market that must bear the fee.)

        "Freely Available" means that no fee is charged for the item
        itself, though there may be fees involved in handling the item.
        It also means that recipients of the item may redistribute it
        under the same conditions they received it.

1. You may make and give away verbatim copies of the source form of the
Standard Version of this Package without restriction, provided that you
duplicate all of the original copyright notices and associated disclaimers.

2. You may apply bug fixes, portability fixes and other modifications
derived from the Public Domain or from the Copyright Holder.  A Package
modified in such a way shall still be considered the Standard Version.

3. You may otherwise modify your copy of this Package in any way, provided
that you insert a prominent notice in each changed file stating how and
when you changed that file, and provided that you do at least ONE of the
following:

    a) place your modifications in the Public Domain or otherwise make them
    Freely Available, such as by posting said modifications to Usenet or
    an equivalent medium, or placing the modifications on a major archive
    site such as uunet.uu.net, or by allowing the Copyright Holder to include
    your modifications in the Standard Version of the Package.

    b) use the modified Package only within your corporation or organization.

    c) rename any non-standard executables so the names do not conflict
    with standard executables, which must also be provided, and provide
    a separate manual page for each non-standard executable that clearly
    documents how it differs from the Standard Version.

    d) make other distribution arrangements with the Copyright Holder.

4. You may distribute the programs of this Package in object code or
executable form, provided that you do at least ONE of the following:

    a) distribute a Standard Version of the executables and library files,
    together with instructions (in the manual page or equivalent) on where
    to get the Standard Version.

    b) accompany the distribution with the machine-readable source of
    the Package with your modifications.

    c) give non-standard executables non-standard names, and clearly
    document the differences in manual pages (or equivalent), together
    with instructions on where to get the Standard Version.

    d) make other distribution arrangements with the Copyright Holder.

5. You may charge a reasonable copying fee for any distribution of this
Package.  You may charge any fee you choose for support of this
Package.  You may not charge a fee for this Package itself.  However,
you may distribute this Package in aggregate with other (possibly
commercial) programs as part of a larger (possibly commercial) software
distribution provided that you do not advertise this Package as a
product of your own.  You may embed this Package's interpreter within
an executable of yours (by linking); this shall be construed as a mere
form of aggregation, provided that the complete Standard Version of the
interpreter is so embedded.

6. The scripts and library files supplied as input to or produced as
output from the programs of this Package do not automatically fall
under the copyright of this Package, but belong to whoever generated
them, and may be sold commercially, and may be aggregated with this
Package.  If such scripts or library files are aggregated with this
Package via the so-called "undump" or "unexec" methods of producing a
binary executable image, then distribution of such an image shall
neither be construed as a distribution of this Package nor shall it
fall under the restrictions of Paragraphs 3 and 4, provided that you do
not represent such an executable image as a Standard Version of this
Package.

7. C subroutines (or comparably compiled subroutines in other
languages) supplied by you and linked into this Package in order to
emulate subroutines and variables of the language defined by this
Package shall not be considered part of this Package, but are the
equivalent of input as in Paragraph 6, provided these subroutines do
not change the language in any way that would cause it to fail the
regression tests for the language.

8. Aggregation of this Package with a commercial distribution is always
permitted provided that the use of this Package is embedded; that is,
when no overt attempt is made to make this Package's interfaces visible
to the end user of the commercial distribution.  Such use shall not be
construed as a distribution of this Package.

9. The name of the Copyright Holder may not be used to endorse or promote
products derived from this software without specific prior written
permission.

10. THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.

                                The End

GNU Lesser General Public License Version 2.1, February 1999

The following applies to all products licensed under the
GNU Lesser General Public License, Version 2.1: You may
not use the identified files except in compliance with
the GNU Lesser General Public License, Version 2.1 (the
"License"). You may obtain a copy of the License at
http://www.gnu.org/licenses/lgpl-2.1.html. A copy of the
license is also reproduced below. Unless required by
applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
or implied. See the License for the specific language governing
permissions and limitations under the License.

                  GNU LESSER GENERAL PUBLIC LICENSE
                       Version 2.1, February 1999

 Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.

[This is the first released version of the Lesser GPL.  It also counts
 as the successor of the GNU Library Public License, version 2, hence
 the version number 2.1.]

                            Preamble

  The licenses for most software are designed to take away your
freedom to share and change it.  By contrast, the GNU General Public
Licenses are intended to guarantee your freedom to share and change
free software--to make sure the software is free for all its users.

  This license, the Lesser General Public License, applies to some
specially designated software packages--typically libraries--of the
Free Software Foundation and other authors who decide to use it.  You
can use it too, but we suggest you first think carefully about whether
this license or the ordinary General Public License is the better
strategy to use in any particular case, based on the explanations below.

  When we speak of free software, we are referring to freedom of use,
not price.  Our General Public Licenses are designed to make sure that
you have the freedom to distribute copies of free software (and charge
for this service if you wish); that you receive source code or can get
it if you want it; that you can change the software and use pieces of
it in new free programs; and that you are informed that you can do
these things.

  To protect your rights, we need to make restrictions that forbid
distributors to deny you these rights or to ask you to surrender these
rights.  These restrictions translate to certain responsibilities for
you if you distribute copies of the library or if you modify it.

  For example, if you distribute copies of the library, whether gratis
or for a fee, you must give the recipients all the rights that we gave
you.  You must make sure that they, too, receive or can get the source
code.  If you link other code with the library, you must provide
complete object files to the recipients, so that they can relink them
with the library after making changes to the library and recompiling
it.  And you must show them these terms so they know their rights.

  We protect your rights with a two-step method: (1) we copyright the
library, and (2) we offer you this license, which gives you legal
permission to copy, distribute and/or modify the library.

  To protect each distributor, we want to make it very clear that
there is no warranty for the free library.  Also, if the library is
modified by someone else and passed on, the recipients should know
that what they have is not the original version, so that the original
author's reputation will not be affected by problems that might be
introduced by others.

  Finally, software patents pose a constant threat to the existence of
any free program.  We wish to make sure that a company cannot
effectively restrict the users of a free program by obtaining a
restrictive license from a patent holder.  Therefore, we insist that
any patent license obtained for a version of the library must be
consistent with the full freedom of use specified in this license.

  Most GNU software, including some libraries, is covered by the
ordinary GNU General Public License.  This license, the GNU Lesser
General Public License, applies to certain designated libraries, and
is quite different from the ordinary General Public License.  We use
this license for certain libraries in order to permit linking those
libraries into non-free programs.

  When a program is linked with a library, whether statically or using
a shared library, the combination of the two is legally speaking a
combined work, a derivative of the original library.  The ordinary
General Public License therefore permits such linking only if the
entire combination fits its criteria of freedom.  The Lesser General
Public License permits more lax criteria for linking other code with
the library.

  We call this license the "Lesser" General Public License because it
does Less to protect the user's freedom than the ordinary General
Public License.  It also provides other free software developers Less
of an advantage over competing non-free programs.  These disadvantages
are the reason we use the ordinary General Public License for many
libraries.  However, the Lesser license provides advantages in certain
special circumstances.

  For example, on rare occasions, there may be a special need to
encourage the widest possible use of a certain library, so that it
becomes a de-facto standard.  To achieve this, non-free programs
must be allowed to use the library.  A more frequent case is that
a free library does the same job as widely used non-free libraries.
In this case, there is little to gain by limiting the free library
to free software only, so we use the Lesser General Public License.

  In other cases, permission to use a particular library in non-free
programs enables a greater number of people to use a large body of
free software.  For example, permission to use the GNU C Library in
non-free programs enables many more people to use the whole GNU
operating system, as well as its variant, the GNU/Linux operating
system.

  Although the Lesser General Public License is Less protective of the
users' freedom, it does ensure that the user of a program that is
linked with the Library has the freedom and the wherewithal to run
that program using a modified version of the Library.

  The precise terms and conditions for copying, distribution and
modification follow.  Pay close attention to the difference between a
"work based on the library" and a "work that uses the library".  The
former contains code derived from the library, whereas the latter must
be combined with the library in order to run.

                  GNU LESSER GENERAL PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

  0. This License Agreement applies to any software library or other
program which contains a notice placed by the copyright holder or
other authorized party saying it may be distributed under the terms of
this Lesser General Public License (also called "this License").
Each licensee is addressed as "you".

  A "library" means a collection of software functions and/or data
prepared so as to be conveniently linked with application programs
(which use some of those functions and data) to form executables.

  The "Library", below, refers to any such software library or work
which has been distributed under these terms.  A "work based on the
Library" means either the Library or any derivative work under
copyright law: that is to say, a work containing the Library or a
portion of it, either verbatim or with modifications and/or translated
straightforwardly into another language.  (Hereinafter, translation is
included without limitation in the term "modification".)

  "Source code" for a work means the preferred form of the work for
making modifications to it.  For a library, complete source code means
all the source code for all modules it contains, plus any associated
interface definition files, plus the scripts used to control
compilation and installation of the library.

  Activities other than copying, distribution and modification are not
covered by this License; they are outside its scope.  The act of
running a program using the Library is not restricted, and output from
such a program is covered only if its contents constitute a work based
on the Library (independent of the use of the Library in a tool for
writing it).  Whether that is true depends on what the Library does
and what the program that uses the Library does.


  1. You may copy and distribute verbatim copies of the Library's
complete source code as you receive it, in any medium, provided that
you conspicuously and appropriately publish on each copy an
appropriate copyright notice and disclaimer of warranty; keep intact
all the notices that refer to this License and to the absence of any
warranty; and distribute a copy of this License along with the
Library.

  You may charge a fee for the physical act of transferring a copy,
and you may at your option offer warranty protection in exchange for a
fee.


  2. You may modify your copy or copies of the Library or any portion
of it, thus forming a work based on the Library, and copy and
distribute such modifications or work under the terms of Section 1
above, provided that you also meet all of these conditions:

    a) The modified work must itself be a software library.

    b) You must cause the files modified to carry prominent notices
    stating that you changed the files and the date of any change.

    c) You must cause the whole of the work to be licensed at no
    charge to all third parties under the terms of this License.

    d) If a facility in the modified Library refers to a function or a
    table of data to be supplied by an application program that uses
    the facility, other than as an argument passed when the facility
    is invoked, then you must make a good faith effort to ensure that,
    in the event an application does not supply such function or
    table, the facility still operates, and performs whatever part of
    its purpose remains meaningful.

    (For example, a function in a library to compute square roots has
    a purpose that is entirely well-defined independent of the
    application.  Therefore, Subsection 2d requires that any
    application-supplied function or table used by this function must
    be optional: if the application does not supply it, the square
    root function must still compute square roots.)

These requirements apply to the modified work as a whole.  If
identifiable sections of that work are not derived from the Library,
and can be reasonably considered independent and separate works in
themselves, then this License, and its terms, do not apply to those
sections when you distribute them as separate works.  But when you
distribute the same sections as part of a whole which is a work based
on the Library, the distribution of the whole must be on the terms of
this License, whose permissions for other licensees extend to the
entire whole, and thus to each and every part regardless of who wrote
it.

Thus, it is not the intent of this section to claim rights or contest
your rights to work written entirely by you; rather, the intent is to
exercise the right to control the distribution of derivative or
collective works based on the Library.

In addition, mere aggregation of another work not based on the Library
with the Library (or with a work based on the Library) on a volume of
a storage or distribution medium does not bring the other work under
the scope of this License.


  3. You may opt to apply the terms of the ordinary GNU General Public
License instead of this License to a given copy of the Library.  To do
this, you must alter all the notices that refer to this License, so
that they refer to the ordinary GNU General Public License, version 2,
instead of to this License.  (If a newer version than version 2 of the
ordinary GNU General Public License has appeared, then you can specify
that version instead if you wish.)  Do not make any other change in
these notices.

  Once this change is made in a given copy, it is irreversible for
that copy, so the ordinary GNU General Public License applies to all
subsequent copies and derivative works made from that copy.

  This option is useful when you wish to copy part of the code of
the Library into a program that is not a library.


  4. You may copy and distribute the Library (or a portion or
derivative of it, under Section 2) in object code or executable form
under the terms of Sections 1 and 2 above provided that you accompany
it with the complete corresponding machine-readable source code, which
must be distributed under the terms of Sections 1 and 2 above on a
medium customarily used for software interchange.

  If distribution of object code is made by offering access to copy
from a designated place, then offering equivalent access to copy the
source code from the same place satisfies the requirement to
distribute the source code, even though third parties are not
compelled to copy the source along with the object code.


  5. A program that contains no derivative of any portion of the
Library, but is designed to work with the Library by being compiled or
linked with it, is called a "work that uses the Library".  Such a
work, in isolation, is not a derivative work of the Library, and
therefore falls outside the scope of this License.

  However, linking a "work that uses the Library" with the Library
creates an executable that is a derivative of the Library (because it
contains portions of the Library), rather than a "work that uses the
library".  The executable is therefore covered by this License.
Section 6 states terms for distribution of such executables.

  When a "work that uses the Library" uses material from a header file
that is part of the Library, the object code for the work may be a
derivative work of the Library even though the source code is not.
Whether this is true is especially significant if the work can be
linked without the Library, or if the work is itself a library.  The
threshold for this to be true is not precisely defined by law.

  If such an object file uses only numerical parameters, data
structure layouts and accessors, and small macros and small inline
functions (ten lines or less in length), then the use of the object
file is unrestricted, regardless of whether it is legally a derivative
work.  (Executables containing this object code plus portions of the
Library will still fall under Section 6.)

  Otherwise, if the work is a derivative of the Library, you may
distribute the object code for the work under the terms of Section 6.
Any executables containing that work also fall under Section 6,
whether or not they are linked directly with the Library itself.


  6. As an exception to the Sections above, you may also combine or
link a "work that uses the Library" with the Library to produce a
work containing portions of the Library, and distribute that work
under terms of your choice, provided that the terms permit
modification of the work for the customer's own use and reverse
engineering for debugging such modifications.

  You must give prominent notice with each copy of the work that the
Library is used in it and that the Library and its use are covered by
this License.  You must supply a copy of this License.  If the work
during execution displays copyright notices, you must include the
copyright notice for the Library among them, as well as a reference
directing the user to the copy of this License.  Also, you must do one
of these things:

    a) Accompany the work with the complete corresponding
    machine-readable source code for the Library including whatever
    changes were used in the work (which must be distributed under
    Sections 1 and 2 above); and, if the work is an executable linked
    with the Library, with the complete machine-readable "work that
    uses the Library", as object code and/or source code, so that the
    user can modify the Library and then relink to produce a modified
    executable containing the modified Library.  (It is understood
    that the user who changes the contents of definitions files in the
    Library will not necessarily be able to recompile the application
    to use the modified definitions.)

    b) Use a suitable shared library mechanism for linking with the
    Library.  A suitable mechanism is one that (1) uses at run time a
    copy of the library already present on the user's computer system,
    rather than copying library functions into the executable, and (2)
    will operate properly with a modified version of the library, if
    the user installs one, as long as the modified version is
    interface-compatible with the version that the work was made with.

    c) Accompany the work with a written offer, valid for at
    least three years, to give the same user the materials
    specified in Subsection 6a, above, for a charge no more
    than the cost of performing this distribution.

    d) If distribution of the work is made by offering access to copy
    from a designated place, offer equivalent access to copy the above
    specified materials from the same place.

    e) Verify that the user has already received a copy of these
    materials or that you have already sent this user a copy.

  For an executable, the required form of the "work that uses the
Library" must include any data and utility programs needed for
reproducing the executable from it.  However, as a special exception,
the materials to be distributed need not include anything that is
normally distributed (in either source or binary form) with the major
components (compiler, kernel, and so on) of the operating system on
which the executable runs, unless that component itself accompanies
the executable.

  It may happen that this requirement contradicts the license
restrictions of other proprietary libraries that do not normally
accompany the operating system.  Such a contradiction means you cannot
use both them and the Library together in an executable that you
distribute.


  7. You may place library facilities that are a work based on the
Library side-by-side in a single library together with other library
facilities not covered by this License, and distribute such a combined
library, provided that the separate distribution of the work based on
the Library and of the other library facilities is otherwise
permitted, and provided that you do these two things:

    a) Accompany the combined library with a copy of the same work
    based on the Library, uncombined with any other library
    facilities.  This must be distributed under the terms of the
    Sections above.

    b) Give prominent notice with the combined library of the fact
    that part of it is a work based on the Library, and explaining
    where to find the accompanying uncombined form of the same work.


  8. You may not copy, modify, sublicense, link with, or distribute
the Library except as expressly provided under this License.  Any
attempt otherwise to copy, modify, sublicense, link with, or
distribute the Library is void, and will automatically terminate your
rights under this License.  However, parties who have received copies,
or rights, from you under this License will not have their licenses
terminated so long as such parties remain in full compliance.


  9. You are not required to accept this License, since you have not
signed it.  However, nothing else grants you permission to modify or
distribute the Library or its derivative works.  These actions are
prohibited by law if you do not accept this License.  Therefore, by
modifying or distributing the Library (or any work based on the
Library), you indicate your acceptance of this License to do so, and
all its terms and conditions for copying, distributing or modifying
the Library or works based on it.

  10. Each time you redistribute the Library (or any work based on the
Library), the recipient automatically receives a license from the
original licensor to copy, distribute, link with or modify the Library
subject to these terms and conditions.  You may not impose any further
restrictions on the recipients' exercise of the rights granted herein.
You are not responsible for enforcing compliance by third parties with
this License.

  11. If, as a consequence of a court judgment or allegation of patent
infringement or for any other reason (not limited to patent issues),
conditions are imposed on you (whether by court order, agreement or
otherwise) that contradict the conditions of this License, they do not
excuse you from the conditions of this License.  If you cannot
distribute so as to satisfy simultaneously your obligations under this
License and any other pertinent obligations, then as a consequence you
may not distribute the Library at all.  For example, if a patent
license would not permit royalty-free redistribution of the Library by
all those who receive copies directly or indirectly through you, then
the only way you could satisfy both it and this License would be to
refrain entirely from distribution of the Library.

If any portion of this section is held invalid or unenforceable under
any particular circumstance, the balance of the section is intended
to apply, and the section as a whole is intended to apply in other
circumstances.

It is not the purpose of this section to induce you to infringe any
patents or other property right claims or to contest validity of any
such claims; this section has the sole purpose of protecting the
integrity of the free software distribution system which is
implemented by public license practices.  Many people have made
generous contributions to the wide range of software distributed
through that system in reliance on consistent application of that
system; it is up to the author/donor to decide if he or she is willing
to distribute software through any other system and a licensee cannot
impose that choice.

This section is intended to make thoroughly clear what is believed to
be a consequence of the rest of this License.

  12. If the distribution and/or use of the Library is restricted in
certain countries either by patents or by copyrighted interfaces, the
original copyright holder who places the Library under this License
may add an explicit geographical distribution limitation excluding
those countries, so that distribution is permitted only in or among
countries not thus excluded.  In such case, this License incorporates
the limitation as if written in the body of this License.

  13. The Free Software Foundation may publish revised and/or new
versions of the Lesser General Public License from time to time.
Such new versions will be similar in spirit to the present version,
but may differ in detail to address new problems or concerns.

Each version is given a distinguishing version number.  If the Library
specifies a version number of this License which applies to it and
"any later version", you have the option of following the terms and
conditions either of that version or of any later version published by
the Free Software Foundation.  If the Library does not specify a
license version number, you may choose any version ever published by
the Free Software Foundation.

  14. If you wish to incorporate parts of the Library into other free
programs whose distribution conditions are incompatible with these,
write to the author to ask for permission.  For software which is
copyrighted by the Free Software Foundation, write to the Free
Software Foundation; we sometimes make exceptions for this.  Our
decision will be guided by the two goals of preserving the free status
of all derivatives of our free software and of promoting the sharing
and reuse of software generally.

                            NO WARRANTY

  15. BECAUSE THE LIBRARY IS LICENSED FREE OF CHARGE, THERE IS NO
WARRANTY FOR THE LIBRARY, TO THE EXTENT PERMITTED BY APPLICABLE LAW.
EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
OTHER PARTIES PROVIDE THE LIBRARY "AS IS" WITHOUT WARRANTY OF ANY
KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE.  THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE
LIBRARY IS WITH YOU.  SHOULD THE LIBRARY PROVE DEFECTIVE, YOU ASSUME
THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.

  16. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN
WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY
AND/OR REDISTRIBUTE THE LIBRARY AS PERMITTED ABOVE, BE LIABLE TO YOU
FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR
CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE
LIBRARY (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING
RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A
FAILURE OF THE LIBRARY TO OPERATE WITH ANY OTHER SOFTWARE), EVEN IF
SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH
DAMAGES.

                     END OF TERMS AND CONDITIONS

           How to Apply These Terms to Your New Libraries

  If you develop a new library, and you want it to be of the greatest
possible use to the public, we recommend making it free software that
everyone can redistribute and change.  You can do so by permitting
redistribution under these terms (or, alternatively, under the terms
of the ordinary General Public License).

  To apply these terms, attach the following notices to the library.
It is safest to attach them to the start of each source file to most
effectively convey the exclusion of warranty; and each file should
have at least the "copyright" line and a pointer to where the full
notice is found.

    <one line to give the library's name and a brief idea of what it does.>
    Copyright (C) <year>  <name of author>

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
    02110-1301  USA

Also add information on how to contact you by electronic and paper mail.

You should also get your employer (if you work as a programmer) or your
school, if any, to sign a "copyright disclaimer" for the library, if
necessary.  Here is a sample; alter the names:

  Yoyodyne, Inc., hereby disclaims all copyright interest in the
  library `Frob' (a library for tweaking knobs) written by James
  Random Hacker.

  <signature of Ty Coon>, 1 April 1990
  Ty Coon, President of Vice

That's all there is to it!

GNU Lesser General Public License Version 2, June 1991

GNU LIBRARY GENERAL PUBLIC LICENSE

Version 2, June 1991

Copyright (C) 1991 Free Software Foundation, Inc.
51 Franklin St, Fifth Floor, Boston, MA  02110-1301, USA
Everyone is permitted to copy and distribute verbatim copies
of this license document, but changing it is not allowed.

[This is the first released version of the library GPL.  It is numbered 2
because it goes with version 2 of the ordinary GPL.]

Preamble

The licenses for most software are designed to take away your freedom to
share and change it. By contrast, the GNU General Public Licenses are
intended to guarantee your freedom to share and change free software--to make
sure the software is free for all its users.

This license, the Library General Public License, applies to some specially
designated Free Software Foundation software, and to any other libraries
whose authors decide to use it. You can use it for your libraries, too.

When we speak of free software, we are referring to freedom, not price. Our
General Public Licenses are designed to make sure that you have the freedom
to distribute copies of free software (and charge for this service if you
wish), that you receive source code or can get it if you want it, that you
can change the software or use pieces of it in new free programs; and that
you know you can do these things.

To protect your rights, we need to make restrictions that forbid anyone to
deny you these rights or to ask you to surrender the rights. These
restrictions translate to certain responsibilities for you if you distribute
copies of the library, or if you modify it.

For example, if you distribute copies of the library, whether gratis or for a
fee, you must give the recipients all the rights that we gave you. You must
make sure that they, too, receive or can get the source code. If you link a
program with the library, you must provide complete object files to the
recipients so that they can relink them with the library, after making
changes to the library and recompiling it. And you must show them these terms
so they know their rights.

Our method of protecting your rights has two steps: (1) copyright the
library, and (2) offer you this license which gives you legal permission to
copy, distribute and/or modify the library.

Also, for each distributor's protection, we want to make certain that
everyone understands that there is no warranty for this free library. If the
library is modified by someone else and passed on, we want its recipients to
know that what they have is not the original version, so that any problems
introduced by others will not reflect on the original authors' reputations.

Finally, any free program is threatened constantly by software patents. We
wish to avoid the danger that companies distributing free software will
individually obtain patent licenses, thus in effect transforming the program
into proprietary software. To prevent this, we have made it clear that any
patent must be licensed for everyone's free use or not licensed at all.

Most GNU software, including some libraries, is covered by the ordinary GNU
General Public License, which was designed for utility programs. This
license, the GNU Library General Public License, applies to certain
designated libraries. This license is quite different from the ordinary one;
be sure to read it in full, and don't assume that anything in it is the same
as in the ordinary license.

The reason we have a separate public license for some libraries is that they
blur the distinction we usually make between modifying or adding to a program
and simply using it. Linking a program with a library, without changing the
library, is in some sense simply using the library, and is analogous to
running a utility program or application program. However, in a textual and
legal sense, the linked executable is a combined work, a derivative of the
original library, and the ordinary General Public License treats it as such.

Because of this blurred distinction, using the ordinary General Public
License for libraries did not effectively promote software sharing, because
most developers did not use the libraries. We concluded that weaker
conditions might promote sharing better.

However, unrestricted linking of non-free programs would deprive the users of
those programs of all benefit from the free status of the libraries
themselves. This Library General Public License is intended to permit
developers of non-free programs to use free libraries, while preserving your
freedom as a user of such programs to change the free libraries that are
incorporated in them. (We have not seen how to achieve this as regards
changes in header files, but we have achieved it as regards changes in the
actual functions of the Library.) The hope is that this will lead to faster
development of free libraries.

The precise terms and conditions for copying, distribution and modification
follow. Pay close attention to the difference between a "work based on the
library" and a "work that uses the library". The former contains code derived
from the library, while the latter only works together with the library.

Note that it is possible for a library to be covered by the ordinary General
Public License rather than by this special one.

TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

0. This License Agreement applies to any software library which contains a
notice placed by the copyright holder or other authorized party saying it may
be distributed under the terms of this Library General Public License (also
called "this License"). Each licensee is addressed as "you".

A "library" means a collection of software functions and/or data prepared so
as to be conveniently linked with application programs (which use some of
those functions and data) to form executables.

The "Library", below, refers to any such software library or work which has
been distributed under these terms. A "work based on the Library" means
either the Library or any derivative work under copyright law: that is to
say, a work containing the Library or a portion of it, either verbatim or
with modifications and/or translated straightforwardly into another language.
(Hereinafter, translation is included without limitation in the term
"modification".)

"Source code" for a work means the preferred form of the work for making
modifications to it. For a library, complete source code means all the source
code for all modules it contains, plus any associated interface definition
files, plus the scripts used to control compilation and installation of the
library.

Activities other than copying, distribution and modification are not covered
by this License; they are outside its scope. The act of running a program
using the Library is not restricted, and output from such a program is
covered only if its contents constitute a work based on the Library
(independent of the use of the Library in a tool for writing it). Whether
that is true depends on what the Library does and what the program that uses
the Library does.

1. You may copy and distribute verbatim copies of the Library's complete
source code as you receive it, in any medium, provided that you conspicuously
and appropriately publish on each copy an appropriate copyright notice and
disclaimer of warranty; keep intact all the notices that refer to this
License and to the absence of any warranty; and distribute a copy of this
License along with the Library.

You may charge a fee for the physical act of transferring a copy, and you may
at your option offer warranty protection in exchange for a fee.

2. You may modify your copy or copies of the Library or any portion of it,
thus forming a work based on the Library, and copy and distribute such
modifications or work under the terms of Section 1 above, provided that you
also meet all of these conditions:

    a) The modified work must itself be a software library.
    b) You must cause the files modified to carry prominent notices stating
that you changed the files and the date of any change.
    c) You must cause the whole of the work to be licensed at no charge to
all third parties under the terms of this License.
    d) If a facility in the modified Library refers to a function or a table
of data to be supplied by an application program that uses the facility,
other than as an argument passed when the facility is invoked, then you must
make a good faith effort to ensure that, in the event an application does not
supply such function or table, the facility still operates, and performs
whatever part of its purpose remains meaningful.

    (For example, a function in a library to compute square roots has a
purpose that is entirely well-defined independent of the application.
Therefore, Subsection 2d requires that any application-supplied function or
table used by this function must be optional: if the application does not
supply it, the square root function must still compute square roots.)

These requirements apply to the modified work as a whole. If identifiable
sections of that work are not derived from the Library, and can be reasonably
considered independent and separate works in themselves, then this License,
and its terms, do not apply to those sections when you distribute them as
separate works. But when you distribute the same sections as part of a whole
which is a work based on the Library, the distribution of the whole must be
on the terms of this License, whose permissions for other licensees extend to
the entire whole, and thus to each and every part regardless of who wrote it.

Thus, it is not the intent of this section to claim rights or contest your
rights to work written entirely by you; rather, the intent is to exercise the
right to control the distribution of derivative or collective works based on
the Library.

In addition, mere aggregation of another work not based on the Library with
the Library (or with a work based on the Library) on a volume of a storage or
distribution medium does not bring the other work under the scope of this
License.

3. You may opt to apply the terms of the ordinary GNU General Public License
instead of this License to a given copy of the Library. To do this, you must
alter all the notices that refer to this License, so that they refer to the
ordinary GNU General Public License, version 2, instead of to this License.
(If a newer version than version 2 of the ordinary GNU General Public License
has appeared, then you can specify that version instead if you wish.) Do not
make any other change in these notices.

Once this change is made in a given copy, it is irreversible for that copy,
so the ordinary GNU General Public License applies to all subsequent copies
and derivative works made from that copy.

This option is useful when you wish to copy part of the code of the Library
into a program that is not a library.

4. You may copy and distribute the Library (or a portion or derivative of it,
under Section 2) in object code or executable form under the terms of
Sections 1 and 2 above provided that you accompany it with the complete
corresponding machine-readable source code, which must be distributed under
the terms of Sections 1 and 2 above on a medium customarily used for software
interchange.

If distribution of object code is made by offering access to copy from a
designated place, then offering equivalent access to copy the source code
from the same place satisfies the requirement to distribute the source code,
even though third parties are not compelled to copy the source along with the
object code.

5. A program that contains no derivative of any portion of the Library, but
is designed to work with the Library by being compiled or linked with it, is
called a "work that uses the Library". Such a work, in isolation, is not a
derivative work of the Library, and therefore falls outside the scope of this
License.

However, linking a "work that uses the Library" with the Library creates an
executable that is a derivative of the Library (because it contains portions
of the Library), rather than a "work that uses the library". The executable
is therefore covered by this License. Section 6 states terms for distribution
of such executables.

When a "work that uses the Library" uses material from a header file that is
part of the Library, the object code for the work may be a derivative work of
the Library even though the source code is not. Whether this is true is
especially significant if the work can be linked without the Library, or if
the work is itself a library. The threshold for this to be true is not
precisely defined by law.

If such an object file uses only numerical parameters, data structure layouts
and accessors, and small macros and small inline functions (ten lines or less
in length), then the use of the object file is unrestricted, regardless of
whether it is legally a derivative work. (Executables containing this object
code plus portions of the Library will still fall under Section 6.)

Otherwise, if the work is a derivative of the Library, you may distribute the
object code for the work under the terms of Section 6. Any executables
containing that work also fall under Section 6, whether or not they are
linked directly with the Library itself.

6. As an exception to the Sections above, you may also compile or link a
"work that uses the Library" with the Library to produce a work containing
portions of the Library, and distribute that work under terms of your choice,
provided that the terms permit modification of the work for the customer's
own use and reverse engineering for debugging such modifications.

You must give prominent notice with each copy of the work that the Library is
used in it and that the Library and its use are covered by this License. You
must supply a copy of this License. If the work during execution displays
copyright notices, you must include the copyright notice for the Library
among them, as well as a reference directing the user to the copy of this
License. Also, you must do one of these things:

    a) Accompany the work with the complete corresponding machine-readable
source code for the Library including whatever changes were used in the work
(which must be distributed under Sections 1 and 2 above); and, if the work is
an executable linked with the Library, with the complete machine-readable
"work that uses the Library", as object code and/or source code, so that the
user can modify the Library and then relink to produce a modified executable
containing the modified Library. (It is understood that the user who changes
the contents of definitions files in the Library will not necessarily be able
to recompile the application to use the modified definitions.)
    b) Accompany the work with a written offer, valid for at least three
years, to give the same user the materials specified in Subsection 6a, above,
for a charge no more than the cost of performing this distribution.
    c) If distribution of the work is made by offering access to copy from a
designated place, offer equivalent access to copy the above specified
materials from the same place.
    d) Verify that the user has already received a copy of these materials or
that you have already sent this user a copy.

For an executable, the required form of the "work that uses the Library" must
include any data and utility programs needed for reproducing the executable
from it. However, as a special exception, the source code distributed need
not include anything that is normally distributed (in either source or binary
form) with the major components (compiler, kernel, and so on) of the
operating system on which the executable runs, unless that component itself
accompanies the executable.

It may happen that this requirement contradicts the license restrictions of
other proprietary libraries that do not normally accompany the operating
system. Such a contradiction means you cannot use both them and the Library
together in an executable that you distribute.

7. You may place library facilities that are a work based on the Library
side-by-side in a single library together with other library facilities not
covered by this License, and distribute such a combined library, provided
that the separate distribution of the work based on the Library and of the
other library facilities is otherwise permitted, and provided that you do
these two things:

    a) Accompany the combined library with a copy of the same work based on
the Library, uncombined with any other library facilities. This must be
distributed under the terms of the Sections above.
    b) Give prominent notice with the combined library of the fact that part
of it is a work based on the Library, and explaining where to find the
accompanying uncombined form of the same work.

8. You may not copy, modify, sublicense, link with, or distribute the Library
except as expressly provided under this License. Any attempt otherwise to
copy, modify, sublicense, link with, or distribute the Library is void, and
will automatically terminate your rights under this License. However, parties
who have received copies, or rights, from you under this License will not
have their licenses terminated so long as such parties remain in full
compliance.

9. You are not required to accept this License, since you have not signed it.
However, nothing else grants you permission to modify or distribute the
Library or its derivative works. These actions are prohibited by law if you
do not accept this License. Therefore, by modifying or distributing the
Library (or any work based on the Library), you indicate your acceptance of
this License to do so, and all its terms and conditions for copying,
distributing or modifying the Library or works based on it.

10. Each time you redistribute the Library (or any work based on the
Library), the recipient automatically receives a license from the original
licensor to copy, distribute, link with or modify the Library subject to
these terms and conditions. You may not impose any further restrictions on
the recipients' exercise of the rights granted herein. You are not
responsible for enforcing compliance by third parties to this License.

11. If, as a consequence of a court judgment or allegation of patent
infringement or for any other reason (not limited to patent issues),
conditions are imposed on you (whether by court order, agreement or
otherwise) that contradict the conditions of this License, they do not excuse
you from the conditions of this License. If you cannot distribute so as to
satisfy simultaneously your obligations under this License and any other
pertinent obligations, then as a consequence you may not distribute the
Library at all. For example, if a patent license would not permit
royalty-free redistribution of the Library by all those who receive copies
directly or indirectly through you, then the only way you could satisfy both
it and this License would be to refrain entirely from distribution of the
Library.

If any portion of this section is held invalid or unenforceable under any
particular circumstance, the balance of the section is intended to apply, and
the section as a whole is intended to apply in other circumstances.

It is not the purpose of this section to induce you to infringe any patents
or other property right claims or to contest validity of any such claims;
this section has the sole purpose of protecting the integrity of the free
software distribution system which is implemented by public license
practices. Many people have made generous contributions to the wide range of
software distributed through that system in reliance on consistent
application of that system; it is up to the author/donor to decide if he or
she is willing to distribute software through any other system and a licensee
cannot impose that choice.

This section is intended to make thoroughly clear what is believed to be a
consequence of the rest of this License.

12. If the distribution and/or use of the Library is restricted in certain
countries either by patents or by copyrighted interfaces, the original
copyright holder who places the Library under this License may add an
explicit geographical distribution limitation excluding those countries, so
that distribution is permitted only in or among countries not thus excluded.
In such case, this License incorporates the limitation as if written in the
body of this License.

13. The Free Software Foundation may publish revised and/or new versions of
the Library General Public License from time to time. Such new versions will
be similar in spirit to the present version, but may differ in detail to
address new problems or concerns.

Each version is given a distinguishing version number. If the Library
specifies a version number of this License which applies to it and "any later
version", you have the option of following the terms and conditions either of
that version or of any later version published by the Free Software
Foundation. If the Library does not specify a license version number, you may
choose any version ever published by the Free Software Foundation.

14. If you wish to incorporate parts of the Library into other free programs
whose distribution conditions are incompatible with these, write to the
author to ask for permission. For software which is copyrighted by the Free
Software Foundation, write to the Free Software Foundation; we sometimes make
exceptions for this. Our decision will be guided by the two goals of
preserving the free status of all derivatives of our free software and of
promoting the sharing and reuse of software generally.

NO WARRANTY

15. BECAUSE THE LIBRARY IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY FOR
THE LIBRARY, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE
STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE
LIBRARY "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND
PERFORMANCE OF THE LIBRARY IS WITH YOU. SHOULD THE LIBRARY PROVE DEFECTIVE,
YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.

16. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING
WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY AND/OR
REDISTRIBUTE THE LIBRARY AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES,
INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING
OUT OF THE USE OR INABILITY TO USE THE LIBRARY (INCLUDING BUT NOT LIMITED TO
LOSS OF DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR
THIRD PARTIES OR A FAILURE OF THE LIBRARY TO OPERATE WITH ANY OTHER
SOFTWARE), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE
POSSIBILITY OF SUCH DAMAGES.
END OF TERMS AND CONDITIONS
How to Apply These Terms to Your New Libraries

If you develop a new library, and you want it to be of the greatest possible
use to the public, we recommend making it free software that everyone can
redistribute and change. You can do so by permitting redistribution under
these terms (or, alternatively, under the terms of the ordinary General
Public License).

To apply these terms, attach the following notices to the library. It is
safest to attach them to the start of each source file to most effectively
convey the exclusion of warranty; and each file should have at least the
"copyright" line and a pointer to where the full notice is found.

one line to give the library's name and an idea of what it does.
Copyright (C) year  name of author

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
Boston, MA  02110-1301, USA.

Also add information on how to contact you by electronic and paper mail.

You should also get your employer (if you work as a programmer) or your
school, if any, to sign a "copyright disclaimer" for the library, if
necessary. Here is a sample; alter the names:

Yoyodyne, Inc., hereby disclaims all copyright interest in
the library `Frob' (a library for tweaking knobs) written
by James Random Hacker.

signature of Ty Coon, 1 April 1990
Ty Coon, President of Vice

That's all there is to it!

MIT License

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

Written Offer for Source Code

   For any software that you receive from Oracle in binary form which is
   licensed under an open source license that gives you the right to
   receive the source code for that binary, you can obtain a copy of the
   applicable source code by visiting
   http://www.oracle.com/goto/opensourcecode. If the source code for the
   binary was not provided to you with the binary, you can also receive a
   copy of the source code on physical media by submitting a written
   request to the address listed below or by sending an email to Oracle
   using the following link:
   http://www.oracle.com/goto/opensourcecode/request.
  Oracle America, Inc.
  Attn: Senior Vice President
  Development and Engineering Legal
  500 Oracle Parkway, 10th Floor
  Redwood Shores, CA 94065

   Your request should include:

     * The name of the binary for which you are requesting the source code

     * The name and version number of the Oracle product containing the
       binary

     * The date you received the Oracle product

     * Your name

     * Your company name (if applicable)

     * Your return mailing address and email, and

     * A telephone number in the event we need to reach you.

   We may charge you a fee to cover the cost of physical media and
   processing.

   Your request must be sent
    a. within three (3) years of the date you received the Oracle product
       that included the binary that is the subject of your request, or
    b. in the case of code licensed under the GPL v3 for as long as Oracle
       offers spare parts or customer support for that product model.
```

### The 2-Clause BSD License

```
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

### The 3-Clause BSD License

```
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the copyright holder nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

## Third-party library copyright attributions

This section lists the copyright attributions and notices for third party library (where applicable) for direct
(i.e., not transitive) dependencies.

### angular

```
Copyright (c) 2010-2024 Google LLC
```

### angular2-notifications

```
Copyright (c) 2016 Florian Copes
```

### Apache Batik (org.apache.xmlgraphics.*)

```
Apache Batik
Copyright: 1999-2024 The Apache Software Foundation
```

### Apache Commons Codec (commons-codec.*)

```
Apache Commons Codec
Copyright 2002-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).
```

### Apache Commons Collections (org.apache.commons.commons-collections4)

```
Apache Commons Collections
Copyright 2001-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).
```

### Apache Commons Configuration (org.apache.commons.commons-configuration2)

```
Apache Commons Configuration
Copyright 2001-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).
```

### Apache Commons FileUpload (commons-fileupload.*)

```
Apache Commons FileUpload
Copyright 2002-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).
```

### Apache Commons IO (commons-io.*)

```
Apache Commons IO
Copyright 2002-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).
```

### Apache Commons Lang (org.apache.commons.commons-lang3)

```
Apache Commons Lang
Copyright 2001-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).
```

### Apache Commons Text (org.apache.commons.commons-text)

```
Apache Commons Text
Copyright 2014-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).
```

### Apache CXF (org.apache.cxf.*)

```
Apache CXF
Copyright 2006-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).
```

### Apache FreeMarker (org.freemarker.*)

```
Apache FreeMarker
Copyright 2015-2018 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).
```

### Apache HttpClient (org.apache.httpcomponents.client5.*)

```
Apache HttpComponents Client
Copyright 1999-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).

This product includes a copy of in https://publicsuffix.org/list/effective_tld_names.dat
in httpclient5/src/test/resources/org/publicsuffix/list/effective_tld_names.dat
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.
```

### Apache HttpCore / Apache HttpComponents Core (org.apache.httpcomponents.*)

```
Apache HttpComponents Core
Copyright 2005-2024 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).
```

### Apache Jena (org.apache.jena.*)

```
Apache Jena
Copyright 2011-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).

Portions of this software were originally based on the following:
  - Copyright 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
  - Copyright 2010, 2011 Epimorphics Ltd.
  - Copyright 2010, 2011 Talis Systems Ltd.
These have been licensed to the Apache Software Foundation under a software grant.

This product includes software developed by
PluggedIn Software under a BSD license.

Portions of this software are from Apache Xerces and were originally based on the following:
  - software copyright (c) 1999, IBM Corporation., http://www.ibm.com.
  - software copyright (c) 1999, Sun Microsystems., http://www.sun.com.

This product includes software developed at the Open Geospatial Consortium:
http://www.opengeospatial.org/ogc/software
```

### Apache Pekko (org.apache.pekko.*)

```
Apache Pekko
Copyright 2022-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).

This product contains significant parts that were originally based on software from Lightbend (Akka <https://akka.io/>).
Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>

Apache Pekko is derived from Akka 2.6.x, the last version that was distributed under the
Apache License, Version 2.0 License.

---------------

pekko-actor contains MurmurHash.scala which has changes made by the Scala-Lang team under an Apache 2.0 license.

Copyright (c) 2002-2023 EPFL
Copyright (c) 2011-2023 Lightbend, Inc.

Scala includes software developed at
LAMP/EPFL (https://lamp.epfl.ch/) and
Lightbend, Inc. (https://www.lightbend.com/).

---------------

pekko-actor contains code from scala-collection-compat which was released under an Apache 2.0 license.

scala-collection-compat
Copyright (c) 2002-2023 EPFL
Copyright (c) 2011-2023 Lightbend, Inc.

Scala includes software developed at
LAMP/EPFL (https://lamp.epfl.ch/) and
Lightbend, Inc. (https://www.lightbend.com/).

---------------

pekko-actor contains code from scala-library which was released under an Apache 2.0 license.

Scala
Copyright (c) 2002-2023 EPFL
Copyright (c) 2011-2023 Lightbend, Inc.

Scala includes software developed at
LAMP/EPFL (https://lamp.epfl.ch/) and
Lightbend, Inc. (https://www.lightbend.com/).

---------------

pekko-actor contains code from Netty which was released under an Apache 2.0 license.

                            The Netty Project
                            =================

Please visit the Netty web site for more information:

  * https://netty.io/

Copyright 2014 The Netty Project

The Netty Project licenses this file to you under the Apache License,
version 2.0 (the "License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at:

  https://www.apache.org/licenses/LICENSE-2.0

---------------

pekko-actor contains code from java-uuid-generator <https://github.com/cowtowncoder/java-uuid-generator>
in `org.apache.pekko.util.UUIDComparator.scala` which was released under an Apache 2.0 license.

Java UUID generator library has been written by Tatu Saloranta (tatu.saloranta@iki.fi)

Other developers who have contributed code are:

* Eric Bie contributed extensive unit test suite which has helped ensure high implementation
  quality

---------------

pekko-remote contains CountMinSketch.java which was developed under an Apache 2.0 license.

stream-lib
Copyright 2016 AddThis

This product includes software developed by AddThis.
```

### Apache PDFBox (org.apache.pdfbox.*)

```
Apache PDFBox
Copyright 2014 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).

Based on source code originally developed in the PDFBox and
FontBox projects.

Copyright (c) 2002-2007, www.pdfbox.org

Based on source code originally developed in the PaDaF project.
Copyright (c) 2010 Atos Worldline SAS

Includes the Adobe Glyph List
Copyright 1997, 1998, 2002, 2007, 2010 Adobe Systems Incorporated.

Includes the Zapf Dingbats Glyph List
Copyright 2002, 2010 Adobe Systems Incorporated.

Includes OSXAdapter
Copyright (C) 2003-2007 Apple, Inc., All Rights Reserved
```

### Apache Tika (org.apache.tika.*)

```
Apache Tika
Copyright 2021 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).

Copyright 1993-2010 University Corporation for Atmospheric Research/Unidata
This software contains code derived from UCAR/Unidata's NetCDF library.

Tika-server component uses CDDL-licensed dependencies: jersey (http://jersey.java.net/) and
Grizzly (http://grizzly.java.net/)

Tika-parsers component uses CDDL/LGPL dual-licensed dependency: jhighlight (https://github.com/codelibs/jhighlight)

OpenCSV: Copyright 2005 Bytecode Pty Ltd. Licensed under the Apache License, Version 2.0

IPTC Photo Metadata descriptions Copyright 2010 International Press Telecommunications Council.

Tika-mimetypes.xml includes mimetype definitions that were adapted from the PRONOM Technical Registry
by The National Archives (http://www.nationalarchives.gov.uk/PRONOM/Default.aspx). PRONOM is published
under the Open Government License 3.0 (http://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/)

-------------------------------
For compatibility with Java > 8
org.glassfish.jaxb:jaxb-runtime

License: CDDL 1.1, GPL2 w/ CPE

COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.1
1. Definitions.

  1.1. "Contributor" means each individual or entity that creates or
  contributes to the creation of Modifications.

  1.2. "Contributor Version" means the combination of the Original
  Software, prior Modifications used by a Contributor (if any), and
  the Modifications made by that particular Contributor.

  1.3. "Covered Software" means (a) the Original Software, or (b)
  Modifications, or (c) the combination of files containing Original
  Software with files containing Modifications, in each case including
  portions thereof.

  1.4. "Executable" means the Covered Software in any form other than
  Source Code.

  1.5. "Initial Developer" means the individual or entity that first
  makes Original Software available under this License.

  1.6. "Larger Work" means a work which combines Covered Software or
  portions thereof with code not governed by the terms of this License.

  1.7. "License" means this document.

  1.8. "Licensable" means having the right to grant, to the maximum
  extent possible, whether at the time of the initial grant or
  subsequently acquired, any and all of the rights conveyed herein.

  1.9. "Modifications" means the Source Code and Executable form of
  any of the following:

  A. Any file that results from an addition to, deletion from or
  modification of the contents of a file containing Original Software
  or previous Modifications;

  B. Any new file that contains any part of the Original Software or
  previous Modification; or

  C. Any new file that is contributed or otherwise made available
  under the terms of this License.

  1.10. "Original Software" means the Source Code and Executable form
  of computer software code that is originally released under this
  License.

  1.11. "Patent Claims" means any patent claim(s), now owned or
  hereafter acquired, including without limitation, method, process,
  and apparatus claims, in any patent Licensable by grantor.

  1.12. "Source Code" means (a) the common form of computer software
  code in which modifications are made and (b) associated
  documentation included in or with such code.

  1.13. "You" (or "Your") means an individual or a legal entity
  exercising rights under, and complying with all of the terms of,
  this License. For legal entities, "You" includes any entity which
  controls, is controlled by, or is under common control with You. For
  purposes of this definition, "control" means (a) the power, direct
  or indirect, to cause the direction or management of such entity,
  whether by contract or otherwise, or (b) ownership of more than
  fifty percent (50%) of the outstanding shares or beneficial
  ownership of such entity.

2. License Grants.

  2.1. The Initial Developer Grant.

  Conditioned upon Your compliance with Section 3.1 below and subject
  to third party intellectual property claims, the Initial Developer
  hereby grants You a world-wide, royalty-free, non-exclusive license:

  (a) under intellectual property rights (other than patent or
  trademark) Licensable by Initial Developer, to use, reproduce,
  modify, display, perform, sublicense and distribute the Original
  Software (or portions thereof), with or without Modifications,
  and/or as part of a Larger Work; and

  (b) under Patent Claims infringed by the making, using or selling of
  Original Software, to make, have made, use, practice, sell, and
  offer for sale, and/or otherwise dispose of the Original Software
  (or portions thereof).

  (c) The licenses granted in Sections 2.1(a) and (b) are effective on
  the date Initial Developer first distributes or otherwise makes the
  Original Software available to a third party under the terms of this
  License.

  (d) Notwithstanding Section 2.1(b) above, no patent license is
  granted: (1) for code that You delete from the Original Software, or
  (2) for infringements caused by: (i) the modification of the
  Original Software, or (ii) the combination of the Original Software
  with other software or devices.

  2.2. Contributor Grant.

  Conditioned upon Your compliance with Section 3.1 below and subject
  to third party intellectual property claims, each Contributor hereby
  grants You a world-wide, royalty-free, non-exclusive license:

  (a) under intellectual property rights (other than patent or
  trademark) Licensable by Contributor to use, reproduce, modify,
  display, perform, sublicense and distribute the Modifications
  created by such Contributor (or portions thereof), either on an
  unmodified basis, with other Modifications, as Covered Software
  and/or as part of a Larger Work; and

  (b) under Patent Claims infringed by the making, using, or selling
  of Modifications made by that Contributor either alone and/or in
  combination with its Contributor Version (or portions of such
  combination), to make, use, sell, offer for sale, have made, and/or
  otherwise dispose of: (1) Modifications made by that Contributor (or
  portions thereof); and (2) the combination of Modifications made by
  that Contributor with its Contributor Version (or portions of such
  combination).

  (c) The licenses granted in Sections 2.2(a) and 2.2(b) are effective
  on the date Contributor first distributes or otherwise makes the
  Modifications available to a third party.

  (d) Notwithstanding Section 2.2(b) above, no patent license is
  granted: (1) for any code that Contributor has deleted from the
  Contributor Version; (2) for infringements caused by: (i) third
  party modifications of Contributor Version, or (ii) the combination
  of Modifications made by that Contributor with other software
  (except as part of the Contributor Version) or other devices; or (3)
  under Patent Claims infringed by Covered Software in the absence of
  Modifications made by that Contributor.

3. Distribution Obligations.

  3.1. Availability of Source Code.

  Any Covered Software that You distribute or otherwise make available
  in Executable form must also be made available in Source Code form
  and that Source Code form must be distributed only under the terms
  of this License. You must include a copy of this License with every
  copy of the Source Code form of the Covered Software You distribute
  or otherwise make available. You must inform recipients of any such
  Covered Software in Executable form as to how they can obtain such
  Covered Software in Source Code form in a reasonable manner on or
  through a medium customarily used for software exchange.

  3.2. Modifications.

  The Modifications that You create or to which You contribute are
  governed by the terms of this License. You represent that You
  believe Your Modifications are Your original creation(s) and/or You
  have sufficient rights to grant the rights conveyed by this License.

  3.3. Required Notices.

  You must include a notice in each of Your Modifications that
  identifies You as the Contributor of the Modification. You may not
  remove or alter any copyright, patent or trademark notices contained
  within the Covered Software, or any notices of licensing or any
  descriptive text giving attribution to any Contributor or the
  Initial Developer.

  3.4. Application of Additional Terms.

  You may not offer or impose any terms on any Covered Software in
  Source Code form that alters or restricts the applicable version of
  this License or the recipients' rights hereunder. You may choose to
  offer, and to charge a fee for, warranty, support, indemnity or
  liability obligations to one or more recipients of Covered Software.
  However, you may do so only on Your own behalf, and not on behalf of
  the Initial Developer or any Contributor. You must make it
  absolutely clear that any such warranty, support, indemnity or
  liability obligation is offered by You alone, and You hereby agree
  to indemnify the Initial Developer and every Contributor for any
  liability incurred by the Initial Developer or such Contributor as a
  result of warranty, support, indemnity or liability terms You offer.

  3.5. Distribution of Executable Versions.

  You may distribute the Executable form of the Covered Software under
  the terms of this License or under the terms of a license of Your
  choice, which may contain terms different from this License,
  provided that You are in compliance with the terms of this License
  and that the license for the Executable form does not attempt to
  limit or alter the recipient's rights in the Source Code form from
  the rights set forth in this License. If You distribute the Covered
  Software in Executable form under a different license, You must make
  it absolutely clear that any terms which differ from this License
  are offered by You alone, not by the Initial Developer or
  Contributor. You hereby agree to indemnify the Initial Developer and
  every Contributor for any liability incurred by the Initial
  Developer or such Contributor as a result of any such terms You offer.

  3.6. Larger Works.

  You may create a Larger Work by combining Covered Software with
  other code not governed by the terms of this License and distribute
  the Larger Work as a single product. In such a case, You must make
  sure the requirements of this License are fulfilled for the Covered
  Software.

4. Versions of the License.

  4.1. New Versions.

  Oracle is the initial license steward and may publish revised and/or
  new versions of this License from time to time. Each version will be
  given a distinguishing version number. Except as provided in Section
  4.3, no one other than the license steward has the right to modify
  this License.

  4.2. Effect of New Versions.

  You may always continue to use, distribute or otherwise make the
  Covered Software available under the terms of the version of the
  License under which You originally received the Covered Software. If
  the Initial Developer includes a notice in the Original Software
  prohibiting it from being distributed or otherwise made available
  under any subsequent version of the License, You must distribute and
  make the Covered Software available under the terms of the version
  of the License under which You originally received the Covered
  Software. Otherwise, You may also choose to use, distribute or
  otherwise make the Covered Software available under the terms of any
  subsequent version of the License published by the license steward.

  4.3. Modified Versions.

  When You are an Initial Developer and You want to create a new
  license for Your Original Software, You may create and use a
  modified version of this License if You: (a) rename the license and
  remove any references to the name of the license steward (except to
  note that the license differs from this License); and (b) otherwise
  make it clear that the license contains terms which differ from this
  License.

5. DISCLAIMER OF WARRANTY.

  COVERED SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "AS IS" BASIS,
  WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED,
  INCLUDING, WITHOUT LIMITATION, WARRANTIES THAT THE COVERED SOFTWARE
  IS FREE OF DEFECTS, MERCHANTABLE, FIT FOR A PARTICULAR PURPOSE OR
  NON-INFRINGING. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF
  THE COVERED SOFTWARE IS WITH YOU. SHOULD ANY COVERED SOFTWARE PROVE
  DEFECTIVE IN ANY RESPECT, YOU (NOT THE INITIAL DEVELOPER OR ANY
  OTHER CONTRIBUTOR) ASSUME THE COST OF ANY NECESSARY SERVICING,
  REPAIR OR CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN
  ESSENTIAL PART OF THIS LICENSE. NO USE OF ANY COVERED SOFTWARE IS
  AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.

6. TERMINATION.

  6.1. This License and the rights granted hereunder will terminate
  automatically if You fail to comply with terms herein and fail to
  cure such breach within 30 days of becoming aware of the breach.
  Provisions which, by their nature, must remain in effect beyond the
  termination of this License shall survive.

  6.2. If You assert a patent infringement claim (excluding
  declaratory judgment actions) against Initial Developer or a
  Contributor (the Initial Developer or Contributor against whom You
  assert such claim is referred to as "Participant") alleging that the
  Participant Software (meaning the Contributor Version where the
  Participant is a Contributor or the Original Software where the
  Participant is the Initial Developer) directly or indirectly
  infringes any patent, then any and all rights granted directly or
  indirectly to You by such Participant, the Initial Developer (if the
  Initial Developer is not the Participant) and all Contributors under
  Sections 2.1 and/or 2.2 of this License shall, upon 60 days notice
  from Participant terminate prospectively and automatically at the
  expiration of such 60 day notice period, unless if within such 60
  day period You withdraw Your claim with respect to the Participant
  Software against such Participant either unilaterally or pursuant to
  a written agreement with Participant.

  6.3. If You assert a patent infringement claim against Participant
  alleging that the Participant Software directly or indirectly
  infringes any patent where such claim is resolved (such as by
  license or settlement) prior to the initiation of patent
  infringement litigation, then the reasonable value of the licenses
  granted by such Participant under Sections 2.1 or 2.2 shall be taken
  into account in determining the amount or value of any payment or
  license.

  6.4. In the event of termination under Sections 6.1 or 6.2 above,
  all end user licenses that have been validly granted by You or any
  distributor hereunder prior to termination (excluding licenses
  granted to You by any distributor) shall survive termination.

7. LIMITATION OF LIABILITY.

  UNDER NO CIRCUMSTANCES AND UNDER NO LEGAL THEORY, WHETHER TORT
  (INCLUDING NEGLIGENCE), CONTRACT, OR OTHERWISE, SHALL YOU, THE
  INITIAL DEVELOPER, ANY OTHER CONTRIBUTOR, OR ANY DISTRIBUTOR OF
  COVERED SOFTWARE, OR ANY SUPPLIER OF ANY OF SUCH PARTIES, BE LIABLE
  TO ANY PERSON FOR ANY INDIRECT, SPECIAL, INCIDENTAL, OR
  CONSEQUENTIAL DAMAGES OF ANY CHARACTER INCLUDING, WITHOUT
  LIMITATION, DAMAGES FOR LOSS OF GOODWILL, WORK STOPPAGE, COMPUTER
  FAILURE OR MALFUNCTION, OR ANY AND ALL OTHER COMMERCIAL DAMAGES OR
  LOSSES, EVEN IF SUCH PARTY SHALL HAVE BEEN INFORMED OF THE
  POSSIBILITY OF SUCH DAMAGES. THIS LIMITATION OF LIABILITY SHALL NOT
  APPLY TO LIABILITY FOR DEATH OR PERSONAL INJURY RESULTING FROM SUCH
  PARTY'S NEGLIGENCE TO THE EXTENT APPLICABLE LAW PROHIBITS SUCH
  LIMITATION. SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OR
  LIMITATION OF INCIDENTAL OR CONSEQUENTIAL DAMAGES, SO THIS EXCLUSION
  AND LIMITATION MAY NOT APPLY TO YOU.

8. U.S. GOVERNMENT END USERS.

  The Covered Software is a "commercial item," as that term is defined
  in 48 C.F.R. 2.101 (Oct. 1995), consisting of "commercial computer
  software" (as that term is defined at 48 C.F.R. §
  252.227-7014(a)(1)) and "commercial computer software documentation"
  as such terms are used in 48 C.F.R. 12.212 (Sept. 1995). Consistent
  with 48 C.F.R. 12.212 and 48 C.F.R. 227.7202-1 through 227.7202-4
  (June 1995), all U.S. Government End Users acquire Covered Software
  with only those rights set forth herein. This U.S. Government Rights
  clause is in lieu of, and supersedes, any other FAR, DFAR, or other
  clause or provision that addresses Government rights in computer
  software under this License.

9. MISCELLANEOUS.

  This License represents the complete agreement concerning subject
  matter hereof. If any provision of this License is held to be
  unenforceable, such provision shall be reformed only to the extent
  necessary to make it enforceable. This License shall be governed by
  the law of the jurisdiction specified in a notice contained within
  the Original Software (except to the extent applicable law, if any,
  provides otherwise), excluding such jurisdiction's conflict-of-law
  provisions. Any litigation relating to this License shall be subject
  to the jurisdiction of the courts located in the jurisdiction and
  venue specified in a notice contained within the Original Software,
  with the losing party responsible for costs, including, without
  limitation, court costs and reasonable attorneys' fees and expenses.
  The application of the United Nations Convention on Contracts for
  the International Sale of Goods is expressly excluded. Any law or
  regulation which provides that the language of a contract shall be
  construed against the drafter shall not apply to this License. You
  agree that You alone are responsible for compliance with the United
  States export administration regulations (and the export control
  laws and regulation of any other countries) when You use, distribute
  or otherwise make available any Covered Software.

10. RESPONSIBILITY FOR CLAIMS.

  As between Initial Developer and the Contributors, each party is
  responsible for claims and damages arising, directly or indirectly,
  out of its utilization of rights under this License and You agree to
  work with Initial Developer and Contributors to distribute such
  responsibility on an equitable basis. Nothing herein is intended or
  shall be deemed to constitute any admission of liability.

NOTICE PURSUANT TO SECTION 9 OF THE COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL)

The code released under the CDDL shall be governed by the laws of the
State of California (excluding conflict-of-law provisions). Any
litigation relating to this License shall be subject to the jurisdiction
of the Federal Courts of the Northern District of California and the
state courts of the State of California, with venue lying in Santa Clara
County, California.

The GNU General Public License (GPL) Version 2, June 1991
Copyright (C) 1989, 1991 Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor
Boston, MA 02110-1335
USA

Everyone is permitted to copy and distribute verbatim copies
of this license document, but changing it is not allowed.

Preamble

The licenses for most software are designed to take away your freedom to
share and change it. By contrast, the GNU General Public License is
intended to guarantee your freedom to share and change free software--to
make sure the software is free for all its users. This General Public
License applies to most of the Free Software Foundation's software and
to any other program whose authors commit to using it. (Some other Free
Software Foundation software is covered by the GNU Library General
Public License instead.) You can apply it to your programs, too.

When we speak of free software, we are referring to freedom, not price.
Our General Public Licenses are designed to make sure that you have the
freedom to distribute copies of free software (and charge for this
service if you wish), that you receive source code or can get it if you
want it, that you can change the software or use pieces of it in new
free programs; and that you know you can do these things.

To protect your rights, we need to make restrictions that forbid anyone
to deny you these rights or to ask you to surrender the rights. These
restrictions translate to certain responsibilities for you if you
distribute copies of the software, or if you modify it.

For example, if you distribute copies of such a program, whether gratis
or for a fee, you must give the recipients all the rights that you have.
You must make sure that they, too, receive or can get the source code.
And you must show them these terms so they know their rights.

We protect your rights with two steps: (1) copyright the software, and
(2) offer you this license which gives you legal permission to copy,
distribute and/or modify the software.

Also, for each author's protection and ours, we want to make certain
that everyone understands that there is no warranty for this free
software. If the software is modified by someone else and passed on, we
want its recipients to know that what they have is not the original, so
that any problems introduced by others will not reflect on the original
authors' reputations.

Finally, any free program is threatened constantly by software patents.
We wish to avoid the danger that redistributors of a free program will
individually obtain patent licenses, in effect making the program
proprietary. To prevent this, we have made it clear that any patent must
be licensed for everyone's free use or not licensed at all.

The precise terms and conditions for copying, distribution and
modification follow.

TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

0. This License applies to any program or other work which contains a
notice placed by the copyright holder saying it may be distributed under
the terms of this General Public License. The "Program", below, refers
to any such program or work, and a "work based on the Program" means
either the Program or any derivative work under copyright law: that is
to say, a work containing the Program or a portion of it, either
verbatim or with modifications and/or translated into another language.
(Hereinafter, translation is included without limitation in the term
"modification".) Each licensee is addressed as "you".

Activities other than copying, distribution and modification are not
covered by this License; they are outside its scope. The act of running
the Program is not restricted, and the output from the Program is
covered only if its contents constitute a work based on the Program
(independent of having been made by running the Program). Whether that
is true depends on what the Program does.

1. You may copy and distribute verbatim copies of the Program's source
code as you receive it, in any medium, provided that you conspicuously
and appropriately publish on each copy an appropriate copyright notice
and disclaimer of warranty; keep intact all the notices that refer to
this License and to the absence of any warranty; and give any other
recipients of the Program a copy of this License along with the Program.

You may charge a fee for the physical act of transferring a copy, and
you may at your option offer warranty protection in exchange for a fee.

2. You may modify your copy or copies of the Program or any portion of
it, thus forming a work based on the Program, and copy and distribute
such modifications or work under the terms of Section 1 above, provided
that you also meet all of these conditions:

    a) You must cause the modified files to carry prominent notices
    stating that you changed the files and the date of any change.

    b) You must cause any work that you distribute or publish, that in
    whole or in part contains or is derived from the Program or any part
    thereof, to be licensed as a whole at no charge to all third parties
    under the terms of this License.

    c) If the modified program normally reads commands interactively
    when run, you must cause it, when started running for such
    interactive use in the most ordinary way, to print or display an
    announcement including an appropriate copyright notice and a notice
    that there is no warranty (or else, saying that you provide a
    warranty) and that users may redistribute the program under these
    conditions, and telling the user how to view a copy of this License.
    (Exception: if the Program itself is interactive but does not
    normally print such an announcement, your work based on the Program
    is not required to print an announcement.)

These requirements apply to the modified work as a whole. If
identifiable sections of that work are not derived from the Program, and
can be reasonably considered independent and separate works in
themselves, then this License, and its terms, do not apply to those
sections when you distribute them as separate works. But when you
distribute the same sections as part of a whole which is a work based on
the Program, the distribution of the whole must be on the terms of this
License, whose permissions for other licensees extend to the entire
whole, and thus to each and every part regardless of who wrote it.

Thus, it is not the intent of this section to claim rights or contest
your rights to work written entirely by you; rather, the intent is to
exercise the right to control the distribution of derivative or
collective works based on the Program.

In addition, mere aggregation of another work not based on the Program
with the Program (or with a work based on the Program) on a volume of a
storage or distribution medium does not bring the other work under the
scope of this License.

3. You may copy and distribute the Program (or a work based on it,
under Section 2) in object code or executable form under the terms of
Sections 1 and 2 above provided that you also do one of the following:

    a) Accompany it with the complete corresponding machine-readable
    source code, which must be distributed under the terms of Sections 1
    and 2 above on a medium customarily used for software interchange; or,

    b) Accompany it with a written offer, valid for at least three
    years, to give any third party, for a charge no more than your cost
    of physically performing source distribution, a complete
    machine-readable copy of the corresponding source code, to be
    distributed under the terms of Sections 1 and 2 above on a medium
    customarily used for software interchange; or,

    c) Accompany it with the information you received as to the offer to
    distribute corresponding source code. (This alternative is allowed
    only for noncommercial distribution and only if you received the
    program in object code or executable form with such an offer, in
    accord with Subsection b above.)

The source code for a work means the preferred form of the work for
making modifications to it. For an executable work, complete source code
means all the source code for all modules it contains, plus any
associated interface definition files, plus the scripts used to control
compilation and installation of the executable. However, as a special
exception, the source code distributed need not include anything that is
normally distributed (in either source or binary form) with the major
components (compiler, kernel, and so on) of the operating system on
which the executable runs, unless that component itself accompanies the
executable.

If distribution of executable or object code is made by offering access
to copy from a designated place, then offering equivalent access to copy
the source code from the same place counts as distribution of the source
code, even though third parties are not compelled to copy the source
along with the object code.

4. You may not copy, modify, sublicense, or distribute the Program
except as expressly provided under this License. Any attempt otherwise
to copy, modify, sublicense or distribute the Program is void, and will
automatically terminate your rights under this License. However, parties
who have received copies, or rights, from you under this License will
not have their licenses terminated so long as such parties remain in
full compliance.

5. You are not required to accept this License, since you have not
signed it. However, nothing else grants you permission to modify or
distribute the Program or its derivative works. These actions are
prohibited by law if you do not accept this License. Therefore, by
modifying or distributing the Program (or any work based on the
Program), you indicate your acceptance of this License to do so, and all
its terms and conditions for copying, distributing or modifying the
Program or works based on it.

6. Each time you redistribute the Program (or any work based on the
Program), the recipient automatically receives a license from the
original licensor to copy, distribute or modify the Program subject to
these terms and conditions. You may not impose any further restrictions
on the recipients' exercise of the rights granted herein. You are not
responsible for enforcing compliance by third parties to this License.

7. If, as a consequence of a court judgment or allegation of patent
infringement or for any other reason (not limited to patent issues),
conditions are imposed on you (whether by court order, agreement or
otherwise) that contradict the conditions of this License, they do not
excuse you from the conditions of this License. If you cannot distribute
so as to satisfy simultaneously your obligations under this License and
any other pertinent obligations, then as a consequence you may not
distribute the Program at all. For example, if a patent license would
not permit royalty-free redistribution of the Program by all those who
receive copies directly or indirectly through you, then the only way you
could satisfy both it and this License would be to refrain entirely from
distribution of the Program.

If any portion of this section is held invalid or unenforceable under
any particular circumstance, the balance of the section is intended to
apply and the section as a whole is intended to apply in other
circumstances.

It is not the purpose of this section to induce you to infringe any
patents or other property right claims or to contest validity of any
such claims; this section has the sole purpose of protecting the
integrity of the free software distribution system, which is implemented
by public license practices. Many people have made generous
contributions to the wide range of software distributed through that
system in reliance on consistent application of that system; it is up to
the author/donor to decide if he or she is willing to distribute
software through any other system and a licensee cannot impose that choice.

This section is intended to make thoroughly clear what is believed to be
a consequence of the rest of this License.

8. If the distribution and/or use of the Program is restricted in
certain countries either by patents or by copyrighted interfaces, the
original copyright holder who places the Program under this License may
add an explicit geographical distribution limitation excluding those
countries, so that distribution is permitted only in or among countries
not thus excluded. In such case, this License incorporates the
limitation as if written in the body of this License.

9. The Free Software Foundation may publish revised and/or new
versions of the General Public License from time to time. Such new
versions will be similar in spirit to the present version, but may
differ in detail to address new problems or concerns.

Each version is given a distinguishing version number. If the Program
specifies a version number of this License which applies to it and "any
later version", you have the option of following the terms and
conditions either of that version or of any later version published by
the Free Software Foundation. If the Program does not specify a version
number of this License, you may choose any version ever published by the
Free Software Foundation.

10. If you wish to incorporate parts of the Program into other free
programs whose distribution conditions are different, write to the
author to ask for permission. For software which is copyrighted by the
Free Software Foundation, write to the Free Software Foundation; we
sometimes make exceptions for this. Our decision will be guided by the
two goals of preserving the free status of all derivatives of our free
software and of promoting the sharing and reuse of software generally.

NO WARRANTY

11. BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO
WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW.
EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND,
EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE
ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL
NECESSARY SERVICING, REPAIR OR CORRECTION.

12. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN
WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY
AND/OR REDISTRIBUTE THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR
DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL
DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM
(INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING RENDERED
INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF
THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR
OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

END OF TERMS AND CONDITIONS

How to Apply These Terms to Your New Programs

If you develop a new program, and you want it to be of the greatest
possible use to the public, the best way to achieve this is to make it
free software which everyone can redistribute and change under these terms.

To do so, attach the following notices to the program. It is safest to
attach them to the start of each source file to most effectively convey
the exclusion of warranty; and each file should have at least the
"copyright" line and a pointer to where the full notice is found.

    One line to give the program's name and a brief idea of what it does.
    Copyright (C) <year> <name of author>

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1335 USA

Also add information on how to contact you by electronic and paper mail.

If the program is interactive, make it output a short notice like this
when it starts in an interactive mode:

    Gnomovision version 69, Copyright (C) year name of author
    Gnomovision comes with ABSOLUTELY NO WARRANTY; for details type
    `show w'. This is free software, and you are welcome to redistribute
    it under certain conditions; type `show c' for details.

The hypothetical commands `show w' and `show c' should show the
appropriate parts of the General Public License. Of course, the commands
you use may be called something other than `show w' and `show c'; they
could even be mouse-clicks or menu items--whatever suits your program.

You should also get your employer (if you work as a programmer) or your
school, if any, to sign a "copyright disclaimer" for the program, if
necessary. Here is a sample; alter the names:

    Yoyodyne, Inc., hereby disclaims all copyright interest in the
    program `Gnomovision' (which makes passes at compilers) written by
    James Hacker.

    signature of Ty Coon, 1 April 1989
    Ty Coon, President of Vice

This General Public License does not permit incorporating your program
into proprietary programs. If your program is a subroutine library, you
may consider it more useful to permit linking proprietary applications
with the library. If this is what you want to do, use the GNU Library
General Public License instead of this License.
Certain source files distributed by Oracle America, Inc. and/or its
affiliates are subject to the following clarification and special
exception to the GPLv2, based on the GNU Project exception for its
Classpath libraries, known as the GNU Classpath Exception, but only
where Oracle has expressly included in the particular source file's
header the words "Oracle designates this particular file as subject to
the "Classpath" exception as provided by Oracle in the LICENSE file
that accompanied this code."

You should also note that Oracle includes multiple, independent
programs in this software package. Some of those programs are provided
under licenses deemed incompatible with the GPLv2 by the Free Software
Foundation and others.  For example, the package includes programs
licensed under the Apache License, Version 2.0.  Such programs are
licensed to you under their original licenses.

Oracle facilitates your further distribution of this package by adding
the Classpath Exception to the necessary parts of its GPLv2 code, which
permits you to use that code in combination with other independent
modules not licensed under the GPLv2.  However, note that this would
not permit you to commingle code under an incompatible license with
Oracle's GPLv2 licensed code by, for example, cutting and pasting such
code into a file also containing Oracle's GPLv2 licensed code and then
distributing the result.  Additionally, if you were to remove the
Classpath Exception from any of the files to which it applies and
distribute the result, you would likely be required to license some or
all of the other code in that distribution under the GPLv2 as well, and
since the GPLv2 is incompatible with the license terms of some items
included in the distribution by Oracle, removing the Classpath
Exception could therefore effectively compromise your ability to
further distribute the package.

Proceed with caution and we recommend that you obtain the advice of a
lawyer skilled in open source matters before removing the Classpath
Exception or making modifications to this package which may
subsequently be redistributed and/or involve the use of third party
software.

CLASSPATH EXCEPTION
Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License version 2 cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from or
based on this library.  If you modify this library, you may extend this
exception to your version of the library, but you are not obligated to
do so.  If you do not wish to do so, delete this exception statement
from your version.


-------------------------------
For compatibility with Java > 8
com.sun.activation:javax.activation

License: CDDL 1.1, GPL2 w/ CPE

COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.1

1. Definitions.

    1.1. "Contributor" means each individual or entity that creates or
    contributes to the creation of Modifications.

    1.2. "Contributor Version" means the combination of the Original
    Software, prior Modifications used by a Contributor (if any), and
    the Modifications made by that particular Contributor.

    1.3. "Covered Software" means (a) the Original Software, or (b)
    Modifications, or (c) the combination of files containing Original
    Software with files containing Modifications, in each case including
    portions thereof.

    1.4. "Executable" means the Covered Software in any form other than
    Source Code.

    1.5. "Initial Developer" means the individual or entity that first
    makes Original Software available under this License.

    1.6. "Larger Work" means a work which combines Covered Software or
    portions thereof with code not governed by the terms of this License.

    1.7. "License" means this document.

    1.8. "Licensable" means having the right to grant, to the maximum
    extent possible, whether at the time of the initial grant or
    subsequently acquired, any and all of the rights conveyed herein.

    1.9. "Modifications" means the Source Code and Executable form of
    any of the following:

    A. Any file that results from an addition to, deletion from or
    modification of the contents of a file containing Original Software
    or previous Modifications;

    B. Any new file that contains any part of the Original Software or
    previous Modification; or

    C. Any new file that is contributed or otherwise made available
    under the terms of this License.

    1.10. "Original Software" means the Source Code and Executable form
    of computer software code that is originally released under this
    License.

    1.11. "Patent Claims" means any patent claim(s), now owned or
    hereafter acquired, including without limitation, method, process,
    and apparatus claims, in any patent Licensable by grantor.

    1.12. "Source Code" means (a) the common form of computer software
    code in which modifications are made and (b) associated
    documentation included in or with such code.

    1.13. "You" (or "Your") means an individual or a legal entity
    exercising rights under, and complying with all of the terms of,
    this License. For legal entities, "You" includes any entity which
    controls, is controlled by, or is under common control with You. For
    purposes of this definition, "control" means (a) the power, direct
    or indirect, to cause the direction or management of such entity,
    whether by contract or otherwise, or (b) ownership of more than
    fifty percent (50%) of the outstanding shares or beneficial
    ownership of such entity.

2. License Grants.

    2.1. The Initial Developer Grant.

    Conditioned upon Your compliance with Section 3.1 below and subject
    to third party intellectual property claims, the Initial Developer
    hereby grants You a world-wide, royalty-free, non-exclusive license:

    (a) under intellectual property rights (other than patent or
    trademark) Licensable by Initial Developer, to use, reproduce,
    modify, display, perform, sublicense and distribute the Original
    Software (or portions thereof), with or without Modifications,
    and/or as part of a Larger Work; and

    (b) under Patent Claims infringed by the making, using or selling of
    Original Software, to make, have made, use, practice, sell, and
    offer for sale, and/or otherwise dispose of the Original Software
    (or portions thereof).

    (c) The licenses granted in Sections 2.1(a) and (b) are effective on
    the date Initial Developer first distributes or otherwise makes the
    Original Software available to a third party under the terms of this
    License.

    (d) Notwithstanding Section 2.1(b) above, no patent license is
    granted: (1) for code that You delete from the Original Software, or
    (2) for infringements caused by: (i) the modification of the
    Original Software, or (ii) the combination of the Original Software
    with other software or devices.

    2.2. Contributor Grant.

    Conditioned upon Your compliance with Section 3.1 below and subject
    to third party intellectual property claims, each Contributor hereby
    grants You a world-wide, royalty-free, non-exclusive license:

    (a) under intellectual property rights (other than patent or
    trademark) Licensable by Contributor to use, reproduce, modify,
    display, perform, sublicense and distribute the Modifications
    created by such Contributor (or portions thereof), either on an
    unmodified basis, with other Modifications, as Covered Software
    and/or as part of a Larger Work; and

    (b) under Patent Claims infringed by the making, using, or selling
    of Modifications made by that Contributor either alone and/or in
    combination with its Contributor Version (or portions of such
    combination), to make, use, sell, offer for sale, have made, and/or
    otherwise dispose of: (1) Modifications made by that Contributor (or
    portions thereof); and (2) the combination of Modifications made by
    that Contributor with its Contributor Version (or portions of such
    combination).

    (c) The licenses granted in Sections 2.2(a) and 2.2(b) are effective
    on the date Contributor first distributes or otherwise makes the
    Modifications available to a third party.

    (d) Notwithstanding Section 2.2(b) above, no patent license is
    granted: (1) for any code that Contributor has deleted from the
    Contributor Version; (2) for infringements caused by: (i) third
    party modifications of Contributor Version, or (ii) the combination
    of Modifications made by that Contributor with other software
    (except as part of the Contributor Version) or other devices; or (3)
    under Patent Claims infringed by Covered Software in the absence of
    Modifications made by that Contributor.

3. Distribution Obligations.

    3.1. Availability of Source Code.

    Any Covered Software that You distribute or otherwise make available
    in Executable form must also be made available in Source Code form
    and that Source Code form must be distributed only under the terms
    of this License. You must include a copy of this License with every
    copy of the Source Code form of the Covered Software You distribute
    or otherwise make available. You must inform recipients of any such
    Covered Software in Executable form as to how they can obtain such
    Covered Software in Source Code form in a reasonable manner on or
    through a medium customarily used for software exchange.

    3.2. Modifications.

    The Modifications that You create or to which You contribute are
    governed by the terms of this License. You represent that You
    believe Your Modifications are Your original creation(s) and/or You
    have sufficient rights to grant the rights conveyed by this License.

    3.3. Required Notices.

    You must include a notice in each of Your Modifications that
    identifies You as the Contributor of the Modification. You may not
    remove or alter any copyright, patent or trademark notices contained
    within the Covered Software, or any notices of licensing or any
    descriptive text giving attribution to any Contributor or the
    Initial Developer.

    3.4. Application of Additional Terms.

    You may not offer or impose any terms on any Covered Software in
    Source Code form that alters or restricts the applicable version of
    this License or the recipients' rights hereunder. You may choose to
    offer, and to charge a fee for, warranty, support, indemnity or
    liability obligations to one or more recipients of Covered Software.
    However, you may do so only on Your own behalf, and not on behalf of
    the Initial Developer or any Contributor. You must make it
    absolutely clear that any such warranty, support, indemnity or
    liability obligation is offered by You alone, and You hereby agree
    to indemnify the Initial Developer and every Contributor for any
    liability incurred by the Initial Developer or such Contributor as a
    result of warranty, support, indemnity or liability terms You offer.

    3.5. Distribution of Executable Versions.

    You may distribute the Executable form of the Covered Software under
    the terms of this License or under the terms of a license of Your
    choice, which may contain terms different from this License,
    provided that You are in compliance with the terms of this License
    and that the license for the Executable form does not attempt to
    limit or alter the recipient's rights in the Source Code form from
    the rights set forth in this License. If You distribute the Covered
    Software in Executable form under a different license, You must make
    it absolutely clear that any terms which differ from this License
    are offered by You alone, not by the Initial Developer or
    Contributor. You hereby agree to indemnify the Initial Developer and
    every Contributor for any liability incurred by the Initial
    Developer or such Contributor as a result of any such terms You offer.

    3.6. Larger Works.

    You may create a Larger Work by combining Covered Software with
    other code not governed by the terms of this License and distribute
    the Larger Work as a single product. In such a case, You must make
    sure the requirements of this License are fulfilled for the Covered
    Software.

4. Versions of the License.

    4.1. New Versions.

    Oracle is the initial license steward and may publish revised and/or
    new versions of this License from time to time. Each version will be
    given a distinguishing version number. Except as provided in Section
    4.3, no one other than the license steward has the right to modify
    this License.

    4.2. Effect of New Versions.

    You may always continue to use, distribute or otherwise make the
    Covered Software available under the terms of the version of the
    License under which You originally received the Covered Software. If
    the Initial Developer includes a notice in the Original Software
    prohibiting it from being distributed or otherwise made available
    under any subsequent version of the License, You must distribute and
    make the Covered Software available under the terms of the version
    of the License under which You originally received the Covered
    Software. Otherwise, You may also choose to use, distribute or
    otherwise make the Covered Software available under the terms of any
    subsequent version of the License published by the license steward.

    4.3. Modified Versions.

    When You are an Initial Developer and You want to create a new
    license for Your Original Software, You may create and use a
    modified version of this License if You: (a) rename the license and
    remove any references to the name of the license steward (except to
    note that the license differs from this License); and (b) otherwise
    make it clear that the license contains terms which differ from this
    License.

5. DISCLAIMER OF WARRANTY.

    COVERED SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "AS IS" BASIS,
    WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED,
    INCLUDING, WITHOUT LIMITATION, WARRANTIES THAT THE COVERED SOFTWARE
    IS FREE OF DEFECTS, MERCHANTABLE, FIT FOR A PARTICULAR PURPOSE OR
    NON-INFRINGING. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF
    THE COVERED SOFTWARE IS WITH YOU. SHOULD ANY COVERED SOFTWARE PROVE
    DEFECTIVE IN ANY RESPECT, YOU (NOT THE INITIAL DEVELOPER OR ANY
    OTHER CONTRIBUTOR) ASSUME THE COST OF ANY NECESSARY SERVICING,
    REPAIR OR CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN
    ESSENTIAL PART OF THIS LICENSE. NO USE OF ANY COVERED SOFTWARE IS
    AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.

6. TERMINATION.

    6.1. This License and the rights granted hereunder will terminate
    automatically if You fail to comply with terms herein and fail to
    cure such breach within 30 days of becoming aware of the breach.
    Provisions which, by their nature, must remain in effect beyond the
    termination of this License shall survive.

    6.2. If You assert a patent infringement claim (excluding
    declaratory judgment actions) against Initial Developer or a
    Contributor (the Initial Developer or Contributor against whom You
    assert such claim is referred to as "Participant") alleging that the
    Participant Software (meaning the Contributor Version where the
    Participant is a Contributor or the Original Software where the
    Participant is the Initial Developer) directly or indirectly
    infringes any patent, then any and all rights granted directly or
    indirectly to You by such Participant, the Initial Developer (if the
    Initial Developer is not the Participant) and all Contributors under
    Sections 2.1 and/or 2.2 of this License shall, upon 60 days notice
    from Participant terminate prospectively and automatically at the
    expiration of such 60 day notice period, unless if within such 60
    day period You withdraw Your claim with respect to the Participant
    Software against such Participant either unilaterally or pursuant to
    a written agreement with Participant.

    6.3. If You assert a patent infringement claim against Participant
    alleging that the Participant Software directly or indirectly
    infringes any patent where such claim is resolved (such as by
    license or settlement) prior to the initiation of patent
    infringement litigation, then the reasonable value of the licenses
    granted by such Participant under Sections 2.1 or 2.2 shall be taken
    into account in determining the amount or value of any payment or
    license.

    6.4. In the event of termination under Sections 6.1 or 6.2 above,
    all end user licenses that have been validly granted by You or any
    distributor hereunder prior to termination (excluding licenses
    granted to You by any distributor) shall survive termination.

7. LIMITATION OF LIABILITY.

    UNDER NO CIRCUMSTANCES AND UNDER NO LEGAL THEORY, WHETHER TORT
    (INCLUDING NEGLIGENCE), CONTRACT, OR OTHERWISE, SHALL YOU, THE
    INITIAL DEVELOPER, ANY OTHER CONTRIBUTOR, OR ANY DISTRIBUTOR OF
    COVERED SOFTWARE, OR ANY SUPPLIER OF ANY OF SUCH PARTIES, BE LIABLE
    TO ANY PERSON FOR ANY INDIRECT, SPECIAL, INCIDENTAL, OR
    CONSEQUENTIAL DAMAGES OF ANY CHARACTER INCLUDING, WITHOUT
    LIMITATION, DAMAGES FOR LOSS OF GOODWILL, WORK STOPPAGE, COMPUTER
    FAILURE OR MALFUNCTION, OR ANY AND ALL OTHER COMMERCIAL DAMAGES OR
    LOSSES, EVEN IF SUCH PARTY SHALL HAVE BEEN INFORMED OF THE
    POSSIBILITY OF SUCH DAMAGES. THIS LIMITATION OF LIABILITY SHALL NOT
    APPLY TO LIABILITY FOR DEATH OR PERSONAL INJURY RESULTING FROM SUCH
    PARTY'S NEGLIGENCE TO THE EXTENT APPLICABLE LAW PROHIBITS SUCH
    LIMITATION. SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OR
    LIMITATION OF INCIDENTAL OR CONSEQUENTIAL DAMAGES, SO THIS EXCLUSION
    AND LIMITATION MAY NOT APPLY TO YOU.

8. U.S. GOVERNMENT END USERS.

    The Covered Software is a "commercial item," as that term is defined
    in 48 C.F.R. 2.101 (Oct. 1995), consisting of "commercial computer
    software" (as that term is defined at 48 C.F.R. §
    252.227-7014(a)(1)) and "commercial computer software documentation"
    as such terms are used in 48 C.F.R. 12.212 (Sept. 1995). Consistent
    with 48 C.F.R. 12.212 and 48 C.F.R. 227.7202-1 through 227.7202-4
    (June 1995), all U.S. Government End Users acquire Covered Software
    with only those rights set forth herein. This U.S. Government Rights
    clause is in lieu of, and supersedes, any other FAR, DFAR, or other
    clause or provision that addresses Government rights in computer
    software under this License.

9. MISCELLANEOUS.

    This License represents the complete agreement concerning subject
    matter hereof. If any provision of this License is held to be
    unenforceable, such provision shall be reformed only to the extent
    necessary to make it enforceable. This License shall be governed by
    the law of the jurisdiction specified in a notice contained within
    the Original Software (except to the extent applicable law, if any,
    provides otherwise), excluding such jurisdiction's conflict-of-law
    provisions. Any litigation relating to this License shall be subject
    to the jurisdiction of the courts located in the jurisdiction and
    venue specified in a notice contained within the Original Software,
    with the losing party responsible for costs, including, without
    limitation, court costs and reasonable attorneys' fees and expenses.
    The application of the United Nations Convention on Contracts for
    the International Sale of Goods is expressly excluded. Any law or
    regulation which provides that the language of a contract shall be
    construed against the drafter shall not apply to this License. You
    agree that You alone are responsible for compliance with the United
    States export administration regulations (and the export control
    laws and regulation of any other countries) when You use, distribute
    or otherwise make available any Covered Software.

10. RESPONSIBILITY FOR CLAIMS.

    As between Initial Developer and the Contributors, each party is
    responsible for claims and damages arising, directly or indirectly,
    out of its utilization of rights under this License and You agree to
    work with Initial Developer and Contributors to distribute such
    responsibility on an equitable basis. Nothing herein is intended or
    shall be deemed to constitute any admission of liability.

------------------------------------------------------------------------

NOTICE PURSUANT TO SECTION 9 OF THE COMMON DEVELOPMENT AND DISTRIBUTION
LICENSE (CDDL)

The code released under the CDDL shall be governed by the laws of the
State of California (excluding conflict-of-law provisions). Any
litigation relating to this License shall be subject to the jurisdiction
of the Federal Courts of the Northern District of California and the
state courts of the State of California, with venue lying in Santa Clara
County, California.



  The GNU General Public License (GPL) Version 2, June 1991

Copyright (C) 1989, 1991 Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor
Boston, MA 02110-1335
USA

Everyone is permitted to copy and distribute verbatim copies
of this license document, but changing it is not allowed.

Preamble

The licenses for most software are designed to take away your freedom to
share and change it. By contrast, the GNU General Public License is
intended to guarantee your freedom to share and change free software--to
make sure the software is free for all its users. This General Public
License applies to most of the Free Software Foundation's software and
to any other program whose authors commit to using it. (Some other Free
Software Foundation software is covered by the GNU Library General
Public License instead.) You can apply it to your programs, too.

When we speak of free software, we are referring to freedom, not price.
Our General Public Licenses are designed to make sure that you have the
freedom to distribute copies of free software (and charge for this
service if you wish), that you receive source code or can get it if you
want it, that you can change the software or use pieces of it in new
free programs; and that you know you can do these things.

To protect your rights, we need to make restrictions that forbid anyone
to deny you these rights or to ask you to surrender the rights. These
restrictions translate to certain responsibilities for you if you
distribute copies of the software, or if you modify it.

For example, if you distribute copies of such a program, whether gratis
or for a fee, you must give the recipients all the rights that you have.
You must make sure that they, too, receive or can get the source code.
And you must show them these terms so they know their rights.

We protect your rights with two steps: (1) copyright the software, and
(2) offer you this license which gives you legal permission to copy,
distribute and/or modify the software.

Also, for each author's protection and ours, we want to make certain
that everyone understands that there is no warranty for this free
software. If the software is modified by someone else and passed on, we
want its recipients to know that what they have is not the original, so
that any problems introduced by others will not reflect on the original
authors' reputations.

Finally, any free program is threatened constantly by software patents.
We wish to avoid the danger that redistributors of a free program will
individually obtain patent licenses, in effect making the program
proprietary. To prevent this, we have made it clear that any patent must
be licensed for everyone's free use or not licensed at all.

The precise terms and conditions for copying, distribution and
modification follow.

TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

0. This License applies to any program or other work which contains a
notice placed by the copyright holder saying it may be distributed under
the terms of this General Public License. The "Program", below, refers
to any such program or work, and a "work based on the Program" means
either the Program or any derivative work under copyright law: that is
to say, a work containing the Program or a portion of it, either
verbatim or with modifications and/or translated into another language.
(Hereinafter, translation is included without limitation in the term
"modification".) Each licensee is addressed as "you".

Activities other than copying, distribution and modification are not
covered by this License; they are outside its scope. The act of running
the Program is not restricted, and the output from the Program is
covered only if its contents constitute a work based on the Program
(independent of having been made by running the Program). Whether that
is true depends on what the Program does.

1. You may copy and distribute verbatim copies of the Program's source
code as you receive it, in any medium, provided that you conspicuously
and appropriately publish on each copy an appropriate copyright notice
and disclaimer of warranty; keep intact all the notices that refer to
this License and to the absence of any warranty; and give any other
recipients of the Program a copy of this License along with the Program.

You may charge a fee for the physical act of transferring a copy, and
you may at your option offer warranty protection in exchange for a fee.

2. You may modify your copy or copies of the Program or any portion of
it, thus forming a work based on the Program, and copy and distribute
such modifications or work under the terms of Section 1 above, provided
that you also meet all of these conditions:

    a) You must cause the modified files to carry prominent notices
    stating that you changed the files and the date of any change.

    b) You must cause any work that you distribute or publish, that in
    whole or in part contains or is derived from the Program or any part
    thereof, to be licensed as a whole at no charge to all third parties
    under the terms of this License.

    c) If the modified program normally reads commands interactively
    when run, you must cause it, when started running for such
    interactive use in the most ordinary way, to print or display an
    announcement including an appropriate copyright notice and a notice
    that there is no warranty (or else, saying that you provide a
    warranty) and that users may redistribute the program under these
    conditions, and telling the user how to view a copy of this License.
    (Exception: if the Program itself is interactive but does not
    normally print such an announcement, your work based on the Program
    is not required to print an announcement.)

These requirements apply to the modified work as a whole. If
identifiable sections of that work are not derived from the Program, and
can be reasonably considered independent and separate works in
themselves, then this License, and its terms, do not apply to those
sections when you distribute them as separate works. But when you
distribute the same sections as part of a whole which is a work based on
the Program, the distribution of the whole must be on the terms of this
License, whose permissions for other licensees extend to the entire
whole, and thus to each and every part regardless of who wrote it.

Thus, it is not the intent of this section to claim rights or contest
your rights to work written entirely by you; rather, the intent is to
exercise the right to control the distribution of derivative or
collective works based on the Program.

In addition, mere aggregation of another work not based on the Program
with the Program (or with a work based on the Program) on a volume of a
storage or distribution medium does not bring the other work under the
scope of this License.

3. You may copy and distribute the Program (or a work based on it,
under Section 2) in object code or executable form under the terms of
Sections 1 and 2 above provided that you also do one of the following:

    a) Accompany it with the complete corresponding machine-readable
    source code, which must be distributed under the terms of Sections 1
    and 2 above on a medium customarily used for software interchange; or,

    b) Accompany it with a written offer, valid for at least three
    years, to give any third party, for a charge no more than your cost
    of physically performing source distribution, a complete
    machine-readable copy of the corresponding source code, to be
    distributed under the terms of Sections 1 and 2 above on a medium
    customarily used for software interchange; or,

    c) Accompany it with the information you received as to the offer to
    distribute corresponding source code. (This alternative is allowed
    only for noncommercial distribution and only if you received the
    program in object code or executable form with such an offer, in
    accord with Subsection b above.)

The source code for a work means the preferred form of the work for
making modifications to it. For an executable work, complete source code
means all the source code for all modules it contains, plus any
associated interface definition files, plus the scripts used to control
compilation and installation of the executable. However, as a special
exception, the source code distributed need not include anything that is
normally distributed (in either source or binary form) with the major
components (compiler, kernel, and so on) of the operating system on
which the executable runs, unless that component itself accompanies the
executable.

If distribution of executable or object code is made by offering access
to copy from a designated place, then offering equivalent access to copy
the source code from the same place counts as distribution of the source
code, even though third parties are not compelled to copy the source
along with the object code.

4. You may not copy, modify, sublicense, or distribute the Program
except as expressly provided under this License. Any attempt otherwise
to copy, modify, sublicense or distribute the Program is void, and will
automatically terminate your rights under this License. However, parties
who have received copies, or rights, from you under this License will
not have their licenses terminated so long as such parties remain in
full compliance.

5. You are not required to accept this License, since you have not
signed it. However, nothing else grants you permission to modify or
distribute the Program or its derivative works. These actions are
prohibited by law if you do not accept this License. Therefore, by
modifying or distributing the Program (or any work based on the
Program), you indicate your acceptance of this License to do so, and all
its terms and conditions for copying, distributing or modifying the
Program or works based on it.

6. Each time you redistribute the Program (or any work based on the
Program), the recipient automatically receives a license from the
original licensor to copy, distribute or modify the Program subject to
these terms and conditions. You may not impose any further restrictions
on the recipients' exercise of the rights granted herein. You are not
responsible for enforcing compliance by third parties to this License.

7. If, as a consequence of a court judgment or allegation of patent
infringement or for any other reason (not limited to patent issues),
conditions are imposed on you (whether by court order, agreement or
otherwise) that contradict the conditions of this License, they do not
excuse you from the conditions of this License. If you cannot distribute
so as to satisfy simultaneously your obligations under this License and
any other pertinent obligations, then as a consequence you may not
distribute the Program at all. For example, if a patent license would
not permit royalty-free redistribution of the Program by all those who
receive copies directly or indirectly through you, then the only way you
could satisfy both it and this License would be to refrain entirely from
distribution of the Program.

If any portion of this section is held invalid or unenforceable under
any particular circumstance, the balance of the section is intended to
apply and the section as a whole is intended to apply in other
circumstances.

It is not the purpose of this section to induce you to infringe any
patents or other property right claims or to contest validity of any
such claims; this section has the sole purpose of protecting the
integrity of the free software distribution system, which is implemented
by public license practices. Many people have made generous
contributions to the wide range of software distributed through that
system in reliance on consistent application of that system; it is up to
the author/donor to decide if he or she is willing to distribute
software through any other system and a licensee cannot impose that choice.

This section is intended to make thoroughly clear what is believed to be
a consequence of the rest of this License.

8. If the distribution and/or use of the Program is restricted in
certain countries either by patents or by copyrighted interfaces, the
original copyright holder who places the Program under this License may
add an explicit geographical distribution limitation excluding those
countries, so that distribution is permitted only in or among countries
not thus excluded. In such case, this License incorporates the
limitation as if written in the body of this License.

9. The Free Software Foundation may publish revised and/or new
versions of the General Public License from time to time. Such new
versions will be similar in spirit to the present version, but may
differ in detail to address new problems or concerns.

Each version is given a distinguishing version number. If the Program
specifies a version number of this License which applies to it and "any
later version", you have the option of following the terms and
conditions either of that version or of any later version published by
the Free Software Foundation. If the Program does not specify a version
number of this License, you may choose any version ever published by the
Free Software Foundation.

10. If you wish to incorporate parts of the Program into other free
programs whose distribution conditions are different, write to the
author to ask for permission. For software which is copyrighted by the
Free Software Foundation, write to the Free Software Foundation; we
sometimes make exceptions for this. Our decision will be guided by the
two goals of preserving the free status of all derivatives of our free
software and of promoting the sharing and reuse of software generally.

NO WARRANTY

11. BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO
WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW.
EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND,
EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE
ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL
NECESSARY SERVICING, REPAIR OR CORRECTION.

12. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN
WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY
AND/OR REDISTRIBUTE THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR
DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL
DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM
(INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING RENDERED
INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF
THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR
OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

END OF TERMS AND CONDITIONS

How to Apply These Terms to Your New Programs

If you develop a new program, and you want it to be of the greatest
possible use to the public, the best way to achieve this is to make it
free software which everyone can redistribute and change under these terms.

To do so, attach the following notices to the program. It is safest to
attach them to the start of each source file to most effectively convey
the exclusion of warranty; and each file should have at least the
"copyright" line and a pointer to where the full notice is found.

    One line to give the program's name and a brief idea of what it does.
    Copyright (C) <year> <name of author>

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1335 USA

Also add information on how to contact you by electronic and paper mail.

If the program is interactive, make it output a short notice like this
when it starts in an interactive mode:

    Gnomovision version 69, Copyright (C) year name of author
    Gnomovision comes with ABSOLUTELY NO WARRANTY; for details type
    `show w'. This is free software, and you are welcome to redistribute
    it under certain conditions; type `show c' for details.

The hypothetical commands `show w' and `show c' should show the
appropriate parts of the General Public License. Of course, the commands
you use may be called something other than `show w' and `show c'; they
could even be mouse-clicks or menu items--whatever suits your program.

You should also get your employer (if you work as a programmer) or your
school, if any, to sign a "copyright disclaimer" for the program, if
necessary. Here is a sample; alter the names:

    Yoyodyne, Inc., hereby disclaims all copyright interest in the
    program `Gnomovision' (which makes passes at compilers) written by
    James Hacker.

    signature of Ty Coon, 1 April 1989
    Ty Coon, President of Vice

This General Public License does not permit incorporating your program
into proprietary programs. If your program is a subroutine library, you
may consider it more useful to permit linking proprietary applications
with the library. If this is what you want to do, use the GNU Library
General Public License instead of this License.

#

Certain source files distributed by Oracle America, Inc. and/or its
affiliates are subject to the following clarification and special
exception to the GPLv2, based on the GNU Project exception for its
Classpath libraries, known as the GNU Classpath Exception, but only
where Oracle has expressly included in the particular source file's
header the words "Oracle designates this particular file as subject to
the "Classpath" exception as provided by Oracle in the LICENSE file
that accompanied this code."

You should also note that Oracle includes multiple, independent
programs in this software package. Some of those programs are provided
under licenses deemed incompatible with the GPLv2 by the Free Software
Foundation and others.  For example, the package includes programs
licensed under the Apache License, Version 2.0.  Such programs are
licensed to you under their original licenses.

Oracle facilitates your further distribution of this package by adding
the Classpath Exception to the necessary parts of its GPLv2 code, which
permits you to use that code in combination with other independent
modules not licensed under the GPLv2.  However, note that this would
not permit you to commingle code under an incompatible license with
Oracle's GPLv2 licensed code by, for example, cutting and pasting such
code into a file also containing Oracle's GPLv2 licensed code and then
distributing the result.  Additionally, if you were to remove the
Classpath Exception from any of the files to which it applies and
distribute the result, you would likely be required to license some or
all of the other code in that distribution under the GPLv2 as well, and
since the GPLv2 is incompatible with the license terms of some items
included in the distribution by Oracle, removing the Classpath
Exception could therefore effectively compromise your ability to
further distribute the package.

Proceed with caution and we recommend that you obtain the advice of a
lawyer skilled in open source matters before removing the Classpath
Exception or making modifications to this package which may
subsequently be redistributed and/or involve the use of third party
software.

CLASSPATH EXCEPTION
Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License version 2 cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from or
based on this library.  If you modify this library, you may extend this
exception to your version of the library, but you are not obligated to
do so.  If you do not wish to do so, delete this exception statement
from your version.

--------------------
The Fakeload library

MIT License

Copyright (c) 2017 Marten Sigwart

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation files
(the "Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

### Bootstrap (org.webjars.bootstrap)

```
Copyright (c) 2011-2024 The Bootstrap Authors
Copyright (c) 2011-2024 Twitter, Inc.
```

### Bouncy Castle (org.bouncycastle.*)

```
Copyright (c) 2000 - 2024 The Legion of the Bouncy Castle Inc. (https://www.bouncycastle.org)
```

### codemirror

```
Copyright (c) 2007-2024 by Marijn Haverbeke and others
```

### Dnsjava (dnsjava.*)

```
Copyright (c) 1998-2019, Brian Wellington
Copyright (c) 2005 VeriSign. All rights reserved.
Copyright (c) 2019-2023, dnsjava authors
All rights reserved.
```

### file-saver

```
Copyright (c) 2011 Eli Grey
```

### Flyway DB

```
Flyway
Copyright 2010-2024 Redgate Software Ltd

This product includes software developed at
Redgate Software Ltd (https://red-gate.com/).
```

### fontawesome

```
Copyright (c) Font Awesome
```

### Jackson (com.fasterxml.jackson.*)

```
# Jackson JSON processor

Jackson is a high-performance, Free/Open Source JSON processing library.
It was originally written by Tatu Saloranta (tatu.saloranta@iki.fi), and has
been in development since 2007.
It is currently developed by a community of developers.

## Copyright

Copyright 2007-, Tatu Saloranta (tatu.saloranta@iki.fi)

## Licensing

Jackson 2.x core and extension components are licensed under Apache License 2.0
To find the details that apply to this artifact see the accompanying LICENSE file.

## Credits

A list of contributors may be found from CREDITS(-2.x) file, which is included
in some artifacts (usually source distributions); but is always available
from the source code management (SCM) system project uses.

## FastDoubleParser

jackson-core bundles a shaded copy of FastDoubleParser <https://github.com/wrandelshofer/FastDoubleParser>.
That code is available under an MIT license <https://github.com/wrandelshofer/FastDoubleParser/blob/main/LICENSE>
under the following copyright.

Copyright © 2023 Werner Randelshofer, Switzerland. MIT License.

See FastDoubleParser-NOTICE for details of other source code included in FastDoubleParser
and the licenses and copyrights that apply to that code.
```

### Jakarta Activation API (jakarta.activation.*)

```
# Notices for Jakarta Activation

This content is produced and maintained by the Jakarta Activation project.

* Project home: https://projects.eclipse.org/projects/ee4j.jaf

## Trademarks

Jakarta Activation is a trademark of the Eclipse Foundation.

## Copyright

All content is the property of the respective authors or their employers. For
more information regarding authorship of content, please consult the listed
source code repository logs.

## Declared Project Licenses

This program and the accompanying materials are made available under the terms
of the Eclipse Public License v. 2.0 which is available at
https://www.eclipse.org/legal/epl-2.0, or the Eclipse Distribution License v1.0
which is available at https://www.eclipse.org/org/documents/edl-v10.php. This
Source Code may also be made available under the following Secondary Licenses
when the conditions for such availability set forth in the Eclipse Public
License v. 2.0 are satisfied: (secondary) GPL-2.0 with Classpath-exception-2.0
which is available at https://openjdk.java.net/legal/gplv2+ce.html.

SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause OR GPL-2.0-only with
Classpath-exception-2.0

## Source Code

The project maintains the following source code repositories:

* https://github.com/jakartaee/jaf-api
* https://github.com/jakartaee/jaf-tck

## Third-party Content

This project leverages the following third party content.

Apache Ant (1.9.6)

* License: Apache License, 2.0, W3C License, Public Domain

Apache Ant (1.9.6)

* License: Apache License, 2.0, W3C License, Public Domain

Apache commons-lang (3.5)

* License: Apache-2.0

font-awesome (4.7.0)

* License: OFL-1.1 AND MIT

jsoup (1.10.2)

* License: MIT

JTHarness (5.0)

* License: (GPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0)
* Project: https://wiki.openjdk.java.net/display/CodeTools/JT+Harness
* Source: http://hg.openjdk.java.net/code-tools/jtharness/

JUnit (4.12)

* License: Eclipse Public License

normalize.css (3.0.2)

* License: MIT
* Project: http://necolas.github.io/normalize.css/
* Source: http://necolas.github.io/normalize.css/

SigTest (4.0)

* License: GPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
* Project: https://wiki.openjdk.java.net/display/CodeTools/sigtest
* Source: http://hg.openjdk.java.net/code-tools/sigtest/file/c57f97e2ac2f

## Cryptography

Content may contain encryption software. The country in which you are currently
may have restrictions on the import, possession, and use, and/or re-export to
another country, of encryption software. BEFORE using any encryption software,
please check the country's laws, regulations and policies concerning the import,
possession, or use, and re-export of encryption software, to see if this is
permitted.
```

### Jakarta mail (com.sun.mail.*)

```
# Notices for Jakarta Mail

This content is produced and maintained by the Jakarta Mail project.

* Project home: https://projects.eclipse.org/projects/ee4j.mail

## Trademarks

Jakarta Mail is a trademark of the Eclipse Foundation.

## Copyright

All content is the property of the respective authors or their employers. For
more information regarding authorship of content, please consult the listed
source code repository logs.

## Declared Project Licenses

This program and the accompanying materials are made available under the terms
of the Eclipse Public License v. 2.0 which is available at
https://www.eclipse.org/legal/epl-2.0. This Source Code may also be made
available under the following Secondary Licenses when the conditions for such
availability set forth in the Eclipse Public License v. 2.0 are satisfied:
(secondary) GPL-2.0 with Classpath-exception-2.0 which is available at
https://openjdk.java.net/legal/gplv2+ce.html.

SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-only with Classpath-exception-2.0

## Source Code

The project maintains the following source code repositories:

* https://github.com/jakartaee/mail-api
* https://github.com/jakartaee/mail-tck
* https://github.com/jakartaee/mail-spec

## Third-party Content

This project leverages the following third party content.

Apache Ant (1.9.6)

* License: Apache License, 2.0, W3C License, Public Domain

commons-lang3 (3.5)

* License: Apache-2.0

font-awesome (4.7.0)

* License: OFL-1.1 AND MIT

jsoup (1.10.2)

* License: MIT

JTHarness (5.0)

* License: GPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0

normalize.css (3.0.2)

* License: MIT

SigTest (4.0)

* License: (GPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0)
* Project: https://wiki.openjdk.java.net/display/CodeTools/sigtest
* Source: http://hg.openjdk.java.net/code-tools/sigtest/file/c57f97e2ac2f

## Cryptography

Content may contain encryption software. The country in which you are currently
may have restrictions on the import, possession, and use, and/or re-export to
another country, of encryption software. BEFORE using any encryption software,
please check the country's laws, regulations and policies concerning the import,
possession, or use, and re-export of encryption software, to see if this is
permitted.
```

### Jakarta SOAP Implementation (com.sun.xml.messaging.saaj.*)

```
[//]: # " Copyright (c) 2018, 2023 Oracle and/or its affiliates. All rights reserved. "
[//]: # "  "
[//]: # " This program and the accompanying materials are made available under the "
[//]: # " terms of the Eclipse Distribution License v. 1.0, which is available at "
[//]: # " http://www.eclipse.org/org/documents/edl-v10.php. "
[//]: # "  "
[//]: # " SPDX-License-Identifier: BSD-3-Clause "

# Notices for Eclipse Metro

This content is produced and maintained by the Eclipse Metro project.

* Project home: https://projects.eclipse.org/projects/ee4j.metro

## Trademarks

Eclipse Metro is a trademark of the Eclipse Foundation.

## Copyright

All content is the property of the respective authors or their employers. For
more information regarding authorship of content, please consult the listed
source code repository logs.

## Declared Project Licenses

This program and the accompanying materials are made available under the terms
of the Eclipse Distribution License v1.0 which is available at
https://www.eclipse.org/org/documents/edl-v10.php.

SPDX-License-Identifier: BSD-3-Clause

## Source Code

The project maintains the following source code repositories:

* https://github.com/eclipse-ee4j/metro-xmlstreambuffer
* https://github.com/eclipse-ee4j/metro-policy
* https://github.com/eclipse-ee4j/metro-wsit
* https://github.com/eclipse-ee4j/metro-mimepull
* https://github.com/eclipse-ee4j/metro-ws-test-harness
* https://github.com/eclipse-ee4j/metro-package-rename-task
* https://github.com/eclipse-ee4j/metro-jax-ws
* https://github.com/eclipse-ee4j/metro-saaj
* https://github.com/eclipse-ee4j/metro-jwsdp-samples
* https://github.com/eclipse-ee4j/jax-rpc-ri
* https://github.com/eclipse-ee4j/jaxr-ri

## Third-party Content

This project leverages the following third party content.

addressing.xml Version: 2004/10 (n/a)

* License: W3C
* Project: https://www.w3.org/Submission/ws-addressing/
* Source: http://schemas.xmlsoap.org/ws/2004/08/addressing/

ant-launcher (1.10.2)

* License: Apache-2.0 AND SAX-PD AND W3C

Apache Ant (1.6)

* License: Apache-1.1
* Project: https://ant.apache.org/
* Source: https://repo1.maven.org/maven2/ant/ant/1.6/ant-1.6-sources.jar

Apache Ant (1.10.2)

* License: Apache-2.0 AND W3C AND LicenseRef-Public-Domain

Apache Commons Discovery (0.5.0)

* License: Apache License, 2.0

asm (7.1)

* License: BSD-3-Clause

ASM (7.2)

* License: BSD-3-Clause

com.fasterxml.woodstox:woodstox-core:6.2.4 (6.2.4)

* License: Apache-2.0

commons-codec:commons-codec (1.13)

* License: Apache-2.0 AND BSD-3-Clause AND LicenseRef-Public-Domain

commons-logging (1.1.2)

* License: Apache-2.0
* Project: https://commons.apache.org/proper/commons-logging/
* Source:
  http://central.maven.org/maven2/commons-logging/commons-logging/1.1.2/commons-logging-1.1.2-sources.jar

Freemarker (2.3.28)

* License: Apache-2.0 AND OFL-1.1 AND CC-BY-SA-3.0

grizzly-framework-http (1.0.40)

* License: (CDDL-1.1 OR GPL-2.0 WITH Classpath-exception-2.0) AND Apache-2.0
* Project: https://javaee.github.io/grizzly/httpserverframework.html
* Source: https://github.com/javaee/grizzly/tree/master/modules/http

jaxb2-basics (0.6.4)

* License: BSD-2-Clause
* Source: https://github.com/highsource/jaxb2-basics

jaxr-impl (1.0.8)

* License: CDDL-1.0
* Project: https://www.oracle.com/technetwork/java/jaxr-138137.html

JUnit (4.12)

* License: Eclipse Public License

junit (4.13.0)

* License: EPL-1.0

maven-core (3.5.2)

* License: Apache-2.0

maven-plugin-annotations (3.5.1)

* License: Apache-2.0
* Project:
  https://maven.apache.org/plugin-tools/maven-plugin-annotations/project-info.html
* Source:
  https://github.com/apache/maven-plugin-tools/tree/maven-plugin-tools-3.5.1/maven-plugin-annotations

maven-plugin-api (3.5.2)

* License: Apache-2.0
* Project: https://maven.apache.org/
* Source: https://github.com/apache/maven/tree/master/maven-plugin-api

maven-resolver-api (1.1.1)

* License: Apache-2.0

maven-resolver-util (1.1.1)

* License: Apache-2.0

maven-settings (3.5.2)

* License: Apache-2.0

maven/mavencentral/org.codehaus.cargo/cargo-core-uberjar/1.7.6 (1.7.6)

* License: Apache-2.0 AND MIT
* Project: https://codehaus-cargo.github.io/cargo/Home.html
* Source:
  https://github.com/codehaus-cargo/cargo/releases/tag/codehaus-cargo-1.7.3

maven/mavencentral/org.relaxng/jing/20181222 (n/a)

* License: BSD-3-Clause AND LicenseRef-ISO
* Project: http://www.thaiopensource.com/relaxng/jing.html
* Source:
  https://repo1.maven.org/maven2/org/relaxng/jing/20181222/jing-20181222-sources.jar

mex.xsd Version: 2004/09 (n/a)

* License: Oasis Style
* Project: https://www.w3.org/Submission/WS-MetadataExchange/#appendix-II
* Source: http://schemas.xmlsoap.org/ws/2004/09/mex/MetadataExchange.xsd

MQ JAXM-API (5.1)

* License: CDDL-1.1 OR GPL-2.0 WITH Classpath-exception-2.0
* Project: https://javaee.github.io/openmq/
* Source:
  https://repo1.maven.org/maven2/org/glassfish/mq/jaxm-api/5.1/jaxm-api-5.1-sources.jar

org.apache.santuario:xmlsec:2.1.5 (2.1.5)

* License: Pending

org.apache.santuario:xmlsec:3.0.0 (3.0.0)

* License: Apache-2.0 AND W3C

plexus-utils (3.1.0)

* License: Apache-2.0 AND Apache-1.1 AND BSD-3-Clause AND Public Domain AND
  Indiana University Extreme! Lab Software License V1.1.1 (Apache 1.1 style)

relaxng-datatype (1.0)

* License: New BSD license

slf4j-api (1.7.30)

* License: MIT

stax-utils Version: 20060502 (n/a)

* License: BSD-3-Clause

stax2-api (4.1)

* License: BSD-3-Clause AND MIT
* Project: https://github.com/FasterXML/stax2-api
* Source:
  http://central.maven.org/maven2/org/codehaus/woodstox/stax2-api/4.1/stax2-api-4.1-sources.jar

testng (6.14.2)

* License: Apache-2.0 AND MIT
* Project: https://testng.org/doc/index.html
* Source: https://github.com/cbeust/testng

woodstox-core-asl (4.4.1)

* License: Apache-2.0

woodstox-core-asl (5.1.0)

* License: Apache-2.0
* Project: https://github.com/FasterXML/woodstox
* Source: https://github.com/FasterXML/woodstox

ws-addr.wsd (1.0)

* License: W3C
* Project: https://www.w3.org/2005/08/addressing/
* Source: https://www.w3.org/2006/03/addressing/ws-addr.xsd

wsat.xsd Version: 2004/10 (n/a)

* License: Oasis Style
* Project: http://schemas.xmlsoap.org/ws/2004/10/wsat/
* Source: http://schemas.xmlsoap.org/ws/2004/10/wsat/wsat.xsd

wscoor.xsd (1.0)

* License: OASIS Style

wscoor.xsd (1.1)

* License: Oasis (Custom)
* Project: http://docs.oasis-open.org/ws-tx/wscoor/2006/06
* Source:
  http://docs.oasis-open.org/ws-tx/wscoor/2006/06/wstx-wscoor-1.1-schema-200701.xsd

wsrm Version: 2005/02 (n/a)

* License: Oasis (Custom)
* Project: http://schemas.xmlsoap.org/ws/2005/02/rm/
* Source:
  http://schemas.xmlsoap.org/ws/2005/02/rm/wsrm.xsd;%20http://schemas.xmlsoap.org/ws/2005/02/rm/wsrm-policy.xsd

wsrm.xsd (1.2)

* License: Oasis

wstx-wsat.xsd (1.1)

* License: Oasis (Custom)

xerces:xercesImpl:2.12.1 (2.12.1)

* License: Apache-2.0 AND W3C

xmlsec (1.5.8)

* License: Apache-2.0
* Project: http://santuario.apache.org/
* Source:
  https://repo1.maven.org/maven2/org/apache/santuario/xmlsec/1.5.8/xmlsec-1.5.8-sources.jar

## Cryptography

Content may contain encryption software. The country in which you are currently
may have restrictions on the import, possession, and use, and/or re-export to
another country, of encryption software. BEFORE using any encryption software,
please check the country's laws, regulations and policies concerning the import,
possession, or use, and re-export of encryption software, to see if this is
permitted.
```

### Jakarta XML Binding API (jakarta.xml.bind.*)

```
# Notices for Jakarta XML Binding

This content is produced and maintained by the Jakarta XML Binding project.

* Project home: https://projects.eclipse.org/projects/ee4j.jaxb

## Trademarks

Jakarta XML Binding™ is a trademark of the Eclipse Foundation.

## Copyright

All content is the property of the respective authors or their employers. For
more information regarding authorship of content, please consult the listed
source code repository logs.

## Declared Project Licenses

This program and the accompanying materials are made available under the terms
of the Eclipse Distribution License v1.0 which is available at
https://www.eclipse.org/org/documents/edl-v10.php.

SPDX-License-Identifier: BSD-3-Clause

## Source Code

The project maintains the following source code repositories:

* https://github.com/jakartaee/jaxb-api
* https://github.com/jakartaee/jaxb-tck

## Cryptography

Content may contain encryption software. The country in which you are currently
may have restrictions on the import, possession, and use, and/or re-export to
another country, of encryption software. BEFORE using any encryption software,
please check the country's laws, regulations and policies concerning the import,
possession, or use, and re-export of encryption software, to see if this is
permitted.
```

### Jakarta Web Services Metadata API (jakarta.jws.*)

```
[//]: # " Copyright (c) 2018, 2023 Oracle and/or its affiliates. All rights reserved. "
[//]: # "  "
[//]: # " This program and the accompanying materials are made available under the "
[//]: # " terms of the Eclipse Distribution License v. 1.0, which is available at "
[//]: # " http://www.eclipse.org/org/documents/edl-v10.php. "
[//]: # "  "
[//]: # " SPDX-License-Identifier: BSD-3-Clause "

# Notices for Jakarta XML Web Services

This content is produced and maintained by the Jakarta XML Web Services project.

* Project home: https://projects.eclipse.org/projects/ee4j.jaxws

## Trademarks

Jakarta XML Web Services is a trademark of the Eclipse Foundation.

## Copyright

All content is the property of the respective authors or their employers. For
more information regarding authorship of content, please consult the listed
source code repository logs.

## Declared Project Licenses

This program and the accompanying materials are made available under the terms
of the Eclipse Distribution License v1.0 which is available at
https://www.eclipse.org/org/documents/edl-v10.php.

SPDX-License-Identifier: BSD-3-Clause

## Source Code

The project maintains the following source code repositories:

* https://github.com/jakartaee/jws-api
* https://github.com/jakartaee/jax-ws-api
* https://github.com/jakartaee/saaj-api

## Third-party Content

This project leverages the following third party content.

None

## Cryptography

Content may contain encryption software. The country in which you are currently
may have restrictions on the import, possession, and use, and/or re-export to
another country, of encryption software. BEFORE using any encryption software,
please check the country's laws, regulations and policies concerning the import,
possession, or use, and re-export of encryption software, to see if this is
permitted.
```

### JAXB Core / JAXB Runtime (com.sun.xml.bind.*)

```
[//]: # " Copyright (c) 2018, 2024 Oracle and/or its affiliates. All rights reserved. "
[//]: # "  "
[//]: # " This program and the accompanying materials are made available under the "
[//]: # " terms of the Eclipse Distribution License v. 1.0, which is available at "
[//]: # " http://www.eclipse.org/org/documents/edl-v10.php. "
[//]: # "  "
[//]: # " SPDX-License-Identifier: BSD-3-Clause "

# Notices for Eclipse Implementation of JAXB

This content is produced and maintained by the Eclipse Implementation of JAXB
project.

* Project home: https://projects.eclipse.org/projects/ee4j.jaxb-impl

## Trademarks

Eclipse Implementation of JAXB is a trademark of the Eclipse Foundation.

## Copyright

All content is the property of the respective authors or their employers. For
more information regarding authorship of content, please consult the listed
source code repository logs.

## Declared Project Licenses

This program and the accompanying materials are made available under the terms
of the Eclipse Distribution License v. 1.0 which is available at
http://www.eclipse.org/org/documents/edl-v10.php.

SPDX-License-Identifier: BSD-3-Clause

## Source Code

The project maintains the following source code repositories:

* https://github.com/eclipse-ee4j/jaxb-ri
* https://github.com/eclipse-ee4j/jaxb-istack-commons
* https://github.com/eclipse-ee4j/jaxb-dtd-parser
* https://github.com/eclipse-ee4j/jaxb-fi
* https://github.com/eclipse-ee4j/jaxb-stax-ex
* https://github.com/eclipse-ee4j/jax-rpc-ri

## Third-party Content

This project leverages the following third party content.

Apache Ant (1.10.2)

* License: Apache-2.0 AND W3C AND LicenseRef-Public-Domain

Apache Ant (1.10.2)

* License: Apache-2.0 AND W3C AND LicenseRef-Public-Domain

Apache Felix (1.2.0)

* License: Apache License, 2.0

args4j (2.33)

* License: MIT License

dom4j (1.6.1)

* License: Custom license based on Apache 1.1

file-management (3.0.0)

* License: Apache-2.0
* Project: https://maven.apache.org/shared/file-management/
* Source:
   https://svn.apache.org/viewvc/maven/shared/tags/file-management-3.0.0/

JUnit (4.12)

* License: Eclipse Public License

JUnit (4.12)

* License: Eclipse Public License

maven-compat (3.5.2)

* License: Apache-2.0
* Project: https://maven.apache.org/ref/3.5.2/maven-compat/
* Source:
   https://mvnrepository.com/artifact/org.apache.maven/maven-compat/3.5.2

maven-core (3.5.2)

* License: Apache-2.0
* Project: https://maven.apache.org/ref/3.5.2/maven-core/index.html
* Source: https://mvnrepository.com/artifact/org.apache.maven/maven-core/3.5.2

maven-plugin-annotations (3.5)

* License: Apache-2.0
* Project: https://maven.apache.org/plugin-tools/maven-plugin-annotations/
* Source:
   https://github.com/apache/maven-plugin-tools/tree/master/maven-plugin-annotations

maven-plugin-api (3.5.2)

* License: Apache-2.0

maven-resolver-api (1.1.1)

* License: Apache-2.0

maven-resolver-api (1.1.1)

* License: Apache-2.0

maven-resolver-connector-basic (1.1.1)

* License: Apache-2.0

maven-resolver-impl (1.1.1)

* License: Apache-2.0

maven-resolver-spi (1.1.1)

* License: Apache-2.0

maven-resolver-transport-file (1.1.1)

* License: Apache-2.0
* Project: https://maven.apache.org/resolver/maven-resolver-transport-file/
* Source:
   https://github.com/apache/maven-resolver/tree/master/maven-resolver-transport-file

maven-resolver-util (1.1.1)

* License: Apache-2.0

maven-settings (3.5.2)

* License: Apache-2.0
* Source:
   https://mvnrepository.com/artifact/org.apache.maven/maven-settings/3.5.2

OSGi Service Platform Core Companion Code (6.0)

* License: Apache License, 2.0

plexus-archiver (3.5)

* License: Apache-2.0
* Project: https://codehaus-plexus.github.io/plexus-archiver/
* Source: https://github.com/codehaus-plexus/plexus-archiver

plexus-io (3.0.0)

* License: Apache-2.0

plexus-utils (3.1.0)

* License: Apache- 2.0 or Apache- 1.1 or BSD or Public Domain or Indiana
   University Extreme! Lab Software License V1.1.1 (Apache 1.1 style)

relaxng-datatype (1.0)

* License: New BSD license

Sax (0.2)

* License: SAX-PD
* Project: http://www.megginson.com/downloads/SAX/
* Source: http://sourceforge.net/project/showfiles.php?group_id=29449

testng (6.14.2)

* License: Apache-2.0 AND (MIT OR GPL-1.0+)
* Project: https://testng.org/doc/index.html
* Source: https://github.com/cbeust/testng

wagon-http-lightweight (3.0.0)

* License: Pending
* Project: https://maven.apache.org/wagon/
* Source:
   https://mvnrepository.com/artifact/org.apache.maven.wagon/wagon-http-lightweight/3.0.0

xz for java (1.8)

* License: LicenseRef-Public-Domain

## Cryptography

Content may contain encryption software. The country in which you are currently
may have restrictions on the import, possession, and use, and/or re-export to
another country, of encryption software. BEFORE using any encryption software,
please check the country's laws, regulations and policies concerning the import,
possession, or use, and re-export of encryption software, to see if this is
permitted.
```

### Jasypt (org.jasypt.*)

```
   Copyright (c) 2007-2010, The JASYPT team (http://www.jasypt.org)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.


---------------------------------



This distribution includes cryptographic software.  The country in
which you currently reside may have restrictions on the import,
possession, use, and/or re-export to another country, of
encryption software.  BEFORE using any encryption software, please
check your country's laws, regulations and policies concerning the
import, possession, or use, and re-export of encryption software, to
see if this is permitted.  See http://www.wassenaar.org/ for more
information.

The U.S. Government Department of Commerce, Bureau of Industry and
Security (BIS), has classified this software as Export Commodity
Control Number (ECCN) 5D002.C.1, which includes information security
software using or performing cryptographic functions with asymmetric
algorithms.  The form and manner of this distribution makes it
eligible for export under the License Exception ENC Technology
Software Unrestricted (TSU) exception (see the BIS Export
Administration Regulations, Section 740.13) for both object code and
source code.

The following provides more details on the cryptographic software
used (note that this software is not included in the distribution):

  * The PBE Encryption facilities require the Java Cryptography
    extensions: http://java.sun.com/javase/technologies/security/.

---------------------------------

Distributions of this software may include software developed by
The Apache Software Foundation (http://www.apache.org/).

---------------------------------


ICU License - ICU 1.8.1 and later

COPYRIGHT AND PERMISSION NOTICE

Copyright (c) 1995-2006 International Business Machines
Corporation and others

All rights reserved.

Permission is hereby granted, free of charge, to any
person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the
Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish,
distribute, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so,
provided that the above copyright notice(s) and this
permission notice appear in all copies of the Software and
that both the above copyright notice(s) and this
permission notice appear in supporting documentation.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT OF THIRD PARTY RIGHTS. IN NO
EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS INCLUDED IN
THIS NOTICE BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL
INDIRECT OR CONSEQUENTIAL DAMAGES, OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE
USE OR PERFORMANCE OF THIS SOFTWARE.

Except as contained in this notice, the name of a copyright
holder shall not be used in advertising or otherwise to
promote the sale, use or other dealings in this Software
without prior written authorization of the copyright holder.
```

### JQuery (org.webjars.jquery)

```
jQuery JavaScript Library v3.7.1
https://jquery.com/

Copyright OpenJS Foundation and other contributors
Released under the MIT license
https://jquery.org/license
```

### JsonSchemaValidator (com.networknt.json-schema-validator)

```
JSON Schema Validator
Copyright (c) 2016 and onwards Network New Technologies Inc.

// ------------------------------------------------------------------
// NOTICE file corresponding to the section 4d of The Apache License,
// Version 2.0, in this case for
// ------------------------------------------------------------------

Apache Commons Validator
Copyright 2001-2023 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).
```

### jsoup Java HTML Parser (org.jsoup.*)

```
Copyright (c) 2009-2025 Jonathan Hedley <https://jsoup.org/>
```

### lodash

```
Copyright JS Foundation and other contributors <https://js.foundation/>
```

### marked

```
Copyright (c) 2011-2024, Christopher Jeffrey and contributors
```

### ngx-bootstrap

```
Copyright (c) 2016 valor-software
```

### ngx-codemirror

```
Copyright (c) Scott Cooper
```

### ngx-color-picker

```
Copyright (c) 2016 Zeeshan Farooqui
```

### ngx-cookie-service

```
Copyright (c) 2017 Steven Meister
```

### ngx-markdown

```
Copyright (c) 2017 Julio Cesar
```

### Openhtmltopdf (com.openhtmltopdf.*)

```
Copyright (c) 2004, 2005 Joshua Marinacci, Torbjoern Gannholm
Copyright (c) 2006 Wisconsin Court System
```

### OWASP HTML Sanitizer (com.googlecode.owasp-java-html-sanitizer.*)

```
Copyright 2012-2024 OWASP Foundation

This product includes software developed at OWASP (https://owasp.org/).

Portions of this product were originally developed by Google Inc. as part of the Closure Templates project and Guava project.
```

### Password4j (com.password4j.*)

```
(C) Copyright 2020 Password4j (http://password4j.com/).
```

### ph-schematron-api / ph-schematron-xslt / ph-schematron-pure / ph-commons / ph-jaxb

```
Copyright (C) 2017-2025 Philip Helger (www.helger.com)
philip[at]helger[dot]com
```

### Play Framework (org.playframework.*)

```
Copyright (C) from 2022 The Play Framework Contributors https://github.com/playframework, 2011-2021 Lightbend Inc. https://www.lightbend.com

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
```

### Pac4j (org.pac4j.*)

```
Copyright 2012 - 2024 pac4j organization

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this software except in compliance with the License.
You may obtain a copy of the License at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

### Reflections (org.reflections.*)

```
Copyright (C) 2004 Sam Hocevar <sam@hocevar.net>
```

### RgxGen (com.github.curious-odd-man.rgxgen)

```
Copyright 2019 Vladislavs Varslavans
```

### rxjs

```
Copyright 2015-2023 Netflix, Inc.
and other contributors.
```

### Saxon-HE (net.sf.saxon.*)

```
Copyright (c) 2004-2024 Saxonica Limited.
All rights reserved.
```

### Slick / Slick HikariCP (org.playframework.play-slick)

```
Copyright 2011-2021 Lightbend, Inc.
All rights reserved.
```

### Spring Boot (org.springframework.boot.*)

```
Copyright 2002-2023 the original author or authors.
```

### Swagger UI (org.webjars.swagger-ui)

```
swagger-ui
Copyright 2020-2021 SmartBear Software Inc.
```

### tinymce

```
Copyright (c) 2022 Ephox Corporation DBA Tiny Technologies, Inc.
```

### tinymce-angular

```
Copyright (c) 2016-2024 Tiny Technologies, Inc.
```

### TopBraid SHACL API (org.topbraid.shacl)

```
TopBraid SHACL API
==================

Original commit content: Copyright 2015-2017 TopQuadrant Inc.
```

### Xerces2-j (xerces.xercesImpl)

```
   =========================================================================
   ==  NOTICE file corresponding to section 4(d) of the Apache License,   ==
   ==  Version 2.0, in this case for the Apache Xerces Java distribution. ==
   =========================================================================

   Apache Xerces Java
   Copyright 1999-2018 The Apache Software Foundation

   This product includes software developed at
   The Apache Software Foundation (http://www.apache.org/).

   Portions of this software were originally based on the following:
     - software copyright (c) 1999, IBM Corporation., http://www.ibm.com.
     - software copyright (c) 1999, Sun Microsystems., http://www.sun.com.
     - voluntary contributions made by Paul Eng on behalf of the
       Apache Software Foundation that were originally developed at iClick, Inc.,
       software copyright (c) 1999.
```

### Zip4j (net.lingala.zip4j.*)

```
Copyright 2010-2024 Srikanth R Lingala
```

### zone.js

```
Copyright 2016 Google Inc.
```
