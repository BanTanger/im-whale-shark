package com.bantanger.codegen.processor.controller;


import com.bantanger.codegen.processor.BaseCodeGenProcessor;
import com.bantanger.codegen.processor.DefaultNameContext;
import com.bantanger.codegen.spi.CodeGenProcessor;
import com.bantanger.codegen.utils.StringUtils;
import com.bantanger.common.enums.CodeEnum;
import com.bantanger.common.model.JsonObject;
import com.bantanger.common.model.PageRequestWrapper;
import com.bantanger.common.model.PageResult;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chensongmin
 * @description 生成controller
 * @date 2025/1/8
 */
@AutoService(value = CodeGenProcessor.class)
public class GenControllerProcessor extends BaseCodeGenProcessor {

    public static final String CONTROLLER_SUFFIX = "Controller";

    @Override
    protected void generateClass(TypeElement typeElement, RoundEnvironment roundEnvironment) {
        DefaultNameContext nameContext = getNameContext(typeElement);
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(
                nameContext.getControllerClassName())
            .addAnnotation(RestController.class)
            .addAnnotation(Slf4j.class)
            .addAnnotation(AnnotationSpec.builder(RequestMapping.class).addMember("value", "$S",
                StringUtils.camel(typeElement.getSimpleName().toString()) + "/v1/").build())
            .addAnnotation(RequiredArgsConstructor.class)
            .addModifiers(Modifier.PUBLIC);
        String serviceFieldName =
            StringUtils.camel(typeElement.getSimpleName().toString()) + "Service";
        if (StringUtils.containsNull(nameContext.getServicePackageName())) {
            return;
        }
        FieldSpec serviceField = FieldSpec
            .builder(ClassName.get(nameContext.getServicePackageName(),
                nameContext.getServiceClassName()), serviceFieldName)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build();
        typeSpecBuilder.addField(serviceField);
        Optional<MethodSpec> createMethod = createMethod(serviceFieldName, typeElement,
            nameContext);
        createMethod.ifPresent(typeSpecBuilder::addMethod);
        Optional<MethodSpec> updateMethod = updateMethod(serviceFieldName, typeElement,
            nameContext);
        updateMethod.ifPresent(typeSpecBuilder::addMethod);
        Optional<MethodSpec> validMethod = validMethod(serviceFieldName, typeElement);
        validMethod.ifPresent(typeSpecBuilder::addMethod);
        Optional<MethodSpec> invalidMethod = inValidMethod(serviceFieldName, typeElement);
        invalidMethod.ifPresent(typeSpecBuilder::addMethod);
        Optional<MethodSpec> findById = findById(serviceFieldName, nameContext);
        findById.ifPresent(typeSpecBuilder::addMethod);
        Optional<MethodSpec> findByPage = findByPage(serviceFieldName, nameContext);
        findByPage.ifPresent(typeSpecBuilder::addMethod);
        genJavaSourceFile(generatePackage(typeElement),
            typeElement.getAnnotation(GenController.class).sourcePath(), typeSpecBuilder);
    }

    @Override
    public Class<? extends Annotation> getAnnotation() {
        return GenController.class;
    }

    @Override
    public String generatePackage(TypeElement typeElement) {
        return typeElement.getAnnotation(GenController.class).pkgName();
    }

    /**
     * 创建方法
     *
     * @param serviceFieldName
     * @param typeElement
     * @param nameContext
     * @return
     */
    private Optional<MethodSpec> createMethod(String serviceFieldName, TypeElement typeElement,
        DefaultNameContext nameContext) {
        boolean containsNull = StringUtils.containsNull(nameContext.getCreatePackageName(),
            nameContext.getCreatorPackageName(), nameContext.getMapperPackageName());
        if (!containsNull) {
            return Optional.of(MethodSpec.methodBuilder("create" + typeElement.getSimpleName())
                .addParameter(ParameterSpec.builder(
                    ClassName.get(nameContext.getCreatePackageName(),
                        nameContext.getCreateClassName()), "request").addAnnotation(
                    RequestBody.class).build())
                .addAnnotation(AnnotationSpec.builder(PostMapping.class)
                    .addMember("value", "$S", "create" + typeElement.getSimpleName()).build())
                .addModifiers(Modifier.PUBLIC)
                .addCode(
                    CodeBlock.of("$T creator = $T.INSTANCE.request2Dto(request);\n",
                        ClassName.get(nameContext.getCreatorPackageName(),
                            nameContext.getCreatorClassName()),
                        ClassName.get(nameContext.getMapperPackageName(),
                            nameContext.getMapperClassName()))
                )
                .addCode(
                    CodeBlock.of("return JsonObject.success($L.create$L(creator));",
                        serviceFieldName, typeElement.getSimpleName().toString())
                )
                .addJavadoc("createRequest")
                .returns(ParameterizedTypeName.get(ClassName.get(JsonObject.class),
                    ClassName.get(Long.class))).build());
        }
        return Optional.empty();
    }

