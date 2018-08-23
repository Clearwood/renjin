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
package org.renjin.utils;

import org.renjin.sexp.IntVector;
import org.renjin.sexp.LogicalVector;

import java.io.PrintWriter;


public class LogicalPrinter implements ColumnPrinter {
  private PrintWriter writer;
  private LogicalVector vector;
  private String naSymbol;

  public LogicalPrinter(PrintWriter writer, LogicalVector vector, String naSymbol) {
    this.writer = writer;
    this.vector = vector;
    this.naSymbol = naSymbol;
  }

  @Override
  public void print(int index) {
    int value = vector.getElementAsRawLogical(index);
    if(IntVector.isNA(value)) {
      writer.write(naSymbol);
    } else if(value == 0) {
      writer.write("FALSE");
    } else {
      writer.write("TRUE");
    }
  }
}
