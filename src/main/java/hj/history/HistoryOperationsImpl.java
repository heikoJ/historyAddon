package hj.history;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.*;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;


/**
 * Implementation of operations this add-on offers.
 *
 * @since 1.1
 */
@Component // Use these Apache Felix annotations to register your commands class in the Roo container
@Service
public class HistoryOperationsImpl implements HistoryOperations {


    private static final Logger LOGGER = HandlerUtils
            .getLogger(HistoryOperationsImpl.class);

    /**
     * Use ProjectOperations to install new dependencies, plugins, properties, etc into the project configuration
     */
    @Reference private ProjectOperations projectOperations;

    /**
     * Use TypeLocationService to find types which are annotated with a given annotation in the project
     */
    @Reference private TypeLocationService typeLocationService;
    
    /**
     * Use TypeManagementService to change types
     */
    @Reference private TypeManagementService typeManagementService;

    @Reference private HistoryEntityCreator historyEntityCreator;

    @Reference private Shell shell;

    /** {@inheritDoc} */
    public boolean isCommandAvailable() {
        // Check if a project has been created
        return projectOperations.isFocusedProjectAvailable();
    }

    /** {@inheritDoc} */
    public void annotateType(JavaType javaType) {
        shell.setDevelopmentMode(true);
        // Use Roo's Assert type for null checks
        Validate.notNull(javaType, "Java type required");

        // Obtain ClassOrInterfaceTypeDetails for this java type
        ClassOrInterfaceTypeDetails existing = typeLocationService.getTypeDetails(javaType);

        ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(existing);

        // Test if the annotation already exists on the target type
        if (existing != null && MemberFindingUtils.getAnnotationOfType(existing.getAnnotations(), new JavaType(RooHistory.class.getName())) == null) {

            // Create JavaType instance for the add-ons trigger annotation
            JavaType rooRooHistory = new JavaType(RooHistory.class.getName());

            // Create Annotation metadata
            AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(rooRooHistory);
            
            // Add annotation to target type
            classOrInterfaceTypeDetailsBuilder.addAnnotation(annotationBuilder.build());
            
            // Save changes to disk
            typeManagementService.createOrUpdateTypeOnDisk(classOrInterfaceTypeDetailsBuilder.build());

            createHistoryType(javaType,existing);

        }
    }


    private void createHistoryType(JavaType entityType, ClassOrInterfaceTypeDetails entityTypeDetails) {
        ClassOrInterfaceTypeDetailsBuilder entityBuilder = new ClassOrInterfaceTypeDetailsBuilder(entityTypeDetails);
        JavaType historyType = new JavaType(entityType.getFullyQualifiedTypeName() + "History");
        String historyMetadataId = PhysicalTypeIdentifier.createIdentifier(historyType, typeLocationService.getTypePath(entityType));

        ClassOrInterfaceTypeDetailsBuilder historyBuilder = new ClassOrInterfaceTypeDetailsBuilder(historyMetadataId,Modifier.PUBLIC,historyType,PhysicalTypeCategory.CLASS);

        copyFieldsFromEntityToHistoryType(entityBuilder, historyBuilder);
        addImplementationsForPersistenceMethods(historyBuilder);
        addPrimaryKeyReferenceField(entityType, historyBuilder);
        addTypeField(historyBuilder);
        addAnnotations(entityType, historyBuilder);

        typeManagementService.createOrUpdateTypeOnDisk(historyBuilder.build());
    }

    private void addAnnotations(JavaType entityType, ClassOrInterfaceTypeDetailsBuilder historyBuilder) {
        historyBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean")));
        AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord"));
        String entityName = entityType.getSimpleTypeName();
        annotationMetadataBuilder.addStringAttribute("finders","find"+ entityName + "HistorysBy" + entityName + "IdEquals");
        historyBuilder.addAnnotation(annotationMetadataBuilder);
        historyBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.tostring.RooToString")));
    }

    private void addTypeField(ClassOrInterfaceTypeDetailsBuilder historyBuilder) {
        FieldMetadataBuilder fieldMetadataBuilder = new FieldMetadataBuilder(historyBuilder.getDeclaredByMetadataId());
        fieldMetadataBuilder.setFieldName(new JavaSymbolName("historyType"));
        fieldMetadataBuilder.setFieldType(JavaType.STRING);
        fieldMetadataBuilder.setModifier(Modifier.PRIVATE);

        historyBuilder.addField(fieldMetadataBuilder);
    }

