/*
 * Renjin : JVM-based interpreter for the R language for the statistical analysis
 * Copyright © 2010-2018 BeDataDriven Groep B.V. and contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.gnu.org/licenses/gpl-2.0.txt
 */
package org.renjin.invoke.codegen;

import com.sun.codemodel.*;
import org.renjin.eval.Context;
import org.renjin.eval.EvalException;
import org.renjin.eval.Session;
import org.renjin.invoke.annotations.Materialize;
import org.renjin.invoke.annotations.SessionScoped;
import org.renjin.invoke.codegen.args.ArgConverterStrategies;
import org.renjin.invoke.codegen.args.ArgConverterStrategy;
import org.renjin.invoke.codegen.scalars.ScalarType;
import org.renjin.invoke.codegen.scalars.ScalarTypes;
import org.renjin.invoke.model.JvmMethod;
import org.renjin.invoke.model.PrimitiveModel;
import org.renjin.repackaged.guava.collect.Lists;
import org.renjin.repackaged.guava.collect.Maps;
import org.renjin.sexp.Environment;
import org.renjin.sexp.LogicalVector;
import org.renjin.sexp.SEXP;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.sun.codemodel.JExpr._new;
import static com.sun.codemodel.JExpr.lit;

public class OverloadWrapperBuilder implements ApplyMethodContext {

  protected JCodeModel codeModel;
  protected JDefinedClass invoker;
  private PrimitiveModel primitive;
  private int arity;

  private List<JVar> arguments = Lists.newArrayList();
  private JVar context;
  private JVar environment;

  public OverloadWrapperBuilder(JCodeModel codeModel, JDefinedClass invoker, PrimitiveModel primitive, int arity) {
    this.codeModel = codeModel;
    this.invoker = invoker;
    this.primitive = primitive;
    this.arity = arity;
  }

  public void build() {
    JMethod method = invoker.method(JMod.STATIC | JMod.PUBLIC, codeModel.ref(SEXP.class), "doApply")
        ._throws(Exception.class);

    context = method.param(Context.class, "context");
    environment = method.param(Environment.class, "environment");
    for(int i=0;i!=arity;++i) {
      JVar argument = method.param(SEXP.class, "arg" + i);
      arguments.add(argument);
    }

    /**
     * Tests the arguments given against those of each Java overload
     */
    IfElseBuilder matchSequence = new IfElseBuilder(method.body());
    List<JvmMethod> overloads = Lists.newArrayList( primitive.overloadsWithPosArgCountOf(arity) );

    if(primitive.isRelationalOperator()) {
      JVar arg0 = arguments.get(0);
      JVar arg1 = arguments.get(1);
      Collections.sort( overloads, new OverloadComparator());
      Collections.reverse(overloads);

      method.body()._if(codeModel.ref(WrapperRuntime.class).staticInvoke("isEmptyOrNull").arg(arg0).cor(codeModel.ref(WrapperRuntime.class).staticInvoke("isEmptyOrNull").arg(arg1)))._then()._return(codeModel.ref(LogicalVector.class).staticRef("EMPTY"));

//      This code will be generated to handle FunctionCall and Symbol coercion to character
//      arg0 = maybeConvertToStringVector(arg0);
//      arg1 = WrapperRuntime.maybeConvertToStringVector(arg1);
      method.body().assign(JExpr.ref("arg0"), codeModel.ref(WrapperRuntime.class).staticInvoke("maybeConvertToStringVector").arg(context).arg(arg0));
      method.body().assign(JExpr.ref("arg1"), codeModel.ref(WrapperRuntime.class).staticInvoke("maybeConvertToStringVector").arg(context).arg(arg1));

      for(JvmMethod overload : overloads) {
        ScalarType scalarType = ScalarTypes.get(overload.getFormals().get(0).getClazz());
        JClass vectorType = codeModel.ref(scalarType.getVectorType());
        JBlock stringBlock = matchSequence
            ._if(arg0._instanceof(vectorType)
                .cor(arg1._instanceof(vectorType)));
        invokeOverload(overload, stringBlock);
      }

    } else {
    /*
     * Sort the overloads so that we test more narrow types first, e.g.,
     * try "int" before falling back to "double".
     */
      Collections.sort( overloads, new OverloadComparator());
      for(JvmMethod overload : overloads) {
      /*
       * If the types match, invoke the Java method
       */
        invokeOverload(overload, matchSequence._if(argumentsMatch(overload)));
      }
    }

    /**
     * No matching methods, throw an exception
     */
    matchSequence._else()._throw(_new(codeModel.ref(EvalException.class))
        .arg(typeMismatchErrorMessage(arguments)));
  }

