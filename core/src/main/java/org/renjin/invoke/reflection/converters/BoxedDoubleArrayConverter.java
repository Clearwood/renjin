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

public class BoxedDoubleArrayConverter extends DoubleArrayConverter {

  public static final BoxedDoubleArrayConverter BOXED_DOUBLE_ARRAY = new BoxedDoubleArrayConverter(Double.class);

  protected BoxedDoubleArrayConverter(Class clazz) {
    super(clazz);
  }

  @Override
  protected Object convertToJavaArray(AtomicVector vector) {
    Double[] array = new Double[vector.length()];
    for (int i = 0; i < vector.length(); i++) {
      array[i] = vector.getElementAsDouble(i);
    }
    return array;
  }
}
