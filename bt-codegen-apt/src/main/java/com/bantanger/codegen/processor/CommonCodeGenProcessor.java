package com.bantanger.codegen.processor;

import com.bantanger.codegen.context.ProcessingEnvironmentHolder;
import com.bantanger.codegen.registry.CodeGenProcessorRegistry;
import com.bantanger.codegen.spi.CodeGenProcessor;
import com.google.auto.service.AutoService;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;


/**
 * @author chensongmin
 * @description 代码生成处理器
 * <p>
 * AbstractProcessor 是一个抽象类，是 Java 注解处理器的基类 通过实现它可以获取 class 对象的任何元素，如注解信息、成员、方法等..
 * </p>
 * @date 2025/1/7
 */
@AutoService(Processor.class)
public class CommonCodeGenProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(an -> {
            Set<? extends Element> typeElements = roundEnv.getElementsAnnotatedWith(an);
            Set<TypeElement> types = ElementFilter.typesIn(typeElements);
            for (TypeElement typeElement : types) {
                CodeGenProcessor codeGenProcessor = CodeGenProcessorRegistry.find(
                    an.getQualifiedName().toString());
                try {
                    codeGenProcessor.generate(typeElement, roundEnv);
                } catch (Exception e) {
                    ProcessingEnvironmentHolder.getEnvironment().getMessager()
                        .printMessage(Kind.ERROR, "代码生成异常:" + e.getMessage());
                }
            }

        });
        return false;
    }

    /**
     * 初始化方法
     * <p>JDK 会在运行时调用这个方法，并传入 ProcessingEnvironment 对象</p>
     *
     * @param processingEnv environment to access facilities the tool framework provides to the
     *                      processor
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        ProcessingEnvironmentHolder.setEnvironment(processingEnv);
        // 初始化所有的 SPI 处理器
        CodeGenProcessorRegistry.initProcessors();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return CodeGenProcessorRegistry.getSupportedAnnotations();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
