/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2022-2022. All rights reserved.
 */

package com.huawei.fusioncheck.dataflow.sootutil;

import lombok.extern.log4j.Log4j2;
import soot.G;
import soot.Hierarchy;
import soot.PhaseOptions;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.util.Chain;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 初始化soot
 *
 * @since 2022-04-29
 */
@Log4j2
public class SootContext {
    /**
     * cg图
     */
    private static CallGraph CALLGRAPH;

    /**
     * 层级关系
     */
    private static Hierarchy HIERARCHY;

    /**
     * 获取应用中的接口
     */
    private static final Set<String> sootInterfaces = new HashSet<>();

    /**
     * 初始化soot
     *
     * @param classDir 解压后单个jar包class所在目录
     */
    public static void initSoot(String classDir, Set<String> librarycp) {
        // 初始化设置
        G.reset();
        CALLGRAPH = null;
        HIERARCHY = null;
        Options.v().set_process_dir(Collections.singletonList(classDir));   // 设置待分析路径,处理所有在dir中发现的类
        Options.v().set_whole_program(true);    // 全程序分析
        Options.v().set_allow_phantom_refs(true);   // 允许虚类存在 不报错, 找不到对应的源代码就被称作是虚类（phantom class）
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_keep_line_number(true);
        Options.v().set_output_format(Options.output_format_jimple);
        // jdk中的rt.jar,jce.jar
        String dependencyJar = System.getProperty("java.home");
        String jdkPath = dependencyJar + File.separator + "lib" + File.separator;
        if (librarycp == null) {
            librarycp = new HashSet<>();
        }
        String jarpath = jdkPath + "rt.jar" + File.pathSeparator + jdkPath + "jce.jar";
        String librarypath = String.join(File.pathSeparator, librarycp);
        jarpath = classDir + File.pathSeparator + jarpath + File.pathSeparator + librarypath;
        Options.v().set_soot_classpath(jarpath);    // 将jdk中的rt.jar和jce.jar 待分析的class路径 三方jar添加到soot的classpath
        PhaseOptions.v().setPhaseOption("jb", "use-original-names:true");
        // 以下完成在Jimple建立的过程中使用源代码中变量名称的设置
        try {
            // 装在Scene环境
            try {
                Scene.v().loadNecessaryClasses();   // 加载soot依赖的类和命令行指定的类
            } catch (Error error) {
                // soot在处理某些方法的时候回导致解析失败，需要将jdk降为1.7，这里不处理这种情况，将问题写入日志
                log.debug("soot init failed, {}", error.getMessage());
                return;
            }
            setCHAPointsToAnalysis();
            setInterfaces();
            CALLGRAPH = Scene.v().getCallGraph();
            HIERARCHY = Scene.v().getActiveHierarchy();
        } catch (Exception exception) {
            log.debug("soot init failed, {}", exception.getMessage());
        }
        Chain<SootClass> chainMainClass = Scene.v().getApplicationClasses();
        log.info(chainMainClass.size());
    }

    /**
     * 设置CHA cg分析
     */
    private static void setCHAPointsToAnalysis() {
        Map<String, String> option = new HashMap<>();
        option.put("enabled", "true");
        option.put("apponly", "true");
        CHATransformer.v().transform("cg.cha", option);
    }

    /**
     * 获取cg图
     *
     * @return cg图
     */
    public static CallGraph getCG() {
        return CALLGRAPH;
    }

    /**
     * 获取层级关系图
     *
     * @return 层级关系图
     */
    public static Hierarchy getHierarchy() {
        return HIERARCHY;
    }

    /**
     * 获取jar包种的自有类
     *
     * @return sootclass列表
     */
    public static Chain<SootClass> getApplicationSootClass() {
        return Scene.v().getApplicationClasses();
    }

    /**
     * 获取该类下所有的方法
     *
     * @param sootClass soot类
     * @return 方法集合
     */
    public static List<SootMethod> getAllMethod(SootClass sootClass) {
        return sootClass.getMethods();
    }

    /**
     * 根据方法签名获取方法
     *
     * @param sootMethodSign 方法签名
     * @return sootmethod对象
     */
    public static SootMethod getSootMethod(String sootMethodSign) {
        return Scene.v().getMethod(sootMethodSign);
    }

    /**
     * 根据方法获取所有接口或抽象方法的直接实现方法
     *
     * @param sootMethod 当前抽象方法
     * @return 子类实体方法
     */
    public static List<SootMethod> getSubSootMethods(SootMethod sootMethod) {
        List<SootMethod> sootMethods = new ArrayList<>();
        SootClass sootClass = sootMethod.getDeclaringClass();
        if (sootMethod.getDeclaringClass().isInterface()) {
            List<SootClass> implementers = HIERARCHY.getImplementersOf(sootClass);
            addSubMethod(sootMethod, sootMethods, implementers);
        } else {
            List<SootClass> subclasses = HIERARCHY.getSubclassesOf(sootClass);
            addSubMethod(sootMethod, sootMethods, subclasses);
        }
        return sootMethods;
    }

    private static void addSubMethod(SootMethod sootMethod, List<SootMethod> sootMethods,
        List<SootClass> implementers) {
        for (SootClass implementer : implementers) {
            for (SootMethod implementerMethod : implementer.getMethods()) {
                if (implementerMethod.hasActiveBody()
                    && implementerMethod.getSubSignature().equals(sootMethod.getSubSignature())) {
                    sootMethods.add(implementerMethod);
                }
            }
        }
    }

    /**
     * 添加所有的接口签名
     */
    public static void setInterfaces() {
        Chain<SootClass> sootClasses = Scene.v().getApplicationClasses();
        for (SootClass sootClass : sootClasses) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.hasActiveBody()) {
                    sootInterfaces.add(sootMethod.getSignature());
                }
            }
        }
    }

    /**
     * 获取所有的接口签名
     *
     * @return 接口签名
     */
    public static Set<String> getInterfaces() {
        return sootInterfaces;
    }

    public static List<SootMethod> getAllApplicationMethodHasBody() {
        List<SootMethod> multMethod = new ArrayList<>();
        Chain<SootClass> sootClasses = getApplicationSootClass();
        for (SootClass sootClass : sootClasses) {
            List<SootMethod> sootMethods = sootClass.getMethods();
            for (SootMethod sootMethod : sootMethods) {
                if (sootMethod.hasActiveBody()) {
                    multMethod.add(sootMethod);
                }
            }
        }
        return multMethod;
    }

    public static List<SootMethod> getAllApplicationMethod() {
        List<SootMethod> multMethod = new ArrayList<>();
        Chain<SootClass> sootClasses = getApplicationSootClass();
        for (SootClass sootClass : sootClasses) {
            List<SootMethod> sootMethods = sootClass.getMethods();
            multMethod.addAll(sootMethods);
        }
        return multMethod;
    }
}