  private JExpression typeMismatchErrorMessage(List<JVar> arguments) {
    JInvocation format = codeModel.ref(String.class).staticInvoke("format");
    format.arg(lit(typeMessageErrorFormat(arguments.size())));
    for(JVar arg : arguments) {
      format.arg(arg.invoke("getTypeName"));
    }
    return format;
  }

  private String typeMessageErrorFormat(int nargs) {

    String escapedFunctionName = primitive.getName().replaceAll("%", "%%");

    StringBuilder message = new StringBuilder();
    message.append("Invalid argument:\n");
    message.append("\t").append(escapedFunctionName).append("(");

    for(int i=0;i<nargs;++i) {
      if(i > 0) {
        message.append(", ");
      }
      message.append("%s");
    }
    message.append(")\n");
    message.append("\tExpected:");
    for(JvmMethod method : primitive.getOverloads()) {
      message.append("\n\t");
      method.appendFriendlySignatureTo(escapedFunctionName, message);
    }
    return message.toString();
  }

  private Map<JvmMethod.Argument, JExpression> mapArguments(JvmMethod overload) {
    Map<JvmMethod.Argument, JExpression> argumentMap = Maps.newHashMap();

    int argumentPos = 0;
    for(JvmMethod.Argument argument : overload.getAllArguments()) {
      if(argument.isContextual()) {
        if(argument.getClazz().equals(Context.class)) {
          argumentMap.put(argument, context);
        } else if(argument.getClazz().equals(Environment.class)){
          argumentMap.put(argument, environment);
        } else if(argument.getClazz().equals(Session.class)) {
          argumentMap.put(argument, context.invoke("getSession"));
        } else if(argument.getClazz().getAnnotation(SessionScoped.class) != null) {
          argumentMap.put(argument, context.invoke("getSingleton").arg(JExpr.dotclass(codeModel.ref(argument.getClazz()))));
        } else {
          throw new UnsupportedOperationException(argument.getClazz().getName());
        }
      } else {
        argumentMap.put(argument, convert(argument, materialize(overload, argument, arguments.get(argumentPos++))));
      }
    }
    return argumentMap;
  }

  private JExpression materialize(JvmMethod overload, JvmMethod.Argument formal, JVar argumentVar) {
    // this is a little tricky.
    // We need to decide when to materialize a deferred tasks. We only need to do this
    // when the method is actually going to access the content of the vector rather than just attributes
    // or length, etc.
    if(overload.isAnnotatedWith(Materialize.class)) {
      return context.invoke("materialize").arg(argumentVar);
    } else {
      return argumentVar;
    }
  }

  private void invokeOverload(JvmMethod overload, JBlock block) {

    if(overload.isDataParallel()) {
      new RecycleLoopBuilder(codeModel, block, context, primitive, overload, mapArguments(overload))
          .build();
    } else {
      invokeSimpleMethod(overload, block);
    }
  }

  /**
   * Invokes with the JVM method simply (without recycling) using the
   * provided arguments.
   */
  private void invokeSimpleMethod(JvmMethod overload, JBlock block) {
    JInvocation invocation = codeModel.ref(overload.getDeclaringClass())
        .staticInvoke(overload.getName());

    Map<JvmMethod.Argument, JExpression> argumentMap = mapArguments(overload);

    for(JvmMethod.Argument argument : overload.getAllArguments()) {
      invocation.arg(argumentMap.get(argument));
    }
    CodeModelUtils.returnSexp(context, codeModel, block, overload, invocation);
  }

  /**
   *
   * @param argument
   * @param sexp
   * @return
   */
  private JExpression convert(JvmMethod.Argument argument, JExpression sexp) {
    return ArgConverterStrategies.findArgConverterStrategy(argument)
        .convertArgument(this, sexp);
  }

  /**
   * Compute the expression that will test whether the provided arguments
   * match the given overload.
   */
  private JExpression argumentsMatch(JvmMethod overload) {
    JExpression condition = JExpr.TRUE;
    List<JvmMethod.Argument> posFormals = overload.getPositionalFormals();
    for (int i = 0; i != posFormals.size(); ++i) {

      ArgConverterStrategy strategy = ArgConverterStrategies
          .findArgConverterStrategy(posFormals.get(i));

      JExpression argCondition = strategy.getTestExpr(codeModel, arguments.get(i));
      if(condition == null) {
        condition = argCondition;
      } else {
        condition = condition.cand(argCondition);
      }
    }
    return condition;
  }

  @Override
  public JExpression getContext() {
    return context;
  }

  @Override
  public JExpression getEnvironment() {
    return environment;
  }

  @Override
  public JClass classRef(Class<?> clazz) {
    return codeModel.ref(clazz);
  }

  @Override
  public JCodeModel getCodeModel() {
    return codeModel;
  }

}