    /**
     * 更新方法
     *
     * @param serviceFieldName
     * @param typeElement
     * @param nameContext
     * @return
     */
    private Optional<MethodSpec> updateMethod(String serviceFieldName, TypeElement typeElement,
        DefaultNameContext nameContext) {
        boolean containsNull = StringUtils.containsNull(nameContext.getUpdatePackageName(),
            nameContext.getUpdaterPackageName(), nameContext.getMapperPackageName());
        if (!containsNull) {
            return Optional.of(MethodSpec.methodBuilder("update" + typeElement.getSimpleName())
                .addParameter(ParameterSpec.builder(
                        ClassName.get(nameContext.getUpdatePackageName(),
                            nameContext.getUpdateClassName()), "request")
                    .addAnnotation(RequestBody.class).build())
                .addAnnotation(AnnotationSpec.builder(PostMapping.class)
                    .addMember("value", "$S", "update" + typeElement.getSimpleName()).build())
                .addModifiers(Modifier.PUBLIC)
                .addCode(
                    CodeBlock.of("$T updater = $T.INSTANCE.request2Updater(request);\n",
                        ClassName.get(nameContext.getUpdaterPackageName(),
                            nameContext.getUpdaterClassName()),
                        ClassName.get(nameContext.getMapperPackageName(),
                            nameContext.getMapperClassName()))
                )
                .addCode(
                    CodeBlock.of("$L.update$L(updater);\n", serviceFieldName,
                        typeElement.getSimpleName().toString())
                )
                .addCode(
                    CodeBlock.of("return $T.success($T.Success.getName());", JsonObject.class,
                        CodeEnum.class)
                )
                .returns(ParameterizedTypeName.get(ClassName.get(JsonObject.class),
                    ClassName.get(String.class)))
                .addJavadoc("update request")
                .build());
        }
        return Optional.empty();
    }

    /**
     * 启用
     *
     * @param serviceFieldName
     * @param typeElement
     * @return
     */
    private Optional<MethodSpec> validMethod(String serviceFieldName, TypeElement typeElement) {
        return Optional.of(MethodSpec.methodBuilder("valid" + typeElement.getSimpleName())
            .addParameter(
                ParameterSpec.builder(Long.class, "id").addAnnotation(PathVariable.class).build())
            .addAnnotation(
                AnnotationSpec.builder(PostMapping.class).addMember("value", "$S", "valid/{id}")
                    .build())
            .addModifiers(Modifier.PUBLIC)
            .addCode(
                CodeBlock.of("$L.valid$L(id);\n",
                    serviceFieldName, typeElement.getSimpleName().toString())
            )
            .addCode(
                CodeBlock.of("return $T.success($T.Success.getName());", JsonObject.class,
                    CodeEnum.class)
            )
            .returns(ParameterizedTypeName.get(ClassName.get(JsonObject.class),
                ClassName.get(String.class)))
            .addJavadoc("valid")
            .build());
    }

    /**
     * 修复不返回方法的问题
     *
     * @param serviceFieldName
     * @param typeElement
     * @return
     */
    private Optional<MethodSpec> inValidMethod(String serviceFieldName, TypeElement typeElement) {
        return Optional.of(MethodSpec.methodBuilder("invalid" + typeElement.getSimpleName())
            .addParameter(
                ParameterSpec.builder(Long.class, "id").addAnnotation(PathVariable.class).build())
            .addAnnotation(
                AnnotationSpec.builder(PostMapping.class).addMember("value", "$S", "invalid/{id}")
                    .build())
            .addModifiers(Modifier.PUBLIC)
            .addCode(
                CodeBlock.of("$L.invalid$L(id);\n",
                    serviceFieldName, typeElement.getSimpleName().toString())
            )
            .addCode(
                CodeBlock.of("return $T.success($T.Success.getName());", JsonObject.class,
                    CodeEnum.class)
            )
            .returns(ParameterizedTypeName.get(ClassName.get(JsonObject.class),
                ClassName.get(String.class)))
            .addJavadoc("invalid")
            .build());
    }

