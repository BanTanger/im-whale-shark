package com.bantanger.codegen.processor.creator;

import com.bantanger.codegen.processor.BaseCodeGenProcessor;
import com.bantanger.codegen.spi.CodeGenProcessor;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.annotation.Annotation;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.Data;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
@AutoService(value = CodeGenProcessor.class)
public class CreatorCodeGenProcessor extends BaseCodeGenProcessor {

    public static final String SUFFIX = "Creator";

    @Override
    public Class<? extends Annotation> getAnnotation() {
        return GenCreator.class;
    }

    @Override
    public String generatePackage(TypeElement typeElement) {
        return typeElement.getAnnotation(GenCreator.class).pkgName();
    }

    @Override
    public void generateClass(TypeElement typeElement, RoundEnvironment roundEnvironment) {
        /**
         * 为啥加@Data 还要生成get set 方法？
         * lombok - mapstruct 集成
         */
        String className = PREFIX + typeElement.getSimpleName() + SUFFIX;
        String sourceClassName = typeElement.getSimpleName() + SUFFIX;
        Builder builder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Schema.class)
            .addAnnotation(Data.class);
        addSetterAndGetterMethod(builder,
            findFields(typeElement, ve -> Objects.isNull(ve.getAnnotation(
                IgnoreCreator.class)) && !dtoIgnore(ve)));
        String packageName = generatePackage(typeElement);
        genJavaFile(packageName, builder);
        genJavaFile(packageName, getSourceType(sourceClassName, packageName, className));
    }

    /**
     * 忽略时间类型
     *
     * @param ve
     * @return
     */
    private boolean dtoIgnore(Element ve) {
        return dtoIgnoreFieldTypes.contains(TypeName.get(ve.asType())) || ve.getModifiers()
            .contains(Modifier.STATIC);
    }

    static final List<TypeName> dtoIgnoreFieldTypes;

    static {
        dtoIgnoreFieldTypes = new ArrayList<>();
        dtoIgnoreFieldTypes.add(TypeName.get(Date.class));
        dtoIgnoreFieldTypes.add(TypeName.get(LocalDateTime.class));
    }


}
