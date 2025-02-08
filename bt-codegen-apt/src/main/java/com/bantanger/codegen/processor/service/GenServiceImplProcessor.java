package com.bantanger.codegen.processor.service;

import com.bantanger.codegen.processor.BaseCodeGenProcessor;
import com.bantanger.codegen.processor.DefaultNameContext;
import com.bantanger.codegen.spi.CodeGenProcessor;
import com.bantanger.codegen.utils.StringUtils;
import com.bantanger.common.enums.CodeEnum;
import com.bantanger.common.exception.BusinessException;
import com.bantanger.common.model.PageRequestWrapper;
import com.bantanger.jpa.support.EntityOperations;
import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import com.querydsl.core.BooleanBuilder;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author chensongmin
 * @description 获取名称时可以先获取上下文再取，不用一个个的取，这样更方便
 * @date 2025/1/8
 */
@AutoService(value = CodeGenProcessor.class)
public class GenServiceImplProcessor extends BaseCodeGenProcessor {

    public static final String IMPL_SUFFIX = "ServiceImpl";

    @Override
    protected void generateClass(TypeElement typeElement, RoundEnvironment roundEnvironment) {
        DefaultNameContext nameContext = getNameContext(typeElement);
        String className = typeElement.getSimpleName() + IMPL_SUFFIX;
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className)
            .addSuperinterface(
                ClassName.get(nameContext.getServicePackageName(),
                    nameContext.getServiceClassName()))
            .addAnnotation(Transactional.class)
            .addAnnotation(Service.class)
            .addAnnotation(Slf4j.class)
            .addAnnotation(RequiredArgsConstructor.class)
            .addModifiers(Modifier.PUBLIC);
        if (StringUtils.containsNull(nameContext.getRepositoryPackageName())) {
            return;
        }
        String repositoryFieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL,
            nameContext.getRepositoryClassName());
        String classFieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL,
            typeElement.getSimpleName().toString());
        FieldSpec repositoryField = FieldSpec
            .builder(ClassName.get(nameContext.getRepositoryPackageName(),
                nameContext.getRepositoryClassName()), repositoryFieldName)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build();
        typeSpecBuilder.addField(repositoryField);
        Optional<MethodSpec> createMethod = createMethod(typeElement, nameContext,
            repositoryFieldName,
            classFieldName);
        createMethod.ifPresent(typeSpecBuilder::addMethod);
        Optional<MethodSpec> updateMethod = updateMethod(typeElement, nameContext,
            repositoryFieldName);
        updateMethod.ifPresent(typeSpecBuilder::addMethod);
        Optional<MethodSpec> validMethod = validMethod(typeElement, repositoryFieldName);
        validMethod.ifPresent(typeSpecBuilder::addMethod);
        Optional<MethodSpec> invalidMethod = invalidMethod(typeElement, repositoryFieldName);
        invalidMethod.ifPresent(typeSpecBuilder::addMethod);
        Optional<MethodSpec> findByIdMethod = findByIdMethod(typeElement, nameContext,
            repositoryFieldName, classFieldName);
        findByIdMethod.ifPresent(typeSpecBuilder::addMethod);
        Optional<MethodSpec> findByPageMethod = findByPageMethod(typeElement, nameContext,
            repositoryFieldName);
        findByPageMethod.ifPresent(typeSpecBuilder::addMethod);
        genJavaSourceFile(generatePackage(typeElement),
            typeElement.getAnnotation(GenServiceImpl.class).sourcePath(), typeSpecBuilder);
    }

    @Override
    public Class<? extends Annotation> getAnnotation() {
        return GenServiceImpl.class;
    }

    @Override
    public String generatePackage(TypeElement typeElement) {
        return typeElement.getAnnotation(GenServiceImpl.class).pkgName();
    }

    private Optional<MethodSpec> createMethod(TypeElement typeElement,
        DefaultNameContext nameContext,
        String repositoryFieldName, String classFieldName) {
        boolean containsNull = StringUtils.containsNull(nameContext.getCreatorPackageName(),
            nameContext.getMapperPackageName());
        if (!containsNull) {
            return Optional.of(MethodSpec.methodBuilder("create" + typeElement.getSimpleName())
                .addParameter(
                    ClassName.get(nameContext.getCreatorPackageName(),
                        nameContext.getCreatorClassName()),
                    "creator")
                .addModifiers(Modifier.PUBLIC)
                .addCode(
                    CodeBlock.of(
                        """
                            Optional<$T> $L = $T.doCreate($L)
                            .create(() -> $T.INSTANCE.dtoToEntity(creator))
                            .update(e -> e.init())
                            .execute();
                            """,
                        typeElement, classFieldName, EntityOperations.class, repositoryFieldName,
                        ClassName.get(nameContext.getMapperPackageName(),
                            nameContext.getMapperClassName()))
                )
                .addCode(
                    CodeBlock.of("return $L.isPresent() ? $L.get().getId() : 0;", classFieldName,
                        classFieldName)
                )
                .addJavadoc("createImpl")
                .addAnnotation(Override.class)
                .returns(Long.class).build());
        }
        return Optional.empty();
    }

    private Optional<MethodSpec> updateMethod(TypeElement typeElement,
        DefaultNameContext nameContext,
        String repositoryFieldName) {
        boolean containsNull = StringUtils.containsNull(nameContext.getUpdaterPackageName());
        if (!containsNull) {
            return Optional.of(MethodSpec.methodBuilder("update" + typeElement.getSimpleName())
                .addParameter(
                    ClassName.get(nameContext.getUpdaterPackageName(),
                        nameContext.getUpdaterClassName()),
                    "updater")
                .addModifiers(Modifier.PUBLIC)
                .addCode(
                    CodeBlock.of("""
                            $T.doUpdate($L)
                            .loadById(updater.getId())
                            .update(e -> updater.update$L(e))
                            .execute();
                            """, EntityOperations.class, repositoryFieldName, typeElement.getSimpleName())
                )
                .addJavadoc("update")
                .addAnnotation(Override.class)
                .build());
        }
        return Optional.empty();
    }

    private Optional<MethodSpec> validMethod(TypeElement typeElement, String repositoryFieldName) {
        return Optional.of(MethodSpec.methodBuilder("valid" + typeElement.getSimpleName())
            .addParameter(Long.class, "id")
            .addModifiers(Modifier.PUBLIC)
            .addCode(
                CodeBlock.of("""
                        $T.doUpdate($L)
                        .loadById(id)
                        .update(e -> e.valid())
                        .execute();
                        """, EntityOperations.class, repositoryFieldName)
            )
            .addJavadoc("valid")
            .addAnnotation(Override.class)
            .build());
    }

    private Optional<MethodSpec> invalidMethod(TypeElement typeElement,
        String repositoryFieldName) {
        return Optional.of(MethodSpec.methodBuilder("invalid" + typeElement.getSimpleName())
            .addParameter(Long.class, "id")
            .addModifiers(Modifier.PUBLIC)
            .addCode(
                CodeBlock.of("""
                        $T.doUpdate($L)
                        .loadById(id)
                        .update(e -> e.invalid())
                        .execute();
                        """, EntityOperations.class, repositoryFieldName)
            )
            .addJavadoc("invalid")
            .addAnnotation(Override.class)
            .build());
    }

    private Optional<MethodSpec> findByIdMethod(TypeElement typeElement,
        DefaultNameContext nameContext, String repositoryFieldName, String classFieldName) {
        boolean containsNull = StringUtils.containsNull(nameContext.getVoPackageName());
        if (!containsNull) {
            return Optional.of(MethodSpec.methodBuilder("findById")
                .addParameter(Long.class, "id")
                .addModifiers(Modifier.PUBLIC)
                .addCode(
                    CodeBlock.of("$T $L =  $L.findById(id);\n",
                        ParameterizedTypeName.get(ClassName.get(Optional.class),
                            ClassName.get(typeElement)), classFieldName, repositoryFieldName)
                ).addCode(
                    CodeBlock.of("return new $T($L.orElseThrow(() -> new $T($T.NotFindError)));",
                        ClassName.get(nameContext.getVoPackageName(), nameContext.getVoClassName()),
                        classFieldName,
                        BusinessException.class, CodeEnum.class)
                )
                .addJavadoc("findById")
                .addAnnotation(Override.class)
                .returns(
                    ClassName.get(nameContext.getVoPackageName(), nameContext.getVoClassName()))
                .build());
        }
        return Optional.empty();
    }

    private Optional<MethodSpec> findByPageMethod(TypeElement typeElement,
        DefaultNameContext nameContext, String repositoryFieldName) {
        boolean containsNull = StringUtils.containsNull(nameContext.getQueryPackageName(),
            nameContext.getVoPackageName());
        if (!containsNull) {
            return Optional.of(MethodSpec.methodBuilder("findByPage")
                .addParameter(ParameterizedTypeName.get(ClassName.get(PageRequestWrapper.class),
                        ClassName.get(nameContext.getQueryPackageName(),
                            nameContext.getQueryClassName())),
                    "query")
                .addModifiers(Modifier.PUBLIC)
                .addCode(
                    CodeBlock.of("$T booleanBuilder = new $T();\n", BooleanBuilder.class,
                        BooleanBuilder.class)
                )
                .addCode(
                    CodeBlock.of("""
                            $T<$T> page = $L.findAll(booleanBuilder,
                                    $T.of(query.getPage() - 1, query.getPageSize(),
                                    $T.by($T.DESC, "createdAt")));
                            """, Page.class, typeElement,
                        repositoryFieldName,
                        PageRequest.class, Sort.class, Direction.class)
                )
                .addCode(
                    CodeBlock.of("""
                            return new $T<>(page.getContent().stream().map($T::new)
                                    .collect($T.toList()), page.getPageable(), page.getTotalElements());
                            """, PageImpl.class,
                        ClassName.get(nameContext.getVoPackageName(), nameContext.getVoClassName()),
                        Collectors.class)
                )
                .addJavadoc("findByPage")
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(ClassName.get(Page.class),
                    ClassName.get(nameContext.getVoPackageName(), nameContext.getVoClassName())))
                .build());
        }
        return Optional.empty();
    }

}
