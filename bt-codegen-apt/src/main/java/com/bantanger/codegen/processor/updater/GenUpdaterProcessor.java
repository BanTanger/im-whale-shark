package com.bantanger.codegen.processor.updater;

import com.bantanger.codegen.processor.BaseCodeGenProcessor;
import com.bantanger.codegen.spi.CodeGenProcessor;
import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import lombok.Data;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
@AutoService(value = CodeGenProcessor.class)
public class GenUpdaterProcessor extends BaseCodeGenProcessor {

    public static final String SUFFIX = "Updater";

    @Override
    protected void generateClass(TypeElement typeElement, RoundEnvironment roundEnvironment) {
        Set<VariableElement> fields = findFields(typeElement,
            p -> Objects.isNull(p.getAnnotation(IgnoreUpdater.class)));
        String className = PREFIX + typeElement.getSimpleName() + SUFFIX;
        String sourceClassName = typeElement.getSimpleName() + SUFFIX;
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Schema.class)
            .addAnnotation(Data.class);
        addSetterAndGetterMethod(typeSpecBuilder, fields);
        CodeBlock.Builder builder = CodeBlock.builder();
        for (VariableElement ve : fields) {
            builder.addStatement("$T.ofNullable($L()).ifPresent(v -> param.$L(v))", Optional.class,
                "get" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL,
                    ve.getSimpleName().toString()),
                "set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL,
                    ve.getSimpleName().toString()));
        }
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(
                "update" + typeElement.getSimpleName())
            .addModifiers(Modifier.PUBLIC)
            .addParameter(TypeName.get(typeElement.asType()), "param")
            .addCode(builder.build())
            .returns(void.class);
        typeSpecBuilder.addMethod(methodBuilder.build());
        typeSpecBuilder.addField(
            FieldSpec.builder(ClassName.get(Long.class), "id", Modifier.PRIVATE).build());
        addIdSetterAndGetter(typeSpecBuilder);
        typeSpecBuilder.addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PROTECTED)
            .build());
        String packageName = generatePackage(typeElement);
        genJavaFile(packageName, typeSpecBuilder);
        genJavaFile(packageName, getSourceType(sourceClassName, packageName, className));
    }

    @Override
    public Class<? extends Annotation> getAnnotation() {
        return GenUpdater.class;
    }

    @Override
    public String generatePackage(TypeElement typeElement) {
        return typeElement.getAnnotation(GenUpdater.class).pkgName();
    }
}