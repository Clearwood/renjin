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
package org.renjin.invoke.reflection.converters;

import org.renjin.sexp.AtomicVector;

public class BoxedFloatArrayConverter extends DoubleArrayConverter {

  public static final BoxedFloatArrayConverter BOXED_FLOAT_ARRAY = new BoxedFloatArrayConverter();

  private BoxedFloatArrayConverter() {
    super(Float.class);
  }


  @Override
  protected Object convertToJavaArray(AtomicVector vector) {
    Float[] array = new Float[vector.length()];
    for (int i = 0; i < vector.length(); i++) {
      array[i] = (float)vector.getElementAsDouble(i);
    }
    return array;
  }
}
