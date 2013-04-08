/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License version 2.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package com.redhat.ceylon.compiler.java.codegen;

import com.redhat.ceylon.compiler.typechecker.model.Class;
import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.Functional;
import com.redhat.ceylon.compiler.typechecker.model.FunctionalParameter;
import com.redhat.ceylon.compiler.typechecker.model.Method;
import com.redhat.ceylon.compiler.typechecker.model.MethodOrValue;
import com.redhat.ceylon.compiler.typechecker.model.Parameter;
import com.redhat.ceylon.compiler.typechecker.model.ParameterList;
import com.redhat.ceylon.compiler.typechecker.model.ProducedType;
import com.redhat.ceylon.compiler.typechecker.model.Setter;
import com.redhat.ceylon.compiler.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.TypeParameter;
import com.redhat.ceylon.compiler.typechecker.model.TypedDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.Value;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnyAttribute;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnyMethod;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AttributeArgument;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AttributeDeclaration;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AttributeSetterDefinition;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ForComprehensionClause;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ForIterator;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.FunctionArgument;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.KeyValueIterator;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.SpecifierStatement;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ValueIterator;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Variable;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;

public abstract class BoxingDeclarationVisitor extends Visitor {

    protected abstract boolean isCeylonBasicType(ProducedType type);
    protected abstract boolean isNull(ProducedType type);
    protected abstract boolean isObject(ProducedType type);
    protected abstract boolean isCallable(ProducedType type);
    protected abstract boolean hasErasure(ProducedType type);
    protected abstract boolean willEraseToObject(ProducedType type);
    protected abstract boolean isRaw(ProducedType type);

    @Override
    public void visit(FunctionArgument that) {
        super.visit(that);
        boxMethod(that.getDeclarationModel());
    }
    
    @Override
    public void visit(AnyMethod that) {
        super.visit(that);
        visitMethod(that.getDeclarationModel());
    }

    private void visitMethod(Method method) {
        boxMethod(method);
        rawTypedDeclaration(method);
        setErasureState(method);
    }
    
    private void setErasureState(TypedDeclaration decl) {
        // deal with invalid input
        if(decl == null)
            return;

        ProducedType type = decl.getType();
        if(type != null && hasErasure(type)){
            decl.setTypeErased(true);
        }
    }

    private void rawTypedDeclaration(TypedDeclaration decl) {
        // deal with invalid input
        if(decl == null)
            return;

        ProducedType type = decl.getType();
        if(type != null){
            if(isRaw(type))
                type.setRaw(true);
        }
    }

    private void boxMethod(Method method) {
        // deal with invalid input
        if(method == null)
            return;
        Declaration refined = CodegenUtil.getTopmostRefinedDeclaration(method);
        // deal with invalid input
        if(refined == null
                || (!(refined instanceof Method)))
            return;
        Method refinedMethod = (Method)refined;
        if (method.getName() != null) {
            // A Callable, which never have primitive parameters
            setBoxingState(method, refinedMethod);
        } else {
            // Anonymous methods are always boxed
            method.setUnboxed(false);
        }
    }

    private void setBoxingState(TypedDeclaration declaration, TypedDeclaration refinedDeclaration) {
        ProducedType type = declaration.getType();
        if(type == null){
            // an error must have already been reported
            return;
        }
        // fetch the real refined declaration if required
        if(declaration == refinedDeclaration
                && declaration instanceof Parameter
                && declaration.getContainer() instanceof Class){
            // maybe it is really inherited from a field?
            MethodOrValue methodOrValueForParam = CodegenUtil.findMethodOrValueForParam((Parameter) declaration);
            if(methodOrValueForParam != null){
                // make sure we get the refined version of that member
                refinedDeclaration = (TypedDeclaration) methodOrValueForParam.getRefinedDeclaration();
            }
        }
        
        // inherit underlying type constraints
        if(refinedDeclaration != declaration && type.getUnderlyingType() == null
                && refinedDeclaration.getType() != null)
            type.setUnderlyingType(refinedDeclaration.getType().getUnderlyingType());
        
        // abort if our boxing state has already been set
        if(declaration.getUnboxed() != null)
            return;
        
        // functional parameter return values are always boxed
        if(declaration instanceof FunctionalParameter){
            declaration.setUnboxed(false);
            return;
        }
        
        if(refinedDeclaration != declaration){
            // make sure refined declarations have already been set
            if(refinedDeclaration.getUnboxed() == null)
                setBoxingState(refinedDeclaration, refinedDeclaration);
            // inherit
            declaration.setUnboxed(refinedDeclaration.getUnboxed());
        } else if (declaration instanceof Method
                && CodegenUtil.isVoid(declaration.getType())
                && Strategy.useBoxedVoid((Method)declaration)
                && !(refinedDeclaration.getTypeDeclaration() instanceof TypeParameter)
                && !(refinedDeclaration.getContainer() instanceof FunctionalParameter)
                && !(refinedDeclaration instanceof Functional && Decl.isMpl((Functional)refinedDeclaration))){
            declaration.setUnboxed(false);
        } else if((isCeylonBasicType(type) || Decl.isUnboxedVoid(declaration))
           && !(refinedDeclaration.getTypeDeclaration() instanceof TypeParameter)
           && !(refinedDeclaration.getContainer() instanceof FunctionalParameter)
           && !(refinedDeclaration instanceof Functional && Decl.isMpl((Functional)refinedDeclaration))){
            declaration.setUnboxed(true);
        } else {
            declaration.setUnboxed(false);
        }
    }

