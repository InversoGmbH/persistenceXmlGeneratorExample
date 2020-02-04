package de.inverso.persistencegenerator;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.lang.ref.Cleaner;
import java.util.*;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_13)
@SupportedAnnotationTypes("javax.persistence.Entity")
public class MappedClassGenerator extends AbstractProcessor {

    private final List<String> classNames = new LinkedList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return true;
        }

        for (TypeElement annotation : annotations) {
            var annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            var annotatedClasses = annotatedElements.stream().filter( element -> element.getKind().isClass()).collect(Collectors.toList());
            var otherElements = annotatedElements.stream().filter( element -> !element.getKind().isClass()).collect(Collectors.toList());

            otherElements.forEach(element ->
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "@Entity must be applied to a class ", element));
            if (annotatedClasses.isEmpty()) {
                continue;
            }
            //System.out.println(annotatedClasses.get(0));
            annotatedClasses.forEach( element -> classNames.add(element.toString()) );
        }
        writePersistenceXml(classNames);
        return true;
    }

    private void writePersistenceXml(List<String> classNames) {
        try {
            final var persistenceFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/persistence.xml");
            final var writer = persistenceFile.openWriter();
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "\n" +
                    "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\"\n" +
                    "             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "             xsi:schemaLocation=\"http://java.sun.com/xml/ns/persistence\n" +
                    "   http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd\"\n" +
                    "             version=\"1.0\">\n" +
                    "\n" +
                    "    <persistence-unit name=\"test\" transaction-type=\"RESOURCE_LOCAL\">\n");
            for (String className : classNames) {
                writer.append("<class>").append(className).append("</class>\n");
            }
            writer.append(
                    "        <exclude-unlisted-classes>false</exclude-unlisted-classes>\n" +
                            "\n" +
                            "        <properties>\n" +
                            "            <property name=\"javax.persistence.jdbc.driver\" value=\"org.h2.Driver\"/>\n" +
                            "            <property name=\"javax.persistence.jdbc.url\" value=\"jdbc:h2:./test-gradle\"/>\n" +
                            "            <property name=\"javax.persistence.jdbc.user\" value=\"sa\"/>\n" +
                            "            <property name=\"javax.persistence.jdbc.password\" value=\"\"/>\n" +
                            "            <property name=\"hibernate.packagesToScan\" value=\"de.inverso.persistencegenerator.example\"/>\n" +
                            "            <property name=\"hibernate.archive.autodetection\" value=\"class, hbm\"/>\n" +
                            "            <property name=\"hibernate.dialect\" value=\"org.hibernate.dialect.H2Dialect\"/>\n" +
                            "            <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>\n" +
                            "            <property name=\"hibernate.temp.use_jdbc_metadata_defaults\" value=\"false\"/>\n" +
                            "            <property name=\"hibernate.generate_statistics\" value=\"true\"/>\n" +
                            "            <property name=\"hibernate.flushMode\" value=\"FLUSH_AUTO\"/>\n" +
                            "            <property name=\"hibernate.show_sql\" value=\"true\"/>\n" +
                            "        </properties>\n" +
                            "    </persistence-unit>\n" +
                            "</persistence>");
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
