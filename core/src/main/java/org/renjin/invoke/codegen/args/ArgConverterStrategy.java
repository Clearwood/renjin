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
package org.renjin.invoke.codegen.args;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JVar;
import org.renjin.invoke.codegen.ApplyMethodContext;
import org.renjin.invoke.model.JvmMethod;
import org.renjin.invoke.model.JvmMethod.Argument;


/**
 * Base class for the different strategies for converting incoming argument (SEXPs) to
 * the types declared in the java method.
 * 
 * @author alex
 *
 */
public abstract class ArgConverterStrategy {

  protected final JvmMethod.Argument formal;
  
  public ArgConverterStrategy(Argument formal) {
    super();
    this.formal = formal;
  }

  public abstract JExpression getTestExpr(JCodeModel codeModel, JVar sexpVariable);

  public abstract JExpression convertArgument(ApplyMethodContext method, JExpression sexp);

}
