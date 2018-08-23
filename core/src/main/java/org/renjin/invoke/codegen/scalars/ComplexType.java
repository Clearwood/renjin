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
package org.renjin.invoke.codegen.scalars;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JVar;
import org.apache.commons.math.complex.Complex;
import org.renjin.sexp.ComplexArrayVector;
import org.renjin.sexp.ComplexVector;


public class ComplexType extends ScalarType{

  
  @Override
  public Class<?> getScalarType() {
    return Complex.class;
  }

  @Override
  public String getConversionMethod() {
    return "convertToComplexPrimitive";
  }

  @Override
  public String getAccessorMethod() {
    return "getElementAsComplex";
  }

  @Override
  public Class getVectorType() {
    return ComplexVector.class;
  }

  @Override
  public Class<ComplexArrayVector.Builder> getBuilderClass() {
    return ComplexArrayVector.Builder.class;
  }

  @Override
  public Class getBuilderArrayElementClass() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class getArrayVectorClass() {
    throw new UnsupportedOperationException();
  }

  @Override
  public JExpression naLiteral(JCodeModel codeModel) {
    throw new UnsupportedOperationException();
  }

  @Override
  public JExpression testNaExpr(JCodeModel codeModel, JVar scalarVariable) {
    return codeModel.ref(ComplexVector.class).staticInvoke("isNaN").arg(scalarVariable);
  }

}