    private Optional<MethodSpec> findById(String serviceFieldName, DefaultNameContext nameContext) {
        boolean containsNull = StringUtils.containsNull(nameContext.getVoPackageName(),
            nameContext.getResponsePackageName(), nameContext.getMapperPackageName());
        if (!containsNull) {
            return Optional.of(MethodSpec.methodBuilder("findById")
                .addParameter(
                    ParameterSpec.builder(Long.class, "id").addAnnotation(PathVariable.class)
                        .build())
                .addAnnotation(AnnotationSpec.builder(GetMapping.class)
                    .addMember("value", "$S", "findById/{id}").build())
                .addModifiers(Modifier.PUBLIC)
                .addCode(
                    CodeBlock.of("$T vo = $L.findById(id);\n",
                        ClassName.get(nameContext.getVoPackageName(), nameContext.getVoClassName()),
                        serviceFieldName)
                )
                .addCode(
                    CodeBlock.of("$T response = $T.INSTANCE.vo2CustomResponse(vo);\n"
                        , ClassName.get(nameContext.getResponsePackageName(),
                            nameContext.getResponseClassName()),
                        ClassName.get(nameContext.getMapperPackageName(),
                            nameContext.getMapperClassName()))
                )
                .addCode(
                    CodeBlock.of("return $T.success(response);", JsonObject.class)
                )
                .addJavadoc("findById")
                .returns(ParameterizedTypeName.get(ClassName.get(JsonObject.class),
                    ClassName.get(nameContext.getResponsePackageName(),
                        nameContext.getResponseClassName())))
                .build());
        }
        return Optional.empty();
    }

    /**
     * 分页
     *
     * @param serviceFieldName
     * @param nameContext
     * @return
     */
    private Optional<MethodSpec> findByPage(String serviceFieldName,
        DefaultNameContext nameContext) {
        boolean containsNull = StringUtils.containsNull(nameContext.getQueryRequestPackageName(),
            nameContext.getQueryPackageName(), nameContext.getMapperPackageName(),
            nameContext.getVoPackageName(), nameContext.getResponsePackageName());
        if (!containsNull) {
            return Optional.of(MethodSpec.methodBuilder("findByPage")
                .addParameter(ParameterSpec.builder(
                        ParameterizedTypeName.get(ClassName.get(PageRequestWrapper.class),
                            ClassName.get(nameContext.getQueryRequestPackageName(),
                                nameContext.getQueryRequestClassName())), "request")
                    .addAnnotation(RequestBody.class).build())
                .addAnnotation(
                    AnnotationSpec.builder(PostMapping.class).addMember("value", "$S", "findByPage")
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .addCode(
                    CodeBlock.of("$T<$T> wrapper = new $T<>();\n"
                        , PageRequestWrapper.class, ClassName.get(nameContext.getQueryPackageName(),
                            nameContext.getQueryClassName()), PageRequestWrapper.class)
                )
                .addCode(
                    CodeBlock.of("wrapper.setBean($T.INSTANCE.request2Query(request.getBean()));\n",
                        ClassName.get(nameContext.getMapperPackageName(),
                            nameContext.getMapperClassName()))
                )
                .addCode(
                    CodeBlock.of("""
                        wrapper.setSorts(request.getSorts());
                            wrapper.setPageSize(request.getPageSize());
                            wrapper.setPage(request.getPage());
                        """)
                )
                .addCode(CodeBlock.of("$T<$T> page = $L.findByPage(wrapper);\n"
                    , Page.class,
                    ClassName.get(nameContext.getVoPackageName(), nameContext.getVoClassName()),
                    serviceFieldName))
                .addCode(
                    CodeBlock.of("""
                            return $T.success(
                                    $T.of(
                                        page.getContent().stream()
                                            .map(vo -> $T.INSTANCE.vo2CustomResponse(vo))
                                            .collect($T.toList()),
                                        page.getTotalElements(),
                                        page.getSize(),
                                        page.getNumber())
                                );""", JsonObject.class, PageResult.class,
                        ClassName.get(nameContext.getMapperPackageName(),
                            nameContext.getMapperClassName()), Collectors.class)
                )
                .addJavadoc("findByPage request")
                .returns(ParameterizedTypeName.get(ClassName.get(JsonObject.class),
                    ParameterizedTypeName.get(ClassName.get(
                        PageResult.class), ClassName.get(nameContext.getResponsePackageName(),
                        nameContext.getResponseClassName()))))
                .build());
        }
        return Optional.empty();
    }
}
