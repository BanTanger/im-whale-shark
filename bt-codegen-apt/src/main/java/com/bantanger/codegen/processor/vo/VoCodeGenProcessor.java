package com.bantanger.codegen.processor.vo;

import com.bantanger.codegen.processor.BaseCodeGenProcessor;
import com.bantanger.codegen.spi.CodeGenProcessor;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import lombok.Data;

/**
 * @author chensongmin
 * @description 生成VO类
 * @date 2025/1/8
 */
@AutoService(value = CodeGenProcessor.class)
public class VoCodeGenProcessor extends BaseCodeGenProcessor {

    public static final String SUFFIX = "VO";

    @Override
    public Class<? extends Annotation> getAnnotation() {
        return GenVo.class;
    }

    @Override
    public String generatePackage(TypeElement typeElement) {
        return typeElement.getAnnotation(GenVo.class).pkgName();
    }

    @Override
    protected void generateClass(TypeElement typeElement, RoundEnvironment roundEnvironment) {
        Set<VariableElement> fields = findFields(typeElement,
            ve -> Objects.isNull(ve.getAnnotation(IgnoreVo.class)));
        String className = PREFIX + typeElement.getSimpleName() + SUFFIX;
        String sourceClassName = typeElement.getSimpleName() + SUFFIX;
        Builder builder = TypeSpec.classBuilder(className)
            .superclass(AbstractBaseJpaVO.class)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Schema.class)
            .addAnnotation(Data.class);
        addSetterAndGetterMethod(builder, fields);
        MethodSpec.Builder constructorSpecBuilder = MethodSpec.constructorBuilder()
            .addParameter(TypeName.get(typeElement.asType()), "source")
            .addModifiers(Modifier.PUBLIC);
        constructorSpecBuilder.addStatement("super(source)");
        fields.forEach(f -> {
            constructorSpecBuilder.addStatement("this.set$L(source.get$L())",
                getFieldDefaultName(f),
                getFieldDefaultName(f));
        });
        builder.addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PROTECTED)
            .build());
        builder.addMethod(constructorSpecBuilder.build());
        String packageName = generatePackage(typeElement);
        genJavaFile(packageName, builder);
        genJavaFile(packageName,
            getSourceTypeWithConstruct(typeElement, sourceClassName, packageName, className));
    }
}
