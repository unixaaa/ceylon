package com.redhat.ceylon.compiler.js;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.redhat.ceylon.compiler.js.GenerateJsVisitor.SuperVisitor;
import com.redhat.ceylon.compiler.js.util.TypeUtils;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.model.typechecker.model.Class;
import com.redhat.ceylon.model.typechecker.model.ClassOrInterface;
import com.redhat.ceylon.model.typechecker.model.Declaration;
import com.redhat.ceylon.model.typechecker.model.Function;
import com.redhat.ceylon.model.typechecker.model.ModelUtil;
import com.redhat.ceylon.model.typechecker.model.Type;
import com.redhat.ceylon.model.typechecker.model.TypeParameter;

public class ClassGenerator {
    
    static void classDefinition(final Tree.ClassDefinition that, final GenerateJsVisitor gen) {
        //Don't even bother with nodes that have errors
        if (TypeGenerator.errVisitor.hasErrors(that))return;
        final Class d = that.getDeclarationModel();
        gen.out("/* " + d, " native=" + d.isNative(), " header=" + d.isNativeHeader(),
                " backend=" + d.getNativeBackend(), " ext=" + that.getExtendedType(), " */");
        //If it's inside a dynamic interface, don't generate anything
        if (d.isClassOrInterfaceMember() && ((ClassOrInterface)d.getContainer()).isDynamic())return;
        final Tree.ParameterList plist = that.getParameterList();
        final List<Tree.Constructor> constructors;
        final Tree.SatisfiedTypes sats = that.getSatisfiedTypes();
        //Find the constructors, if any
        Tree.Constructor defconstr = null;
        if (d.hasConstructors()) {
            constructors = new ArrayList<>(3);
            for (Tree.Statement st : that.getClassBody().getStatements()) {
                if (st instanceof Tree.Constructor) {
                    Tree.Constructor constr = (Tree.Constructor)st;
                    constructors.add(constr);
                    if (constr.getDeclarationModel().getName() == null) {
                        defconstr = constr;
                    }
                }
            }
        } else {
            constructors = null;
        }
        gen.comment(that);
        if (TypeUtils.isNativeExternal(d)) {
            boolean bye = false;
            if (d.hasConstructors() && defconstr == null) {
                gen.out(GenerateJsVisitor.function, gen.getNames().name(d));
                gen.out("(){");
                gen.generateThrow("Exception", d.getQualifiedNameString() + " has no default constructor.", that);
                gen.out(";}"); gen.endLine();
            }
            if (gen.stitchNative(d, that)) {
                if (d.isShared()) {
                    gen.share(d);
                }
                TypeGenerator.initializeType(that, gen);
                bye = true;
            }
            if (d.hasConstructors()) {
                for (Tree.Constructor cnstr : constructors) {
                    Constructors.classConstructor(cnstr, that, constructors, gen);
                }
            }
            if (bye)return;
        }
        gen.out(GenerateJsVisitor.function, gen.getNames().name(d));
        //If there's a default constructor, create a different function with this code
        if (d.hasConstructors() || d.hasEnumerated()) {
            if (defconstr == null) {
                gen.out("(){");
                gen.generateThrow("Exception", d.getQualifiedNameString() + " has no default constructor.", that);
                gen.out(";}"); gen.endLine();
                gen.out(GenerateJsVisitor.function, gen.getNames().name(d));
            }
            gen.out("$$c");
        }
        final boolean withTargs = TypeGenerator.generateParameters(that.getTypeParameterList(), plist, d, gen);
        gen.beginBlock();
        if (!d.hasConstructors()) {
            //This takes care of top-level attributes defined before the class definition
            gen.out("$init$", gen.getNames().name(d), "();");
            gen.endLine();
            gen.declareSelf(d);
            gen.referenceOuter(d);
        }
        final String me = gen.getNames().self(d);
        if (withTargs) {
            gen.out(gen.getClAlias(), "set_type_args(", me, ",$$targs$$);");
            gen.endLine();
        } else {
            //Check if any of the satisfied types have type arguments
            if (sats != null) {
                for(Tree.StaticType sat : sats.getTypes()) {
                    boolean first = true;
                    Map<TypeParameter,Type> targs = sat.getTypeModel().getTypeArguments();
                    if (targs != null && !targs.isEmpty()) {
                        if (first) {
                            gen.out(me, ".$$targs$$=");
                            TypeUtils.printTypeArguments(that, targs, gen, false, null);
                            gen.endLine(true);
                            first = false;
                        } else {
                            gen.out("/*TODO: more type arguments*/");
                        }
                    }
                }
            }
        }
        if (!d.isToplevel() && d.getContainer() instanceof Function
                && !((Function)d.getContainer()).getTypeParameters().isEmpty()) {
            gen.out(gen.getClAlias(), "set_type_args(", me, ",",
                    gen.getNames().typeArgsParamName((Function)d.getContainer()), ")");
            gen.endLine(true);
        }
        if (plist != null) {
            gen.initParameters(plist, d, null);
        }

        final List<Declaration> superDecs = new ArrayList<Declaration>(3);
        if (!gen.opts.isOptimize()) {
            that.getClassBody().visit(new SuperVisitor(superDecs));
        }
        final Tree.ExtendedType extendedType = that.getExtendedType();
        TypeGenerator.callSupertypes(sats == null ? null : TypeUtils.getTypes(sats.getTypes()),
                extendedType == null ? null : extendedType.getType(),
                d, that, superDecs, extendedType == null ? null : extendedType.getInvocationExpression(),
                extendedType == null ? null : ((Class) d.getExtendedType().getDeclaration()).getParameterList(), gen);

        if (!gen.opts.isOptimize() && plist != null) {
            //Fix #231 for lexical scope
            for (Tree.Parameter p : plist.getParameters()) {
                if (!p.getParameterModel().isHidden()){
                    gen.generateAttributeForParameter(that, d, p.getParameterModel());
                }
            }
        }
        if (!d.hasConstructors()) {
            if (TypeUtils.isNativeExternal(d)) {
                gen.stitchConstructorHelper(that, "_cons_before");
            }
            gen.visitStatements(that.getClassBody().getStatements());
            if (d.isNative()) {
                gen.stitchConstructorHelper(that, "_cons_after");
            }
            gen.out("return ", me, ";");
        }
        gen.endBlockNewLine();
        if (defconstr != null) {
            //Define a function as the class and call the default constructor in there
            String _this = "undefined";
            if (!d.isToplevel()) {
                final ClassOrInterface coi = ModelUtil.getContainingClassOrInterface(d.getContainer());
                if (coi != null) {
                    if (d.isClassOrInterfaceMember()) {
                        _this = "this";
                    } else {
                        _this = gen.getNames().self(coi);
                    }
                }
            }
            gen.out(GenerateJsVisitor.function, gen.getNames().name(d), "(){return ",
                    gen.getNames().name(d), "_", gen.getNames().name(defconstr.getDeclarationModel()), ".apply(",
                    _this, ",arguments);}");
            gen.endLine();
        }
        if (d.hasConstructors()) {
            for (Tree.Constructor cnstr : constructors) {
                Constructors.classConstructor(cnstr, that, constructors, gen);
            }
        }
        if (d.hasEnumerated()) {
            for (Tree.Statement st : that.getClassBody().getStatements()) {
                if (st instanceof Tree.Enumerated) {
                    Singletons.valueConstructor((Tree.Enumerated)st, gen);
                }
            }
        }
        //Add reference to metamodel
        gen.out(gen.getNames().name(d), ".$crtmm$=");
        TypeUtils.encodeForRuntime(d, that.getAnnotationList(), gen);
        gen.endLine(true);
        gen.share(d);
        TypeGenerator.initializeType(that, gen);
        if (d.isSerializable()) {
            SerializationHelper.addDeserializer(that, d, gen);
        }
    }

}