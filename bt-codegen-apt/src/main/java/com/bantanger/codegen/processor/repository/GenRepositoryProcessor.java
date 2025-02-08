package com.bantanger.codegen.processor.repository;


import com.bantanger.jpa.support.BaseRepository;
import com.google.auto.service.AutoService;
import com.bantanger.codegen.processor.BaseCodeGenProcessor;
import com.bantanger.codegen.spi.CodeGenProcessor;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.lang.annotation.Annotation;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
@AutoService(value = CodeGenProcessor.class)
public class GenRepositoryProcessor extends BaseCodeGenProcessor {

    public static final String REPOSITORY_SUFFIX = "Repository";

    @Override
    protected void generateClass(TypeElement typeElement, RoundEnvironment roundEnvironment) {
        String className = typeElement.getSimpleName() + REPOSITORY_SUFFIX;
        TypeSpec.Builder typeSpecBuilder = TypeSpec.interfaceBuilder(className)
            .addSuperinterface(ParameterizedTypeName.get(ClassName.get(BaseRepository.class),
                ClassName.get(typeElement), ClassName.get(Long.class)))
            .addModifiers(Modifier.PUBLIC);
        genJavaSourceFile(generatePackage(typeElement),
            typeElement.getAnnotation(GenRepository.class).sourcePath(), typeSpecBuilder);
    }

    @Override
    public Class<? extends Annotation> getAnnotation() {
        return GenRepository.class;
    }

    @Override
    public String generatePackage(TypeElement typeElement) {
        return typeElement.getAnnotation(GenRepository.class).pkgName();
    }
}
