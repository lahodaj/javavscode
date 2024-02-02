/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.openjdk.compute.disabled.modules;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Arg;
import org.netbeans.spi.sendopts.ArgsProcessor;
import org.netbeans.spi.sendopts.Description;
import org.netbeans.spi.sendopts.Env;
import org.openide.*;
import org.openide.modules.Dependency;
import org.openide.modules.ModuleInfo;
import org.openide.util.EditableProperties;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

public class ComputeRequiredModules implements ArgsProcessor {

    @Override
    public void process(Env env) throws CommandException {
        if (targetProperties != null) {
            try {
                computeDependencies();
            } catch (IOException ex) {
                throw (CommandException) new CommandException(1).initCause(ex);
            }
        }
    }

    @Arg(longName="compute-disabled-modules")
    @Description(shortDescription="#DESC_ComputeDisabledModules")
    @NbBundle.Messages("DESC_ComputeDisabledModules=Compue and set disabled modules")
    public String targetProperties;

    private void computeDependencies() throws IOException {
        String[] rootModules = {
            //the basic server:
            "org.netbeans.modules.java.lsp.server", //the Java LSP Server
            //support for OSGi modules in nbcode:
            "org.netbeans.modules.netbinox", //OSGi modules support
            //dependencies of nbcode.integration:
            "org.netbeans.modules.project.dependency", //used by nbcode.integration
            "org.netbeans.modules.updatecenters", //used by nbcode.integration
            "org.netbeans.swing.laf.flatlaf", //used by nbcode.integration
            "org.netbeans.core.execution", //used by nbcode.integration, to fulfil org.openide.execution.ExecutionEngine.defaultLookup
            //additional dependencies needed to make things work:
            "org.netbeans.modules.autoupdate.cli", //to get --modules option (used by nbcode.ts
            "org.netbeans.modules.editor", //DocumentFactory
            "org.netbeans.modules.editor.mimelookup.impl", //so that MimeLookup from layers works
            "org.netbeans.modules.lexer.nbbridge", //so that lexer(s) work
            "org.netbeans.modules.java.j2seplatform", //so that JRT FS works
            "org.netbeans.modules.masterfs.nio2", //listening on FS
            "org.netbeans.modules.masterfs.ui", //filesystem
        };
        Set<String> rootModulesSet = new HashSet<>(Arrays.asList(rootModules));
        Set<ModuleInfo> todo = new HashSet<>();
        Map<String, ModuleInfo> codeNameBase2ModuleInfo = new HashMap<>();
        Map<String, Set<String>> capability2Modules = new HashMap<>();

        for (ModuleInfo mi : Lookup.getDefault().lookupAll(ModuleInfo.class)) {
            codeNameBase2ModuleInfo.put(mi.getCodeNameBase(), mi);
            Arrays.asList(mi.getProvides()).forEach(p -> capability2Modules.computeIfAbsent(p, b -> new HashSet<>()).add(mi.getCodeNameBase()));
            if (rootModulesSet.contains(mi.getCodeNameBase())) {
                rootModulesSet.remove(mi.getCodeNameBase());
                todo.add(mi);
            }
        }
        
        if (!rootModulesSet.isEmpty()) {
            throw new IllegalStateException("not found: " + rootModulesSet);
        }

        Set<ModuleInfo> allDependencies = new HashSet<>();
        Set<String> seenNeeds = new HashSet<>();
        Set<String> seenRequires = new HashSet<>();
        Set<String> seenRecommends = new HashSet<>();
        
        while (!todo.isEmpty()) {
            ModuleInfo currentModule = todo.iterator().next();

            todo.remove(currentModule);

            if (allDependencies.add(currentModule)) {
                for (Dependency d : currentModule.getDependencies()) {
                    switch (d.getType()) {
                        case Dependency.TYPE_MODULE:
                            String depName = d.getName();
                            int slash = depName.indexOf("/");

                            if (slash != (-1)) {
                                depName = depName.substring(0, slash);
                            }

                            ModuleInfo dependency = codeNameBase2ModuleInfo.get(depName);
                            if (dependency == null) {
                                System.err.println("cannot find module: " + depName);
                            } else {
                                todo.add(dependency);
                            }
                            break;
                        case Dependency.TYPE_NEEDS:
                            if (seenNeeds.add(d.getName())) {
                                Set<String> fullfillingModules = capability2Modules.get(d.getName());
                                if (fullfillingModules == null) {
                                    System.err.println("module: " + currentModule.getCodeNameBase() + ", needs capability: '" + d.getName() + "', but there are no modules providing this capability");
                                } else if (fullfillingModules.size() == 1) {
                                    todo.add(codeNameBase2ModuleInfo.get(fullfillingModules.iterator().next()));
                                } else {
                                    System.err.println("module: " + currentModule.getCodeNameBase() + ", needs capability: '" + d.getName() + "', modules that provide that capability are: " + fullfillingModules);
                                }
                            }
                            break;
                        case Dependency.TYPE_REQUIRES:
                            if (seenRequires.add(d.getName())) {
                                Set<String> fullfillingModules = capability2Modules.get(d.getName());
                                if (fullfillingModules == null) {
                                    System.err.println("module: " + currentModule.getCodeNameBase() + ", needs capability: '" + d.getName() + "', but there are no modules providing this capability");
                                } else if (fullfillingModules.size() == 1) {
                                    todo.add(codeNameBase2ModuleInfo.get(fullfillingModules.iterator().next()));
                                } else {
                                    System.err.println("module: " + currentModule.getCodeNameBase() + ", requires capability: '" + d.getName() + "', modules that provide that capability are: " + fullfillingModules);
                                }
                            }
                            break;
                        case Dependency.TYPE_RECOMMENDS:
                            if (seenRecommends.add(d.getName())) {
                                Set<String> fullfillingModules = capability2Modules.get(d.getName());
                                System.err.println("module: " + currentModule.getCodeNameBase() + ", recommends capability: '" + d.getName() + "', modules that provide that capability are: " + fullfillingModules);
                            }
                            break;
                        case Dependency.TYPE_JAVA:
                            break;
                        default:
                            System.err.println("unhandled dependency: " + d);
                    }
                }
            }
        }

        Set<String> requiredCNBBases = allDependencies.stream().map(mi -> mi.getCodeNameBase()).collect(Collectors.toSet());
        String disabledModules = codeNameBase2ModuleInfo.keySet().stream().filter(cnbb -> !requiredCNBBases.contains(cnbb)).collect(Collectors.joining(","));
        EditableProperties props = new EditableProperties(false);

        if (Files.isReadable(Paths.get(targetProperties))) {
            try (InputStream in = new FileInputStream(targetProperties)) {
                props.load(in);
            }
        }

        props.put("disabled.modules", disabledModules);

        try (OutputStream out = new FileOutputStream(targetProperties)) {
            props.store(out);
        }

        LifecycleManager.getDefault().exit();
    }

}