    private void addPrimaryKeyReferenceField(JavaType entityType, ClassOrInterfaceTypeDetailsBuilder historyBuilder) {
        FieldMetadataBuilder fieldMetadataBuilder = new FieldMetadataBuilder(historyBuilder.getDeclaredByMetadataId());
        String uncapitalizedEntityName = StringUtils.uncapitalize(entityType.getSimpleTypeName());
        fieldMetadataBuilder.setFieldName(new JavaSymbolName(uncapitalizedEntityName + "Id"));
        fieldMetadataBuilder.setFieldType(JavaType.LONG_OBJECT);
        fieldMetadataBuilder.setModifier(Modifier.PRIVATE);

        historyBuilder.addField(fieldMetadataBuilder);
    }

    private void addImplementationsForPersistenceMethods(ClassOrInterfaceTypeDetailsBuilder historyBuilder) {
        String historyMetadataId = historyBuilder.getDeclaredByMetadataId();
        ImportMetadataBuilder importMetadataBuilder = new ImportMetadataBuilder(historyMetadataId);
        importMetadataBuilder.setImportType(new JavaType("sun.reflect.generics.reflectiveObjects.NotImplementedException"));
        historyBuilder.add(importMetadataBuilder.build());

        historyBuilder.addMethod(getNotImplementedMethod(historyMetadataId,"persist"));
        historyBuilder.addMethod(getNotImplementedMethod(historyMetadataId,"remove"));
        historyBuilder.addMethod(getNotImplementedMethod(historyMetadataId,"flush"));
        historyBuilder.addMethod(getNotImplementedMethod(historyMetadataId,"clear"));
        historyBuilder.addMethod(getNotImplementedMethod(historyMetadataId,"merge"));
    }

    private void copyFieldsFromEntityToHistoryType(ClassOrInterfaceTypeDetailsBuilder entityBuilder, ClassOrInterfaceTypeDetailsBuilder historyBuilder) {
        JavaType transientAnnotation = new JavaType("javax.persistence.Transient");
        fieldAdditions: for (final FieldMetadataBuilder field : entityBuilder.getDeclaredFields()) {
            List<AnnotationMetadataBuilder>  annotations = field.getAnnotations();
            for(AnnotationMetadataBuilder annotation : annotations) {
                if(annotation.getAnnotationType().equals(transientAnnotation)) {
                    continue fieldAdditions;
                }
            }

            FieldMetadataBuilder builder = new FieldMetadataBuilder(historyBuilder.getDeclaredByMetadataId(),field.build());

            historyBuilder.addField(builder);
        }
    }

    private MethodMetadata getNotImplementedMethod(String declaredbyMetadataId,String methodName) {
        MethodMetadataBuilder methodMetadataBuilder = new MethodMetadataBuilder(declaredbyMetadataId);
        methodMetadataBuilder.setMethodName(new JavaSymbolName(methodName));
        methodMetadataBuilder.setModifier(Modifier.PUBLIC);
        methodMetadataBuilder.setReturnType(JavaType.VOID_PRIMITIVE);

        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("throw new NotImplementedException();");
        methodMetadataBuilder.setBodyBuilder(bodyBuilder);

        return methodMetadataBuilder.build();

    }

    /** {@inheritDoc} */
    public void annotateAll() {
        // Use the TypeLocationService to scan project for all types with a specific annotation
        for (JavaType type: typeLocationService.findTypesWithAnnotation(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean"))) {
            annotateType(type);
        }
    }
    
    /** {@inheritDoc} */
    public void setup() {
        // Install the add-on Google code repository needed to get the annotation 
        projectOperations.addRepository("", new Repository("History Roo add-on repository", "History Roo add-on repository", "https://hj-history.googlecode.com/svn/repo"));
        
        List<Dependency> dependencies = new ArrayList<Dependency>();
        
        // Install the dependency on the add-on jar (
        dependencies.add(new Dependency("hj.history", "hj.history", "0.1.0.BUILD-SNAPSHOT", DependencyType.JAR, DependencyScope.PROVIDED));
        
        // Install dependencies defined in external XML file
        for (Element dependencyElement : XmlUtils.findElements("/configuration/batch/dependencies/dependency", XmlUtils.getConfiguration(getClass()))) {
            dependencies.add(new Dependency(dependencyElement));
        }

        // Add all new dependencies to pom.xml
        projectOperations.addDependencies("", dependencies);
    }
}