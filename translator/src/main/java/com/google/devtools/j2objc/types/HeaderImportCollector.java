/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.j2objc.types;

import com.google.common.collect.Sets;
import com.google.devtools.j2objc.util.ASTUtil;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.Set;

/**
 * Collects the set of imports needed to resolve type references in a header.
 *
 * @author Tom Ball
 */
public class HeaderImportCollector extends ErrorReportingASTVisitor {

  private Set<Import> forwardDecls = Sets.newLinkedHashSet();
  private Set<Import> superTypes = Sets.newLinkedHashSet();
  private Set<Import> declaredTypes = Sets.newHashSet();

  public void collect(CompilationUnit unit, String sourceName) {
    run(unit);
    for (Import imp : superTypes) {
      if (forwardDecls.contains(imp)) {
        forwardDecls.remove(imp);
      }
    }
  }

  public Set<Import> getForwardDeclarations() {
    return forwardDecls;
  }

  public Set<Import> getSuperTypes() {
    return superTypes;
  }

  private void addForwardDecl(Type type) {
    if (type != null) {
      addForwardDecl(Types.getTypeBinding(type));
    }
  }

  private void addForwardDecl(ITypeBinding type) {
    forwardDecls.addAll(Sets.difference(Import.getImports(type), declaredTypes));
  }

  private void addSuperType(Type type) {
    if (type != null) {
      addSuperType(Types.getTypeBinding(type));
    }
  }

  private void addSuperType(ITypeBinding type) {
    superTypes.addAll(Sets.difference(Import.getImports(type), declaredTypes));
  }

  private void addDeclaredType(ITypeBinding type) {
    Import.addImports(type, declaredTypes);
  }

  @Override
  public boolean visit(FieldDeclaration node) {
    addForwardDecl(node.getType());
    return true;
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    addForwardDecl(node.getReturnType2());
    for (SingleVariableDeclaration param : ASTUtil.getParameters(node)) {
      addForwardDecl(param.getType());
    }
    return true;
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    ITypeBinding binding = Types.getTypeBinding(node);
    addDeclaredType(binding);
    if (binding.isEqualTo(Types.getNSObject())) {
      return false;
    }
    addSuperType(node.getSuperclassType());
    for (Type interfaze : ASTUtil.getSuperInterfaceTypes(node)) {
      addSuperType(interfaze);
    }
    return true;
  }

  @Override
  public boolean visit(EnumDeclaration node) {
    superTypes.add(new Import("JavaLangEnum", "java.lang.Enum", false));
    ITypeBinding binding = Types.getTypeBinding(node);
    addDeclaredType(binding);
    for (ITypeBinding interfaze : binding.getInterfaces()) {
      addSuperType(interfaze);
    }
    return true;
  }
}