    private void boxAttribute(TypedDeclaration declaration) {
        // deal with invalid input
        if(declaration == null)
            return;
        TypedDeclaration refinedDeclaration = null;
        refinedDeclaration = (TypedDeclaration)CodegenUtil.getTopmostRefinedDeclaration(declaration);
        // deal with invalid input
        if(refinedDeclaration == null)
            return;
        setBoxingState(declaration, refinedDeclaration);
    }
    
    @Override
    public void visit(Tree.Parameter that) {
        super.visit(that);
        TypedDeclaration declaration = that.getDeclarationModel();
        visitAttributeOrParameter(declaration);
    }
    
    @Override
    public void visit(AnyAttribute that) {
        super.visit(that);
        TypedDeclaration declaration = that.getDeclarationModel();
        visitAttributeOrParameter(declaration);
    }
    
    private void visitAttributeOrParameter(TypedDeclaration declaration) {
        boxAttribute(declaration);
        rawTypedDeclaration(declaration);
        setErasureState(declaration);
        if (declaration.getContainer() instanceof TypeDeclaration 
                && CodegenUtil.isSmall(declaration)
                && declaration.getType() != null) {
            declaration.getType().setUnderlyingType("int");
        }
    }
    
    @Override
    public void visit(AttributeDeclaration that) {
        if(that.getSpecifierOrInitializerExpression() != null
                && that.getDeclarationModel() != null
                && that.getType() instanceof Tree.ValueModifier
                && that.getDeclarationModel().getType() == that.getSpecifierOrInitializerExpression().getExpression().getTypeModel()){
            that.getDeclarationModel().setType(that.getDeclarationModel().getType().withoutUnderlyingType());
        }
        super.visit(that);
    }

    @Override
    public void visit(AttributeArgument that) {
        super.visit(that);
        boxAttribute(that.getDeclarationModel());
    }

    @Override
    public void visit(AttributeSetterDefinition that) {
        super.visit(that);
        Setter declarationModel = that.getDeclarationModel();
        // deal with invalid input
        if(declarationModel == null)
            return;
        TypedDeclaration declaration = declarationModel.getParameter();
        boxAttribute(declaration);
    }

    @Override
    public void visit(Variable that) {
        super.visit(that);
        TypedDeclaration declaration = that.getDeclarationModel();
        // deal with invalid input
        if(declaration == null)
            return;
        setBoxingState(declaration, declaration);
    }

    @Override
    public void visit(SpecifierStatement that) {
        super.visit(that);
        TypedDeclaration declaration = that.getDeclaration();
        if(declaration == null)
            return;
        if(declaration instanceof Method)
            visitMethod((Method) declaration);
        else if(declaration instanceof Value)
            visitAttributeOrParameter(declaration);
    }

    @Override
    public void visit(ForComprehensionClause that) {
        super.visit(that);
        // sort of a hack, because normal visiting rules would declare iterator variables to be potentially
        // unboxed, but the implementation always boxes them for now, so override it after we visit the comprehension
        ForIterator iter = that.getForIterator();
        if (iter instanceof ValueIterator) {
            ((ValueIterator) iter).getVariable().getDeclarationModel().setUnboxed(false);
        } else if (iter instanceof KeyValueIterator) {
            ((KeyValueIterator) iter).getKeyVariable().getDeclarationModel().setUnboxed(false);
            ((KeyValueIterator) iter).getValueVariable().getDeclarationModel().setUnboxed(false);
        }
    }
}
