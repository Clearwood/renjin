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
package org.renjin.primitives.vector;

import org.renjin.sexp.AttributeMap;
import org.renjin.sexp.IntVector;
import org.renjin.sexp.SEXP;
import org.renjin.sexp.Vector;

/**
 * Created by alex on 14-9-16.
 */
public class MemoizedIntVector extends IntVector implements MemoizedComputation {
  @Override
  public Vector forceResult() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setResult(Vector result) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isCalculated() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Vector[] getOperands() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getComputationName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int length() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getElementAsInt(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isConstantAccessTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected SEXP cloneWithNewAttributes(AttributeMap attributes) {
    throw new UnsupportedOperationException();
  }
}
